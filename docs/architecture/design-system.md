# Design system

Tokens and theme live in `app/src/main/java/com/primestrider/kotlinstarter/core/designsystem/theme/`.

## Token files

| File | Contents |
| --- | --- |
| `Palette.kt` | Raw colors: `white`, `black`, every scale (`Palette.Gray.c50` ... `c950`, `Primary`, `Taupe`, `Mauve`, `Mist`, `Olive`, semantic `Success`/`Warning`/`Error`/`Info`) and `Palette.scales` for the Colors showcase page. |
| `AppColors.kt` | `AppColors` semantic roles, `LightColors`, `DarkColors`, `LocalAppColors`. |
| `Spacing.kt` | `Spacing.s0` ... `s96` (4dp based, `s0_5` = 2dp). |
| `Radii.kt` | `Radii.none` ... `full` (`DEFAULT` in RN equals `md`). |
| `Type.kt` | `PlusJakartaSans`, `FontSize`, `LineHeight`, `LetterSpacing`, `AppTypography`. |
| `AppTheme.kt` | `AppTheme(darkTheme)` and `AppThemeTokens`. |

## Reading colors

- `AppThemeTokens.colors` gives the full semantic set (`AppColors`), including `success`, `warning` and `info`, which Material 3 has no slot for.
- `MaterialTheme.colorScheme` is what Material components read. Use it when styling with Material roles.

### Form control colours

`theme/ControlColors.kt` derives the colours every form control uses from the semantic roles, so changing the brand colour restyles all of them:

- `controlFill` (= `secondary`): resting fill of fields and unselected controls; it differs from both `background` and `card`, so a field stays visible on a card in dark theme, at rest, focused and in error.
- `controlFocusFill`: `primary` at 12% over `controlFill`, the fill of the field being typed in (over the resting fill, not `background`, so it also stands out on a dark card).
- `controlErrorFill`: `destructive` at 10% over `controlFill`, the fill of a field in error.
- `controlRaised`: the selected segment (white in light theme, a lighter grey in dark theme).
- `ControlShapes` (field radius 16, checkbox 7dp, pill) and `CONTROL_ANIMATION_MS` (180).

Fields never get an outline or glow. Focus is shown by the tint plus a primary label and cursor; the tint alone may stay under 3:1 contrast against the resting fill. That is an accepted trade-off of the chosen look, so do not add an outline back.

Never hardcode a `Color` in a screen. Use `AppThemeTokens.colors`, `MaterialTheme.colorScheme`, or (for showcase pages only) `Palette`.

## Material 3 color mapping

| ColorScheme | AppColors |
| --- | --- |
| primary / onPrimary | primary / primaryForeground |
| background / onBackground | background / foreground |
| surface / onSurface | card / cardForeground |
| surfaceVariant / onSurfaceVariant | secondary / muted |
| secondaryContainer / onSecondaryContainer | secondary / secondaryForeground |
| error / onError | destructive / destructiveForeground |
| outline, outlineVariant | border |
| tertiary / onTertiary | info / white |
| surfaceTint | primary |
| scrim | black |

Every other role is mapped to the palette as well, so stock M3 components never show the M3 baseline purple (`AppTheme.kt`, `toColorScheme`):

| Role | Light | Dark |
| --- | --- | --- |
| primaryContainer / onPrimaryContainer | Primary.c100 / Primary.c900 | Primary.c800 / Primary.c100 |
| inversePrimary | Primary.c300 | Primary.c600 |
| secondary / onSecondary | Gray.c700 / white | Gray.c300 / Gray.c950 |
| tertiaryContainer / onTertiaryContainer | Info.c50 / Info.c700 | Blue.c900 / Blue.c100 |
| errorContainer / onErrorContainer | Error.c50 / Error.c700 | Red.c950 / Red.c200 |
| surfaceDim / surfaceBright | Gray.c100 / white | Gray.c950 / Gray.c800 |
| surfaceContainerLowest / Low / (default) / High / Highest | white / Gray.c50 / Gray.c100 / Gray.c200 / Gray.c300 | Gray.c950 / Gray.c900 / Gray.c900 / Gray.c800 / Gray.c700 |
| inverseSurface / inverseOnSurface | Gray.c800 / Gray.c50 | Gray.c100 / Gray.c900 |

`AppThemeTest` fails if any of these roles falls back to the baseline.

Shapes: small = `Radii.sm`, medium = `Radii.md`, large = `Radii.lg`, extraLarge = `Radii.xl`.

## Typography mapping

| Material 3 | RN variant | Size / line height | Weight |
| --- | --- | --- | --- |
| headlineLarge | h1 | xl3 | ExtraBold, tight spacing |
| headlineMedium | h2 | xl2 | Bold, tight spacing |
| headlineSmall | h3 | xl | SemiBold |
| titleLarge / titleMedium / titleSmall | title | lg / base / sm | SemiBold |
| bodyLarge / bodyMedium | body | base / sm | Normal |
| bodySmall | caption | xs | Normal |
| labelLarge | label | sm | Medium |
| labelMedium / labelSmall | | xs | Medium |
| displayLarge / Medium / Small | | xl6 / xl5 / xl4 | Bold |

All styles use Plus Jakarta Sans (400 to 800).

## Theme preference

- The mode (`SYSTEM`, `LIGHT`, `DARK`) is stored in the `settings` Preferences DataStore under the key `app.theme` (`StorageKeys.themeMode`), via `ThemePreferenceRepository`. Unknown or missing values resolve to `SYSTEM`.
- `AppViewModel.themeMode` is `null` until the first stored value arrives. `MainActivity` keeps the splash screen visible while it is `null`, so the first frame is drawn in the correct theme (no flash of the wrong theme on cold start).
- If reading DataStore throws an `IOException`, the repository falls back to `SYSTEM` instead of crashing.
- System bar icon colors follow the resolved theme: `enableEdgeToEdge(SystemBarStyle.auto(...))` is re-applied whenever it changes.

## Showcase pages

`feature/example/styling/` holds seven pages (Colors, Typography, Spacing, Sizing, Layout, Appearance, Theme) built on `ExamplePageScaffold` (`feature/example/ExamplePageScaffold.kt`). They read tokens from `AppThemeTokens`, `MaterialTheme`, `Spacing`, `Radii`, `FontSize`, and `Palette` (swatches only). The Theme page reuses `ThemeModeSelector`, the same System / Light / Dark control as Settings.

RN's `helpers` page (`view()`, `space()`, `gridCol()`) is intentionally not ported: those are React Native style-object utilities with no Compose counterpart (use `Modifier`, `Arrangement`, and `LazyVerticalGrid` directly).

## Using components

Screens call Jetpack Compose / Material 3 composables directly (`Button`, `AlertDialog`, `ModalBottomSheet`, `SnackbarHost`, …), except form controls, which always come from `core/designsystem/component` (see [components.md](components.md)). Because every `ColorScheme` role is mapped to a token (see above), stock components already use the brand colors in light and dark mode; pass `colors = …Defaults.xxxColors(...)` only for a deliberate variant, such as a destructive button using `colorScheme.error`.

Examples live in `feature/example/components/`, one page per group:

| Page | File | Shows |
| --- | --- | --- |
| Actions | `ActionsScreen.kt` | Button variants, loading button, icon buttons, FABs, chips |
| Inputs | `InputsScreen.kt` | Filled text fields in every state (hint, error, disabled, counter), keyboard types, phone `+62` prefix, password toggle, checkbox / tri-state, radio, switch, select, slider and range slider, single and multi segmented choice, tabs |
| Display | `DisplayScreen.kt` | Cards, list items, badges, dividers, progress, plus patterns M3 lacks: initials avatar, skeleton, empty state, banner alert |
| Overlays | `OverlaysScreen.kt` | Dialogs (incl. non-dismissable), bottom sheet, snackbars with undo, dropdown menu, tooltips |

`feature/example/form/` shows a validated form: pure `validate(FormValues)` rules, a `FormExampleViewModel` that validates on submit and re-validates on change, and a stateless `FormExampleScreen`.

Conventions the examples follow:

- Make the whole row the touch target for checkboxes, radios and switches (`Modifier.toggleable` / `selectable` with a `Role`) and pass `null` as the control's own callback.
- Give icon-only buttons a translated `contentDescription`.
- Use `maxLines` + `TextOverflow.Ellipsis` for single-line rows so trailing content stays on screen.
- Disable a button while its action runs, so a double tap runs it once.
- Material icons come from `material-icons-core` only; the extended set is not a dependency.

To add a component example: put it in the page that matches its group (or add a page: route in `feature/catalog/api/src/main/java/com/primestrider/kotlinstarter/feature/example/CatalogRoutes.kt`, `composable<…>` in `catalogComponentsDestinations`, entry in `componentEntries` in `ExampleCatalog.kt`), add its strings to the catalog module's `values/` and `values-in/`, and cover its behavior in a Robolectric test next to the page's existing test.
