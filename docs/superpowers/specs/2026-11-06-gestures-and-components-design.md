# Gesture Examples and Fuller Compose Components — Design

Date: 2026-11-06. Branch: `feat/gesture-handler`.

## Intent

The starter's example app is used two ways: as the owner's own **reference while coding** (find the pattern,
jump to the file, copy it) and as a **showcase** for other people (open the app, try things, be convinced).
Two gaps hurt both uses today:

1. **Gestures.** The only gesture code is `SwipeToDismissBox`, a `HorizontalPager`, `PredictiveBackHandler` and
   `PullToRefreshBox`. There is no `draggable`, `anchoredDraggable`, `transformable`, `detectTapGestures`,
   `detectDragGestures`, `combinedClickable`, raw pointer handling or nested scroll anywhere.
2. **Components.** The Components tab covers most basic Material 3 components, but several from
   <https://developer.android.com/develop/ui/compose/components> are missing entirely: date/time pickers,
   search bar, bottom app bar, navigation drawer, top app bar variants, carousel and the M3 Expressive set.

Success: for any common gesture or any component on that docs page, the owner opens the app, finds it in under a
minute, feels it working, reads which API makes it, and knows which file to open.

## Goals

### Part 1 — Gestures (new feature module)

A new example feature `:feature:gestures:api` + `:feature:gestures:impl`, built like `:feature:animation`
(reference: `feature/animation/impl/.../animation/AnimationNavigation.kt`, `AnimationPages.kt`, `AnimExample.kt`).
It contributes a Home section **"Gestures"** with `order = 35` (Home order becomes Features, Device APIs,
Animations, Gestures, Styling) and six pages:

1. **Tap** — `clickable` with ripple, `combinedClickable` (click / long-press / double-tap with a visible event
   log), `detectTapGestures` (`onTap`, `onDoubleTap`, `onLongPress`, `onPress` with press-and-release) marking
   where the finger touched.
2. **Drag** — `draggable` on one axis (e.g. a custom slider handle), free 2D drag with `detectDragGestures`
   (an object you can move around a bounded area), `anchoredDraggable` snapping between anchors (e.g. a
   three-state sheet or a custom toggle), and a list reordered by long-press-then-drag.
3. **Transform** — pinch to zoom, two-finger pan and rotate on an image-like surface (`transformable` /
   `detectTransformGestures`), double-tap to zoom in/out, pan clamped to the content bounds, reset button.
4. **Swipe & fling** — swipe a row to reveal actions (`anchoredDraggable`, not `SwipeToDismissBox`, which the
   Components tab shows), a stack of cards swiped left/right off the stack with rotation, and fling with decay
   (`VelocityTracker` + `Animatable.animateDecay`) of an object that slides and slows down.
5. **Scroll** — a custom `scrollable` (e.g. a dial/wheel), `nestedScroll` with a `NestedScrollConnection` for a
   collapsing header, and overscroll behaviour.
6. **Advanced pointer** — a multi-touch visualiser (every active pointer drawn with its id, via
   `awaitPointerEventScope` / `awaitEachGesture`), a finger-drawing canvas with clear, and a gesture-conflict demo
   that shows which handler consumes an event (parent vs child, `consume()`, `requireUnconsumed`).

### Part 2 — Fuller Components tab (catalog feature)

New pages on the Components tab (`feature/catalog/impl/.../feature/example/components/`), each a list of
`DemoSection`s:

- **Pickers** — `DatePicker`, `DateRangePicker`, `TimePicker` (and `TimeInput`), each inline and inside its dialog
  form, showing the chosen value.
- **Navigation & app bars** — `TopAppBar` variants (small, center-aligned, medium, large) with a scroll behaviour
  you can feel, `BottomAppBar` with actions and FAB, `ModalNavigationDrawer` and `DismissibleNavigationDrawer`,
  `SearchBar` (docked and expanding, with suggestions).
- **Carousel** — `HorizontalMultiBrowseCarousel`, `HorizontalUncontainedCarousel`, and a hero layout.
- **Expressive — deferred (owner's decision).** `ButtonGroup`, `SplitButton`, `LoadingIndicator`,
  `FloatingToolbar`, `FloatingActionButtonMenu` and wavy progress ship only in Material 3 1.5.0 (rc01 today); the
  Compose BOM `2026.09.00` resolves Material 3 1.4.0. Do **not** pin Material 3 outside the BOM. Instead add a TODO
  entry (in `docs/architecture/components.md`) to build an Expressive page once a BOM ships Material 3 1.5 stable.

Also add **`PullToRefreshBox`** and **`SwipeToDismissBox`** demos to the Components tab so every component on the
docs page is findable from one place (they already exist inside other features, which is not where a developer
looks for a component).

### For both parts — the reference affordance

Every example shows: a title, one sentence on when to use it, the API name(s) it uses (e.g.
`anchoredDraggable`, `TimePicker`), and the source file it lives in (file name, e.g. `DragScreen.kt`). Keep each
page in one focused file (plus small helpers next to it) so the file named is the file to open.

## Non-goals

- No new third-party libraries. Compose Foundation, Material 3 (versions from the Compose BOM) and what is
  already in `gradle/libs.versions.toml` are enough.
- No network images; visuals are colour blocks, gradients and the app's icons (offline, deterministic tests).
- No change to the app's own navigation, shell or existing feature behaviour.
- No new reusable design-system components unless a page needs a form control that only exists as a raw
  Material control (see Constraints). Picker/app-bar/carousel/expressive demos use Material 3 directly.
- No in-app source-code viewer; the file name is enough.

## Anti-goals

- A demo that moves but teaches nothing: every example names its API and file.
- Gesture demos that fight the page scroll (e.g. a drag area inside a vertically scrolling page that the page
  steals, or vice versa) — gesture surfaces must work on a phone emulator without hunting for the right angle.
- Janky motion: transforms via `graphicsLayer`, no recomposition per pointer move where a lambda modifier will do.
- Copying a specific app's look, name or content (e.g. the swipe card stack must not look like a dating app).

## Constraints (from the repo)

- Follow `AGENTS.md` and the skills `add-feature`, `add-screen-route`, `add-component-example`,
  `add-translation`.
- Routes are `@Serializable` in the feature's `:api`, package `com.primestrider.kotlinstarter.feature.example`
  (`RouteNamesTest`).
- Pages are built on `ExamplePageScaffold` / `DemoSection` from `core:ui`; user-chosen state in `rememberSaveable`.
- Form controls (checkbox, radio, switch, slider, segmented, text fields) only from `core:designsystem`
  (`NoRawMaterialControlsTest`). A `SearchBar`'s input field is a Material component, not `TextField`, so it is
  allowed; anything that would import a banned control must go through a design-system wrapper.
- Every screen file ends with a private light/dark `@ThemePreviews` preview in `PreviewTheme`
  (`PreviewCoverageTest`).
- Strings in `values/` (en) and `values-in/` (id), names unique across modules (`TranslationCompletenessTest`).
- Gestures is an **example** feature: wired in `app/build.gradle.kts` and `AppNavHost.kt` inside
  `// region example:gestures` blocks, added to `scripts/remove-examples.sh` (`EXAMPLES` list and the package
  pattern), and documented in `docs/examples.md`.
- New components listed in `docs/architecture/components.md`.
- Robolectric tests at `@Config(sdk = [36])` for every new page: each page renders, and the key behaviour of each
  demo is asserted (gesture demos via `performTouchInput` — tap, double-tap, long-press, swipe, pinch where
  Robolectric supports it).
- Proof: `gradlew.bat test ktlintCheck lint assembleDevDebug` green. The first `lint` of the new module creates
  its `lint-baseline.xml`; run again and commit it.

## Decisions made after the builder check (author's calls, marked)

- **Full-screen demos (author's call).** `ExamplePageScaffold` always scrolls vertically. Demos that need their own
  scaffold or scroll (TopAppBar scroll behaviours, BottomAppBar + FAB, navigation drawers, expanding SearchBar,
  `nestedScroll` collapsing header, overscroll, custom `scrollable` dial) each open a **full-screen sub-route**
  from a button in their `DemoSection` (own `@Serializable` route, own scaffold). Gesture surfaces that stay
  inline (drag area, transform surface, drawing canvas, multi-touch visualiser) are **fixed-height boxes** that
  consume their pointer input so the page scroll never steals them; a sub-route is fine where that is not enough.
- **Reference affordance scope (author's call).** API + file label is required on **new** pages only. It is added
  as optional parameters on `DemoSection` in `core:ui`, so existing pages can adopt it later; retrofitting
  existing pages is out of scope.
- **Coverage of the docs page (author's call).** Part 2's list is a minimum. The builder first audits
  <https://developer.android.com/develop/ui/compose/components> against the existing Components pages and adds any
  other missing component (e.g. `BottomSheetScaffold`) to the most fitting new or existing page. Components that
  exist only behind a design-system wrapper (e.g. segmented buttons via `SegmentedChoice`) count as covered.

## Left to the builder

- Exact page and file names, string wording, section order within pages, and which concrete visuals each demo uses.
- How the API name and file name are shown (extend `DemoSection` in `core:ui` with optional parameters, or a
  small shared header in each feature) — prefer one shared place over copying.
- The Home section icon (an icon already available; add `material-icons-extended` only if it is already a
  dependency).
- Which existing Components page gets `PullToRefreshBox` and `SwipeToDismissBox`, or a small new page.
- Whether the swipe card stack and reorderable list need a tiny ViewModel or plain saveable state.
- Gesture behaviours Robolectric cannot simulate reliably (e.g. multi-finger rotate): test the state math as
  plain unit tests instead and note it.
