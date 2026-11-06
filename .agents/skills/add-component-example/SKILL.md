---
name: add-component-example
description: Use when showing a UI component on the Components tab or adding a reusable component to the design system - which page it belongs to, DemoSection, design-system form controls, strings, Robolectric and Roborazzi tests, and the components.md catalog.
---

# Add a component example (or a reusable component)

Component examples live in the catalog feature (`feature/catalog/impl`, package `…feature.example.components`); the
reusable components themselves live in `core:designsystem` (`…core.designsystem.component`). Check
`docs/architecture/components.md` before writing a new composable: reuse or extend what is there.

## An example on an existing page

1. Pick the page by group: `ActionsScreen.kt`, `InputsScreen.kt`, `DisplayScreen.kt`, `OverlaysScreen.kt`,
   `FeedbackScreen.kt` (all in `feature/catalog/impl/.../feature/example/components/`).
2. Add a `DemoSection(R.string.components_section_<x>) { … }` (from `core:ui`) with the live example. Keep user-chosen
   state in `rememberSaveable` (or `rememberSaveableStateList`) so it survives rotation.
3. Form controls (text fields, checkbox, radio, switch, segmented, slider) only from `core:designsystem`:
   `AppTextField`, `AppCheckbox`, `AppRadioButton`, `AppSwitch`, `SegmentedChoice`, `LabeledSlider`, … A stock
   Material control fails `NoRawMaterialControlsTest`.
4. Strings `components_…` in the catalog module's `values/` and `values-in/` (see `add-translation`).
5. Test next to the page's existing test (e.g. `InputsScreenTest` in `feature/catalog/impl/src/test/…`): render the page
   in `AppTheme`, scroll to the section, assert behaviour and semantics (roles, selected/toggle state, content
   descriptions).

## A new page

Route in `feature/catalog/api/src/main/java/com/primestrider/kotlinstarter/feature/example/CatalogRoutes.kt`,
`composable<…>` in `catalogComponentsDestinations` (`CatalogNavigation.kt`), an `ExampleEntry` in `componentEntries`
(`ExampleCatalog.kt`) with title/subtitle strings, a page built on `ExamplePageScaffold`, and its test.

## A new reusable component

1. Add it to `core/designsystem/src/main/java/com/primestrider/kotlinstarter/core/designsystem/component/`: stateless,
   `modifier: Modifier = Modifier` first optional parameter, colours from `LocalAppColors` / `MaterialTheme`
   (control colours: `ControlColors.kt`), strings with the `component_` prefix, 48dp touch targets, semantics for
   TalkBack.
2. A Robolectric test in `core/designsystem/src/test/.../component/`; pixel checks need
   `@GraphicsMode(GraphicsMode.Mode.NATIVE)` in their own class (`TextFieldLookTest`).
3. Add it to `ControlsScreenshotTest` if it is a control; reference images are recorded by the CI job only (never
   locally, see `docs/architecture/testing.md`).
4. List it in `docs/architecture/components.md` and show it on a Components page.

## Checklist

- [ ] `./gradlew test ktlintCheck lint assembleDevDebug` green.
- [ ] Light and dark theme checked (both are in the screenshot test).
- [ ] The component or page file ends with a private `@ThemePreviews` preview wrapped in `PreviewTheme` (`PreviewCoverageTest`).
