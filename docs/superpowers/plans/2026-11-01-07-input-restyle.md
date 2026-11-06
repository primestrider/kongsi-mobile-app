# Input & Choice Controls Restyle Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the stock Material 3 look of every text input and choice control with the filled, borderless, floating-label style chosen by the owner, in light and dark theme, without changing behaviour or accessibility.

**Architecture:** All look lives in `core/designsystem`: derived colours in a new `theme/ControlColors.kt`, text inputs in `component/TextFields.kt` + `component/SelectField.kt` (built on Material 3 filled `TextField` with transparent indicators), choice controls in `component/ChoiceControls.kt` (custom-drawn checkbox/radio/switch/segmented, Material `Slider`/`RangeSlider` with custom track and thumb). Feature screens stop calling Material controls directly and use these components; a source-scan test keeps it that way.

**Tech Stack:** Kotlin 2.4.21, Jetpack Compose Material 3, Robolectric 4.17 (`@Config(sdk = [36])`), ktlint.

**Spec:** `docs/superpowers/specs/2026-10-31-input-restyle-design.md` (mockups: `.superpowers/brainstorm/18994-1793425886/content/input-focus-noborder-v2.html` option 2, `choice-controls.html` option 1).

## Global Constraints

- Inputs: filled, **no border and no glow in any state**, corner radius **16dp**, single-line height 56–60dp, floating label inside the box, label and value start at the **same x (16dp)**.
- Focus: container becomes a tint of the theme `primary`; label and cursor turn primary; transition ~150–200ms. Error: reddish container derived from `destructive`, label in error colour, error text below. Never a hard-coded blue/red; the palette (`Palette.kt`) does not change.
- No decorative trailing icons (no "!", no ✓, no ✕ while typing). Kept: password show/hide, search clear button, `SelectField` chevron.
- Existing public parameters of `AppTextField`, `PasswordTextField`, `SearchField`, `SelectField`, `SegmentedChoice`, `SwitchRow`, `LabeledSlider` keep working; call sites compile unchanged.
- Choice controls: checkbox 22dp rounded square (~7dp radius), radio 22dp circle, switch ~46×28dp iOS-like pill with a white thumb that slides and never resizes; unselected = neutral fill, no border; selected = primary fill with white mark.
- Accessibility: toggle/selected/error semantics kept, 48dp touch targets, TalkBack reads an empty field's label and the error text, keyboard/D-pad usable. RTL via start/end only.
- Strings in `values/` and `values-in/`. Docs in English.
- Robolectric tests: `@RunWith(AndroidJUnit4::class)`, `@Config(sdk = [36])`.
- Proof per task: `gradlew.bat testDebugUnitTest ktlintCheck assembleDebug` (Windows), all green.

## Review Focus

1. **Dark theme on a card**: a field placed on a `card` surface (dark `Gray.c900`) must still show its fill. Pinned by `control_fill_differs_from_background_and_card` in Task 1.
2. **Brand colour swap**: changing `primary` (e.g. to green) changes the focus tint and the checked colour. Pinned by `focus_tint_follows_primary` in Task 1.
3. **Row-owned controls** (`onCheckedChange = null` / `onClick = null` inside a `toggleable` row): the control must add no second click target or duplicate semantics node, so TalkBack announces the row once. Pinned by `checkbox_without_callback_adds_no_semantics_of_its_own` in Task 2.
4. **RTL**: the segmented pill must sit under the selected segment in RTL. Pinned by `segmented_pill_follows_selection_in_rtl` in Task 3.
5. **Disabled controls**: a disabled checkbox/switch/segment ignores clicks and reports disabled. Pinned by `disabled_switch_ignores_clicks` in Task 2 and `disabled_segment_ignores_clicks` in Task 3.

---

### Task 1: Field colours and restyled text inputs

**Files:**
- Create: `app/src/main/java/com/primestrider/kotlinstarter/core/designsystem/theme/ControlColors.kt`
- Modify: `app/src/main/java/com/primestrider/kotlinstarter/core/designsystem/component/TextFields.kt`
- Modify: `app/src/main/java/com/primestrider/kotlinstarter/core/designsystem/component/SelectField.kt`
- Test: `app/src/test/java/com/primestrider/kotlinstarter/core/designsystem/theme/ControlColorsTest.kt` (new)
- Test: `app/src/test/java/com/primestrider/kotlinstarter/core/designsystem/component/TextFieldsTest.kt`, `SelectFieldTest.kt`

**Interfaces:**
- Produces (theme, `ControlColors.kt`):
  - `val AppColors.controlFill: Color` = `secondary` (light `Gray.c100`, dark `Gray.c800`; visible on both `background` and `card`).
  - `val AppColors.controlFocusFill: Color` = `primary.copy(alpha = 0.12f).compositeOver(background)`.
  - `val AppColors.controlErrorFill: Color` = `destructive.copy(alpha = 0.10f).compositeOver(background)`.
  - `val ControlShapes.field: Shape` = `RoundedCornerShape(Radii.xl)` (16dp); `ControlShapes` is an `object` in the same file, later tasks add `checkbox` (7dp) and `pill` (`Radii.full`).
  - `const val CONTROL_ANIMATION_MS = 180`.
- Produces (component): `AppTextField(..., prefix: String? = null)` — new trailing optional parameter, rendered as a small chip filled with `background` (so it reads on the field fill), radius `Radii.md`, in the leading slot, after `leadingIcon` if both are set. Internal `@Composable fun appFieldColors(): TextFieldColors` shared by all four fields.

- [ ] **Step 1: Write the failing tests**

`ControlColorsTest` (plain JUnit, no Robolectric):
```kotlin
@Test fun focus_tint_follows_primary() {
    val green = LightColors.copy(primary = Palette.Success.c500)
    assertNotEquals(LightColors.controlFocusFill, green.controlFocusFill)
    assertEquals(Palette.Primary.c500.copy(alpha = 0.12f).compositeOver(Palette.white), LightColors.controlFocusFill)
}
@Test fun error_tint_follows_destructive() =
    assertEquals(Palette.Error.c500.copy(alpha = 0.10f).compositeOver(DarkColors.background), DarkColors.controlErrorFill)
@Test fun control_fill_differs_from_background_and_card() {
    listOf(LightColors, DarkColors).forEach {
        assertNotEquals(it.background, it.controlFill); assertNotEquals(it.card, it.controlFill)
    }
}
```
`TextFieldsTest` additions:
- `field_has_no_border_and_uses_fill`: render `AppTextField(value = "", label = "Email")` in `AppTheme(darkTheme = false)` inside a 300dp-wide box with tag; `captureToImage()` and assert the pixel at (2dp inset from the start edge, vertical centre) equals `LightColors.controlFill` (an outline would paint `border` there) — compare with `toPixelMap()` and a ±2 per-channel tolerance helper.
- `focused_field_switches_to_focus_tint`: request focus (`performClick()`), `mainClock.advanceTimeBy(CONTROL_ANIMATION_MS + 50L)`, same pixel equals `LightColors.controlFocusFill`.
- `error_field_uses_error_tint_and_keeps_error_text`: `errorText = "Minimal 8 karakter"`; pixel equals `controlErrorFill`; node with text exists; `SemanticsProperties.Error` present on the field.
- `empty_field_exposes_its_label`: `onNodeWithText("Email").assertExists()` with empty value (label readable by TalkBack).
- `prefix_is_shown_before_the_value`: `prefix = "+62"`, value `"812"`; both texts exist and the prefix node's `left` < value node's `left` (`getUnclippedBoundsInRoot()`).
- `no_trailing_icon_by_default`: `AppTextField` with error has no node with `Role.Button` descendants (`onAllNodes(hasClickAction()).filter(hasAnyAncestor(hasTestTag(tag)))` count is 0 besides the field itself).
- Existing tests (counter, emoji, password toggle, search clear, IME) must stay as they are.

`SelectFieldTest` addition: `select_field_uses_fill_not_outline` — same pixel check as above on a `SelectField`.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `gradlew.bat testDebugUnitTest --tests "*ControlColorsTest" --tests "*TextFieldsTest" --tests "*SelectFieldTest"`
Expected: FAIL (unresolved `controlFocusFill`, `prefix`; pixel asserts see the outline).

- [ ] **Step 3: Implement `ControlColors.kt`**

Exactly the Interfaces above. Pixel tests compare against these properties, so the values must come from them.

- [ ] **Step 4: Restyle `TextFields.kt` and `SelectField.kt`**

- Swap `OutlinedTextField` for Material 3 `TextField` in `AppTextField`, `SearchField`, `SelectField`; `shape = ControlShapes.field`; `colors = appFieldColors()`.
- `appFieldColors()` reads `LocalAppColors.current`: all four indicator colours `Color.Transparent`; containers unfocused/disabled = `controlFill`, focused = `controlFocusFill`, error = `controlErrorFill`; focused label + cursor = `primary`; error label/cursor/supporting = `destructive`; unfocused label/placeholder/supporting = `muted`; disabled text/label at alpha 0.38.
- Container colour animation: Material's filled `TextField` already animates container changes; if the pixel test shows an instant jump is fine, keep it; do not add a second animation.
- `minHeight`: `Modifier.heightIn(min = 58.dp)` on single-line fields. Multi-line keeps radius 16 and grows.
- `SearchField`: same colours/shape, placeholder (no label), leading search icon, clear button only when query is not empty (unchanged).
- `prefix`: leading slot `Row(start padding 12dp, spacedBy 8dp) { leadingIcon?; PrefixChip(prefix) }`; `PrefixChip` = `Text` in `labelLarge` with `background` fill, `RoundedCornerShape(Radii.md)`, horizontal padding `Spacing.s2`, vertical `Spacing.s1`. The chip is decoration: `clearAndSetSemantics {}` is wrong (TalkBack must read +62) — leave its text semantics.
- Remove the class KDoc word "Outlined"; describe the filled style.

- [ ] **Step 5: Run the tests to verify they pass**

Run: `gradlew.bat testDebugUnitTest --tests "*ControlColorsTest" --tests "*TextFieldsTest" --tests "*SelectFieldTest" --tests "*AppThemeTest" --tests "*TokensTest"`
Expected: PASS.

- [ ] **Step 6: Full proof and commit**

Run: `gradlew.bat testDebugUnitTest ktlintCheck assembleDebug` → BUILD SUCCESSFUL.
```bash
git add app/src/main/java/com/primestrider/kotlinstarter/core/designsystem app/src/test/java/com/primestrider/kotlinstarter/core/designsystem
git commit -m "feat(designsystem): restyle text fields as filled, borderless fields with a primary focus tint and a prefix chip"
```

---

### Task 2: AppCheckbox, AppRadioButton, AppSwitch

**Files:**
- Modify: `app/src/main/java/com/primestrider/kotlinstarter/core/designsystem/theme/ControlColors.kt` (add `ControlShapes.checkbox = RoundedCornerShape(7.dp)`, `ControlShapes.pill = RoundedCornerShape(Radii.full)`)
- Create: `app/src/main/java/com/primestrider/kotlinstarter/core/designsystem/component/Toggles.kt`
- Modify: `app/src/main/java/com/primestrider/kotlinstarter/core/designsystem/component/ChoiceControls.kt` (`SwitchRow` uses `AppSwitch`)
- Test: `app/src/test/java/com/primestrider/kotlinstarter/core/designsystem/component/TogglesTest.kt` (new), `ChoiceControlsTest.kt`

**Interfaces:**
- Consumes: `controlFill`, `ControlShapes`, `CONTROL_ANIMATION_MS` from Task 1.
- Produces:
  - `@Composable fun AppCheckbox(state: ToggleableState, onClick: (() -> Unit)?, modifier: Modifier = Modifier, enabled: Boolean = true)` — indeterminate = primary fill with a white dash.
  - `@Composable fun AppCheckbox(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier, enabled: Boolean = true)` — delegates to the tri-state one.
  - `@Composable fun AppRadioButton(selected: Boolean, onClick: (() -> Unit)?, modifier: Modifier = Modifier, enabled: Boolean = true)` — selected = primary fill with an 8dp white centre dot.
  - `@Composable fun AppSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier, enabled: Boolean = true)` — 46×28dp pill, 24dp white thumb with 2dp elevation shadow, 2dp inset, thumb offset animated over `CONTROL_ANIMATION_MS` (use `Modifier.offset`, which mirrors in RTL); track off = `controlFill`, on = `primary`.
- Same contract as Material: with a non-null callback the control is its own target (`triStateToggleable` / `selectable` / `toggleable` with `Role.Checkbox` / `RadioButton` / `Switch`, plus `minimumInteractiveComponentSize()` for 48dp); with a null callback it draws only and adds no click or semantics (the row owns them). Disabled = content alpha 0.38 and no clicks. Draw with `Box` + `background(shape)` + `Icon(Icons.Filled.Check)` 16dp / a 10×2dp dash; no border anywhere.

- [ ] **Step 1: Write the failing tests** (`TogglesTest`, Robolectric, `AppTheme` wrapper)

```kotlin
@Test fun checkbox_toggles_and_reports_state() // click tag → callback(true); assertIsOff() before, re-render checked → assertIsOn()
@Test fun indeterminate_checkbox_reports_indeterminate() // SemanticsProperties.ToggleableState == Indeterminate
@Test fun checkbox_without_callback_adds_no_semantics_of_its_own() {
    // Row(Modifier.toggleable(value = false, role = Role.Checkbox) {}.testTag("row")) { AppCheckbox(false, null); Text("Terms") }
    // onAllNodes(hasClickAction()).assertCountEquals(1); onNodeWithTag("row").assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
}
@Test fun radio_reports_selected_and_role() // assertIsSelected(); Role.RadioButton
@Test fun switch_has_switch_role_and_48dp_target() // Role.Switch; assertHeightIsAtLeast(48.dp); assertWidthIsAtLeast(48.dp)
@Test fun disabled_switch_ignores_clicks() // enabled = false: assertIsNotEnabled(); performClick() → callback never called
@Test fun checked_checkbox_paints_primary() // captureToImage; pixel 4dp in from the top-start corner (off the check mark) == LightColors.primary ±2; unchecked == controlFill
```
`ChoiceControlsTest`: existing `switch_row_toggles_from_a_click_on_the_label_and_has_switch_role` must keep passing, plus `switch_row_has_one_click_target` (`onAllNodes(hasClickAction()).assertCountEquals(1)`).

- [ ] **Step 2: Run to verify failure**

Run: `gradlew.bat testDebugUnitTest --tests "*TogglesTest" --tests "*ChoiceControlsTest"` → FAIL (unresolved references).

- [ ] **Step 3: Implement `Toggles.kt` and switch `SwitchRow` to `AppSwitch(checked, onCheckedChange = null, enabled = enabled)`**

- [ ] **Step 4: Run to verify pass** — same command → PASS.

- [ ] **Step 5: Full proof and commit**

Run: `gradlew.bat testDebugUnitTest ktlintCheck assembleDebug` → BUILD SUCCESSFUL.
```bash
git add app/src/main/java/com/primestrider/kotlinstarter/core/designsystem app/src/test/java/com/primestrider/kotlinstarter/core/designsystem
git commit -m "feat(designsystem): add borderless AppCheckbox, AppRadioButton and iOS-style AppSwitch"
```

---

### Task 3: Segmented controls and sliders

**Files:**
- Modify: `app/src/main/java/com/primestrider/kotlinstarter/core/designsystem/component/ChoiceControls.kt`
- Test: `app/src/test/java/com/primestrider/kotlinstarter/core/designsystem/component/ChoiceControlsTest.kt`

**Interfaces:**
- Consumes: `controlFill`, `ControlShapes`, `CONTROL_ANIMATION_MS`.
- Produces:
  - `SegmentedChoice(options, selected, onSelect, label, modifier, enabled)` — signature unchanged. Track: `controlFill`, `RoundedCornerShape(14.dp)`, 4dp inner padding, min height 48dp. Selected pill: `RoundedCornerShape(10.dp)`, colour `val AppColors.controlRaised` (add to `ControlColors.kt`) = `card` when `background.luminance() >= 0.5f` (white in light), else `foreground.copy(alpha = 0.14f).compositeOver(controlFill)` (a lighter grey in dark); 1dp shadow. Pill slides with `animateDpAsState` on `Modifier.offset` (mirrors in RTL); segment width = track width / count (`BoxWithConstraints`). Selected label `FontWeight.SemiBold`. Semantics: container `selectableGroup()`, each segment `selectable(role = Role.RadioButton)`.
  - `@Composable fun <T> MultiSegmentedChoice(options: List<T>, selected: Set<T>, onToggle: (T) -> Unit, label: (T) -> String, modifier: Modifier = Modifier, enabled: Boolean = true)` — same track; each selected segment drawn raised (no sliding); each segment `toggleable(role = Role.Checkbox)`.
  - `LabeledSlider(...)` — signature unchanged; Material `Slider` with custom `track` (6dp high, `controlFill` inactive, `primary` active, no stop indicator, no thumb gap) and `thumb` (22dp white circle, 2dp shadow, keeps the 48dp touch target from Material).
  - `@Composable fun LabeledRangeSlider(label: String, value: ClosedFloatingPointRange<Float>, onValueChange: (ClosedFloatingPointRange<Float>) -> Unit, valueRange: ClosedFloatingPointRange<Float>, valueText: String, modifier: Modifier = Modifier, steps: Int = 0)` — Material `RangeSlider`, same track/thumbs.

- [ ] **Step 1: Write the failing tests**

```kotlin
@Test fun segmented_pill_follows_selection_in_rtl() // CompositionLocalProvider(LocalLayoutDirection provides Rtl); 3 options, selected = third; pill (tag SEGMENTED_PILL_TAG, internal const) left bound < first segment's left bound + 1dp, i.e. third option sits at the visual left in RTL
@Test fun segmented_reports_selection_semantics() // option nodes have Role.RadioButton; selected one assertIsSelected()
@Test fun disabled_segment_ignores_clicks() // enabled = false → click → onSelect never called
@Test fun multi_segmented_toggles_independently() // click "Bold" then "Italic" → onToggle called with each; nodes Role.Checkbox, assertIsOn for those in `selected`
@Test fun range_slider_shows_label_and_value_text() // texts exist; two nodes with ProgressBarRangeInfo
```
Keep `segmented_choice_reports_the_clicked_option` and `labeled_slider_shows_label_and_value_text`.

- [ ] **Step 2: Run to verify failure** — `gradlew.bat testDebugUnitTest --tests "*ChoiceControlsTest"` → FAIL.

- [ ] **Step 3: Implement** in `ChoiceControls.kt` (+ `controlRaised` in `ControlColors.kt`).

- [ ] **Step 4: Run to verify pass** — same command → PASS.

- [ ] **Step 5: Full proof and commit**

Run: `gradlew.bat testDebugUnitTest ktlintCheck assembleDebug` → BUILD SUCCESSFUL.
Commit: `feat(designsystem): restyle segmented choice and sliders, add multi-select segments and a range slider`

---

### Task 4: Migrate every screen, guard it, document it

**Files:**
- Modify: `feature/example/auth/SignInScreen.kt:82` (Checkbox → `AppCheckbox`), `feature/example/components/DisplayScreen.kt:133` (Switch → `AppSwitch`, keep `LONG_ROW_SWITCH_TAG`), `feature/example/components/FeedbackScreen.kt:115`, `feature/example/ThemeModeSelector.kt:22`, `feature/example/todos/TodosScreen.kt:88` (segmented rows → `SegmentedChoice`), `TodosScreen.kt:150` (→ `AppCheckbox`), `feature/example/settings/SettingsScreen.kt:78` (→ `AppRadioButton`), `core/designsystem/component/BottomSheets.kt:124` (→ `AppRadioButton`)
- Modify: `feature/example/components/InputsScreen.kt` — raw `TextField`/`OutlinedTextField` demos (name with hint + leading icon, username error, disabled, bio with counter `BIO_MAX` via `maxLength`) → `AppTextField`; phone `KeyboardField` gets `prefix = "+62"` (string resource `components_field_phone_prefix` in `values/` and `values-in/`); checkbox/`TriStateCheckbox`/radio/switch → `AppCheckbox`/`AppRadioButton`/`AppSwitch` (rows keep their `toggleable`/`selectable` modifiers, controls get `null` callbacks); `Slider`/`RangeSlider` → `LabeledSlider`/`LabeledRangeSlider` with the existing value strings; single and multi segmented rows → `SegmentedChoice`/`MultiSegmentedChoice`. Tabs stay Material (non-goal).
- Create test: `app/src/test/java/com/primestrider/kotlinstarter/core/designsystem/NoRawMaterialControlsTest.kt`
- Modify docs: `docs/architecture/components.md` (new components, `prefix`, filled look, "use these, never Material controls directly"), `docs/architecture/design-system.md` (control colours derived from `primary`/`destructive`, the accepted focus-contrast trade-off, no outline rule).
- Tests of migrated screens that reference Material-specific nodes (e.g. `InputsScreenTest`, `TodosScreenTest`, `SettingsScreenTest`, `SignInScreenTest`, `DisplayScreenTest`, `FeedbackScreenTest`, `BottomSheetsTest`) must keep passing; fix selectors only if they relied on Material internals, never weaken assertions.

**Interfaces:**
- Consumes: everything from Tasks 1–3.

- [ ] **Step 1: Write the failing guard test**

`NoRawMaterialControlsTest` (plain JUnit): walk `File("src/main/java")` (Robolectric/Gradle runs with the module dir as working dir; assert the dir exists first so the test cannot pass vacuously), skip `core/designsystem/component/`, and fail listing every file whose text matches
`Regex("""import androidx\.compose\.material3\.(Checkbox|TriStateCheckbox|RadioButton|Switch|Slider|RangeSlider|SegmentedButton|SingleChoiceSegmentedButtonRow|MultiChoiceSegmentedButtonRow|TextField|OutlinedTextField)\b""")`.
Test name: `feature_code_uses_design_system_controls_only`.

- [ ] **Step 2: Run to verify failure**

Run: `gradlew.bat testDebugUnitTest --tests "*NoRawMaterialControlsTest"` → FAIL listing the eight files above.

- [ ] **Step 3: Migrate each file** as listed. `SegmentedChoice` call sites pass `label = { stringResource(...) }`-style lambdas; read strings outside the lambda if `label` is not composable (it is `(T) -> String`: map options to resolved strings first).

- [ ] **Step 4: Run the guard and the migrated screens' tests**

Run: `gradlew.bat testDebugUnitTest` → PASS, test count ≥ the count before Task 1 plus the new tests.

- [ ] **Step 5: Update the two docs**

- [ ] **Step 6: Full proof, manual look, commit**

Run: `gradlew.bat testDebugUnitTest ktlintCheck assembleDebug` → BUILD SUCCESSFUL.
Install on the emulator/device if one is attached (`adb devices`; Windows Git Bash needs `MSYS_NO_PATHCONV=1`) and check the Inputs page in light and dark: no outlines, label aligned with text, focus tint, error tint, +62 chip, sliding switch and segment pill. Report if no device was available.
```bash
git add -A app/src docs/architecture
git commit -m "refactor(example): use the restyled design-system controls everywhere and guard against raw Material controls"
```
