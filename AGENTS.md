# AGENTS.md

Kotlin Android starter kit (Compose + Hilt), a port of a React Native boilerplate. Modular, in the style of Google's
Now in Android: `:app`, `:core:*`, and one `:api` + `:impl` pair per feature, with shared build setup in `build-logic/`.

## Toolchain

AGP 9.4.0 (built-in Kotlin), Gradle 9.6.0, Kotlin 2.4.21, compileSdk/targetSdk 37, minSdk 24, JVM target 17.
All dependencies and plugins are declared in `gradle/libs.versions.toml`. Parallel builds, the build cache and the
configuration cache are on (`gradle.properties`).

## Learning the codebase

`docs/learning/README.md` is a guided tour (module map, app start, data and auth, navigation, build and release) with
diagrams and quoted code, plus a chapter mapping every concept to React and Vue.

## Project map

Classes keep the package they had before the split (root `com.primestrider.kotlinstarter`); each module's
namespace follows its path (e.g. `…core.network`, `…feature.products.impl`).

```
build-logic/                 convention plugins: kotlinstarter.android.{application,library,test,compose}, kotlinstarter.hilt,
                             kotlinstarter.feature.{api,impl}; AndroidConfig (SDK levels), localOrEnv, verifyReleaseKeeps
app/                         MainActivity, AppViewModel, MainShell (tabs) and AppNavHost.kt: the only place that wires features;
                             EnvironmentModule provides @ApiBaseUrl (BuildConfig.API_URL) and @CertificatePins (API_PINS); PlaceholderHomeScreen
core/common                  @ApplicationScope, formatters (core.util), AppLanguage/LanguageController (core.i18n), CrashRecorder, AppConfig
core/model                   ThemeMode (shared domain types)
core/datastore               DataStore module, StorageKeys, DataStoreFiles, ThemePreferenceRepository
core/designsystem            theme, tokens, ControlColors, reusable components incl. form controls and AppNavigationSuite (bar/rail),
                             @ThemePreviews + PreviewTheme (docs/architecture/components.md)
core/network                 Retrofit/OkHttp, ApiError, safeApiCall, @Anonymous, @AppInterceptors extension point, optional
                             certificate pinning (@CertificatePins) (docs/architecture/networking.md)
core/auth                    SessionRepository, AuthAdapter seam (@AuthBackend, NotConfiguredAuthAdapter), bearer + 401 refresh (docs/architecture/auth-session.md)
core/ui                      CrashFallbackScreen, PermissionGate, PiP host/frame, ExamplePageScaffold/DemoSection
core/testing                 shared test doubles: in-memory DataStore, FakeSessionCipher, TestApi, resetAndroidUiDispatcher
feature/<name>/api           @Serializable routes other modules may navigate to
feature/<name>/impl          screens, ViewModels, data/ (api, dto, entity, dao, repository, mappers), Hilt modules, strings, tests
baselineprofile/             startup baseline profile generator and startup benchmark (needs a device)
scripts/remove-examples.sh   removes every example feature, optionally keeping them in .examples/ (see below)
scripts/rename-project.sh    renames package/applicationId, app name, classes, scheme and plugin ids for a new app (README)
```

Example features (all deletable): `catalog` (Home, Components tab, component and styling pages, form), `products`
(offline-first list cached in Room by a `RemoteMediator`, list-detail on wide screens), `todos` (plus the Glance
home-screen widget), `account` (Profile hub, sign-in, protected Account, DummyJSON `AuthAdapter`), `settings`, `devices`
(the Device APIs demos incl. the music foreground service, share target, in-app update and Play Integrity;
docs/examples.md), `animation`.

Every screen and design-system component file has a light/dark `@ThemePreviews` preview (`PreviewCoverageTest`).

## Layer rules (enforced by Gradle dependencies)

- `core:*` never depends on `feature:*`.
- `feature:X:impl` depends on `core:*` and on other features' `:api` only, never on another `:impl`.
- `feature:X:api` holds routes only (kotlinx serialization; `catalog:api` also the Home entry types).
- `:app` is the only module that depends on `:impl` modules; it wires them inside `// region example:<feature>` blocks.
- Before writing a new composable, check `docs/architecture/components.md`; form controls always come from `core/designsystem` (`NoRawMaterialControlsTest`).
- Screens are split into a stateless `XxxScreen(state, callbacks)` and a thin route that obtains the ViewModel via `hiltViewModel()`. One `StateFlow` UI state per ViewModel (MVVM + UDF); no use-case layer by default.
- DTOs and Room entities stay in a feature's `data/` package and are mapped to models before a ViewModel sees them; repositories are an `interface` + `Default…` implementation bound with `@Binds`.
- Every module carries its own strings in `values/` (en, fallback) and `values-in/` (id); string names are unique across modules (`TranslationCompletenessTest`). Docs, comments and README are written in English.

## Adding a page to a feature

1. Add a `@Serializable` route to the feature's `:api` module (package `com.primestrider.kotlinstarter.feature.example` for the examples, so serial names stay stable; `RouteNamesTest`).
2. Write a stateless screen and register `composable<YourRoute> { … }` in the feature's `NavGraphBuilder.<feature>Destinations(navController)` function.
3. To list it on Home, contribute a `CatalogEntry` with `@Provides @IntoSet` from the feature's Hilt module (and a `CatalogSection` if the feature owns a section).
4. Cover it with a Robolectric test in the feature module.

## Adding a feature module

1. Create `feature/<name>/api` (plugin `kotlinstarter.feature.api`) and `feature/<name>/impl` (plugin `kotlinstarter.feature.impl`, `api(projects.feature.<name>.api)`), and `include` both in `settings.gradle.kts`.
2. In `:app`: add both as dependencies and call `<name>Destinations(navController)` in the right tab graph of `AppNavHost.kt` (or add a `TopLevelTab` to `topLevelTabs` for a new tab).
3. Put the feature's manifest entries (permissions, providers, receivers) in its own `src/main/AndroidManifest.xml`.

## Removing the examples

`bash scripts/remove-examples.sh <dir>` deletes `feature/`, every `// region example:` block and the `:app` tests that
drive examples, then runs `ktlintFormat`. Run it on a copy (`git worktree add`), check `./gradlew test assembleDevDebug`,
then apply it to your project. Without examples the app shows `PlaceholderHomeScreen` with one tab, and sign-in reports
"not configured" until a feature binds an `@AuthBackend AuthAdapter`. CI runs this check on every push.

`bash scripts/remove-examples.sh --keep .examples` copies everything it deletes into `.examples/` first (the feature
modules, the `:app` files as they were before their example regions were stripped, and the deleted `:app` tests, each at
its original path) and adds that folder to `.gitignore`. It is not part of the Gradle build.

### Example code in `.examples/` (for agents)

If `.examples/` exists, it is the reference code of the removed examples. Before writing a feature, screen, API call,
form or Device API demo, look there for the matching example and follow its patterns. Rules:

- Search tools that respect `.gitignore` or skip hidden folders (ripgrep, Grep/Glob) do not see it: list or read it by
  explicit path (e.g. `ls .examples/feature`, then read the files), or search with `rg --no-ignore --hidden ... .examples`.
- Read and copy only. Never `include` it in `settings.gradle.kts`, depend on it or import from it; code you take from it
  moves into your own module and package.
- How a feature is wired into `:app` (dependencies, destinations, tabs): `.examples/app/`, whose files still contain the
  `// region example:<feature>` blocks.

## Testing conventions

Robolectric tests use `@RunWith(AndroidJUnit4::class)` and `@Config(sdk = [36])` (Robolectric 4.17 does not yet support SDK 37).
Hilt-injected components use `@HiltAndroidTest` with `HiltAndroidRule` ordered before the Compose rule and `@Config(application = HiltTestApplication::class)`.
Screenshot references (Roborazzi, `core/designsystem`) are recorded only by the manual CI job on Linux. See docs/architecture/testing.md.

## Commands

```
./gradlew test                 # unit + Robolectric tests of every module (:app runs on the dev flavour)
./gradlew ktlintCheck          # ktlint; ktlintFormat fixes formatting
./gradlew lint                 # Android Lint against each module's lint-baseline.xml
./gradlew assembleDevDebug     # debug APK of the dev flavour
./gradlew assembleProdRelease verifyReleaseKeeps   # minified release + check that R8 kept the routes
```

Proof command for every change: `./gradlew test ktlintCheck lint assembleDevDebug`.
On Windows use `gradlew.bat`.

## Skills

Step-by-step guides for the repeated tasks of this starter, one `SKILL.md` per folder in `.agents/skills/` (frontmatter
`name` + `description`, so agents that support skills can load them; anyone can read them). Pick the one matching the
task before starting; they link into `docs/architecture/` for the why.

| Skill | Use it to |
| --- | --- |
| `add-feature` | add a feature module (`:api` + `:impl`, data/model/ui, Hilt, wiring in `:app`, tests) |
| `add-screen-route` | add a screen and route, navigate between them, protect a page, add a deep link |
| `add-api-endpoint` | call a backend endpoint (Retrofit, DTO, mapping, `safeApiCall`, MockWebServer tests) |
| `add-translation` | add or change strings in English and Indonesian |
| `add-component-example` | show a component on the Components tab, or add a reusable design-system component |
| `add-permission-feature` | build a feature that needs a runtime permission or a device API |
| `swap-auth-backend` | connect sign-in and token refresh to a real backend |
| `remove-examples` | remove the seven example features safely (copy first, then apply) |
| `release-build` | bump the version, build and verify a signed release, distribute to testers |

`SkillsTest` checks the frontmatter and that this table lists every skill; `DocsLinksTest` checks the paths they quote.
