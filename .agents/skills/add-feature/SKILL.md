---
name: add-feature
description: Use when adding a new feature module to this starter (a new area of the app such as Orders or Chat) - creates the :feature:<name>:api and :impl modules with data/, model/ and ui/ packages, Hilt bindings, navigation wiring in :app, strings and tests, following the Now-in-Android layout and the layer rules Gradle enforces.
---

# Add a feature module

A feature is two Gradle modules: `:feature:<name>:api` (routes other modules may navigate to) and
`:feature:<name>:impl` (everything else). Only `:app` depends on `:impl`. Read `AGENTS.md` (Layer rules) first;
`products` is the reference feature to copy from (`feature/products/impl/build.gradle.kts`).

## Steps

1. **Modules.** Create `feature/<name>/api/build.gradle.kts` with `id("kotlinstarter.feature.api")` and
   `feature/<name>/impl/build.gradle.kts` with `id("kotlinstarter.feature.impl")`, each with its own
   `android { namespace = "com.primestrider.kotlinstarter.feature.<name>.api|impl" }`. In `impl` add
   `api(projects.feature.<name>.api)` plus only what you use (`projects.core.network`, `projects.core.datastore`,
   `projects.core.auth`, another feature's `:api`, libraries from `gradle/libs.versions.toml`). Never another `:impl`.
   Add both `include(":feature:<name>:api")` / `include(":feature:<name>:impl")` lines to `settings.gradle.kts`.
2. **Routes** (`api`): `@Serializable data object <Name>Route` / `data class <Name>DetailRoute(val id: Long)`. See the
   `add-screen-route` skill for naming and package rules.
3. **Data layer** (`impl`, package `…<name>.data`):
   - API types (`@Serializable` DTOs, Retrofit interface) or Room entities/DAO stay here (see `add-api-endpoint`).
   - A model in `…<name>.model` (plain data class) and a mapper `fun <Name>Dto.to<Name>()`.
   - `interface <Name>Repository` + `class Default<Name>Repository @Inject constructor(...)` that returns models.
   - A Hilt module (`@Module @InstallIn(SingletonComponent::class) abstract class <Name>Module`) with
     `@Binds abstract fun bindRepository(impl: Default<Name>Repository): <Name>Repository` and `@Provides` for the
     Retrofit interface or database. Example: `feature/products/impl/src/main/java/com/primestrider/kotlinstarter/feature/example/products/data/ProductsModule.kt`.
4. **UI** (`…<name>.ui`): a stateless `<Name>Screen(state, callbacks…)`, a `@HiltViewModel` exposing one
   `StateFlow` of UI state, and `fun NavGraphBuilder.<name>Destinations(navController: NavController)` that registers
   `composable<Route> { … hiltViewModel() … }`. Form controls only from `core:designsystem`.
5. **Strings** in the module's `src/main/res/values/strings.xml` and `values-in/strings.xml`, prefixed `<name>_`
   (see `add-translation`). Permissions, providers or receivers go in the module's own
   `src/main/AndroidManifest.xml` (see `add-permission-feature`).
6. **Wire in `:app`.** In `app/build.gradle.kts` add `implementation(projects.feature.<name>.api)` and `.impl`. In
   `app/src/main/java/com/primestrider/kotlinstarter/app/navigation/AppNavHost.kt` call `<name>Destinations(navController)`
   inside the tab graph it belongs to (`homeGraph`, or a `navigation<…Graph>` in `featureGraphs`); a new tab is a
   `TopLevelTab` in `topLevelTabs`. Do **not** wrap a real feature in `// region example:` markers: those mark the
   examples `scripts/remove-examples.sh` deletes.
7. **Optional Home row:** contribute `@Provides @IntoSet fun homeEntry(): CatalogEntry` (and a `CatalogSection` if the
   feature owns a section) from the feature's Hilt module; needs `implementation(projects.feature.catalog.api)`.
   **Caution:** `catalog` is one of the examples `scripts/remove-examples.sh` deletes. A real feature that depends on
   `catalog:api` stops compiling after that; if you plan to remove the examples, give your app its own Home instead (or
   keep the catalog feature).

## Tests (in the feature module)

- Mapper: DTO → model for every field the UI shows (`ProductMappersTest`).
- ViewModel with a fake repository implementing the interface (`ProductsViewModelTest`, `ProductFixtures.kt`).
- Screen: Robolectric + Compose (`@Config(sdk = [36])`), states and callbacks (`ProductsScreenTest`).
- Paging screens call `resetAndroidUiDispatcher()` (from `core:testing`) in `@Before`.

## Checklist

- [ ] `./gradlew test ktlintCheck lint assembleDevDebug` green. The first `lint` run of a new module creates its
      `lint-baseline.xml` and stops once; run it again and commit the (normally empty) baseline.
- [ ] No `import …feature.<other>.impl` anywhere (it would not compile anyway).
- [ ] Strings present in both locales (`TranslationCompletenessTest`).
- [ ] Every `XxxScreen` file ends with a private `@ThemePreviews` preview of its stateless content (`PreviewCoverageTest`).

See also: `docs/architecture/overview.md`, `docs/architecture/navigation.md`, `docs/architecture/data-loading.md`,
`docs/architecture/testing.md`.
