# Architecture & Modularisation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Split the single `:app` module into a Now-in-Android layout (`:core:*`, `:feature:<name>:api` + `:impl`, `build-logic`), with layer rules enforced by Gradle, a data layer that stops DTOs/entities at `data/`, a production-ready release build, and CI.

**Architecture:** Move code module by module, leaves first (`core:testing/common/model` → `datastore` → `designsystem` → `network`/`auth` → `ui`), then one feature at a time, keeping the build green after every task. Each feature exposes `@Serializable` routes in `:api` and a `NavGraphBuilder` extension + Hilt bindings in `:impl`; `:app` is the only module that depends on `:impl` modules and wires them inside marked `// region example:<name>` blocks, so a script can delete examples and prove the app still runs.

**Tech Stack:** AGP 9.4.0 (built-in Kotlin), Gradle 9.6.0, Kotlin 2.4.21, Hilt + KSP, Navigation Compose (type-safe), Room, Retrofit/OkHttp, Robolectric 4.17, Roborazzi, androidx.baselineprofile, GitHub Actions.

**Spec:** `docs/superpowers/specs/2026-10-31-architecture-modularisation-design.md`

## Global Constraints

- Toolchain stays: AGP 9.4.0, Gradle 9.6.0, Kotlin 2.4.21, compileSdk/targetSdk 37, minSdk 24, JVM 17; every dependency and plugin in `gradle/libs.versions.toml`.
- Module graph rules (enforced by Gradle): `core:*` never depends on `feature:*`; `feature:X:impl` depends only on `core:*` and `feature:Y:api`; `feature:X:api` depends on kotlinx-serialization only (at most `core:common`); `:app` is the only module depending on `feature:*:impl`.
- Namespaces: `com.primestrider.kotlinstarter.<path>` with dots for colons, e.g. `…core.network`, `…feature.products.impl`, `…feature.products.api`. Keep existing package names of moved classes when they already match; otherwise move the package to the module namespace.
- Each module owns its strings in `src/main/res/values/` and `values-in/`; `generateLocaleConfig` stays on `:app`.
- MVVM + UDF: one `StateFlow` UI state per ViewModel, stateless `XxxScreen`, `XxxRoute` with `hiltViewModel()`. No MVI framework, no use-case layer by default.
- Robolectric tests: `@Config(sdk = [36])`; Hilt tests: `HiltAndroidRule` before the Compose rule, `HiltTestApplication`; on Windows DataStore tests use the in-memory fake.
- Nothing lost in the move: no behaviour, string, accessibility or test removed; test count never drops except for deliberately merged tests (note each merge in the ledger). Deep links `<appScheme>://example/...`, saved state and Room migration tests keep working.
- Proof after every task (Windows: `gradlew.bat`): until Task 14 `gradlew.bat test ktlintCheck assembleDebug`; from Task 14 on (flavours exist) `gradlew.bat test ktlintCheck assembleDevDebug`; from Task 15 on add `lint`. `test` runs every module's debug unit tests (AGP 9 creates debug unit tests only).
- One commit per green step at least; never one giant commit.

## Current state (facts the tasks rely on)

- Single module `:app`, root package `com.primestrider.kotlinstarter`, 159 test files, 671 tests.
- Cycle: `core/network/NetworkModule.kt` imports `core.auth.AuthInterceptor` and `TokenAuthenticator`; `core/auth` imports `core.network` (`ApiBaseUrl`, `isAnonymous`).
- `core/designsystem/theme/ThemePreferenceRepository.kt` imports `core.storage.StorageKeys`; `core/auth/SessionExpiryAnnouncer.kt` uses designsystem `Messenger`/`UiText` and string `auth_session_expired`.
- `feature/example/todos/ExampleDatabase.kt` (v2, `example.db`) holds `TodoEntity` and the devices demo's `sdk/sqlite/TransactionEntity` + `TransactionDao`; `MIGRATION_1_2` creates `transactions`; schemas in `app/schemas/…ExampleDatabase/{1,2}.json`; `ExampleDatabaseMigrationTest` lives under `sdk/sqlite` tests.
- Root `feature/example/` files: `ExampleRoutes.kt` (all routes), `ExampleCatalog.kt` (`ExampleEntry`, `ExampleSection`, `componentEntries`, `exampleSections`, which call `sdkEntries()` and `animationEntries()`), `ExamplePageScaffold.kt` (+ `DemoSection`), `HomeScreen.kt`, `HomeFilter.kt`, `ComponentsHomeScreen.kt`, `ThemeModeSelector.kt`, `SaveableState.kt`.
- `app/navigation/AppNavHost.kt` registers every destination in `homeGraph`, `componentsGraph`, `profileGraph`; `MainShell.kt` has `enum TopLevelTab { HOME, COMPONENTS, PROFILE }`; `ThemeRoute` uses `SettingsViewModel`; `ProfileScreen` uses `AccountViewModel` and links to Settings.
- `BuildConfig` users: `APP_SCHEME` (AppNavHost deep link, sdk browser, sdk notifications), `API_URL` (NetworkModule), `DEBUG` (NetworkModule, sdk background, SettingsScreen, migration test).
- Tests reading paths: `NoRawMaterialControlsTest` (`src/main/java`), `TranslationCompletenessTest` (`src/main/res`).
- Shared test helpers: `testing/InMemoryPreferencesDataStore`, `GatedPreferencesDataStore`, `AndroidUiDispatcherReset`; `core/auth/FakeSessionCipher`, `FakeAuthAdapter`; `core/network/TestApi`.

## Rulings made in this plan (the spec leaves these open)

1. **auth ↔ network cycle:** `core:network` no longer knows auth. It builds the app `OkHttpClient` from a Hilt `@Multibinds Set<Interceptor>` (qualifier `@AppInterceptors`) and an optional `@BindsOptionalOf Authenticator`. `core:auth` depends on `core:network` and contributes `AuthInterceptor` `@IntoSet` and binds `TokenAuthenticator` as the `Authenticator`.
2. **API URL per flavour:** `:app` owns environment config and provides `@ApiBaseUrl HttpUrl` from its `BuildConfig.API_URL` (Hilt module in `:app`); `core:network` only consumes it. `APP_SCHEME` is not flavour-dependent: `core:common` gets a `BuildConfig.APP_SCHEME` field set by build-logic from the same source as the app's manifest placeholder, exposed as `AppConfig.appScheme`. Modules needing `DEBUG` enable their own `buildConfig`.
3. **Todos and devices share a database today.** Each feature gets its own: todos keeps `example.db` and adds `MIGRATION_2_3` dropping `transactions` (version 3); devices gets `DevicesDatabase` (`devices.db`, version 1, `transactions`). The devices demo's saved rows are not carried over on upgrade (demo data; documented). Both modules export schemas to `<module>/schemas` and keep migration tests.
4. **Theme:** `ThemeMode` moves to `core:model`; `ThemePreferenceRepository` to `core:datastore`; `ThemeModeSelector` to `core:designsystem/component` (strings renamed `component_theme_*`). The Theme styling page gets its own small `ThemeViewModel` in `catalog:impl` instead of `SettingsViewModel`.
5. **Shared example UI** (`ExamplePageScaffold`, `DemoSection`, `rememberSaveableStateList`) goes to `core:ui` with the crash screen, `PermissionGate`, PiP frame/host. `Messenger`/snackbar host stays in `core:designsystem`.
6. **Strings used by more than one feature** are duplicated into each feature under its own prefix (e.g. account's link to Settings gets `account_settings_link`); strings used by core stay in the core module that uses them (`auth_session_expired` → `core:auth` renamed `core_auth_session_expired`; `common_error_*` → `core:network`; `crash_*`, `permission_*` → `core:ui`; `nav_*`, `app_name` → `:app`; `component_*` → `core:designsystem`).
7. **Home catalog:** `catalog:api` defines `CatalogEntry(title, subtitle, route: Any, section: CatalogSection)` and `CatalogSection(id, title, description, icon, order)`, plus Hilt multibinding sets `Set<CatalogEntry>` (`@HomeEntries`) and (`@ComponentEntries`). Each `impl` contributes `@IntoSet` from its own Hilt module with its own strings; `catalog:impl` renders the set grouped by section (sorted by `order`, entries keep a per-entry `order`).
8. **Tabs:** `:app` defines `val topLevelTabs: List<TopLevelTab>` (data class, not enum) assembled in `AppTabs.kt` inside region markers; when the list is empty, `:app`'s `PlaceholderHomeScreen` is the single tab and the bottom bar hides with one tab.
9. **Deletability proof:** `scripts/remove-examples.sh <dir>` deletes `feature/*` folders, their `include` lines and every `// region example:` … `// endregion` block (Kotlin) / `# region example` block (Gradle) in a copy, then the CI job `examples-removed` builds and tests the copy. Locally it is a documented manual run.
10. **Path-scanning tests** (`NoRawMaterialControlsTest`, `TranslationCompletenessTest`) live in `:app` tests and scan every module: walk `rootProject` (`File("..")` from `app/`) for `*/src/main/java` and `*/src/main/res`, skipping `build/` and `build-logic/`; each asserts it scanned more than 10 modules so it cannot pass vacuously. The guard also matches wildcard imports and fully-qualified calls (closes the plan-07 deferred minor).
11. **Baseline profile:** the `:baselineprofile` module and startup journey are added and build in CI; generating needs a device, so the profile file is produced by the documented local step and committed by the owner. Until then release ships without one.
12. **Screenshot references** are recorded only by the manual `record-screenshots` CI job on Linux; the `verify` step runs only when `core/designsystem/src/test/screenshots/` exists.

## Review Focus

1. **Process death / saved state across module boundaries:** routes moved to `:api` keep the same serial names, so a restored back stack still decodes. Pinned by `restored_back_stack_survives_route_move` in Task 7 (encode a route with the old FQN → decode after move fails only if names change: assert `serializer<ProductDetailRoute>().descriptor.serialName` equals the pre-move value captured in Task 7 step 1).
2. **Deep links** `kotlinstarter://example/sdk?id=…` and notification/payment deep links still open the demo after devices moves. Pinned by the existing `DeepLinkTest` / `CrashDeepLinkTest`, which must stay in `:app` and pass in Task 12.
3. **Release build after R8** crashes on reflection (kotlinx serialization routes, Retrofit interfaces, Hilt workers, Room). Pinned by Task 14's `assembleProdRelease` + launching the minified APK on the emulator if one exists, and the keep-rule check `release_mapping_keeps_routes` (grep `mapping.txt` for every `@Serializable` route class kept).
4. **Upgrade from the single-module app:** existing installs keep todos (Room `example.db` 2→3), DataStore files and the encrypted session (same file names, same keystore alias). Pinned by `migrate_2_to_3_keeps_todos_and_drops_transactions` (Task 9) and `datastore_file_names_unchanged` (Task 3).
5. **All examples removed:** app builds, tests, starts, shows the placeholder Home, sign-in reports "not configured", no duplicate Hilt bindings. Pinned by Task 13's `examples-removed` run and `not_configured_adapter_reports_not_set_up` (Task 5).

---

### Task 1: build-logic and Gradle properties

**Files:**
- Create: `build-logic/settings.gradle.kts`, `build-logic/convention/build.gradle.kts`, `build-logic/convention/src/main/kotlin/` plugins: `AndroidApplicationConventionPlugin` (`kotlinstarter.android.application`), `AndroidLibraryConventionPlugin` (`kotlinstarter.android.library`), `AndroidComposeConventionPlugin` (`kotlinstarter.android.compose`), `HiltConventionPlugin` (`kotlinstarter.hilt`), `AndroidFeatureImplConventionPlugin` (`kotlinstarter.feature.impl`: library + compose + hilt + deps on `core:designsystem`, `core:ui`, `core:common`, navigation, hilt-navigation-compose, lifecycle), `FeatureApiConventionPlugin` (`kotlinstarter.feature.api`: library + serialization), `AndroidTestConventionPlugin` (applied by library/application: Robolectric `testOptions`, `--add-opens` jvmArg, common test deps), `KtlintConventionPlugin`.
- Modify: `settings.gradle.kts` (`pluginManagement { includeBuild("build-logic") }`), `gradle/libs.versions.toml` (plugin ids for the conventions + `android-gradlePlugin`, `kotlin-gradlePlugin`, `ksp-gradlePlugin` artifacts for build-logic), `gradle.properties` (`org.gradle.parallel=true`, `org.gradle.caching=true`, `org.gradle.configuration-cache=true`), `app/build.gradle.kts` (apply `kotlinstarter.android.application`, `kotlinstarter.android.compose`, `kotlinstarter.hilt`; keep only app-specific config).
- Shared constants in build-logic: `compileSdk = 37`, `minSdk = 24`, `targetSdk = 37`, JVM 17; `fun Project.localOrEnv(key: String, env: String, default: String): String` (local.properties → env → default), used for `app.scheme`/`APP_SCHEME` and `api.url`/`API_URL`.

- [ ] **Step 1:** Record the baseline: `gradlew.bat test` → note the test count (expected 671, 1 skipped) in the ledger.
- [ ] **Step 2:** Create build-logic and move `:app` onto the conventions without changing any behaviour.
- [ ] **Step 3:** Enable the three `gradle.properties` flags; fix anything that breaks configuration cache (e.g. `local.properties` reads must go through `providers`), never disable it.
- [ ] **Step 4:** Run `gradlew.bat test ktlintCheck assembleDebug` twice; expected: BUILD SUCCESSFUL, same test count, second run reports "Configuration cache entry reused".
- [ ] **Step 5:** Commit `build: add build-logic convention plugins and enable parallel, build and configuration caches`.

### Task 2: core:testing, core:common, core:model

**Files:**
- Create modules `core/testing`, `core/common`, `core/model` (+ `include` lines).
- Move to `core:common`: `core/common/CoroutinesModule.kt` (`@ApplicationScope`), `core/util/*` (formatters, `currentLocale`), `core/i18n/*` (`AppLanguage`, `LanguageController`), `core/crash/CrashRecorder.kt` (not the screen); new `AppConfig` object exposing `appScheme` from `core:common`'s `BuildConfig.APP_SCHEME` (set by build-logic, ruling 2).
- Move to `core:model`: `ThemeMode` (pure Kotlin, no Android dependency; make it a JVM/Kotlin library module or an Android library without resources).
- Move to `core:testing` (main source set, so other modules' tests use it via `testImplementation(projects.core.testing)`): `InMemoryPreferencesDataStore`, `GatedPreferencesDataStore`, `AndroidUiDispatcherReset`, a `MainDispatcherRule` if present in tests.
- Tests move with their code (`core/util`, `core/i18n`, `core/crash` recorder tests).

**Interfaces:**
- Produces: `object AppConfig { val appScheme: String }` in `com.primestrider.kotlinstarter.core.common`; `ThemeMode` in `com.primestrider.kotlinstarter.core.model`; test helpers in `com.primestrider.kotlinstarter.core.testing`.

- [ ] **Step 1: Failing test** `AppConfigTest.app_scheme_matches_build_value`: `assertEquals("kotlinstarter", AppConfig.appScheme)` (default when no override).
- [ ] **Step 2:** Run `gradlew.bat :core:common:testDebugUnitTest` → FAIL (module/class missing).
- [ ] **Step 3:** Create modules, move code with `git mv`, update imports in `:app`.
- [ ] **Step 4:** `gradlew.bat test ktlintCheck assembleDebug` → green, test count unchanged + 1.
- [ ] **Step 5:** Commit `refactor: extract core:common, core:model and core:testing modules`.

### Task 3: core:datastore

**Files:** module `core/datastore`; move `core/storage/*` (`DataStoreModule`, `StorageKeys`, `@SessionStore`) and `core/designsystem/theme/ThemePreferenceRepository.kt` (ruling 4) with their tests; package becomes `…core.datastore`.

- [ ] **Step 1: Failing test** `DataStoreFileNamesTest.datastore_file_names_unchanged`: assert the preference and session file names the module uses equal the current literals (read them from `DataStoreModule.kt` before moving and paste the exact values into the test).
- [ ] **Step 2:** Run → FAIL (module missing).
- [ ] **Step 3:** Move; `core:designsystem` code stays in `:app` for now and imports `ThemePreferenceRepository` from `core:datastore`.
- [ ] **Step 4:** Proof → green.
- [ ] **Step 5:** Commit `refactor: extract core:datastore`.

### Task 4: core:designsystem

**Files:** module `core/designsystem` (compose convention); move `core/designsystem/theme/*`, `core/designsystem/component/*` (incl. `Messenger`), `feature/example/ThemeModeSelector.kt` → `component/ThemeModeSelector.kt` (ruling 4), fonts `res/font/plus_jakarta_sans_*`, every `component_*` string and the `settings_theme_*` strings renamed `component_theme_*` (both locales); designsystem tests incl. `PixelAssertions`, `TextFieldLookTest`, `TogglesTest`.
- Move `NoRawMaterialControlsTest` and `TranslationCompletenessTest` to `app/src/test/.../architecture/` and make them scan all modules (ruling 10).
- `AppTheme` takes `ThemeMode` from `core:model`.

- [ ] **Step 1: Failing tests:** in the moved `NoRawMaterialControlsTest`, add `scans_every_module` (`assertTrue(scannedModules >= 2)` now; Task 13 raises the floor to 10 once all modules exist) and `flags_wildcard_and_qualified_use` (feed the matcher the strings `import androidx.compose.material3.*` and `androidx.compose.material3.Switch(` → both flagged).
- [ ] **Step 2:** Run → FAIL.
- [ ] **Step 3:** Move the module, widen the guard regex, update imports.
- [ ] **Step 4:** Proof → green; `TranslationCompletenessTest` still finds en/id parity across both modules.
- [ ] **Step 5:** Commit `refactor: extract core:designsystem and scan every module in the architecture tests`.

### Task 5: core:network and core:auth without the cycle

**Files:** modules `core/network`, `core/auth`.
- `core:network`: `ApiError`, `safeApiCall`, `toApiError`, `@Anonymous`, `@ApiBaseUrl`, `@RefreshClient`, `ApiErrorMessage` (+ `common_error_*` strings), `NetworkModule` changed per ruling 1: `@Multibinds @AppInterceptors abstract fun interceptors(): Set<Interceptor>`, `@BindsOptionalOf abstract fun authenticator(): Authenticator`; the app client adds every interceptor in the set and the authenticator when present. It no longer provides `@ApiBaseUrl` (ruling 2).
- `:app`: new `app/di/EnvironmentModule.kt` providing `@ApiBaseUrl HttpUrl` from `BuildConfig.API_URL`.
- `core:auth` (depends on network, datastore, designsystem, common): session, cipher, interceptor (`@IntoSet @AppInterceptors`), `TokenAuthenticator` (`@Binds` as `Authenticator`), refresher, `SessionExpiryAnnouncer` (+ `core_auth_session_expired`), `AuthAdapter`, and new `NotConfiguredAuthAdapter` used through `@BindsOptionalOf AuthAdapter` + a provider returning the bound one or the fallback.
- Move `FakeSessionCipher`, `FakeAuthAdapter`, `TestApi` into `core:testing` (they are used across modules).

**Interfaces:**
- Produces: `@AppInterceptors` qualifier (`core.network`); `NotConfiguredAuthAdapter : AuthAdapter` whose `signIn` returns `AuthResult` failure with message resource `core_auth_not_configured` ("Sign-in is not set up yet") and whose `refresh` throws so the session stays signed out.

- [ ] **Step 1: Failing tests:** `NetworkModuleTest.app_client_uses_contributed_interceptors_and_optional_authenticator` (empty set + absent authenticator builds a client; a contributed interceptor runs on a MockWebServer call) and `NotConfiguredAuthAdapterTest.not_configured_adapter_reports_not_set_up`.
- [ ] **Step 2:** Run → FAIL.
- [ ] **Step 3:** Move and rewire. Existing `AuthFlowTest`, refresh single-flight, stale-401 and host-check tests move with `core:auth` unchanged.
- [ ] **Step 4:** Proof → green; Gradle must reject an import of `core.auth` from `core:network` (verify by `gradlew.bat :core:network:dependencies --configuration debugCompileClasspath` showing no `core:auth`).
- [ ] **Step 5:** Commit `refactor: extract core:network and core:auth, break the auth/network cycle with an interceptor multibinding`.

### Task 6: core:ui

**Files:** module `core/ui`; move `core/crash/CrashFallbackScreen.kt` (+ `crash_*`), `core/permission/*` (+ `permission_*`), `core/pip/*`, and from `feature/example/`: `ExamplePageScaffold.kt` (`ExamplePageScaffold`, `DemoSection` becomes public), `SaveableState.kt`; their tests.

- [ ] **Step 1: Failing test:** `ExamplePageScaffoldTest.demo_section_title_is_a_heading` (heading semantics on the section title; skip if an equivalent test already exists and move that one instead, noting it).
- [ ] **Step 2:** Run → FAIL. **Step 3:** Move. **Step 4:** Proof → green. **Step 5:** Commit `refactor: extract core:ui`.

### Task 7: catalog feature and the app wiring pattern

**Files:**
- `feature/catalog/api`: routes `ComponentsHomeRoute`, `ActionsRoute`, `InputsRoute`, `DisplayRoute`, `OverlaysRoute`, `FeedbackRoute`, `FormExampleRoute`, styling routes `ColorsRoute` … `ThemeRoute`; `CatalogEntry`, `CatalogSection`, qualifiers `@HomeEntries`, `@ComponentEntries` (ruling 7).
- `feature/catalog/impl`: `HomeScreen`, `HomeFilter`, `ComponentsHomeScreen`, component pages, styling pages, form example, `ThemeViewModel` (ruling 4), `CatalogNavigation.kt` with `fun NavGraphBuilder.catalogHomeDestinations(onOpen: (Any) -> Unit, onBack: () -> Unit)` and `fun NavGraphBuilder.catalogComponentsDestinations(...)`, Hilt module contributing its own entries (Features: Form; Styling section; Components entries) and declaring the sections `features`, `devices`, `animation`, `styling` with their order.
- `:app`: `AppNavHost.kt` calls the feature extensions inside `// region example:catalog` blocks; `AppTabs.kt` with `data class TopLevelTab(label, icon, graph, startRoute)` and `topLevelTabs` (ruling 8); `HomeRoute`/graph objects stay in `:app`.
- Keep `ProductsRoute`, `TodosRoute`, `SdkDemoRoute`, `Anim*Route`, account/settings routes in `:app` (`app/navigation/LegacyRoutes.kt`) until their own task moves them.

- [ ] **Step 1: Failing tests:** `RouteNamesTest.restored_back_stack_survives_route_move` — before moving, capture `serializer<X>().descriptor.serialName` for every route into the test as literals, then assert them after the move (if Kotlin derives names from the FQN, annotate moved routes with `@SerialName("<old fqn>")` so restored back stacks decode); `HomeEntriesTest.home_lists_contributed_entries_by_section_order` (fake set with two sections out of order renders in `order`).
- [ ] **Step 2:** Run → FAIL. **Step 3:** Move and wire. **Step 4:** Proof → green; existing `AppNavHostTest`, `MainShellTest`, Home tests pass. **Step 5:** Commit `refactor: extract the catalog feature and wire features through region-marked blocks in app`.

### Task 8: products feature with a mapped data layer

**Files:** `feature/products/api` (`ProductsRoute`, `ProductDetailRoute(id)`), `feature/products/impl` with `data/` (`ProductsApi`, `ProductDto`, `ProductsPagingSource`, `ProductsRepository` interface + `DefaultProductsRepository`, `ProductMappers.kt`) and `ui/` (screens, routes, ViewModels); model `Product` in `…feature.products.impl.model` (feature-local; `core:model` only when a second feature needs it); Hilt `@Binds`; catalog entry `@IntoSet` in the Features section; strings `products_*`.
- `ProductFixtures`/`FakeProductsRepository` implement the interface.

- [ ] **Step 1: Failing tests:** `ProductMappersTest.dto_maps_to_product` (every field the UI shows, incl. null/blank handling as today); `ProductsArchitectureTest.ui_never_sees_dto` (scan `impl/src/main/java/**/ui/` for `Dto` → none).
- [ ] **Step 2:** Run → FAIL. **Step 3:** Move, add mapping, switch `open class` to interface + default. **Step 4:** Proof → green; paging/search/detail tests unchanged in assertions. **Step 5:** Commit `refactor: extract the products feature and map DTOs to a Product model`.

### Task 9: todos feature with its own database

**Files:** `feature/todos/api` (`TodosRoute`), `feature/todos/impl`: `data/` (`ExampleDatabase` v3 with `TodoEntity` only, `TodoDao`, `MIGRATION_1_2` kept, new `MIGRATION_2_3` drops `transactions`, `TodosRepository` interface + default, `TodoMappers.kt` → model `Todo`), `ui/`; Room/KSP config `room.schemaLocation = "$projectDir/schemas"`; move `app/schemas/…ExampleDatabase/*.json` to `feature/todos/impl/schemas/` (keep the directory name of the database class FQN; if the package changes, keep `@Database` class FQN identical or copy schemas under the new FQN folder); debug assets source set for schemas; migration test moves here.
- Leaves `TransactionEntity`/`TransactionDao` in `:app` devices code backed by a temporary `DevicesDatabase` until Task 12 moves it (ruling 3).

- [ ] **Step 1: Failing tests:** `ExampleDatabaseMigrationTest.migrate_2_to_3_keeps_todos_and_drops_transactions` (insert todo rows at v2, migrate, todos intact, table `transactions` gone); `DevicesDatabaseTest.transactions_live_in_devices_db`; `TodosArchitectureTest.ui_never_sees_entity`.
- [ ] **Step 2:** Run → FAIL. **Step 3:** Implement. **Step 4:** Proof → green; existing 1→2 migration test still passes. **Step 5:** Commit `refactor: extract the todos feature with its own database and map entities to a Todo model`.

### Task 10: settings feature

**Files:** `feature/settings/api` (`SettingsRoute`), `feature/settings/impl` (`SettingsScreen`, `SettingsViewModel`, developer "crash the app" option, strings `settings_*`).
- [ ] **Step 1: Failing test:** `SettingsNavigationTest.settings_destination_is_registered_by_feature` (a `TestNavHostController` graph built only from `settingsDestinations()` can navigate to `SettingsRoute`).
- [ ] **Step 2–5:** run → FAIL, move, proof → green, commit `refactor: extract the settings feature`.

### Task 11: account feature

**Files:** `feature/account/api` (`ProfileRoute`, `AccountRoute`, `SignInRoute`), `feature/account/impl`: profile hub, sign-in, protected Account (guard logic unchanged), `data/` `DummyJsonAuthAdapter`, `DummyJsonApi` + DTOs, `ExampleAuthModule` binding `AuthAdapter` (fills the `@BindsOptionalOf`), strings `auth_*` (+ `account_settings_link`, ruling 6); depends on `feature:settings:api` for the Settings link.
- [ ] **Step 1: Failing test:** `AuthAdapterBindingTest.account_feature_replaces_not_configured_adapter` (Hilt test in `:app`: injected `AuthAdapter` is `DummyJsonAuthAdapter`).
- [ ] **Step 2–5:** FAIL, move, proof → green (`ProtectedRouteTest`, `AccountRestoreTest` pass), commit `refactor: extract the account feature`.

### Task 12: devices feature

**Files:** `feature/devices/api` (`SdkDemoRoute(id, amount?)`), `feature/devices/impl`: every `sdk/*` package, `DevicesDatabase` + `TransactionEntity`/`TransactionDao` (+ schemas folder), `SyncWorker` (`@HiltWorker`), `NotificationReceiver`, catalog entries (Device APIs section), strings `sdk_*`, drawable `sdk_ic_notification`, `xml/file_paths.xml`; manifest of `impl` declares its permissions, `<queries>`, FileProvider `${applicationId}.files`, receiver; `:app` manifest keeps INTERNET/ACCESS_NETWORK_STATE, the activity, deep-link filter, locale service, and the `InitializationProvider` WorkManager removal.
- [ ] **Step 1: Failing test:** `DevicesManifestTest.merged_manifest_keeps_device_permissions` (`:app` Robolectric test reading `PackageManager` requested permissions: CAMERA, POST_NOTIFICATIONS, READ_CONTACTS, … all present).
- [ ] **Step 2–5:** FAIL, move, proof → green (`DeepLinkTest`, `CrashDeepLinkTest`, notification/payment deep links), commit `refactor: extract the devices feature with its own manifest and database`.

### Task 13: animation feature, placeholder Home, deletability proof

**Files:** `feature/animation/api` (`Anim*Route`), `feature/animation/impl` (pages + entries + `anim_*` strings); `:app` `PlaceholderHomeScreen` (`app_placeholder_title` "Start building here", `app_placeholder_body`), `AppTabs.kt` fallback (ruling 8); `scripts/remove-examples.sh` (ruling 9); raise `scans_every_module` floor to 10.
- [ ] **Step 1: Failing tests:** `AppTabsTest.empty_feature_tabs_fall_back_to_placeholder_home` (with `topLevelTabsOf(emptyList())` the shell shows the placeholder and no bottom bar); `MainShellTest.single_tab_hides_bottom_bar`.
- [ ] **Step 2:** Run → FAIL. **Step 3:** Move animation, add placeholder + fallback + script.
- [ ] **Step 4:** Proof → green. Then run the script on a copy (`git worktree add ../ks-noexamples HEAD` or a temp copy), `bash scripts/remove-examples.sh ../ks-noexamples`, and in the copy `gradlew.bat test assembleDebug` → green; install and launch on an emulator if one exists. Record the result in the ledger; remove the copy.
- [ ] **Step 5:** Commit `refactor: extract the animation feature, add the placeholder Home and the examples-removed check`.

### Task 14: release build, flavours, signing

**Files:** `app/build.gradle.kts` (via conventions where generic): flavour dimension `environment`, flavours `dev` (`applicationIdSuffix = ".dev"`, `versionNameSuffix = "-dev"`) and `prod`; `buildConfigField API_URL` per flavour, both default `https://dummyjson.com/` with the `local.properties api.url` → env `API_URL` override on top; release `isMinifyEnabled = true`, `isShrinkResources = true`; `signingConfigs.release` from env `ANDROID_KEYSTORE_PATH`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD` (or the same keys in `local.properties`), falling back to the debug signing config when absent; `app/proguard-rules.pro` with only the keep rules the minified build proves necessary (serialization routes/DTOs, Retrofit, Hilt workers if needed).
- [ ] **Step 1: Failing check** `release_mapping_keeps_routes`: a small Gradle verification task (`verifyReleaseKeeps`, in build-logic) that reads `build/outputs/mapping/prodRelease/mapping.txt` and fails if any class annotated `@Serializable` in a `:api` module is renamed. Run `gradlew.bat assembleProdRelease verifyReleaseKeeps` → FAIL (task missing / minify off).
- [ ] **Step 2:** Implement flavours, signing, minify, keep rules.
- [ ] **Step 3:** `gradlew.bat test ktlintCheck assembleDevDebug assembleProdRelease verifyReleaseKeeps` → green; `apksigner verify` on the release APK (debug-signed fallback is fine locally); if an emulator is attached, install prod release and open Home, Products, Todos, a Device API demo and sign-in; otherwise ledger that it was not run.
- [ ] **Step 4:** Commit `build: add dev and prod flavours, env-based release signing, and R8 with resource shrinking`.

### Task 15: Android Lint and CI

**Files:** build-logic lint config for every module (`abortOnError = true`, `warningsAsErrors = false`, `checkDependencies = true` on `:app`, `baseline = file("lint-baseline.xml")`); generated `lint-baseline.xml` files; `.github/workflows/ci.yml` (ubuntu-latest, JDK 17, `gradle/actions/setup-gradle` cache): job `check` runs `./gradlew test ktlintCheck lint assembleDevDebug`, uploads test reports on failure; job `examples-removed` runs the script on a copy and `./gradlew test assembleDevDebug` there; job `record-screenshots` (`workflow_dispatch` only) defined in Task 16.
- [ ] **Step 1:** Run `gradlew.bat lint` with the config → baselines created; second run → BUILD SUCCESSFUL.
- [ ] **Step 2:** Validate the workflow YAML (parse it with `node -e` + a YAML parser available locally, or `actionlint` if installed); ledger that it has not run on GitHub (no remote yet).
- [ ] **Step 3:** Commit `ci: add Android Lint with baselines and a GitHub Actions workflow`.

### Task 16: Roborazzi screenshot tests

**Files:** Roborazzi plugin + deps in the catalog and the compose library convention; `core/designsystem/src/test/.../screenshot/ControlsScreenshotTest.kt` capturing light and dark: `AppTextField` (empty, focused, filled, error, disabled, prefix), `SearchField`, `SelectField`, `AppCheckbox` (off/on/indeterminate), `AppRadioButton`, `AppSwitch`, `SegmentedChoice`, `MultiSegmentedChoice`, `LabeledSlider`, `LabeledRangeSlider`; output dir `core/designsystem/src/test/screenshots/`; CI: `check` job runs `verifyRoborazziDebug` only if that directory exists; `record-screenshots` job runs `recordRoborazziDebug` and uploads the images as an artifact. Docs (Task 18) say never record on Windows.
- [ ] **Step 1:** `gradlew.bat :core:designsystem:recordRoborazziDebug` locally once to prove the tests run (do not commit those images).
- [ ] **Step 2:** `gradlew.bat test` → green (screenshot tests run in compare-off mode by default).
- [ ] **Step 3:** Commit `test: add Roborazzi screenshot tests for the design-system controls`.

### Task 17: baseline profile module

**Files:** `baselineprofile/` module (`com.android.test` + `androidx.baselineprofile`), `StartupBaselineProfile` generator (cold start → Home visible → open Products), `StartupBenchmark` macrobenchmark; `:app` applies `androidx.baselineprofile` consumer and `profileinstaller`. Ruling 11.
- [ ] **Step 1:** `gradlew.bat :baselineprofile:assemble` → BUILD SUCCESSFUL (module builds without a device).
- [ ] **Step 2:** Proof → green. **Step 3:** Commit `perf: add a baseline profile and startup benchmark module`.

### Task 18: documentation

**Files:** `AGENTS.md` (project map by module, Gradle-enforced rules, commands with flavour tasks, "add a feature module", "add a page", "delete the examples" with the script), `README.md`, `docs/architecture/overview.md`, `navigation.md`, `data-loading.md`, `storage-database.md` (fix: `./gradlew test` runs `testDebugUnitTest` per module; AGP 9 creates debug unit tests only; mention the two databases and the dropped `transactions` table), `components.md`, `testing.md` (core:testing, screenshot recording only in CI), `release.md` (flavours, signing env vars, R8, baseline profile step).
- [ ] **Step 1:** Write the docs; every file path mentioned must exist (`DocsLinksTest.every_code_path_in_docs_exists` in `:app` tests: regex backticked paths ending in `.kt`/`.kts`/`.md`/`.xml`, assert each exists relative to root). Run → FAIL first on stale paths, fix docs → PASS.
- [ ] **Step 2:** Full proof → green. **Step 3:** Commit `docs: describe the modular architecture, flavours, CI and how to remove the examples`.
