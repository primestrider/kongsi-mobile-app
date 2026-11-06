# Gestures and Fuller Components Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a Gestures example feature (six pages) and the missing Material 3 components (pickers, app bars,
drawers, search, bottom-sheet scaffold, carousel, grids, pull-to-refresh, swipe-to-dismiss) to the Components tab.

**Architecture:** A new `:feature:gestures:{api,impl}` example module is built like `:feature:animation`: routes in
`:api`, a page registry `gesturePages` feeding Home through a Hilt `@IntoSet` section. New Components pages live in
the catalog feature. Full-screen component demos share one route, `ComponentDemoRoute(id)`, backed by a registry
`componentDemos`, the way `SdkDemoRoute`/`sdkDemos` works. `DemoSection` in `core:ui` gains optional
usage/API/file labels used by every new example.

**Tech Stack:** Kotlin 2.4, Jetpack Compose (BOM 2026.09.00, Material 3 1.4.0), Navigation Compose 2.10 type-safe
routes, Hilt multibindings, Robolectric 4.17 (`@Config(sdk = [36])`), Compose UI test.

**Spec:** `docs/superpowers/specs/2026-11-06-gestures-and-components-design.md`

## Global Constraints

- No new dependencies, and no Material 3 version pinned outside the Compose BOM. The Expressive page is deferred: add a TODO only.
- Routes are `@Serializable`, package `com.primestrider.kotlinstarter.feature.example`. Every new route is added to `RouteNamesTest`.
- Pages use `ExamplePageScaffold`/`DemoSection` (`core:ui`). User-chosen state goes in `rememberSaveable`.
- Form controls come only from `core:designsystem` (`NoRawMaterialControlsTest`): no `TextField`, `Switch`, `Slider`, `Checkbox`, `RadioButton` or `SegmentedButton` imports, and no `androidx.compose.material3.*` wildcard.
- Every file with a public `fun XxxScreen(` ends with a private `@ThemePreviews` preview wrapped in `PreviewTheme` (`PreviewCoverageTest`).
- Strings go in `values/` (en) and `values-in/` (id). Names are unique across modules: prefix `gesture_` in the gestures module, `components_` / `example_components_` in catalog.
- Gestures is an example: `// region example:gestures` blocks in `app/build.gradle.kts` and `AppNavHost.kt`, and the module is listed in `scripts/remove-examples.sh`.
- Inline gesture surfaces are fixed-height boxes that consume their pointer input, so the page's `verticalScroll` never steals the gesture.
- Every new example passes `usage`, `api` and `file` to `DemoSection` (`file` = the source file name, e.g. `"DragScreen.kt"`).
- Motion uses `graphicsLayer`/lambda modifiers (`offset { }`), so pointer moves don't trigger recomposition.
- Visuals are colour blocks, gradients and core icons only: no network images, and no look copied from a specific app.
- Proof command: `gradlew.bat test ktlintCheck lint assembleDevDebug` (Windows; `./gradlew` elsewhere).

## Review Focus

- **Gesture inside the scrolling page:** a vertical drag starting on a drag, transform, canvas or dial surface must move that surface, not the page. Tasks 3–7 each assert that a vertical `swipeUp` on the surface changes the demo state.
- **Rotation / process death:** the zoom level, drawn strokes, the card stack position and the reorder order survive `StateRestorationTester` (Tasks 4–7).
- **Pan clamp at any zoom:** at scale 1 the content can't be panned away, and at max zoom pan stops at the edge (`clampPan` unit tests, Task 4).
- **Accessibility alternative:** every gesture-only action (reveal actions, dismiss card, reorder, zoom) also has a button or semantics custom action, so TalkBack users can reach it (asserted in Tasks 3, 4, 5).
- **Picker dialog cancel:** cancelling a date or time dialog keeps the previous value (Task 8).

---

### Task 1: Reference labels on `DemoSection`

**Files:**
- Modify: `core/ui/src/main/java/com/primestrider/kotlinstarter/feature/example/ExamplePageScaffold.kt`
- Test: `core/ui/src/test/java/com/primestrider/kotlinstarter/feature/example/ExamplePageScaffoldTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  @Composable fun DemoSection(
      @StringRes title: Int,
      @StringRes usage: Int? = null,
      api: String? = null,
      file: String? = null,
      content: @Composable ColumnScope.() -> Unit,
  )
  ```
  - `usage` is shown as `bodySmall`, `onSurfaceVariant`.
  - `api` is shown as `labelSmall` monospace in `primary`.
  - `file` is shown as `labelSmall` monospace in `onSurfaceVariant`, with the text `"File: $file"`. Use a `core:ui` string `example_file_label` = `"File: %1$s"` / id `"Berkas: %1$s"`.
  - Existing callers compile unchanged.

- [ ] **Step 1: Write the failing test** `demo_section_shows_usage_api_and_file`. Render `DemoSection(R.string.crash_title, usage = R.string.crash_title, api = "anchoredDraggable", file = "DragScreen.kt") { Text("Body") }` and assert that the nodes `"anchoredDraggable"` and `"File: DragScreen.kt"` exist. Also assert that a `DemoSection` without the labels shows no `"File:"` text (`onNodeWithText("File:", substring = true).assertDoesNotExist()`).
- [ ] **Step 2: Run the test and confirm it fails.** Run: `gradlew.bat :core:ui:testDebugUnitTest --tests "*ExamplePageScaffoldTest"`. Expected: a compilation failure (no `usage` parameter).
- [ ] **Step 3: Implement the parameters** and add the string in both locales.
- [ ] **Step 4: Run the test and confirm it passes.** Same command. Expected: PASS.
- [ ] **Step 5: Commit** `feat(ui): let DemoSection name the API and source file of an example`

### Task 2: Gestures module and the Tap page

**Files:**
- Create: `feature/gestures/api/build.gradle.kts` (`id("kotlinstarter.feature.api")`, namespace `com.primestrider.kotlinstarter.feature.gestures.api`)
- Create: `feature/gestures/api/src/main/java/com/primestrider/kotlinstarter/feature/example/GestureRoutes.kt`
- Create: `feature/gestures/impl/build.gradle.kts`. Copy `feature/animation/impl/build.gradle.kts`, with namespace `…feature.gestures.impl` and `api(projects.feature.gestures.api)`.
- Create, in `feature/gestures/impl/src/main/java/com/primestrider/kotlinstarter/feature/example/gestures/`:
  - `GesturePages.kt`
  - `GesturesNavigation.kt`
  - `TapScreen.kt`
- Create: `feature/gestures/impl/src/main/res/values/strings.xml` and `values-in/strings.xml`
- Create: `feature/gestures/impl/lint-baseline.xml`. It is generated by the first `lint` run; run lint again afterwards.
- Modify: `settings.gradle.kts`. Add two `include` lines after animation's.
- Modify: `app/build.gradle.kts`. Add a `// region example:gestures` block after `example:animation`.
- Modify: `app/src/main/java/com/primestrider/kotlinstarter/app/navigation/AppNavHost.kt`. Add a `// region example:gestures` block calling `gestureDestinations(navController)` after the animation block in `homeGraph`.
- Modify: `feature/catalog/api/src/main/java/com/primestrider/kotlinstarter/feature/catalog/api/Catalog.kt`. Add `const val GESTURES = "gestures"`.
- Modify: `scripts/remove-examples.sh`. Add `gestures` to `EXAMPLES` and to the `feature\.example\.(…)` alternation.
- Modify, in `app/src/test/java/com/primestrider/kotlinstarter/`:
  - `feature/example/AppHomeSections.kt`
  - `feature/example/HomeSectionsOrderTest.kt`
  - `architecture/RouteNamesTest.kt`
- Modify: `docs/examples.md`. Add a "The Gestures section" chapter after "The Animations section", plus "Adding a gesture page" and "Deleting the Gestures section", mirroring the animation chapter.
- Test, in `feature/gestures/impl/src/test/java/com/primestrider/kotlinstarter/feature/example/gestures/`:
  - `GesturePagesTest.kt`
  - `TapScreenTest.kt`

**Interfaces:**
- Produces:
  - Routes: `@Serializable data object GestureTapRoute`, `GestureDragRoute`, `GestureTransformRoute`, `GestureSwipeRoute`, `GestureScrollRoute`, `GesturePointerRoute`. Declare all six now, so Tasks 3–7 only add pages.
  - `data class GesturePage(val route: Any, @StringRes val title: Int, @StringRes val subtitle: Int)`
  - `val gesturePages: List<GesturePage>`
  - `fun gestureEntries(): List<CatalogEntry>`
  - `fun NavGraphBuilder.gestureDestinations(navController: NavController)`, which uses `val back = { navController.popBackStack() }`.
  - `object GesturesCatalogModule`, with `section()` (order `35`, icon `Icons.Filled.ThumbUp`, strings `gesture_home_title` = "Gestures" / "Gestur") and `entries()`.
  - `@Composable fun TapScreen(onBack: () -> Unit)`
- Page order in `gesturePages`: Tap, Drag, Transform, Swipe & fling, Scroll, Advanced pointer. Each later task appends its entry and `composable<…>`.

- [ ] **Step 1: Write the failing tests.**
  - `GesturePagesTest`: copy `AnimationPagesTest`, using `gestureEntries`/`gesturePages`.
  - `HomeSectionsOrderTest.home_sections_are_in_order` expects `listOf("Features", "Device APIs", "Animations", "Gestures", "Styling")`.
  - `RouteNamesTest`: add the six gesture serializers, and set the count to `32`.
  - `TapScreenTest`, with `show()` rendering `TapScreen(onBack = {})` in `AppTheme(false)`:
    - `combined_clickable_logs_click_long_press_and_double_tap`: `performClick()`, `performTouchInput { longClick() }` and `performTouchInput { doubleClick() }` on tag `"combinedTarget"`. Assert the log texts `"Click"`, `"Long press"` and `"Double tap"` exist.
    - `tap_marks_the_touch_position`: `performTouchInput { click(center) }` on tag `"tapArea"`. Assert a node with tag `"tapMark"` exists.
    - `long_press_target_has_a_custom_action`: assert tag `"combinedTarget"` has `SemanticsActions.OnLongClick`.
- [ ] **Step 2: Run the tests and confirm they fail.** Run: `gradlew.bat :app:testDevDebugUnitTest --tests "*HomeSectionsOrderTest" --tests "*RouteNamesTest"`. Expected: a compile failure or FAIL.
- [ ] **Step 3: Implement the module, wiring, registry and Tap page.** `TapScreen` has three `DemoSection`s, each with `usage`, `api` and `file = "TapScreen.kt"`:
  - `clickable` with ripple, plus a tap counter.
  - `combinedClickable`: an event log of the last 5 events, saveable.
  - `pointerInput { detectTapGestures(onTap, onDoubleTap, onLongPress, onPress = { tryAwaitRelease() }) }`: draws a dot at the last position, and shows the pressed state while held.
  - Add `AppHomeSections` with `+ GesturesCatalogModule.section()` and `+ GesturesCatalogModule.entries()`, and add `implementation(projects.feature.gestures.impl)` to the app test classpath if it isn't already covered.
- [ ] **Step 4: Run the tests and confirm they pass.** Run: `gradlew.bat :feature:gestures:impl:testDebugUnitTest :app:testDevDebugUnitTest`. Expected: PASS. Then run `gradlew.bat :feature:gestures:impl:lint` twice: the first run creates the baseline, the second must pass.
- [ ] **Step 5: Commit** `feat(gestures): add the Gestures example section with a Tap page`

### Task 3: Drag page

**Files:**
- Create: `feature/gestures/impl/src/main/java/com/primestrider/kotlinstarter/feature/example/gestures/DragScreen.kt`
- Create: `feature/gestures/impl/src/main/java/com/primestrider/kotlinstarter/feature/example/gestures/GestureMath.kt`
- Modify: `GesturePages.kt` and `GesturesNavigation.kt`. Add the entry and `composable<GestureDragRoute>`.
- Test: `GestureMathTest.kt` and `DragScreenTest.kt` (same test package)

**Interfaces:**
- Produces, in `GestureMath.kt`:
  - `fun <T> List<T>.moved(from: Int, to: Int): List<T>`, which returns this list unchanged when an index is out of bounds.
  - `enum class SheetAnchor { Collapsed, Half, Expanded }`
- `@Composable fun DragScreen(onBack: () -> Unit)`

- [ ] **Step 1: Write the failing tests.**
  - `GestureMathTest.moved_reorders_and_ignores_bad_indices`:
    - `listOf("a","b","c").moved(0, 2) == listOf("b","c","a")`
    - `.moved(2, 0) == listOf("c","a","b")`
    - `.moved(5, 0) == listOf("a","b","c")`
  - `DragScreenTest` cases:
    - `horizontal_drag_moves_the_handle`: `swipeRight()` on tag `"dragHandle"` changes the text `"Value: 0"` to something else.
    - `free_drag_inside_the_page_scroll_moves_the_box`: `swipeUp()` on tag `"freeDragArea"` changes the `"Position: 0, 0"` text. This proves the page scroll didn't take the gesture.
    - `anchored_sheet_snaps_to_an_anchor`: `swipeUp()` on tag `"anchoredSheet"`, then `waitForIdle()`. Assert the text `"Anchor: Half"` or `"Anchor: Expanded"` exists.
    - `reorder_has_move_buttons`: tapping the content description `"Move Item 1 down"` puts `"Item 2"` before `"Item 1"`. Check this through the tag `"reorderList"` children order.
    - `drag_state_survives_recreation`: use `StateRestorationTester` and assert the anchor survives.
- [ ] **Step 2: Run the tests and confirm they fail.** Run: `gradlew.bat :feature:gestures:impl:testDebugUnitTest --tests "*GestureMathTest" --tests "*DragScreenTest"`. Expected: FAIL (unresolved references).
- [ ] **Step 3: Implement.** Use one `DemoSection` per item, with `file = "DragScreen.kt"`:
  - `Modifier.draggable(rememberDraggableState { … }, Orientation.Horizontal)` handle, with its value from 0 to 100.
  - `pointerInput { detectDragGestures { change, drag -> change.consume(); … } }` on a 200dp box, with the position clamped inside it.
  - `AnchoredDraggableState<SheetAnchor>` with `Modifier.anchoredDraggable(state, Orientation.Vertical)` inside a 320dp frame.
  - A reorderable column: `detectDragGesturesAfterLongPress` reorders through `moved`, and each row also has up/down `IconButton`s as the accessible alternative.
  - Saveable state uses `rememberSaveable`; the anchor is saved by enum name.
- [ ] **Step 4: Run the tests and confirm they pass.** Same command. Expected: PASS.
- [ ] **Step 5: Commit** `feat(gestures): add the Drag page`

### Task 4: Transform page

**Files:**
- Create: `TransformScreen.kt` (gestures package)
- Modify: `GestureMath.kt`, `GesturePages.kt` and `GesturesNavigation.kt`
- Test: `GestureMathTest.kt` (extend it) and `TransformScreenTest.kt`

**Interfaces:**
- Produces:
  - `const val MinZoom = 1f`
  - `const val MaxZoom = 4f`
  - `const val DoubleTapZoom = 2.5f`
  - `fun clampPan(pan: Offset, scale: Float, size: Size): Offset`, which allows at most `size * (scale - 1) / 2` per axis.
  - `fun toggleZoom(scale: Float): Float`, which returns `DOUBLE_TAP_ZOOM` when `scale < DOUBLE_TAP_ZOOM - 0.01f`, otherwise `MIN_ZOOM`.
- `@Composable fun TransformScreen(onBack: () -> Unit)`

- [ ] **Step 1: Write the failing tests.**
  - `clamp_pan_locks_at_scale_one`: `clampPan(Offset(50f, 50f), 1f, Size(100f, 100f)) == Offset.Zero`.
  - `clamp_pan_stops_at_edge`: `clampPan(Offset(500f, -500f), 2f, Size(100f, 100f)) == Offset(50f, -50f)`.
  - `toggle_zoom_alternates`: `toggleZoom(1f) == 2.5f`, `toggleZoom(2.5f) == 1f` and `toggleZoom(4f) == 1f`.
  - `TransformScreenTest` cases:
    - `double_tap_zooms_and_reset_restores`: `doubleClick()` on tag `"transformSurface"` makes the text `"Zoom: 2.5×"` exist. Tapping `"Reset"` gives `"Zoom: 1.0×"`.
    - `zoom_buttons_are_the_accessible_alternative`: tapping the content description `"Zoom in"` raises the zoom.
    - `pinch_zooms`: `performTouchInput { pinch(…) }` on the surface raises the zoom text. If Robolectric can't run `pinch`, keep the test, mark it `@Ignore("Robolectric multi-touch")` with a comment, and rely on the math tests (the spec allows this).
    - `zoom_survives_recreation`: use `StateRestorationTester`.
- [ ] **Step 2: Run the tests and confirm they fail.**
- [ ] **Step 3: Implement.** Use a 280dp surface with a gradient grid. `pointerInput { detectTransformGestures { centroid, pan, zoom, rotation -> … } }` applies scale (clamped to `MinZoom..MaxZoom`), `clampPan` and rotation through `graphicsLayer`. `detectTapGestures(onDoubleTap = { scale = toggleZoom(scale) })` goes in a second `pointerInput`. Add Zoom in, Zoom out and Reset buttons. A second `DemoSection` shows `Modifier.transformable(rememberTransformableState { … })` for rotation only.
- [ ] **Step 4: Run the tests and confirm they pass.**
- [ ] **Step 5: Commit** `feat(gestures): add the Transform page`

### Task 5: Swipe & fling page

**Files:**
- Create: `SwipeScreen.kt`
- Modify: `GestureMath.kt`, `GesturePages.kt` and `GesturesNavigation.kt`
- Test: `GestureMathTest.kt` and `SwipeScreenTest.kt`

**Interfaces:**
- Produces:
  - `enum class SwipeOutcome { Left, Right, Return }`
  - `fun swipeOutcome(offsetX: Float, velocityX: Float, widthPx: Float): SwipeOutcome`. The result is `Left` or `Right` when `|offsetX| > widthPx * 0.4f` or `|velocityX| > 1500f` (the sign picks the side), otherwise `Return`.
- `@Composable fun SwipeScreen(onBack: () -> Unit)`

- [ ] **Step 1: Write the failing tests.**
  - `swipe_outcome_by_distance_and_velocity`:
    - `swipeOutcome(50f, 0f, 1000f) == Return`
    - `swipeOutcome(-500f, 0f, 1000f) == Left`
    - `swipeOutcome(10f, 2000f, 1000f) == Right`
  - `SwipeScreenTest` cases:
    - `swipe_row_reveals_actions`: `swipeLeft()` on tag `"revealRow"` shows the action `"Delete"` as displayed.
    - `card_stack_swipe_advances`: the top card text is `"Card 1"`. After `swipeRight()` on tag `"cardStack"` and `waitForIdle()`, it is `"Card 2"`. The content descriptions `"Like"` and `"Skip"` buttons do the same.
    - `stack_end_offers_restart`: after skipping every card, `"Start over"` exists.
    - `fling_moves_the_puck`: `swipeRight()` on tag `"flingArea"` changes `"Position: 0"`.
    - `stack_position_survives_recreation`
- [ ] **Step 2: Run the tests and confirm they fail.**
- [ ] **Step 3: Implement.**
  - Reveal row: `AnchoredDraggableState` with anchors `0f` and `-actionsWidthPx`, plus a semantics custom action `"Show actions"`.
  - Card stack of 5 cards: `pointerInput` with `detectDragGestures` plus a `VelocityTracker`. On release, `swipeOutcome` decides the result, and an `Animatable` flies the card off or springs it back. Rotation is `offsetX / width * 15f` degrees, through `graphicsLayer`.
  - Fling area: a `VelocityTracker`, then `Animatable.animateDecay(velocity, exponentialDecay())`, with `updateBounds` set to the area width.
- [ ] **Step 4: Run the tests and confirm they pass.**
- [ ] **Step 5: Commit** `feat(gestures): add the Swipe & fling page`

### Task 6: Scroll page

**Files:**
- Create: `ScrollScreen.kt`
- Modify: `GestureMath.kt`, `GesturePages.kt` and `GesturesNavigation.kt`
- Test: `GestureMathTest.kt` and `ScrollScreenTest.kt`

**Interfaces:**
- Produces: `fun headerHeightAfter(scrollDelta: Float, current: Float, min: Float, max: Float): Pair<Float, Float>`. It returns the new height and the consumed delta: scrolling up shrinks the header before the list moves, and scrolling down grows it only once the list is at its top. The caller handles the "at top" part.
- `@Composable fun ScrollScreen(onBack: () -> Unit)`

- [ ] **Step 1: Write the failing tests.**
  - `header_consumes_until_collapsed`: `headerHeightAfter(-50f, 200f, 64f, 200f) == 150f to -50f`, and `headerHeightAfter(-500f, 100f, 64f, 200f) == 64f to -36f`.
  - `ScrollScreenTest` cases:
    - `dial_scrolls_its_value`: `swipeUp()` on tag `"dial"` changes `"Value: 0"`.
    - `nested_header_collapses_before_list_scrolls`: `swipeUp()` on tag `"nestedList"` shrinks tag `"nestedHeader"`. Compare `getBoundsInRoot().height` before and after.
    - `overscroll_list_renders`: the tag `"overscrollList"` exists and contains `"Row 1"`.
- [ ] **Step 2: Run the tests and confirm they fail.**
- [ ] **Step 3: Implement.**
  - Dial: `Modifier.scrollable(rememberScrollableState { delta -> … ; delta }, Orientation.Vertical)` drives a value from 0 to 100, which is drawn as a rotating dial in `graphicsLayer`. Add `-`/`+` buttons.
  - Nested scroll: a 360dp frame whose `NestedScrollConnection.onPreScroll`/`onPostScroll` use `headerHeightAfter`.
  - Overscroll: a 240dp `LazyColumn` using the default `rememberOverscrollEffect()` with `Modifier.overscroll`, and a short usage note.
- [ ] **Step 4: Run the tests and confirm they pass.**
- [ ] **Step 5: Commit** `feat(gestures): add the Scroll page`

### Task 7: Advanced pointer page

**Files:**
- Create: `PointerScreen.kt`
- Modify: `GesturePages.kt` and `GesturesNavigation.kt`
- Test: `PointerScreenTest.kt`

**Interfaces:**
- `@Composable fun PointerScreen(onBack: () -> Unit)`
- Strokes are saved as `List<List<Offset>>` through a custom `Saver` (a flat `FloatArray` list), local to the file.

- [ ] **Step 1: Write the failing tests.**
  - `pointer_visualiser_counts_pointers`: `performTouchInput { down(center) }` on tag `"pointerArea"` makes `"Pointers: 1"` exist. Then `up()` gives `"Pointers: 0"`.
  - `drawing_adds_a_stroke_and_clear_removes_it`: `swipeUp()` on tag `"canvas"` gives `"Strokes: 1"`, and tapping `"Clear"` gives `"Strokes: 0"`.
  - `child_consumes_before_parent`: tapping tag `"childTarget"` logs `"Child handled"`, and the parent log stays `"Parent: —"`. Toggling the design-system switch `"Child consumes"` off and tapping again logs `"Parent handled"`.
  - `strokes_survive_recreation`
- [ ] **Step 2: Run the tests and confirm they fail.**
- [ ] **Step 3: Implement.**
  - Visualiser: `pointerInput { awaitEachGesture { do { val e = awaitPointerEvent(); … } while (e.changes.any { it.pressed }) } }`. It draws a circle plus the id for each pressed pointer.
  - Canvas: `detectDragGestures` appends to the current stroke, and `Canvas` draws paths.
  - Conflict demo: a parent `pointerInput` uses `detectTapGestures` with `requireUnconsumed`. The child's `awaitEachGesture` consumes the down/up only when "Child consumes" is on (`SwitchRow` from the design system).
- [ ] **Step 4: Run the tests and confirm they pass.** Then run `gradlew.bat :feature:gestures:impl:testDebugUnitTest :app:testDevDebugUnitTest`.
- [ ] **Step 5: Commit** `feat(gestures): add the Advanced pointer page`

### Task 8: Pickers page

**Files:**
- Modify: `feature/catalog/api/src/main/java/com/primestrider/kotlinstarter/feature/example/CatalogRoutes.kt`. Add `@Serializable data object PickersRoute`.
- Create: `feature/catalog/impl/src/main/java/com/primestrider/kotlinstarter/feature/example/components/PickersScreen.kt`
- Modify: `feature/catalog/impl/.../ExampleCatalog.kt` (`componentEntries`) and `CatalogNavigation.kt` (`catalogComponentsDestinations`)
- Modify: the catalog impl `values/strings.xml` and `values-in/strings.xml`
- Modify: `app/src/test/java/com/primestrider/kotlinstarter/architecture/RouteNamesTest.kt`. Add the serializer and set the count to `33`.
- Modify: `docs/architecture/components.md`. Add a section "Components tab pages" listing every page and what it shows, plus a **TODO: Expressive page**: `ButtonGroup`, `SplitButton`, `LoadingIndicator`, `FloatingToolbar`, `FloatingActionButtonMenu` and wavy progress, to build once a Compose BOM ships Material 3 1.5 stable.
- Test: `feature/catalog/impl/src/test/java/com/primestrider/kotlinstarter/feature/example/components/PickersScreenTest.kt`

**Interfaces:**
- `@Composable fun PickersScreen(onBack: () -> Unit)`
- Dates are formatted with the existing formatter in `core:common` if one fits; otherwise use `java.time` with the `UTC` zone (picker millis are UTC).

- [ ] **Step 1: Write the failing tests**, using `createAndroidComposeRule<ComponentActivity>()` as in `OverlaysScreenTest`:
  - `inline_date_picker_renders`: the tag `"inlineDatePicker"` exists.
  - `date_dialog_cancel_keeps_previous_value`: first `"Selected: none"`. Open `"Pick a date"` and tap `"Cancel"`; the text is still `"Selected: none"`.
  - `time_dialog_confirm_shows_time`: open `"Pick a time"` and tap `"OK"`; the text `"Time: "` (substring) is no longer `"Time: none"`.
  - `range_picker_and_time_input_render`: the tags `"rangePicker"` and `"timeInput"` exist.
- [ ] **Step 2: Run the tests and confirm they fail.** Run: `gradlew.bat :feature:catalog:impl:testDebugUnitTest --tests "*PickersScreenTest"`.
- [ ] **Step 3: Implement.** Each `DemoSection` has `usage`, `api` and `file = "PickersScreen.kt"`:
  - inline `DatePicker` (`rememberDatePickerState`)
  - `DatePickerDialog` with Cancel/OK
  - `DateRangePicker` in a 480dp-high box
  - `TimePicker` in a dialog (`AlertDialog` / `TimePickerDialog`, whichever 1.4.0 provides)
  - inline `TimeInput`
- [ ] **Step 4: Run the tests and confirm they pass.**
- [ ] **Step 5: Commit** `feat(components): add the Pickers page`

### Task 9: App bars & navigation page with full-screen demos

**Files:**
- Modify: `CatalogRoutes.kt`. Add `@Serializable data object AppBarsRoute` and `@Serializable data class ComponentDemoRoute(val id: String)`.
- Create, in `feature/catalog/impl/.../feature/example/components/`:
  - `AppBarsScreen.kt`: the page listing the demos.
  - `ComponentDemos.kt`: the registry and each full-screen demo, which may split into `ComponentDemoScreens.kt` if it grows past about 400 lines.
- Modify: `ExampleCatalog.kt`, `CatalogNavigation.kt`, the strings, and `RouteNamesTest` (count `35`)
- Modify: `docs/architecture/components.md` (the page list)
- Test: `AppBarsScreenTest.kt` and `ComponentDemosTest.kt`

**Interfaces:**
- Produces:
  - `data class ComponentDemo(val id: String, @StringRes val title: Int, val content: @Composable (onBack: () -> Unit) -> Unit)`
  - `val componentDemos: List<ComponentDemo>`, with ids in this order: `top-app-bar-small`, `top-app-bar-center`, `top-app-bar-medium`, `top-app-bar-large`, `bottom-app-bar`, `modal-drawer`, `dismissible-drawer`, `search-bar`, `bottom-sheet-scaffold`.
  - `@Composable fun AppBarsScreen(onBack: () -> Unit, onOpenDemo: (String) -> Unit)`
  - `composable<ComponentDemoRoute> { entry -> val id = entry.toRoute<ComponentDemoRoute>().id; componentDemos.firstOrNull { it.id == id }?.content?.invoke(back) ?: LaunchedEffect(Unit) { back() } }`
- Each demo screen owns its own `Scaffold` (Material `Scaffold` / `AppScaffold`, whichever fits the demo) with back navigation, and enough list content to scroll.

- [ ] **Step 1: Write the failing tests.**
  - `ComponentDemosTest.ids_are_unique_and_in_order`: assert `componentDemos.map { it.id }` equals the list above.
  - `ComponentDemosTest` render tests, one per demo, each rendering `content {}`:
    - `large_top_app_bar_collapses_on_scroll`: `swipeUp()` on tag `"demoList"` reduces the height of tag `"topAppBar"`.
    - `bottom_app_bar_has_fab_and_actions`
    - `modal_drawer_opens_from_menu`: tapping the content description `"Open navigation drawer"` shows `"Inbox"`.
    - `dismissible_drawer_toggles`
    - `search_bar_expands_and_filters`: tap the search bar and type `"ap"` with `performTextInput` on the input field. A suggestion containing `"Apple"` shows and `"Banana"` does not.
    - `bottom_sheet_scaffold_peeks_and_expands`: the peek text exists; `swipeUp()` on tag `"sheetHandle"` shows `"Sheet content"`.
  - `AppBarsScreenTest.each_demo_button_opens_its_id`: tapping each `"Open"` button reports the matching id to `onOpenDemo`.
- [ ] **Step 2: Run the tests and confirm they fail.**
- [ ] **Step 3: Implement.**
  - `AppBarsScreen` has one `DemoSection` per demo (`usage`, `api`, and `file = "ComponentDemos.kt"`), each with an `AppButton` "Open" and a small static preview where it is cheap.
  - The demos use `TopAppBarDefaults.pinnedScrollBehavior` / `enterAlwaysScrollBehavior` / `exitUntilCollapsedScrollBehavior` with `Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)`, then `BottomAppBar(actions, floatingActionButton)`, `ModalNavigationDrawer`, `DismissibleNavigationDrawer`, `SearchBar` with `SearchBarDefaults.InputField` and `BottomSheetScaffold`.
- [ ] **Step 4: Run the tests and confirm they pass.**
- [ ] **Step 5: Commit** `feat(components): add app bars, drawers, search and bottom-sheet scaffold demos`

### Task 10: Carousel & grids page

**Files:**
- Modify: `CatalogRoutes.kt` (`@Serializable data object CarouselRoute`), `ExampleCatalog.kt`, `CatalogNavigation.kt`, the strings, `RouteNamesTest` (count `36`) and `components.md`
- Create: `feature/catalog/impl/.../components/CarouselScreen.kt`
- Test: `CarouselScreenTest.kt`

**Interfaces:** `@Composable fun CarouselScreen(onBack: () -> Unit)`

- [ ] **Step 1: Write the failing tests.**
  - `carousels_render`: the tags `"multiBrowseCarousel"`, `"uncontainedCarousel"` and `"heroCarousel"` exist.
  - `multi_browse_swipe_reveals_later_items`: `swipeLeft()` on `"multiBrowseCarousel"` makes the node `"Item 4"` exist.
  - `grids_render`: the tags `"staggeredGrid"` and `"horizontalGrid"` contain `"Tile 1"`.
- [ ] **Step 2: Run the tests and confirm they fail.**
- [ ] **Step 3: Implement.** Use `file = "CarouselScreen.kt"`:
  - `HorizontalMultiBrowseCarousel(rememberCarouselState { 8 }, preferredItemWidth = 186.dp)` with `Modifier.maskClip(MaterialTheme.shapes.extraLarge)` coloured items.
  - `HorizontalUncontainedCarousel(itemWidth = 186.dp)`
  - Hero: `HorizontalCenteredHeroCarousel` if 1.4.0 has it, otherwise a multi-browse carousel with a large `preferredItemWidth`, labelled as such.
  - `LazyVerticalStaggeredGrid(StaggeredGridCells.Fixed(2))` in a 360dp box.
  - `LazyHorizontalGrid(GridCells.Fixed(2))` in a 200dp box.
  - The grids are fixed-height, so they don't nest inside the page scroll.
- [ ] **Step 4: Run the tests and confirm they pass.**
- [ ] **Step 5: Commit** `feat(components): add the Carousel & grids page`

### Task 11: Pull-to-refresh and swipe-to-dismiss on the Display page

**Files:**
- Modify: `feature/catalog/impl/.../components/DisplayScreen.kt`, the strings, and `components.md`
- Test: `feature/catalog/impl/src/test/.../components/DisplayScreenTest.kt`

**Interfaces:** none new. `DisplayScreen` keeps its signature.

- [ ] **Step 1: Write the failing tests.**
  - `pull_to_refresh_updates_the_list`: `swipeDown()` on tag `"pullRefreshList"`, then `mainClock.advanceTimeBy(2_000)`. Assert `"Refreshed 1×"`.
  - `refresh_button_is_the_accessible_alternative`: tapping `"Refresh"` does the same.
  - `swipe_to_dismiss_removes_row_and_undo_restores`: `swipeLeft()` on tag `"dismissRow-1"` removes `"Message 1"`. Tapping `"Undo"` restores it. Each row also has a `"Delete Message 1"` content-description button.
- [ ] **Step 2: Run the tests and confirm they fail.**
- [ ] **Step 3: Implement.** Add two `DemoSection`s with `usage`, `api` and `file = "DisplayScreen.kt"`:
  - `PullToRefreshBox(isRefreshing, onRefresh)` around a 280dp `LazyColumn`, with a fake 1.5 s refresh in a `LaunchedEffect`.
  - `SwipeToDismissBox` rows with the undo inline. No snackbar host: the page has none, so add a text button `"Undo"` that is shown after a removal.
- [ ] **Step 4: Run the tests and confirm they pass.**
- [ ] **Step 5: Commit** `feat(components): show pull-to-refresh and swipe-to-dismiss on the Display page`

### Task 12: Full verification

- [ ] **Step 1:** Run `gradlew.bat test ktlintCheck lint assembleDevDebug`. Expected: BUILD SUCCESSFUL. Fix any guard-test failures (`PreviewCoverageTest`, `NoRawMaterialControlsTest`, `TranslationCompletenessTest`, `DocsLinksTest`) in the task that introduced them.
- [ ] **Step 2:** Run `bash scripts/remove-examples.sh` on a throwaway copy of the repo (see AGENTS.md), then `./gradlew test assembleDevDebug` in that copy. Expected: success, with no `gestures` leftovers.
- [ ] **Step 3: Commit** any fixes: `chore: green build after gestures and components`.
