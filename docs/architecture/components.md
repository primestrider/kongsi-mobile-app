# Reusable components

`core/designsystem/component/` holds the components every feature can use. They are thin, stateless wrappers over Material 3: state and callbacks go in, UI comes out. Prefer one of these before writing a new composable; when a pattern repeats in two features, add it here.

Rules:

- Package `com.primestrider.kotlinstarter.core.designsystem.component`; never import `feature/*` or `app/*`.
- `modifier: Modifier = Modifier` is the first optional parameter.
- Colors only from `MaterialTheme.colorScheme` / `AppThemeTokens.colors`.
- Strings use the `component_` prefix, in `values/` and `values-in/`.
- Icon-only controls have a `contentDescription`; touch targets are at least 48dp.
- Form controls (text fields, checkbox, radio, switch, segmented, slider) always come from here, never from Material directly; `NoRawMaterialControlsTest` fails the build if a screen imports a stock Material control.
- Each component has a Robolectric test in `app/src/test/.../core/designsystem/component/`.

| Component | File | Use it for |
| --- | --- | --- |
| `AppScaffold`, `AppTopBar` | `AppScaffold.kt` | Every screen: title, optional back arrow (`onBack = null` hides it), actions, snackbar/FAB slots |
| `AppNavigationBar`, `NavBarItem` | `AppNavigationBar.kt` | Bottom navigation for top-level destinations, with optional count badge ("99+") |
| `AppNavigationSuite` | `AppNavigationSuite.kt` | Top-level navigation that follows the window: `AppNavigationBar` on compact width, a `NavigationRail` from medium width |
| `AppButton`, `ButtonVariant` | `AppButton.kt` | Any button: `PRIMARY`, `SECONDARY` (tonal), `OUTLINED`, `TEXT`, `DESTRUCTIVE`; `loading = true` shows a spinner, reports disabled and ignores clicks (no double submit); `fullWidth`, `leadingIcon` |
| `AppTextField` | `TextFields.kt` | Full-width filled text input: no border, radius 16, floating label aligned with the value, tinted with `primary` while focused and with `destructive` in error. Placeholder, `errorText` (wins over `supportingText`), `maxLength` (counted in characters, emoji-safe) with an "n/max" counter, keyboard type, IME action, and `prefix` (a small chip before the value, e.g. `+62`). No decorative trailing icons |
| `PasswordTextField` | `TextFields.kt` | Password input with its own show/hide toggle |
| `SearchField` | `TextFields.kt` | Filled search box (placeholder, no label) with a clear button once there is a query |
| `SelectField`, `SelectOption` | `SelectField.kt` | Pick one value from a dropdown, same filled look as `AppTextField`; disabled options are shown but not selectable |
| `ConfirmDialog` | `Dialogs.kt` | Confirmation modal: `destructive` styles the confirm action, `dismissible = false` ignores back/outside taps, `dismissLabel = null` hides the dismiss button |
| `AppBottomSheet` | `BottomSheets.kt` | Modal bottom sheet with a title and any content; `dismissible = false` keeps it open on back, swipe and outside taps |
| `OptionsBottomSheet` | `BottomSheets.kt` | Single choice from a sheet (sort, filter); a tap hides the sheet, reports `onSelect` once, then `onDismiss` |
| `Messenger`, `UiMessage`, `UiText`, `MessageVariant` | `Messenger.kt` | App-wide snackbar messages (info / success / warning / error, optional action) from anywhere, see below |
| `MessengerHost` | `Messenger.kt` | Renders the messages; already placed in `MainShell`'s `snackbarHost` |
| `LoadingState`, `EmptyState`, `ErrorState` | `StateViews.kt` | Whole-area states of a screen or section: spinner, nothing-yet with optional action, failure with Retry |
| `LoadingOverlay` | `StateViews.kt` | Blocking spinner over its parent `Box` while a critical action runs; swallows touches and is announced only as "Loading" |
| `Banner`, `BannerVariant` | `Banner.kt` | Inline alert in the page (info / success / warning / error) with optional action and dismiss |
| `Avatar`, `AvatarGroup`, `AvatarSize`, `AvatarStatus`, `initialsOf` | `Avatar.kt` | Avatar (sizes 32/40/48/64dp, circle or custom shape, presence dot announced to TalkBack). Pass `imageUrl` to load a picture with Coil (`SubcomposeAsyncImage`, clipped to the shape); the initials show while it loads and if it fails. Overlapping group with "+N" overflow (initials only) |
| `Skeleton`, `SkeletonText` | `Skeleton.kt` | Pulsing placeholders while content loads; `SkeletonText` is announced as "Loading" |
| `SectionHeader` | `SectionHeader.kt` | Section title marked as an accessibility heading, with an optional "See all" style action |
| `InfoRow` | `InfoRow.kt` | Label / value pairs (order id, status, address); long values wrap |
| `AppCheckbox` | `Toggles.kt` | Borderless 22dp checkbox, two-state (`checked`) or tri-state (`ToggleableState`, indeterminate shows a dash) |
| `AppRadioButton` | `Toggles.kt` | Borderless 22dp radio |
| `AppSwitch` | `Toggles.kt` | iOS-style pill switch with a sliding white thumb |
| `SegmentedChoice` | `ChoiceControls.kt` | One of up to four short options: neutral track with a raised pill that slides to the selection; more options use `SelectField` |
| `MultiSegmentedChoice` | `ChoiceControls.kt` | Several of up to four options on the same track, each selected one raised |
| `SwitchRow` | `ChoiceControls.kt` | Label plus `AppSwitch` where the whole row toggles and reports the switch role |
| `LabeledSlider`, `LabeledRangeSlider` | `ChoiceControls.kt` | Thin slider (6dp track, white thumb) with its label and a value readout (`valueText`); the range version has two thumbs |

Checkbox, radio and switch follow Material's contract: pass a callback and the control is its own 48dp target with the right role; pass `null` inside a `toggleable` / `selectable` row and the row owns the click, so TalkBack announces it once.
| `InfoCard` | `InfoCard.kt` | Outlined card of `InfoRow`s from (label, value) pairs; null or blank values show an em dash |

## Previews

Every screen file (a public `fun XxxScreen(`) and every design-system component file ends with a private preview:

```kotlin
@ThemePreviews
@Composable
private fun TodosScreenPreview() {
    PreviewTheme { TodosScreen(state = sampleState, onAdd = {}, /* ... */) }
}
```

`@ThemePreviews` renders it twice (light and dark); `PreviewTheme` applies `AppTheme` for that mode on a surface with a `Messenger`. Screens that need a ViewModel or hardware preview their stateless content (`MusicContent`, `AppUpdateContent`). `PreviewCoverageTest` fails the build when a screen or component file has no preview.

## Showing messages

- From a composable: `val messenger = LocalMessenger.current` then `messenger.success(UiText.Resource(R.string.saved))`.
- From a ViewModel: inject `Messenger` (it is a Hilt `@Singleton`) and call the same methods; see `FeedbackViewModel`.
- Prefer `UiText.Resource` so the text follows the app language; `UiText.Plain` is for text that is already final (e.g. a server message).
- `UiMessage(text, variant, actionLabel, onAction, long)` adds an action ("Undo", "Retry"); `onAction` runs only if the user taps it.
- Messages are queued in a buffered channel (up to 64): one sent while no screen is collecting (rotation, before the UI starts) is shown once the host collects, and several are shown one after another in order. Keep `onAction` lambdas short-lived; they live until the message is shown.
- `MainActivity` provides `LocalMessenger`; tests that replace the activity content must provide it too (see `MainShellTest`).

## Components tab pages

The Components tab (`feature/catalog/impl/.../feature/example/components/`) shows Material 3 and design-system components working. Each page is listed in `componentEntries` (`ExampleCatalog.kt`) and registered in `catalogComponentsDestinations` (`CatalogNavigation.kt`). New examples pass `usage`, `api` and `file` to `DemoSection`, so each one says when to use it, which API it is, and which file holds it.

| Page | File | Shows |
| --- | --- | --- |
| Actions | `ActionsScreen.kt` | Buttons, icon buttons, floating action buttons, chips |
| Inputs | `InputsScreen.kt` | Text fields, selection controls, select, sliders, tabs |
| Display | `DisplayScreen.kt`, `RefreshDismissSections.kt` | Cards, list items, badges, progress, avatars, skeletons, banners, `SwipeToDismissBox` (swipe towards the start, a delete button per row, Undo) and `PullToRefreshBox` (with a "Refresh list" button as the non-gesture alternative) |
| Overlays | `OverlaysScreen.kt` | Dialogs, bottom sheets, menus, tooltips |
| Feedback | `FeedbackScreen.kt` | App-wide messages, loading, empty and error states |
| Pickers | `PickersScreen.kt` | `DatePicker` inline, `DatePickerDialog` (Cancel keeps the previous value), `DateRangePicker`, `TimePickerDialog` + `TimePicker`, `TimeInput`. Picker millis are midnight UTC, so dates are formatted in UTC |
| App bars & navigation | `AppBarsScreen.kt`, `ComponentDemos.kt` | Full-screen demos opened through `ComponentDemoRoute(id)` (registry `componentDemos`): `TopAppBar` (pinned), `CenterAlignedTopAppBar` (enter always), `MediumTopAppBar` and `LargeTopAppBar` (exit until collapsed), `BottomAppBar` with a FAB, `ModalNavigationDrawer`, `DismissibleNavigationDrawer`, `SearchBar` + `ExpandedFullScreenSearchBar` and `ExpandedDockedSearchBar` with filtered suggestions (a tap fills the query and collapses), `BottomSheetScaffold` (peek, swipe or Expand sheet). An unknown id goes back |
| Carousel & grids | `CarouselScreen.kt` | `HorizontalMultiBrowseCarousel`, `HorizontalUncontainedCarousel`, `HorizontalCenteredHeroCarousel` (items clipped with `maskClip`), `LazyVerticalStaggeredGrid` and `LazyHorizontalGrid` (fixed height inside the scrolling page; `LazyVerticalGrid` is on the Styling > Layout page) |

**TODO: Expressive page.** `ButtonGroup`, `SplitButton`, `LoadingIndicator`, `FloatingToolbar`, `FloatingActionButtonMenu` and the wavy progress indicators ship only in Material 3 1.5, while the Compose BOM `2026.09.00` still resolves Material 3 1.4.0. Build this page once a Compose BOM ships Material 3 1.5 stable; do not pin Material 3 outside the BOM.
