# Input & Choice Controls Restyle — Design

Date: 2026-10-31. Starts only after the Animations plan (plan 06) is finished and merged into the working branch.
Comes before the architecture/modularisation project (`2026-10-31-architecture-modularisation-design.md`).

## Intent

The form controls in `core/designsystem/component` still look like stock Material 3 (outlined text fields,
Material checkbox/radio/switch/segmented buttons). The owner wants the look of modern consumer apps (Netflix,
WhatsApp, Threads, Gojek/Grab): cool but tidy and clean. The direction was chosen from browser mockups (kept in
`.superpowers/brainstorm/18994-1793425886/content/`, the chosen screens are `input-focus-noborder-v2.html`
option 2 and `choice-controls.html` option 1).

Success: every text input and choice control in the app has the new look in light and dark theme, every screen
that uses them changes automatically, and nothing in behaviour or accessibility gets worse.

## Goals

### Text inputs (`AppTextField`, `PasswordTextField`, `SearchField`, `SelectField`)

- **Filled, no border.** Container is a soft neutral fill (light: slate-100-like; dark: zinc-900-like surface
  slightly lighter than the background). No outline and no glow in any state.
- **Corner radius 16dp.** Height about 56–60dp for single-line fields (owner said the original was "not rounded
  enough"; 16 was the chosen step).
- **Floating label inside the box**: the label sits where the text goes when empty and shrinks to the top when
  focused or filled. Label and value start at the **same x (16dp)**. The owner explicitly flagged a misaligned
  label as wrong.
- **Focus = background tint of the brand colour** (light: very light primary, e.g. `#E8F4FE` for the current
  blue; dark: a deep primary, e.g. `#10243A`), with a short colour transition. Label and cursor turn primary.
  The tint is derived from the theme's `primary`, never hard-coded, so changing the brand colour (e.g. to green)
  changes it.
- **Error** = reddish fill (light ≈ `#FEF2F2`, dark ≈ `#2A1416`, derived from `destructive`), label in the error
  colour, and the error text below the box in the error colour. Error text wins over supporting text, as today.
- **Supporting text and the `n/max` counter** stay below the box, muted.
- **No decorative trailing icons.** No error "!" icon, no valid ✓, no clear ✕ while typing. Owner's words: "no
  icon needed on the right".
  - Functional exceptions kept (owner was told and did not object): the password show/hide toggle, the search
    field's clear button, and `SelectField`'s dropdown chevron.
- `SearchField`: same filled look, placeholder instead of a floating label, leading search icon kept.
- **Phone prefix chip** (`🇮🇩 +62` style leading chip, from the Gojek mockup): owner liked it as part of the
  Gojek version. Add an optional leading `prefix` slot/variant usable for a phone field, and show it on the
  Inputs example page. *(Builder's call: exact API; a `prefix: String?` rendered as a small filled chip is
  enough.)*
- The existing public parameters of these components keep working (`label`, `placeholder`, `supportingText`,
  `errorText`, `leadingIcon`, `trailingIcon`, `keyboardType`, `imeAction`, `onImeAction`, `singleLine`,
  `minLines`, `maxLength`, `enabled`, `visualTransformation`), so call sites do not change. Multi-line fields
  keep radius 16 and grow in height.
- Disabled: same fill, reduced content alpha.

### Choice controls ("plain controls", mockup option 1)

New core components, because feature screens currently call Material `Checkbox`, `RadioButton`, `Switch`,
`SegmentedButton` directly:

- `AppCheckbox`: 22dp rounded square (≈7dp radius). Unchecked = neutral fill, **no border**. Checked = primary
  fill with a white check.
- `AppRadioButton`: 22dp circle. Unselected = neutral fill, no border. Selected = primary fill with a white
  centre dot.
- `AppSwitch`: iOS-like pill (≈46×28dp). Off = neutral track; on = primary track; white round thumb with a
  small shadow that slides (animated). The thumb does not change size between states.
- `SegmentedChoice` (existing): filled neutral track (radius ≈14dp, 4dp inner padding); the selected segment is a
  raised "pill" (white in light, a lighter grey in dark) that slides between segments; selected label semibold.
- `LabeledSlider` (existing): thin 6dp neutral track, primary active part, white 22dp thumb with shadow.
- `SwitchRow` (existing) uses `AppSwitch`.

Migrate every direct Material usage to these components: `SignInScreen` (checkbox), `InputsScreen` (checkbox,
radio, switch, slider, segmented), `DisplayScreen` (switch), `FeedbackScreen` (segmented), `SettingsScreen`
(radio), `ThemeModeSelector` (segmented), `TodosScreen` (checkbox, segmented), `BottomSheets.kt` option list
(radio). Include any usages added by the Animations plan.

*(Designer's call, after the builder check.)* The Inputs example page also shows `TriStateCheckbox`,
`RangeSlider`, `MultiChoiceSegmentedButtonRow` and raw Material `TextField`/`OutlinedTextField` demos (error,
disabled, bio with counter). All of them get the new look too, so no bordered Material control is left anywhere:
- `AppCheckbox` supports the indeterminate state (primary fill with a white dash), or a tri-state sibling.
- `LabeledSlider` gets a range variant with the same track and two thumbs.
- `SegmentedChoice` gets a multi-select variant: each selected segment raised, same track.
- The raw text-field demos become `AppTextField` calls showing the same states.

## Non-goals

- Buttons, cards, dialogs, sheets, navigation bar: unchanged.
- No new form library, no validation framework; validation stays where it is.
- No change to palette or brand colour.

## Anti-goals (a failure even if it "works")

- Any border or glow on inputs.
- Label not aligned with the value text.
- Hard-coded blue that does not follow the theme's primary colour.
- Lost accessibility: the controls must keep toggle/selected/error semantics, 48dp touch targets (the visual is
  22dp; the touch target is the row or a padded area), TalkBack still reads the label of an empty field and the
  error text, and controls stay usable with a keyboard/D-pad.
- Light theme fine, dark theme broken (or the reverse).

## Constraints

- Lives in `core/designsystem` (theme tokens in `theme/`, components in `component/`). Rules in `AGENTS.md`:
  `core` never imports `feature`; check `docs/architecture/components.md` before writing composables.
- Colours come from `AppColors` / the M3 colour scheme. New semantic tokens (input fill, input focus tint,
  input error tint, control track) go into `AppColors` for both themes; the existing reflection test that guards
  colour-role mapping must stay green.
- RTL must work (start/end, not left/right), as earlier review rounds required.
- Strings in `values/` and `values-in/`.
- Proof: `./gradlew testDebugUnitTest ktlintCheck assembleDebug` (Windows: `gradlew.bat`), Robolectric
  `@Config(sdk = [36])`.

## Accepted trade-off

Focus is shown by a background tint plus a coloured label and cursor, with no outline. The tint against the
neutral fill may not reach a 3:1 non-text contrast ratio. The owner chose this look explicitly; the label colour
change and the cursor are the additional cues. Do not add an outline back.

## Left to the builder

- Whether to build the text fields on Material 3 `TextField` (filled, with transparent indicator, custom shape
  and per-state container colours: preferred if it can match the look) or on `BasicTextField` with a custom
  decorator.
- Whether the choice controls are custom-drawn or customised Material components, as long as the look and the
  semantics above hold.
- How to derive the tints from `primary` / `destructive` (blending in code is fine; the palette itself must not
  change; the hex values above are examples of the intended result, not palette entries).
- Animation durations (short, ~150–200ms) and exact neutral shades within the described intent.
- Updating `docs/architecture/components.md` and `docs/architecture/design-system.md`, the Inputs example page
  showing all states (empty, focused, filled, error, disabled, prefix), and Robolectric tests for the new
  components' semantics and states.
