# Architecture & Modularisation — Design

Date: 2026-10-31. Starts after the Animations plan (plan 06) and after the input restyle
(`2026-10-31-input-restyle-design.md`). It takes over the CI part of the original plan 6 ("Release & knowledge
base"); the project skills / knowledge-base part of plan 6 stays for later and must be written against the new
module layout.

## Intent

The owner asked whether the starter follows the structure and patterns of big production Kotlin apps. An audit
found the foundations right (MVVM + unidirectional data flow, Screen/Route/ViewModel split, Hilt, type-safe
navigation, version catalog, Room schema export, Paging, solid networking/auth) but several gaps versus big apps:
a single `:app` module where layer rules are only a convention, DTO/Entity types reaching the UI, repositories
that are `open class` for testing, flat feature folders, a release build that is not production-ready, no CI,
ktlint as the only static check, no baseline profile, no screenshot tests, default `gradle.properties`.

The owner's instruction was to follow what Google recommends ("saya mengikuti best practice yang disarankan
google"), and, after hearing that a one-module-per-feature layout would be simpler, explicitly chose the Now in
Android style **with `api` + `impl` modules per feature**. That choice is final.

Success: the starter has the module layout and build setup of a Google-reference app (Now in Android), every
layer rule is enforced by Gradle rather than by documentation, any example feature can be removed by deleting
its modules and their wiring, and a fresh clone builds, tests and lints in CI.

## Goals

### 1. Module layout (Now in Android style)

```
:app                          shell: MainActivity, AppViewModel, MainShell, NavHost wiring every feature; the
                              only module that depends on feature :impl modules
:core:designsystem            theme + reusable components (incl. the restyled inputs)
:core:ui                      shared composables that are not design-system atoms (PermissionGate,
                              PictureInPictureFrame/Host, CrashFallbackScreen, Messenger host if it fits)
:core:network                 Retrofit/OkHttp clients, ApiError, safeApiCall, @Anonymous, base URL
:core:auth                    SessionRepository, AuthAdapter seam, interceptor, authenticator, refresher
:core:datastore               DataStore module + preference keys/repositories (theme, language…)
:core:common                  dispatchers/CoroutinesModule, formatters, locale, i18n controller, crash recorder
:core:testing                 shared test doubles, rules (main dispatcher, in-memory DataStore), Hilt test setup
:feature:<name>:api           @Serializable route types and anything another feature may navigate to
:feature:<name>:impl          screens, ViewModels, data/ and ui/ packages, Hilt modules, strings, tests
build-logic/                  included build with convention plugins (application, library, compose, hilt,
                              feature, test) so module build files are a few lines
```

Features, all example content (each one `api` + `impl`):

| Feature | Contents today |
|---|---|
| `catalog` | Home (sections, search, filter chips), Components tab home, component pages (actions, inputs, display, overlays, feedback), styling pages, form example |
| `products` | list (Paging, debounced search, tabs, add-to-cart sheet), detail |
| `todos` | Room `ExampleDatabase`, DAO, repository, screen; owns `schemas/` and its migration tests |
| `account` | Profile hub, sign-in, protected Account, DummyJSON `AuthAdapter` binding |
| `settings` | language, theme, developer options (crash the app) |
| `devices` | the "Device APIs" section: `sdkDemos` registry and all 21 demos |
| `animation` | the Animations section from plan 06 |

Dependency rules (enforced by Gradle, documented in `AGENTS.md` and `docs/architecture/overview.md`):
- `core:*` never depends on `feature:*`.
- `feature:X:impl` may depend on `core:*` and on `feature:Y:api`, never on `feature:Y:impl`.
- `feature:X:api` depends on nothing but what route types need (kotlinx serialization; at most `core:common`).
- `:app` depends on every `feature:*:impl` and wires their graphs into the tabs (Home / Components / Profile).
- Cross-feature navigation (e.g. Home opens Products) uses the target's `api` route type.

**Home catalog entries** *(designer's call, after the builder check)*: `catalog:api` defines the entry type
(title/subtitle string resources, icon, route, section) and a Hilt multibinding set. Each feature's `impl`
contributes its own entries (with its own strings), and `catalog:impl` renders whatever the set contains. So
deleting a feature removes its Home entries by itself, and `catalog:impl` never holds another feature's strings.

Removing an example feature = delete its two module folders, its `include` lines in `settings.gradle.kts`, and its
wiring in `:app`. Document this in `docs/` and `AGENTS.md`; the build must still pass after removing all seven.

**What remains after deleting every example** (owner chose "a minimal app that still runs"): with all seven
example features removed, the app must build, start and be usable, not just compile.
- `:app` owns a simple placeholder Home ("start building here") that shows when no catalog feature is present, and
  the shell works with however many tabs the wired features provide (one tab is fine; the bottom bar may hide
  when there is only one).
- `core:auth` works with no example `AuthAdapter`: a default "not configured" adapter (sign-in reports it is not
  set up, refresh fails cleanly, so the session stays signed out) is used unless a feature supplies one. How the
  override works (e.g. Hilt `@BindsOptionalOf`/`Optional`, or a default binding in `:app` replaced when `account`
  is wired) is the builder's call; no duplicate-binding errors in either configuration.
- Proof: a check (a script or CI job, or at least a documented manual run) that removes all example modules on a
  throwaway copy, then builds, tests and launches.

### 2. Data layer inside features

- DTOs (`@Serializable` API types) and Room entities stay inside the feature's `data/` package and are mapped to
  feature models before reaching a ViewModel. Today `ProductDto` reaches `ProductsScreen` /
  `ProductDetailScreen` and `TodoEntity` reaches `TodosScreen` / `TodosViewModel`; those must stop.
- Repositories become `interface XxxRepository` + `DefaultXxxRepository` bound with Hilt `@Binds`; tests use a
  fake (in `core:testing` if shared, otherwise in the feature's test sources). No `open class` for testing.
- Each `impl` uses `data/` (api, dto, entity, dao, repository, mappers) and `ui/` (screen, route, viewmodel)
  packages.
- MVVM + UDF stays the UI pattern: ViewModel exposes one `StateFlow` UI state; screen is stateless; events go up
  as callbacks. No MVI framework, no use-case layer by default (Google marks the domain layer optional; add a use
  case only where logic is shared by several ViewModels).

### 3. Build & release

- `release`: R8 (`isMinifyEnabled = true`) and `isShrinkResources = true`, with whatever keep rules the
  libraries in use need; the release build must start and the main flows must work after minification.
- Product flavours `dev` and `prod` (one dimension, e.g. `environment`) with the API base URL per flavour; the
  current override (`local.properties` `api.url` → env `API_URL`) keeps working on top. `dev` may use an
  `applicationIdSuffix` so both can be installed. *(Designer's call: there is no real backend, so both flavours
  default to DummyJSON; `prod`'s URL is the one line a real app changes, documented as such. The bearer-token host
  check in `core:network` keeps following whatever base URL the build uses.)*
- Release signing read from env vars / `local.properties` (never committed); when absent, release falls back to
  unsigned or debug signing so CI and fresh clones still build.
- `gradle.properties`: enable parallel builds, build cache and configuration cache (fix anything that breaks
  configuration cache rather than disabling it).

### 4. Quality gates

- **CI on GitHub Actions (Linux)**: `testDebugUnitTest` (or the per-flavour equivalent), `ktlintCheck`, Android
  Lint, `assembleDebug`. Note that AGP 9 creates debug unit-test tasks only; one DataStore corruption test is
  skipped on Windows and must run on Linux.
- **Android Lint** configured (`lint { warningsAsErrors/abortOnError …, baseline }`) across modules; a baseline
  file is fine for existing findings.
- **Screenshot tests with Roborazzi** (runs on the existing Robolectric setup, SDK 36) for the design-system
  components in light and dark, at least the restyled inputs and choice controls, recorded images committed;
  CI verifies them. *(Designer's call: Linux is the source of truth, because Windows renders fonts differently.
  Reference images are recorded by a manually triggered CI job, which commits them or uploads them as an
  artifact. Local runs on Windows only compare with a tolerance or are skipped. The docs say not to record
  locally.)*
- **Baseline profile**: a `:baselineprofile` module (androidx.baselineprofile + macrobenchmark) with a startup
  journey; the generated profile is committed and packaged in release. Generating needs a device/emulator, so it
  is a documented local step, not a CI step.

### 5. Docs

Update `AGENTS.md` (project map, layer rules, commands, "adding a page/feature" steps), `README.md`, and
`docs/architecture/*` (overview, navigation, data-loading, storage-database, components) for the new layout,
including how to add a new feature module and how to delete the examples. Fix the known error in
`storage-database.md` (it says `./gradlew test` runs `testReleaseUnitTest`).

## Non-goals

- No Kotlin Multiplatform, no domain/use-case layer for every feature, no MVI framework.
- No new user-facing features or behaviour changes (beyond the restyle already done before this).
- No splitting a feature into further modules (e.g. separate `data` modules per feature).
- No dynamic feature modules / Play Feature Delivery.

## Anti-goals

- Any behaviour, string, accessibility or test coverage lost in the move. Existing tests move with their code and
  stay green; the test count must not drop except for tests deliberately merged.
- Layer rules that still depend on documentation instead of Gradle.
- Copy-pasted build configuration in every module instead of convention plugins.
- A release build that compiles but crashes after R8.
- Breaking deep links (`<appScheme>://example/...`), saved state / process-death restoration, or the Room
  migration tests.

## Constraints

- Toolchain stays: AGP 9.4.0 (built-in Kotlin), Gradle 9.6.0, Kotlin 2.4.21, compileSdk/targetSdk 37, minSdk 24,
  JVM 17; all dependencies and plugins in `gradle/libs.versions.toml`.
- Robolectric tests use `@Config(sdk = [36])`; Hilt tests use `HiltAndroidRule` before the Compose rule and
  `HiltTestApplication`. On Windows, DataStore tests use the in-memory fake.
- Supported languages `en` and `id` (`values-in/`): each module carries its own strings; `generateLocaleConfig`
  stays on `:app`.
- Root package `com.primestrider.kotlinstarter`; each module gets its own namespace below it
  (e.g. `com.primestrider.kotlinstarter.core.network`, `…feature.products.impl`).
- Proof for every change: `./gradlew testDebugUnitTest ktlintCheck assembleDebug` (adjust task names for flavours,
  and add `lint` once configured).
- The move is done in small, green steps (module by module), not one giant commit.

## Left to the builder

- The exact split between `core:ui` and `core:designsystem`, and where shared example helpers
  (`ExamplePageScaffold`, `DemoSection`/`AnimExample`, `SaveableState`, `ThemeModeSelector`) live, as long as no
  `impl` depends on another `impl` (a small shared example-UI module or `core:ui` are both acceptable).
- Whether `api` modules expose only route types or also `NavController.navigateToX()` helpers.
- Order of the migration steps, convention-plugin names, CI workflow layout and caching.
- Whether Messenger/snackbar host lives in `core:designsystem` or `core:ui`.
- Keep rules needed for R8 after testing the minified build.
