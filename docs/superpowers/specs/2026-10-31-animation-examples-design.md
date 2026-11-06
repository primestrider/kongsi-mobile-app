# Animation Examples — Design

Date: 2026-10-31. Branch: `feat/sdk-demos` (same branch as Plan 5, separate commits).

## Intent

Developers who start from this Kotlin starter keep rebuilding the same motion: page transitions, card
interactions, list and loading motion, small feedback animations. Big consumer apps (streaming, music,
social) use a small, repeated set of these patterns, most of them standardised by Material Motion. This adds
an **Animations** section to the example app that shows those patterns working, each next to the Compose API
that produces it, so a developer can see the pattern, feel it on a device, and copy it.

Success: a developer opens Home → Animations, finds the pattern they need in under a minute, sees which API
makes it, and lifts the code from one focused file.

## Goals

- A new Home section **"Animations"**, placed after "Device APIs" (Home order: Features, Device APIs,
  Animations, Styling).
- Four pages, each a list of small live examples. Every example has a title, one sentence on when to use it,
  and the API name(s) it uses (e.g. `SharedTransitionLayout`, `AnimatedContent`).

### 1. Page transitions
Material Motion's four patterns plus predictive back, each demonstrated by navigating between small demo
pages **inside this example only**:
- **Container transform** — a card grows into its detail page and shrinks back (shared element via
  `SharedTransitionLayout` / `sharedBounds`/`sharedElement`). The "poster → title detail" pattern.
- **Shared axis** — X (forward/back steps, e.g. onboarding), Y (vertical steps), Z (parent/child, scale+fade).
- **Fade through** — outgoing fades out, incoming fades in with a slight scale-up (bottom-nav tab switch).
- **Fade** — for elements entering over the page (dialog/menu style).
- **Predictive back** — the demo page follows the back gesture (shrinks/translates) and can be cancelled.

### 2. Cards
- Press feedback: card scales down slightly while pressed, springs back.
- Horizontal carousel where the centred card is larger (scale/alpha by scroll offset).
- Expand / collapse card (`animateContentSize` or `AnimatedVisibility`).
- Swipe to dismiss / archive with the card following the finger and settling or flying off
  (`anchoredDraggable` or `SwipeToDismissBox`), with undo.
- Flip card (front/back via `graphicsLayer` rotationY).

### 3. Lists & loading
- Skeleton shimmer while loading, then content (`rememberInfiniteTransition`). Reuse or extend the existing
  `core/designsystem` Skeleton if it fits.
- List items animate on add / remove / reorder (`Modifier.animateItem()`).
- Collapsing hero header with parallax image on scroll (detail-page style).

### 4. Micro-interactions
- Like button that pops/bounces on tap (spring scale, colour change).
- Number counting up/down (`AnimatedContent` with a slide per digit or `animateIntAsState`).
- Sliding tab indicator.
- Content that changes with a transition (`AnimatedContent` with `togetherWith`), e.g. a state badge.

## Non-goals

- **Not** changing the app's own page transitions. The main `NavHost` keeps its current default transitions;
  transitions are demonstrated only inside the Animations pages.
- No new libraries (no Lottie, no third-party animation libs). Compose Animation, Foundation, Material 3 and
  Navigation Compose already in the project are enough. Only add a catalog entry if an existing artifact
  needs to be declared explicitly (e.g. `androidx.compose.animation` if not already transitively present —
  versions via the Compose BOM).
- No network images. Visuals are colour blocks, gradients and the app's own icons, so everything works
  offline and deterministically.

## Anti-goals

- **No copying a specific app's look**, name, branding or content. Patterns are generic; colours and titles
  are invented ("Title 1", "Mountain Trip", etc.).
- A page that animates but teaches nothing: every example must name its API and be findable in code.
- Janky motion: animations must run smoothly on the emulator; no layout-thrashing tricks; prefer
  `graphicsLayer` for transforms.
- A SurfaceView-style surprise: nothing in these pages may ignore the page fade (see the camera fix in commit
  231e348).

## Constraints

- Follow the existing example patterns: `feature/example/...`, `ExamplePageScaffold`, `ExampleSection` in
  `ExampleCatalog.kt`, type-safe `@Serializable` routes in `ExampleRoutes.kt`, registration in the Home graph.
  A small registry (like `sdkDemos`) is fine if it keeps adding a page to one entry.
- `core/*` never imports `feature/*`. `feature/example` stays deletable; `docs/examples.md` documents the new
  section and its removal.
- Strings in `values/` and `values-in/`, keys prefixed `anim_`, Indonesian casual-formal using "kamu";
  `TranslationCompletenessTest` stays green. API names stay untranslated.
- minSdk 24. Anything API-gated (predictive back needs Android 14 for the system gesture animation) shows a
  clear fallback line on older devices, as the rest of the example app does.
- Accessibility / reduced motion: when the system animator duration scale is 0 ("Remove animations"), every
  example still works and reaches its end state; examples don't rely on motion alone to convey state.
  Interactive examples have content descriptions; swipe has a button alternative (undo / archive button).
- State that the user chose (selected transition pattern, expanded card, list contents) survives rotation and
  process death via `rememberSaveable`/`SavedStateHandle`, consistent with the rest of the app.
- Tests: Robolectric/Compose tests for logic-driven state (e.g. list add/remove/reorder state, counter value,
  expand state, swipe-dismiss + undo, which pattern is selected, navigation between demo pages reaches the
  right page and back). Pure helpers (carousel scale-by-offset, parallax offset math) get unit tests. Don't
  try to assert animation frames.
- Proof command: `./gradlew testDebugUnitTest ktlintCheck assembleDebug`. Each page opened once on the
  emulator (Pixel_9, API 36) and animations eyeballed before commit.
- Commits: separate commits on `feat/sdk-demos`; do not create a new branch; do not push.

## Decided by the user

- A new example section for learning, not an app-wide transition change.
- Built from scratch here; does not need to match the RN boilerplate.
- Content: the four groups above (page transitions incl. Material Motion + predictive back, cards, lists &
  loading, micro-interactions), modelled on what well-known apps commonly use.
- Same branch, separate commits.

## Calls made by Claude (user can override)

- Section name "Animations", placed after "Device APIs" on Home.
- Four pages grouped as above; each example labelled with its API.
- No new libraries; offline colour-block visuals.
- Reduced-motion support and the accessibility points above.

- **Routes:** four routes, one per page (`@Serializable` objects in `ExampleRoutes.kt`); the examples are
  sections inside each page. A small `animationPages` list feeds the Home section, so adding a page is one
  entry plus its route and strings.
- **Page-transition hosting:** the Page transitions page hosts a **nested `NavHost`** with its own small
  `@Serializable` demo routes, and each pattern sets `enterTransition`/`exitTransition`/
  `popEnterTransition`/`popExitTransition` on its destinations. This shows developers the real Navigation
  Compose API they will copy, rather than a local-state imitation. Container transform uses
  `SharedTransitionLayout` wrapping that nested `NavHost`, with `sharedBounds`/`sharedElement` keyed by item
  id and the `AnimatedVisibilityScope` of each destination. The selected pattern is `rememberSaveable`; the
  nested back stack is saved by Navigation.
- **Predictive back:** in-app, Compose-driven. The demo destination follows the gesture (scale/translate by
  progress) through Navigation Compose's built-in predictive-back support for `NavHost` transitions, or
  `PredictiveBackHandler` where a custom transform is shown, and cancelling restores it. Back pops the nested
  demo stack first, then leaves the page. No manifest change: targetSdk 37 already opts in to predictive
  back. Below Android 14 the page shows the fallback line ("Predictive back needs Android 14.") and back
  works normally.
- **Reorder:** by buttons (move up/down, shuffle) with `animateItem()`, not drag handles. Drag-to-reorder
  needs a lot of custom gesture code and is out of scope.
- **Gesture tests:** tests drive state through plain state holders and the accessible button alternatives
  (archive/undo, move, add/remove). Gestures themselves are checked on the emulator, not simulated.
- **Reduced motion:** Compose already scales its animations by the system animator duration scale, so no
  helper is needed. Check once on the emulator with "Remove animations" on that each example reaches its
  end state.
- **Dependencies:** use the Compose BOM's animation/foundation artifacts. If one is not on the classpath
  transitively, add it to the catalog without a version, so the BOM supplies it.

## Left to the builder

- Exact visual design within the existing design system (spacing, radii, colours from the theme).
- Exact durations/easings, following Material Motion's recommended values.
- File split per page (one file per page, helpers in their own small files).
