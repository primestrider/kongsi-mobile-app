# Platform Features Implementation Plan (plan 10)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Cover the remaining common Android concepts: Compose previews, adaptive layouts, offline-first server data, a media foreground service, a Glance widget, a share target, Play in-app updates, Play Integrity and certificate pinning.

**Architecture:** Everything follows the existing module rules. Previews and the adaptive navigation component live in `core:designsystem`; pinning in `core:network` (configured by `:app` per flavour, off by default); offline-first in `feature:products`; the widget in `feature:todos`; the media service, share target, in-app update and Integrity demos in `feature:devices` as new Device APIs entries.

**Tech Stack:** Compose `@Preview`, `material3-adaptive-navigation-suite` (BOM) + `androidx.compose.material3.adaptive` 1.3.0, Paging `RemoteMediator` + `room-paging` 2.8.5, Media3 `media3-session` 1.11.1, Glance 1.2.0 (+ testing), Play `app-update-ktx` 2.1.0, Play `integrity` 1.6.0, OkHttp `CertificatePinner` + `okhttp-tls` 5.5.1 (tests).

**Spec:** none written: the user approved the list in chat and opted out of design questions ("langsung sajalah"); the defaults stated in chat are the design: music player for the foreground service, Todos widget, share target in Device APIs, pinning off by default, products cache for the unfiltered list only.

## Global Constraints

- Layer rules unchanged (`AGENTS.md`); new device demos register in `sdkDemos` and get strings in `values/` + `values-in/`.
- Every new manifest entry lives in the owning feature's manifest.
- Pinning must never be on by default (DummyJSON rotates certificates; a stale pin bricks networking).
- Integrity and in-app update must degrade gracefully outside Play installs and without configuration (show why, never crash).
- Robolectric `@Config(sdk = [36])`; proof per task `gradlew.bat test ktlintCheck lint assembleDevDebug`; release check `assembleProdRelease verifyReleaseKeeps` after Tasks 4, 5, 6, 7 (new reflection-heavy libraries).

## Review Focus

1. Offline: products open with no network after one online visit (cached rows), and a failed refresh keeps them; pinned by `cached_products_show_when_offline` (Task 4).
2. A wrong pin fails the TLS call; an empty pin list adds no pinner; pinned by `PinningTest` (Task 5).
3. Process death / restart while music plays: the service is foreground with a media notification and stops when playback ends; pinned by `music_service_is_declared_for_media_playback` + controller state tests (Task 6).
4. Widget shows current counts after a todo changes; pinned by `widget_shows_open_count` (Task 7).
5. A share intent with no text/stream, or an unsupported type, shows an empty state instead of crashing; pinned by `SharedContentTest` (Task 8).

---

### Task 1: Compose previews everywhere
- Create `core/designsystem/.../theme/ThemePreviews.kt`: `@Preview(name = "Light") @Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES) annotation class ThemePreviews` and `@Composable fun PreviewTheme(content)` wrapping `AppTheme(isSystemInDarkTheme())` + `Surface` + a `LocalMessenger` stub.
- Add `@ThemePreviews` previews (private, at the end of each file) for every public component file in `core/designsystem/.../component/` and every `fun XxxScreen(` in feature `impl` modules, with representative state (fixtures where needed); `debugImplementation(ui-tooling)` already comes from the compose convention.
- Test `PreviewCoverageTest` (`:app` architecture): every Kotlin file under any module's `src/main` that declares a public `fun \w+Screen(` or lives in `core/designsystem/.../component/` and declares a public `@Composable` contains `@ThemePreviews` (or `@Preview`); asserts > 30 files scanned. RED first.
- Commit `feat: add light and dark Compose previews for every component and screen`.

### Task 2: Adaptive navigation
- Add `material3-adaptive-navigation-suite` (BOM) to the catalog; `core/designsystem/.../component/AppNavigationSuite.kt`: `AppNavigationSuite(items, selectedIndex, onSelect, modifier, content)` using `NavigationSuiteScaffold` (bar on compact width, rail on medium, drawer on expanded via the default `NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo`), hiding navigation with one item; test tags `APP_NAVIGATION_BAR_TAG` / `APP_NAVIGATION_RAIL_TAG`.
- `MainShell` uses it instead of `Scaffold` + `AppNavigationBar` (behaviour of tab switching unchanged).
- Camera demo: make the landscape layout scroll so Capture is reachable (`verticalScroll` on the controls column).
- Tests: `AdaptiveShellTest` with `@Config(qualifiers = "w400dp-h800dp")` → bar tag exists; `"w840dp-h900dp"` → rail tag exists, bar absent; one item → none. Existing `AppTabsTest`/`MainShellTest` stay green. Camera: `controls_scroll_in_landscape` (qualifiers `land`, Capture reachable via `performScrollTo`).
- Commit `feat: switch the shell between bar and rail by window size and fix the landscape camera controls`.

### Task 3: Products list-detail on wide screens
- Add `androidx.compose.material3.adaptive:adaptive-layout`/`adaptive-navigation` 1.3.0; in `ProductsNavigation.kt` render `ProductsListDetail` = `ListDetailPaneScaffold` (navigator `rememberListDetailPaneScaffoldNavigator<Long>()`): list pane = `ProductsScreen`, detail pane = the existing detail content for the selected id (its own `ProductDetailViewModel` keyed by id via `hiltViewModel(key = "detail-$id")` with a `SavedStateHandle` default arg). On compact width the list keeps navigating to `ProductDetailRoute` as today (deep links and back unchanged).
- Tests: `ProductsListDetailTest` at `w1000dp-h800dp`: clicking a row shows the detail title in the same screen (no navigation); at `w400dp` clicking navigates (existing behaviour).
- Commit `feat(products): show list and detail side by side on wide screens`.

### Task 4: Offline-first products
- `feature/products/impl` gets `ProductsDatabase` (`products.db`, v1, `ProductEntity` + `RemoteKeyEntity`, schema export, debug assets) and `ProductsRemoteMediator` (`@OptIn(ExperimentalPagingApi::class)`): REFRESH clears and refills, APPEND uses the stored next offset; `pager("")` = `Pager(config, remoteMediator = …) { dao.pagingSource() }` mapped to `Product`; non-blank queries keep the network `ProductsPagingSource`. `product(id)`: network first, on `IOException` fall back to the cached entity, else rethrow.
- Tests: `ProductsRemoteMediatorTest` (in-memory DB + fake API: refresh inserts, append appends, IO error → `MediatorResult.Error`, end reached); `cached_products_show_when_offline` (repository with an API that throws IOException after one successful refresh still pages the cached rows); `detail_falls_back_to_cache_offline`; migration-free schema `1.json` exported.
- Docs: `docs/architecture/data-loading.md` "Offline-first" section.
- Commit `feat(products): cache the product list in Room with a RemoteMediator for offline use`.

### Task 5: Certificate pinning
- `core:network`: qualifier `@CertificatePins` (`Set<String>`, `@Multibinds`-free: provided by `:app`); `NetworkModule` adds `CertificatePinner.Builder().add(apiHost, *pins)` to **both** clients when the set is non-empty.
- `:app`: `EnvironmentModule` provides `@CertificatePins` from `BuildConfig.API_PINS` (comma-separated `sha256/…`), set per flavour from `localOrEnv("api.pins", "API_PINS", "")` (empty default).
- Tests (`core:network`, `okhttp-tls` test dep): `PinningTest`: HTTPS `MockWebServer` with a `HeldCertificate`; client trusting it + correct pin → 200; wrong pin → `SSLPeerUnverifiedException`; empty set → no pinner on the client.
- Docs: `docs/architecture/networking.md` "Certificate pinning" (how to get pins, keep a backup pin, rotation risk), `release.md` variables, skill `release-build` one line.
- Commit `feat(network): optional certificate pinning configured per flavour`.

### Task 6: Media foreground service (music)
- `feature/devices/impl`: `media3-session` dep; `sdk/music/MusicService.kt` (`MediaSessionService` with an `ExoPlayer`, `onGetSession`, stops itself when playback ends or the task is removed while paused); manifest: `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `<service android:foregroundServiceType="mediaPlayback" android:exported="true">` with the `androidx.media3.session.MediaSessionService` action; the demo plays a bundled 10-second tone (`res/raw/sdk_music_sample.wav`, generated once with a Node one-off script and committed), so it works offline and has no licensing question.
- `sdk/music/MusicScreen.kt` + `MusicViewModel` using a `MediaController` (connect via `SessionToken`), play/pause/stop, position, notification permission note (API 33+).
- Tests: `music_service_is_declared_for_media_playback` (merged manifest: service + permissions + type); `MusicViewModel` state mapping with a fake controller wrapper; registry/strings tests via `SdkDemosTest`.
- Commit `feat(devices): add a music player demo running as a media playback foreground service`.

### Task 7: Glance widget for todos
- `feature/todos/impl`: `glance-appwidget` + `glance-material3` (+ `glance-appwidget-testing` test); `widget/TodosWidget.kt` (`GlanceAppWidget`, reads open count + 3 newest open titles through a Hilt `@EntryPoint` to `TodosRepository`, tap opens the app at `TodosRoute` via a deep link `<scheme>://example/todos`), `TodosWidgetReceiver`, `res/xml/todos_widget_info.xml`, manifest receiver; `DefaultTodosRepository` writes trigger `TodosWidget().updateAll(context)` through a small `TodosWidgetUpdater` interface (no-op fake in tests).
- `todosDestinations` registers the deep link (scheme passed like devices).
- Tests: `widget_shows_open_count` with `runGlanceAppWidgetUnitTest`; repository calls the updater after add/toggle/remove/clear.
- Commit `feat(todos): add a home-screen widget with the open todos`.

### Task 8: Share target
- `feature/devices/impl`: `sdk/sharetarget/ShareReceiverActivity.kt` (`@AndroidEntryPoint`, `setContent { AppTheme { SharedContentScreen(...) } }`), manifest intent filters `SEND` (`text/plain`, `image/*`) and `SEND_MULTIPLE` (`image/*`), label from strings; `SharedContent.from(intent)` pure parser (text, subject, up to 10 image URIs, empty); screen shows text and image thumbnails (Coil) with an empty state; a Device APIs entry "Share target" explains how to try it (share from another app).
- Tests: `SharedContentTest` (text, image, multiple, missing extras, unsupported type); `ShareReceiverActivityTest` launching with a text intent shows it.
- Commit `feat(devices): receive text and images shared from other apps`.

### Task 9: In-app update and Play Integrity demos
- Deps `app-update-ktx`, `integrity`; `sdk/appupdate/` screen + ViewModel over `AppUpdateManager` (check availability, start flexible flow, show download progress, "complete update"; outside Play shows the API's error as "not installed from Play"); tests with `FakeAppUpdateManager`.
- `sdk/integrity/` screen + ViewModel: `StandardIntegrityManager` prepare + request with a request hash; cloud project number from `BuildConfig.PLAY_INTEGRITY_PROJECT` (devices module, `localOrEnv("play.integrity.project", "PLAY_INTEGRITY_PROJECT", "0")`); `0` → "not configured" state; token shown truncated with the explanation that a server must decode and verify it; tests for state mapping with a fake wrapper.
- Docs: `docs/examples.md` entries for Tasks 6, 8 and 9, the widget in the todos section, setup notes (Play-only behaviour, Integrity project number).
- Commit `feat(devices): add in-app update and Play Integrity demos`.

### Task 10: Docs and guide
- Update `AGENTS.md` project map lines, `docs/architecture/navigation.md` (adaptive shell, list-detail), `docs/architecture/components.md` (AppNavigationSuite, previews convention), `docs/learning/` (chapter 7 previews tip, chapter 3 offline-first + pinning paragraph), skills `add-component-example` / `add-feature` (previews required), `add-permission-feature` (foreground service types).
- Full proof + `assembleProdRelease verifyReleaseKeeps`.
- Commit `docs: describe previews, adaptive layouts, offline products and the new platform demos`.
