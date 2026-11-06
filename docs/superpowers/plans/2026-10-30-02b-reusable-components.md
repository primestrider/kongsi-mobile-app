# Reusable Components & Bottom-Nav Shell Implementation Plan (Plan 2b)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a reusable component layer in `core/designsystem/component/` (screen scaffold, bottom navigation, dialogs, sheets, form fields, buttons, global messages, state views, avatar, skeleton, banner and a few common extras), restructure the app into a 3-tab bottom-navigation shell, and switch the example pages to the new components.

**Architecture:** Components are thin, stateless composables built on Material 3 (never re-implementing M3 behavior). App-wide messages go through a Hilt singleton `Messenger` (a `Channel`) that ViewModels and composables can call; one `MessengerHost` in the shell renders them as snackbars. The shell is a `Scaffold` with `AppNavigationBar` and one Navigation Compose nested graph per tab, using `saveState`/`restoreState` for independent back stacks.

**Tech Stack:** Compose BOM + Material 3, material-icons-core, Navigation Compose (type-safe), Hilt, Robolectric + Compose UI test, kotlinx-coroutines-test + Turbine. No new dependencies.

**Spec:** `docs/superpowers/specs/2026-10-30-kotlin-android-starter-design.md` §5.2 (revisi: komponen reusable, shell navigasi)

## Global Constraints

- Branch `feat/foundation` (user: stay on it, do not merge). Toolchain unchanged.
- Components live in `core/designsystem/component/`, package `com.primestrider.kotlinstarter.core.designsystem.component`; they must not import `feature/*` or `app/*`.
- Components are stateless (state + callbacks in, UI out), take `modifier: Modifier = Modifier` as the first optional parameter, and read colors only from `MaterialTheme.colorScheme` / `AppThemeTokens.colors`.
- Component strings use the `component_` prefix and exist in `values/` and `values-in/`.
- Accessibility: icon-only controls have a `contentDescription`; touch targets ≥ 48dp; state carried in semantics (selected tab, error text, disabled).
- No new dependencies; icons from material-icons-core only. No image loading (avatars are initials-based; image avatars come with networking in a later plan).
- Tests: Robolectric `@Config(sdk = [36])`, `androidx.compose.ui.test.junit4.v2.*`; Hilt tests via `@HiltAndroidTest` + `HiltTestApplication`; no file-backed DataStore in JVM tests.
- Proof command: `./gradlew testDebugUnitTest ktlintCheck assembleDebug`.

## Review Focus

1. **Switching tabs** → each tab keeps its own back stack (open Colors under Home, go to Profile, back to Home → Colors still shown); re-selecting the current tab pops it to its root; system back at a non-Home tab root returns to Home, then exits. Test in Task 1.
2. **A message sent while no host is collecting** (e.g. during rotation, or from a ViewModel before the UI subscribes) → is delivered once the host collects, not dropped. Test in Task 4.
3. **Double tap on a loading `AppButton`** → `onClick` fires once; the button reports disabled while loading. Test in Task 2.
4. **Non-dismissible `ConfirmDialog` / `AppBottomSheet`** → back press does not close them; `OptionsBottomSheet` calls `onSelect` exactly once and closes. Test in Task 3.
5. **Long labels** (button text, select option, banner title) → single-line labels ellipsize, body text wraps; nothing pushes trailing icons off screen. Test in Task 2 (select) and Task 4 (banner).

## File Structure

```
core/designsystem/component/
  AppScaffold.kt        # AppScaffold, AppTopBar                       (Task 1)
  AppNavigationBar.kt   # NavBarItem, AppNavigationBar                 (Task 1)
  AppButton.kt          # ButtonVariant, AppButton (loading)           (Task 2)
  TextFields.kt         # AppTextField, PasswordTextField, SearchField (Task 2)
  SelectField.kt        # SelectOption, SelectField                    (Task 2)
  Dialogs.kt            # ConfirmDialog                                (Task 3)
  BottomSheets.kt       # AppBottomSheet, OptionsBottomSheet           (Task 3)
  Messenger.kt          # UiText, MessageVariant, UiMessage, Messenger, LocalMessenger, MessengerHost (Task 4)
  StateViews.kt         # LoadingState, EmptyState, ErrorState, LoadingOverlay (Task 4)
  Banner.kt             # Banner                                       (Task 4)
  Avatar.kt             # AvatarSize, AvatarStatus, Avatar, AvatarGroup, initialsOf (Task 5)
  Skeleton.kt           # Skeleton, SkeletonText                       (Task 5)
  SectionHeader.kt, InfoRow.kt                                         (Task 5)
app/navigation/MainShell.kt     # tabs, nested graphs, bottom bar, MessengerHost (Task 1, Task 4)
feature/example/ComponentsHomeScreen.kt   # Components tab root (Task 1)
feature/example/components/FeedbackScreen.kt   # messages + state views demo (Task 4)
docs/architecture/components.md # component catalog (grows per task)
```

---

### Task 1: Scaffold, navigation bar, 3-tab shell

**Files:**
- Create: `core/designsystem/component/AppScaffold.kt`, `AppNavigationBar.kt`, `app/navigation/MainShell.kt`, `feature/example/ComponentsHomeScreen.kt`, `docs/architecture/components.md`
- Modify: `app/navigation/AppNavHost.kt` (becomes the per-tab graph builders), `app/MainActivity.kt` (content = `MainShell()`), `feature/example/ExamplePageScaffold.kt` (built on `AppScaffold`), `feature/example/ExampleCatalog.kt`, `feature/example/ExampleRoutes.kt`, `docs/architecture/navigation.md`, `AGENTS.md`, `strings.xml` ×2
- Test: `core/designsystem/component/AppScaffoldTest.kt`, `AppNavigationBarTest.kt`, `app/MainShellTest.kt`; update `app/AppNavHostTest.kt`

**Interfaces:**
- Produces:
  - `@Composable fun AppTopBar(title: String, modifier: Modifier = Modifier, onBack: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {})` — back arrow (`component_back` content description) only when `onBack != null`; title single line with ellipsis.
  - `@Composable fun AppScaffold(title: String, modifier: Modifier = Modifier, onBack: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {}, snackbarHost: @Composable () -> Unit = {}, floatingActionButton: @Composable () -> Unit = {}, content: @Composable (PaddingValues) -> Unit)`.
  - `data class NavBarItem(val label: String, val icon: ImageVector, val badgeCount: Int? = null)`; `@Composable fun AppNavigationBar(items: List<NavBarItem>, selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier)` — M3 `NavigationBar`/`NavigationBarItem`; `badgeCount` > 0 shows a `Badge` ("99+" above 99).
  - Tabs: `enum class TopLevelTab(@StringRes val label: Int, val icon: ImageVector, val startRoute: Any) { HOME(nav_home, Icons.Filled.Home, HomeRoute), COMPONENTS(nav_components, Icons.Filled.List, ComponentsHomeRoute), PROFILE(nav_profile, Icons.Filled.Person, SettingsRoute) }` in `MainShell.kt`; graph routes `@Serializable data object HomeGraph`, `ComponentsGraph`, `ProfileGraph`.
  - `@Composable fun MainShell(navController: NavHostController = rememberNavController())` — `Scaffold(bottomBar = AppNavigationBar(...))`; NavHost with three nested graphs; tab click = `navigate(graph) { popUpTo(startDestinationId) { saveState = true }; launchSingleTop = true; restoreState = true }`; re-selecting the current tab pops its graph to its start route; NavHost gets `Modifier.padding(bottom = inner.calculateBottomPadding()).consumeWindowInsets(inner)` so inner `AppScaffold`s do not double the bottom inset.
  - `@Composable fun ComponentsHomeScreen(entries: List<ExampleEntry>, onOpen: (Any) -> Unit)` with route `ComponentsHomeRoute`; it lists the Components section (Actions, Inputs, Display, Overlays, and in Task 4 Feedback). Home's catalog keeps Features (Form) and Styling; Settings leaves the Home catalog and becomes the Profile tab root (its back arrow is hidden there: `SettingsScreen(onBack = null)` — make `onBack` nullable).
  - `ExamplePageScaffold` keeps its signature and delegates to `AppScaffold` (scroll + subtitle + IME padding stay in `ExamplePageScaffold`).
- Strings: `nav_home` Home/Beranda, `nav_components` Components/Komponen, `nav_profile` Profile/Profil, `component_back` Back/Kembali (replaces `common_action_back` usages; delete the old key).

- [ ] **Step 1: Write failing tests**: `AppScaffoldTest` — with `onBack = null` no node with content description "Back"; with `onBack` set, clicking it calls it; a 200-char title still leaves an action icon `assertIsDisplayed()`. `AppNavigationBarTest` — 3 items, `selectedIndex = 1` → second item `assertIsSelected()`; clicking third calls `onSelect(2)`; `badgeCount = 120` shows "99+". `MainShellTest` (Hilt, `createAndroidComposeRule<MainActivity>`, content replaced with `MainShell(navController)` like `AppNavHostTest`): (a) open "Colors" from Home, tap Profile tab, tap Home tab → "Colors" top bar title displayed (Review Focus 1); (b) while on Colors, tap Home tab again → Home root ("Kotlin Starter") displayed; (c) on Profile root, `pressBack` via `activity.onBackPressedDispatcher.onBackPressed()` → Home root displayed. Update `AppNavHostTest` to navigate every catalog entry and every Components entry through `MainShell`.
- [ ] **Step 2: Run** `./gradlew testDebugUnitTest --tests "*AppScaffoldTest" --tests "*AppNavigationBarTest" --tests "*MainShellTest"` → FAIL (unresolved).
- [ ] **Step 3: Implement** components, shell, graphs, `ComponentsHomeScreen`, catalog changes, strings.
- [ ] **Step 4: Run** full proof command → PASS.
- [ ] **Step 5: Docs**: create `docs/architecture/components.md` (purpose, rules from Global Constraints, a table row per component with file + one-line usage); rewrite `navigation.md` for the shell (tabs, graphs, back behavior, where to add a destination); update `AGENTS.md` project map and "adding a page" steps.
- [ ] **Step 6: Commit** `feat(navigation): add AppScaffold, AppNavigationBar and 3-tab shell`

---

### Task 2: Buttons and form fields

**Files:**
- Create: `core/designsystem/component/AppButton.kt`, `TextFields.kt`, `SelectField.kt`
- Modify: `feature/example/form/FormExampleScreen.kt`, `feature/example/components/ActionsScreen.kt` (loading section), `InputsScreen.kt` (password + select sections), `docs/architecture/components.md`, `strings.xml` ×2
- Test: `core/designsystem/component/AppButtonTest.kt`, `TextFieldsTest.kt`, `SelectFieldTest.kt`

**Interfaces:**
- Produces:
  - `enum class ButtonVariant { PRIMARY, SECONDARY, OUTLINED, TEXT, DESTRUCTIVE }` (M3: `Button`, `FilledTonalButton`, `OutlinedButton`, `TextButton`, `Button` with error colors).
  - `@Composable fun AppButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, variant: ButtonVariant = ButtonVariant.PRIMARY, enabled: Boolean = true, loading: Boolean = false, leadingIcon: ImageVector? = null, fullWidth: Boolean = false)` — disabled while `loading`, shows an 18dp `CircularProgressIndicator` in place of the icon, `onClick` guarded so a second click while `loading` is ignored; text `maxLines = 1`, ellipsis.
  - `@Composable fun AppTextField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, placeholder: String? = null, supportingText: String? = null, errorText: String? = null, leadingIcon: ImageVector? = null, trailingIcon: @Composable (() -> Unit)? = null, keyboardType: KeyboardType = KeyboardType.Text, imeAction: ImeAction = ImeAction.Default, singleLine: Boolean = true, minLines: Int = 1, maxLength: Int? = null, enabled: Boolean = true, visualTransformation: VisualTransformation = VisualTransformation.None)` — `OutlinedTextField`; `errorText` wins over `supportingText` and sets `isError`; `maxLength` truncates input and shows "n/max" (reuse `component_counter`).
  - `@Composable fun PasswordTextField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, errorText: String? = null, imeAction: ImeAction = ImeAction.Done)` — owns the visibility toggle (`rememberSaveable`), Lock icons, `component_show_password` / `component_hide_password`.
  - `@Composable fun SearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier, placeholder: String = stringResource(R.string.component_search))` — search icon, clear button (`component_clear_search`) only when not empty, `ImeAction.Search`.
  - `data class SelectOption<T>(val value: T, val label: String, val enabled: Boolean = true)`; `@Composable fun <T> SelectField(label: String, options: List<SelectOption<T>>, selected: T?, onSelect: (T) -> Unit, modifier: Modifier = Modifier, placeholder: String? = null, errorText: String? = null, enabled: Boolean = true)` — `ExposedDropdownMenuBox`, read-only field, options single-line with ellipsis, disabled options not clickable.
- Example pages switch to these: Form uses `AppTextField`/`PasswordTextField`/`AppButton`; Actions' loading section uses `AppButton(loading = …)`; Inputs' password and select sections use `PasswordTextField`/`SelectField`. Existing page tests must stay green (update only selectors that rely on removed test tags, keeping their assertions).

- [ ] **Step 1: Write failing tests**: `AppButtonTest` — clicking a `loading = true` button never calls `onClick` and the node `assertIsNotEnabled()`; with `loading` false a click calls it once; each `ButtonVariant` renders its text. `TextFieldsTest` — `errorText` shown and node has `SemanticsProperties.Error`; `maxLength = 5` typing "abcdefg" leaves "abcde" and shows "5/5"; `PasswordTextField` draws "•••" until "Show password" is clicked (drawn-text check as in `InputsScreenTest`); `SearchField` clear button absent for "" and clears "abc" to "". `SelectFieldTest` — choosing option 2 calls `onSelect(value2)` once; a disabled option click does nothing; with a 120-char option label the dropdown arrow `assertIsDisplayed()` (Review Focus 5).
- [ ] **Step 2: Run** `./gradlew testDebugUnitTest --tests "*AppButtonTest" --tests "*TextFieldsTest" --tests "*SelectFieldTest"` → FAIL.
- [ ] **Step 3: Implement** components and strings; switch the example pages.
- [ ] **Step 4: Run** full proof command → PASS (existing Actions/Inputs/Form tests included).
- [ ] **Step 5: Docs** rows in `components.md`. **Commit** `feat(designsystem): add AppButton, text fields and SelectField`

---

### Task 3: Dialogs and bottom sheets

**Files:**
- Create: `core/designsystem/component/Dialogs.kt`, `BottomSheets.kt`
- Modify: `feature/example/components/OverlaysScreen.kt`, `docs/architecture/components.md`, `strings.xml` ×2
- Test: `core/designsystem/component/DialogsTest.kt`, `BottomSheetsTest.kt`

**Interfaces:**
- Produces:
  - `@Composable fun ConfirmDialog(title: String, message: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier, dismissLabel: String? = stringResource(R.string.component_cancel), icon: ImageVector? = null, destructive: Boolean = false, dismissible: Boolean = true)` — `AlertDialog`; `destructive` colors the confirm text with `colorScheme.error`; `dismissible = false` sets `DialogProperties(false, false)` and makes `onDismissRequest` a no-op; `dismissLabel = null` hides the dismiss button.
  - `@Composable fun AppBottomSheet(onDismiss: () -> Unit, modifier: Modifier = Modifier, title: String? = null, dismissible: Boolean = true, sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true, confirmValueChange = { dismissible || it != SheetValue.Hidden }), content: @Composable ColumnScope.() -> Unit)` — `ModalBottomSheet` with `properties = ModalBottomSheetProperties(shouldDismissOnBackPress = dismissible)`; title `titleMedium`; content padded `Spacing.s4` and above the nav bar.
  - `@Composable fun <T> OptionsBottomSheet(title: String, options: List<SelectOption<T>>, selected: T?, onSelect: (T) -> Unit, onDismiss: () -> Unit)` — radio rows (`selectable`, `Role.RadioButton`); a tap hides the sheet, then calls `onSelect` once, then `onDismiss`.
- Overlays page uses `ConfirmDialog` for both dialogs and `OptionsBottomSheet` for "Sort by"; the raw `ModalBottomSheet` example is replaced, and a second button "Open custom sheet" shows `AppBottomSheet` with free content (a short text + `AppButton` "Done").

- [ ] **Step 1: Write failing tests** (`createAndroidComposeRule<ComponentActivity>`, back via the dialog's `OnBackPressedDispatcher` as in `OverlaysScreenTest`): `DialogsTest` — confirm click calls `onConfirm`; dismiss button calls `onDismiss`; `dismissible = false` + back → `onDismiss` not called and dialog still shown; `dismissLabel = null` → no "Cancel" node. `BottomSheetsTest` — `OptionsBottomSheet`: click option 2 → `onSelect` called exactly once with value 2 and `onDismiss` called once (after `waitForIdle`); `AppBottomSheet(dismissible = false)` + back → `onDismiss` not called.
- [ ] **Step 2: Run** `./gradlew testDebugUnitTest --tests "*DialogsTest" --tests "*BottomSheetsTest"` → FAIL.
- [ ] **Step 3: Implement** and switch the Overlays page; keep `OverlaysScreenTest` green.
- [ ] **Step 4: Run** full proof command → PASS.
- [ ] **Step 5: Docs** rows in `components.md`. **Commit** `feat(designsystem): add ConfirmDialog and bottom sheets`

---

### Task 4: Global messages, state views, banner, Feedback page

**Files:**
- Create: `core/designsystem/component/Messenger.kt`, `StateViews.kt`, `Banner.kt`, `feature/example/components/FeedbackScreen.kt`, `feature/example/components/FeedbackViewModel.kt`
- Modify: `app/navigation/MainShell.kt` (host), `app/MainActivity.kt` (provide `LocalMessenger`), `ExampleRoutes.kt` (`FeedbackRoute`), Components catalog, `OverlaysScreen.kt` (snackbar section removed — it moves to Feedback), `DisplayScreen.kt` (banners + empty state use the components), `docs/architecture/components.md`, `strings.xml` ×2
- Test: `core/designsystem/component/MessengerTest.kt`, `MessengerHostTest.kt`, `StateViewsTest.kt`, `BannerTest.kt`, `feature/example/components/FeedbackScreenTest.kt`

**Interfaces:**
- Produces:
  - `sealed interface UiText { data class Plain(val value: String) : UiText; data class Resource(@StringRes val id: Int, val args: List<Any> = emptyList()) : UiText }` + `@Composable fun UiText.asString(): String`.
  - `enum class MessageVariant { INFO, SUCCESS, WARNING, ERROR }`; `data class UiMessage(val text: UiText, val variant: MessageVariant = MessageVariant.INFO, val actionLabel: UiText? = null, val onAction: (() -> Unit)? = null, val long: Boolean = false)`.
  - `@Singleton class Messenger @Inject constructor() { val messages: Flow<UiMessage>; fun show(message: UiMessage); fun info(text: UiText); fun success(text: UiText); fun warning(text: UiText); fun error(text: UiText) }` — backed by `Channel<UiMessage>(Channel.BUFFERED)` + `receiveAsFlow()`, so each message is delivered once and nothing sent before collection is lost.
  - `val LocalMessenger = staticCompositionLocalOf<Messenger> { error("No Messenger provided") }` — `MainActivity` injects `Messenger` and provides it around `MainShell()`.
  - `@Composable fun MessengerHost(messenger: Messenger, hostState: SnackbarHostState)` — collects `messages` (in a `LaunchedEffect`), dismisses the current snackbar, shows the new one (`SnackbarDuration.Long` if `long` else `Short`), invokes `onAction` on `ActionPerformed`; the shell's `Scaffold` uses `SnackbarHost(hostState) { data -> VariantSnackbar(data) }` where the variant (carried via a custom `SnackbarVisuals`) picks a leading icon and accent: info=`AppThemeTokens.colors.info`, success=`success`, warning=`warning`, error=`colorScheme.error`.
  - `@Composable fun LoadingState(modifier: Modifier = Modifier, message: String? = null)`; `@Composable fun EmptyState(title: String, modifier: Modifier = Modifier, description: String? = null, icon: ImageVector = Icons.Filled.Info, actionLabel: String? = null, onAction: (() -> Unit)? = null)`; `@Composable fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier, title: String = stringResource(R.string.component_error_title))` (retry label `component_retry`); `@Composable fun LoadingOverlay(visible: Boolean, modifier: Modifier = Modifier)` — scrim + spinner that consumes touches when visible.
  - `enum class BannerVariant { INFO, SUCCESS, WARNING, ERROR }`; `@Composable fun Banner(title: String, modifier: Modifier = Modifier, message: String? = null, variant: BannerVariant = BannerVariant.INFO, onDismiss: (() -> Unit)? = null, action: (@Composable () -> Unit)? = null)` — title single line + ellipsis, message wraps, dismiss icon `component_dismiss` ("Dismiss %1$s").
  - `@HiltViewModel class FeedbackViewModel @Inject constructor(private val messenger: Messenger) : ViewModel() { fun saveSucceeded(); fun saveFailed() }` — shows that a ViewModel can raise messages (success / error with "Retry" action).
  - `@Composable fun FeedbackScreen(onBack: () -> Unit, onSaveSucceeded: () -> Unit, onSaveFailed: () -> Unit)` — sections: Messages (buttons for the 4 variants via `LocalMessenger`, one with "Undo" action showing "Restored", and two buttons calling the ViewModel), State views (segmented control switching Loading / Empty / Error / Content in a 200dp box), Loading overlay (button showing it for 1.5 s), Banners (the 4 variants, dismissible).

- [ ] **Step 1: Write failing tests**: `MessengerTest` (plain JVM, `runTest` + Turbine) — `show()` called **before** collecting is received once collection starts (Review Focus 2); two collectors are not needed — one message is received exactly once; `success(text)` produces `variant == SUCCESS`. `MessengerHostTest` — a `Messenger` with a message carrying action "Undo" → snackbar text displayed; clicking "Undo" invokes `onAction`. `StateViewsTest` — `ErrorState` "Retry" calls `onRetry`; `EmptyState` without `actionLabel` has no button; `LoadingOverlay(visible = true)` blocks a click on a button beneath it (button callback not invoked). `BannerTest` — dismiss calls `onDismiss`; with a 150-char title the dismiss icon `assertIsDisplayed()` (Review Focus 5). `FeedbackScreenTest` (Hilt + `MainShell` like `MainShellTest`): open Components → Feedback, click "Success" → success message text displayed.
- [ ] **Step 2: Run** `./gradlew testDebugUnitTest --tests "*Messenger*" --tests "*StateViewsTest" --tests "*BannerTest" --tests "*FeedbackScreenTest"` → FAIL.
- [ ] **Step 3: Implement**, wire the host into the shell and `LocalMessenger` into `MainActivity`, add the Feedback page and entry, switch Display's banners and empty state, drop Overlays' snackbar section (update `OverlaysScreenTest`: remove `snackbar_undo_restores`, which `MessengerHostTest` + `FeedbackScreenTest` now cover).
- [ ] **Step 4: Run** full proof command → PASS.
- [ ] **Step 5: Docs** rows in `components.md` + a "Showing messages" section (from a composable via `LocalMessenger.current`, from a ViewModel by injecting `Messenger`, use `UiText.Resource` so messages follow the app language). **Commit** `feat(designsystem): add Messenger, state views, Banner and Feedback page`

---

### Task 5: Avatar, skeleton, section header, info row

**Files:**
- Create: `core/designsystem/component/Avatar.kt`, `Skeleton.kt`, `SectionHeader.kt`, `InfoRow.kt`
- Modify: `feature/example/components/DisplayScreen.kt` (avatars + skeleton use the components; `initialsOf` moves to `Avatar.kt`), `feature/example/components/InitialsTest.kt` → `core/designsystem/component/InitialsTest.kt`, `docs/architecture/components.md`, `strings.xml` ×2
- Test: `core/designsystem/component/AvatarTest.kt`, `SkeletonTest.kt`, `SectionHeaderTest.kt`, `InfoRowTest.kt`

**Interfaces:**
- Produces:
  - `enum class AvatarSize(val dp: Dp) { SMALL(32.dp), MEDIUM(40.dp), LARGE(48.dp), XLARGE(64.dp) }`; `enum class AvatarStatus { ONLINE, OFFLINE, BUSY, AWAY }` (colors: success, `colorScheme.outline`, `colorScheme.error`, warning).
  - `fun initialsOf(name: String): String` (moved, same behavior).
  - `@Composable fun Avatar(name: String, modifier: Modifier = Modifier, size: AvatarSize = AvatarSize.MEDIUM, shape: Shape = CircleShape, status: AvatarStatus? = null)` — initials on `primaryContainer`; semantics `contentDescription = name` (+ ", online" etc. via `component_status_*` strings).
  - `@Composable fun AvatarGroup(names: List<String>, modifier: Modifier = Modifier, max: Int = 4, size: AvatarSize = AvatarSize.MEDIUM)` — overlapping by 25 % of the size, ring in `colorScheme.surface`, overflow chip "+N".
  - `@Composable fun Skeleton(modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(Radii.sm))` and `@Composable fun SkeletonText(lines: Int = 3, modifier: Modifier = Modifier, lastLineFraction: Float = 0.6f)` — shared pulse (0.4 ↔ 1.0 alpha, 800 ms); semantics `contentDescription = component_loading` on the group.
  - `@Composable fun SectionHeader(title: String, modifier: Modifier = Modifier, actionLabel: String? = null, onAction: (() -> Unit)? = null)` — `titleMedium`, `heading()` semantics, optional `TextButton` at the end.
  - `@Composable fun InfoRow(label: String, value: String, modifier: Modifier = Modifier)` — label `onSurfaceVariant`, value end-aligned, value wraps.

- [ ] **Step 1: Write failing tests**: `AvatarTest` — `Avatar("Ada Lovelace")` shows "AL"; with `status = ONLINE` the node's content description contains "Online"; `AvatarGroup` of 6 names, `max = 4` shows "+2" and exactly 4 initials nodes. `SkeletonTest` — `SkeletonText(lines = 3)` has content description "Loading". `SectionHeaderTest` — node has `heading` semantics; action click calls `onAction`. `InfoRowTest` — label and value both displayed for a 120-char value. Move `InitialsTest` (assertions unchanged).
- [ ] **Step 2: Run** `./gradlew testDebugUnitTest --tests "*AvatarTest" --tests "*SkeletonTest" --tests "*SectionHeaderTest" --tests "*InfoRowTest" --tests "*InitialsTest"` → FAIL.
- [ ] **Step 3: Implement**; switch the Display page's avatar and skeleton sections; add a "Section header & info rows" section to Display using both.
- [ ] **Step 4: Run** full proof command → PASS.
- [ ] **Step 5: Docs** rows in `components.md`; `AGENTS.md` mentions `core/designsystem/component/` and "prefer a component from components.md before writing a new one". **Commit** `feat(designsystem): add Avatar, Skeleton, SectionHeader and InfoRow`
- [ ] **Step 6: Manual check** (if `adb devices` lists a device): switch tabs with nested pages open, back behavior per Review Focus 1, every component page in light/dark and en/id, a message from Feedback while rotating.
