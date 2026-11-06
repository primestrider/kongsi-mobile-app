# Animation Examples Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an "Animations" Home section with four pages of live, labelled motion patterns (micro-interactions, cards, lists & loading, page transitions) that developers can copy.

**Architecture:** A small registry `feature/example/animation/AnimationPages.kt` lists the four pages (route, title, subtitle) and feeds a new `ExampleSection` placed after "Device APIs". Each page is one screen file under `feature/example/animation/<page>/`, built from a shared `AnimExample` card that shows the example, one usage sentence and the API name. Logic that decides state lives in plain, unit-tested holders or pure functions; the page-transitions page hosts its own nested `NavHost` inside a `SharedTransitionLayout`, leaving the app's `NavHost` untouched.

**Tech Stack:** Compose Animation / Foundation / Material 3 (from Compose BOM 2026.09.00), Navigation Compose 2.10.2, kotlinx.serialization, Robolectric + Compose UI tests.

**Spec:** `docs/superpowers/specs/2026-10-31-animation-examples-design.md`

## Global Constraints

- Branch `feat/sdk-demos`, separate commits, do not create a branch, do not push.
- No new libraries. Compose animation/foundation artifacts come from the BOM; add a catalog entry without version only if one is not on the classpath.
- The app `NavHost` (`app/navigation/AppNavHost.kt`) keeps its current transitions; only the new Home-graph destinations are added.
- No copying a specific app's look, name, branding or content; invented titles ("Mountain Trip", "Title 1") and theme colours/gradients only; no network images.
- `core/*` never imports `feature/*`; `feature/example` stays deletable; `docs/examples.md` documents the section and its removal.
- Strings in `values/` and `values-in/`, keys prefixed `anim_`, Indonesian uses "kamu"; API names (e.g. `AnimatedContent`) stay untranslated; `TranslationCompletenessTest` stays green.
- minSdk 24; predictive back below Android 14 shows the fallback line "Predictive back needs Android 14." / "Predictive back butuh Android 14." and back still works.
- Reduced motion: with "Remove animations" on, every example reaches its end state; no state is conveyed by motion alone; swipe has a button alternative.
- User choices (selected pattern, expanded/flipped card, liked, counter, list contents, archived items) survive rotation and process death (`rememberSaveable` with a `Saver` or `SavedStateHandle`); SavedStateHandle keys never `id`/`amount`.
- Robolectric hygiene: destroy activities, cancel `viewModelScope` before `resetMain`.
- Prefer `graphicsLayer` for transforms; nothing uses a `SurfaceView`.
- Proof command for every task: `./gradlew testDebugUnitTest ktlintCheck assembleDebug`. Each page is opened on the emulator (Pixel_9, API 36) and its animations eyeballed, including once with "Remove animations" on, before commit.

## Review Focus

1. **Rotation / process death mid-example** (expanded card, flipped card, list after add/remove/reorder, selected transition pattern, open nested demo page) → state comes back as left. Tests in Tasks 2, 3, 4 (`StateRestorationTester`).
2. **Back inside the page-transitions demo** (system back while a nested demo page is open, including mid-transition) → pops the nested page first; only back from the demo list leaves the page; never two pages popped. Test in Task 4.
3. **Rapid repeated input** (double-tap "Open", spam like / add / remove / shuffle) → no duplicate navigation, list ids stay unique, counter exact. Tests in Tasks 1, 3, 4.
4. **Undo after the list changed** (archive item, then add/move others, then undo) → item returns at a sensible index clamped to the list, never lost or duplicated; archiving the last item shows the empty state. Test in Task 2.
5. **Reduced motion** ("Remove animations") → every example still reaches its end state. Checked on the emulator in every task; logic tests assert end states, never frames.

---

### Task 1: Animations section, shared example card, Micro-interactions page

**Files:**
- Create: `feature/example/animation/AnimationPages.kt`, `feature/example/animation/AnimExample.kt`, `feature/example/animation/micro/MicroInteractionsScreen.kt`
- Modify: `feature/example/ExampleRoutes.kt` (four routes), `feature/example/ExampleCatalog.kt` (section after Device APIs), `app/navigation/AppNavHost.kt` (`homeGraph`: one `composable<…>` per page; pages not yet built in this task are added by their task), `res/values/strings.xml`, `res/values-in/strings.xml`, `docs/examples.md`, `docs/architecture/navigation.md` (link to the transitions page, filled in Task 4)
- Test: `feature/example/animation/AnimationPagesTest.kt`, `feature/example/animation/micro/MicroInteractionsScreenTest.kt`; update `feature/example/sdk/SdkDemosTest.kt` (`home_section_sits_between_features_and_styling` → Home section order `Features, Device APIs, Animations, Styling`)

**Interfaces:**
- Produces:
  - Routes in `ExampleRoutes.kt`: `@Serializable data object AnimMicroRoute`, `AnimCardsRoute`, `AnimListsRoute`, `AnimTransitionsRoute`.
  - `data class AnimationPage(val route: Any, @StringRes val title: Int, @StringRes val subtitle: Int)`; `val animationPages: List<AnimationPage>` in order Page transitions, Cards, Lists & loading, Micro-interactions (only pages that exist are listed; each later task appends its page in this order); `fun animationEntries(): List<ExampleEntry>`.
  - `@Composable fun AnimExample(@StringRes title: Int, @StringRes usage: Int, api: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit)` — title, usage sentence (bodySmall), `api` in a monospace label, then the live content; reuse the existing `DemoSection`/card styling used by `SdkSection`.
- Copy (en / id): section "Animations" / "Animasi", "Motion patterns well-known apps use, with the Compose API behind each" / "Pola gerak yang dipakai app terkenal, lengkap dengan API Compose di baliknya"; pages: "Page transitions" / "Transisi halaman", "Container transform, shared axis, fade through, predictive back"; "Cards" / "Card", "Press, carousel, expand, swipe and flip"; "Lists & loading" / "List & loading", "Shimmer, animated list changes, collapsing header"; "Micro-interactions" / "Interaksi mikro", "Like, counter, tab indicator, content change". Example titles and usage sentences are the builder's, under `anim_<page>_…` keys.
- Micro-interactions examples: like button (spring scale pop ~1 → 1.3 → 1 + colour change, `animateFloatAsState`/`Animatable`), counter (+/− buttons, `AnimatedContent` sliding up on increase and down on decrease), sliding tab indicator (3 tabs, indicator offset/width animated), content change (`AnimatedContent` + `togetherWith` cycling three states, e.g. "Pending → Paid → Shipped").

- [ ] **Step 1: Write failing tests**: `AnimationPagesTest` — `animationEntries()` maps 1:1 to `animationPages` routes; Home shows section titles in order `Features, Device APIs, Animations, Styling` (update the existing SdkDemosTest assertion instead of duplicating). `MicroInteractionsScreenTest` — tapping like sets its content description from "Like" to "Liked" and back; five taps on "+" then two on "−" shows "3" (Review Focus 3); content change cycles Pending → Paid → Shipped → Pending; the like, counter and content-change states survive `StateRestorationTester.emulateSavedInstanceStateRestore()`.
- [ ] **Step 2: Run** `./gradlew testDebugUnitTest --tests "*.example.animation.*" --tests "*SdkDemosTest"` → FAIL (unresolved references).
- [ ] **Step 3: Implement** the routes, registry, Home section, `AnimExample`, the Micro-interactions page and its `composable<AnimMicroRoute>`; strings en/id.
- [ ] **Step 4: Run** the proof command → PASS; emulator: open Home → Animations → Micro-interactions, try each example, repeat with "Remove animations" on.
- [ ] **Step 5: Docs** — `docs/examples.md`: an "Animations" block (what the section shows, how to add a page: route + `animationPages` entry + `composable` + strings, how to remove the section) — and **Commit** `feat(example): add the Animations section with micro-interaction examples`.

### Task 2: Cards page

**Files:**
- Create: `feature/example/animation/cards/{CardsScreen,CarouselMath,ArchiveState}.kt`
- Modify: `AnimationPages.kt` (append Cards before Micro-interactions per the order), `AppNavHost.kt`, strings ×2, `docs/examples.md`
- Test: `feature/example/animation/cards/{CarouselMathTest,ArchiveStateTest,CardsScreenTest}.kt`

**Interfaces:**
- Consumes: `AnimExample`, `AnimCardsRoute`, `animationPages` (Task 1).
- Produces:
  - `fun carouselScale(pageOffset: Float, minScale: Float = 0.85f): Float` — 1f at offset 0, linear down to `minScale` at |offset| ≥ 1, clamped; `fun carouselAlpha(pageOffset: Float, minAlpha: Float = 0.6f): Float` same shape.
  - `data class DemoCard(val id: Int, val title: String, val colorIndex: Int)` (`@Serializable` or `Parcelable`-free saver).
  - `class ArchiveState(initial: List<DemoCard>)` — `val cards: List<DemoCard>` (snapshot state), `val lastArchived: DemoCard?`, `fun archive(id: Int)`, `fun undo()` (re-inserts at `min(originalIndex, cards.size)`; no-op when nothing archived), `fun reset()`; `companion val Saver`.
- Examples: press feedback (card scales to 0.96 while pressed via `interactionSource.collectIsPressedAsState()` + spring), carousel (`HorizontalPager` with `carouselScale`/`carouselAlpha` applied in `graphicsLayer` from `pagerState.getOffsetDistanceInPages`), expand/collapse (`animateContentSize`, chevron rotation), swipe to archive (`SwipeToDismissBox` with archive + undo snackbar, and an "Archive" icon button per card as the accessible alternative; empty state "All cards archived" + "Reset"), flip card (`graphicsLayer.rotationY` 0→180 with `cameraDistance`, back face shown past 90°, tap or "Flip" button).

- [ ] **Step 1: Write failing tests**: `CarouselMathTest` — `carouselScale(0f) == 1f`, `carouselScale(1f) == 0.85f`, `carouselScale(-0.5f) == 0.925f`, `carouselScale(3f) == 0.85f`; `carouselAlpha(0f) == 1f`, `carouselAlpha(1f) == 0.6f`. `ArchiveStateTest` — archive then undo restores the card at its index; archive id 2, remove two others, undo → card back at an index ≤ size and present once (Review Focus 4); undo with nothing archived is a no-op; archiving every card leaves an empty list; Saver round-trips cards and lastArchived. `CardsScreenTest` — tapping the archive button of "Mountain Trip" removes it and "Undo" brings it back; archiving all shows "All cards archived" and "Reset" restores; expand toggles the extra text; flip button shows the back text; expanded + flipped survive `StateRestorationTester` (Review Focus 1).
- [ ] **Step 2: Run** `./gradlew testDebugUnitTest --tests "*.example.animation.cards.*"` → FAIL.
- [ ] **Step 3: Implement** the helpers, `ArchiveState`, `CardsScreen`, its `composable<AnimCardsRoute>`, registry entry, strings.
- [ ] **Step 4: Run** the proof command → PASS; emulator: press, swipe the carousel, expand, swipe-to-archive + undo, flip; again with "Remove animations".
- [ ] **Step 5: Docs** (`examples.md` Cards bullets with the API per example) and **Commit** `feat(example): add card animation examples`.

### Task 3: Lists & loading page

**Files:**
- Create: `feature/example/animation/lists/{ListsScreen,DemoListState,HeaderMath}.kt`
- Modify: `AnimationPages.kt`, `AppNavHost.kt`, strings ×2, `docs/examples.md`; reuse `core/designsystem/component/Skeleton.kt` (`Skeleton`/`SkeletonText`) — extend it only if a shimmer brush is missing, keeping its existing callers unchanged
- Test: `feature/example/animation/lists/{DemoListStateTest,HeaderMathTest,ListsScreenTest}.kt`

**Interfaces:**
- Consumes: `AnimExample`, `AnimListsRoute`, `animationPages` (Task 1).
- Produces:
  - `data class DemoRow(val id: Int, val label: String)`; `class DemoListState(initial: List<DemoRow>)` — `val rows`, `fun add()` (new unique id = max+1, label "Item <id>", inserted at top), `fun remove(id: Int)`, `fun moveUp(id: Int)`, `fun moveDown(id: Int)` (no-op at the edges), `fun shuffle(random: Random)`; `companion val Saver`.
  - `fun collapseFraction(scrollPx: Int, rangePx: Int): Float` (0..1, clamped; rangePx ≤ 0 → 1f); `fun parallaxOffset(scrollPx: Int, factor: Float = 0.5f): Float` (= scrollPx × factor, never negative).
- Examples: skeleton shimmer → content ("Reload" shows ~1.5 s shimmer via `rememberInfiniteTransition`, then rows; with "Remove animations" the content still appears), animated list (`LazyColumn` with `Modifier.animateItem()`, buttons Add / Shuffle and per-row ↑ ↓ ✕; empty state "No items" with "Add"), collapsing hero header (a fixed-height scroll area inside the example: gradient hero whose height shrinks by `collapseFraction`, image block translated by `parallaxOffset`, title fades into a small toolbar title).

- [ ] **Step 1: Write failing tests**: `DemoListStateTest` — add twice yields unique ids and the newest first; five rapid adds then removes keep ids unique (Review Focus 3); moveUp on the first and moveDown on the last are no-ops; moveDown swaps neighbours; shuffle with `Random(42)` is a permutation of the same ids; removing all leaves empty; Saver round-trip. `HeaderMathTest` — `collapseFraction(0, 300) == 0f`, `(150, 300) == 0.5f`, `(900, 300) == 1f`, `(10, 0) == 1f`; `parallaxOffset(200) == 100f`, `parallaxOffset(-5) == 0f`. `ListsScreenTest` — Add shows "Item 4" (initial three items); ✕ on "Item 1" removes it; removing all shows "No items"; list contents survive `StateRestorationTester` (Review Focus 1).
- [ ] **Step 2: Run** `./gradlew testDebugUnitTest --tests "*.example.animation.lists.*"` → FAIL.
- [ ] **Step 3: Implement** the state, helpers, `ListsScreen`, its `composable<AnimListsRoute>`, registry entry, strings.
- [ ] **Step 4: Run** the proof command → PASS; emulator: reload shimmer, add/remove/move/shuffle, scroll the header; again with "Remove animations".
- [ ] **Step 5: Docs** and **Commit** `feat(example): add list and loading animation examples`.

### Task 4: Page transitions page

**Files:**
- Create: `feature/example/animation/transitions/{TransitionsScreen,TransitionPattern,DemoNavHost,PredictiveBackDemo}.kt`
- Modify: `AnimationPages.kt` (Page transitions first), `AppNavHost.kt`, strings ×2, `docs/examples.md`, `docs/architecture/navigation.md` (section "Page transitions": NavHost-level vs per-destination transitions, where the app's own defaults live, pointer to this page)
- Test: `feature/example/animation/transitions/{TransitionPatternTest,TransitionsScreenTest}.kt`

**Interfaces:**
- Consumes: `AnimExample`, `AnimTransitionsRoute`, `animationPages` (Task 1).
- Produces:
  - `enum class TransitionPattern { CONTAINER_TRANSFORM, SHARED_AXIS_X, SHARED_AXIS_Y, SHARED_AXIS_Z, FADE_THROUGH, FADE }` with `@StringRes label` and `api: String`.
  - `object MotionSpec { const val SHARED_AXIS_MS = 300; const val FADE_THROUGH_MS = 300; const val FADE_THROUGH_OUT_MS = 90; const val FADE_IN_MS = 150; const val FADE_OUT_MS = 75; const val CONTAINER_MS = 300; val SHARED_AXIS_SLIDE = 30.dp; const val SHARED_AXIS_Z_SCALE_IN = 0.8f; const val SHARED_AXIS_Z_SCALE_OUT = 1.1f; const val FADE_THROUGH_SCALE_IN = 0.92f }` (Material Motion values).
  - `fun TransitionPattern.enter(forward: Boolean): EnterTransition`, `fun TransitionPattern.exit(forward: Boolean): ExitTransition` — shared axis X slides by `SHARED_AXIS_SLIDE` (direction flips when `forward == false`) + fade; Y the same vertically; Z scales in from 0.8 / out to 1.1 + fade; fade through = exit fadeOut(90 ms), enter fadeIn(210 ms, delay 90) + scaleIn(0.92); fade = fadeIn(150)/fadeOut(75); container transform uses fade only on the destination (the morph comes from `sharedBounds`).
  - Nested demo routes (private to the page): `@Serializable data object DemoListRoute`, `@Serializable data class DemoDetailRoute(val itemId: Int)`.
- Page layout: a `SegmentedChoice`/`SelectField` of `TransitionPattern` (saveable), then a demo frame (fixed height, clipped) hosting `SharedTransitionLayout { NavHost(demoNavController, DemoListRoute) { … } }`. `DemoListRoute` shows three invented items ("Mountain Trip", "City Lights", "Ocean Days") as cards; tapping opens `DemoDetailRoute(itemId)` with the chosen pattern's transitions (`enterTransition`/`exitTransition`/`popEnterTransition`/`popExitTransition` reading the selected pattern); for `CONTAINER_TRANSFORM` the card and detail share `Modifier.sharedBounds(rememberSharedContentState("item-$itemId"), animatedVisibilityScope = this@composable)`. Detail has a "Back" button. A guard ignores "Open" while a navigation transition is running (Review Focus 3). Back: a `BackHandler(enabled = demo stack has a detail)` pops the nested controller; otherwise back leaves the page (Review Focus 2). Below the frame, an `AnimExample` per pattern explaining when to use it and its API.
  - Predictive back example (`PredictiveBackDemo.kt`): API 34+ — a "Open preview" card opens a panel; `PredictiveBackHandler` scales it 1 → 0.9 and translates by progress, completes on commit, springs back on cancel. Below 34 the example shows "Predictive back needs Android 14." and the panel closes with normal back.

- [ ] **Step 1: Write failing tests**: `TransitionPatternTest` — every pattern has a non-blank `api` and a label; `MotionSpec` values equal the constants above. `TransitionsScreenTest` (Compose rule hosting `TransitionsScreen(onBack = { backs++ })`) — tapping "Mountain Trip" shows its detail text; pressing the detail "Back" returns to the list; system back (`Espresso.pressBack()` or `activity.onBackPressedDispatcher.onBackPressed()`) while detail is open returns to the list and `backs == 0`, and a second system back gives `backs == 1` (Review Focus 2); double-tapping "Mountain Trip" opens exactly one detail (one back returns to the list) (Review Focus 3); selecting `SHARED_AXIS_Z` and the open detail survive `StateRestorationTester` (Review Focus 1); at `@Config(sdk = [33])` the predictive back example shows "Predictive back needs Android 14.".
- [ ] **Step 2: Run** `./gradlew testDebugUnitTest --tests "*.example.animation.transitions.*"` → FAIL.
- [ ] **Step 3: Implement** the pattern, motion spec, nested host, predictive-back example, `TransitionsScreen`, its `composable<AnimTransitionsRoute>`, registry entry (first in the list), strings.
- [ ] **Step 4: Run** the proof command → PASS; emulator: each pattern open/close, container transform morph both ways, system back gesture on the detail (predictive back on API 36 follows the finger and cancels cleanly), predictive back example; again with "Remove animations"; confirm the app's own page transitions are unchanged (Home → any other page still fades as before).
- [ ] **Step 5: Docs** (`examples.md` Page transitions block; `navigation.md` "Page transitions" section; the physical-device checklist gains "predictive back gesture on the transitions demo") and **Commit** `feat(example): add page transition examples with shared elements and predictive back`.
