# Testing

Every module has JVM unit tests under `src/test/`, run with Robolectric where Android is needed. There are no
instrumented (`androidTest`) tests; the device-only work is the baseline profile module (see [release.md](release.md)).

## Running

- `./gradlew test` runs every module's debug unit tests; `./gradlew -p build-logic test` runs the build-logic tests (versionCode rules), which `test` does not reach (AGP 9 creates debug unit-test tasks only). `:app`'s tests run
  on the `dev` flavour only (`testDevDebugUnitTest`); they do not depend on the environment.
- One module: `./gradlew :feature:products:impl:testDebugUnitTest`; one class: add `--tests "*ProductsScreenTest"`.
- On Windows the DataStore corruption test is skipped (file locking); CI runs it on Linux.

## Conventions

- Robolectric: `@RunWith(AndroidJUnit4::class)` and `@Config(sdk = [36])`.
- Hilt: `@HiltAndroidTest`, `HiltAndroidRule` ordered before the Compose rule, `@Config(application = HiltTestApplication::class)`.
  Tests that need the whole app (the shell, deep links, Home with every feature) live in `:app`.
- Paging screens: call `resetAndroidUiDispatcher()` in `@Before`.
- Pixel checks (`assertColorClose`, `pixelAt`) need `@GraphicsMode(GraphicsMode.Mode.NATIVE)` and live in their own class.

## Shared test code: `core:testing`

Add it with `testImplementation(projects.core.testing)` (feature `:impl` modules get it from their convention):
`InMemoryPreferencesDataStore`, `GatedPreferencesDataStore`, `resetAndroidUiDispatcher()`, `FakeSessionCipher`,
and `TestApi` for network tests. Fakes used by one module stay in that module's tests.

## Architecture tests (in `:app`)

They read the source tree, so they cannot pass by scanning nothing:

- `NoRawMaterialControlsTest`: no module uses stock Material form controls.
- `TranslationCompletenessTest`: each module's `values/` and `values-in/` have the same keys and placeholders, and no string name is defined in two modules.
- `RouteNamesTest`: route serial names (stored in the saved back stack) never change.
- `DocsLinksTest`: every path quoted in the docs and the project skills exists.
- `SkillsTest`: every `.agents/skills/<name>/SKILL.md` has frontmatter matching its folder, and `AGENTS.md` lists it.
- `DeployWorkflowTest`: the guards of the distribution workflow (bump only on main, a fail-closed signature check, `[skip ci]`, tag last, build-logic tests in both workflows).

A test in `:app` that drives example screens without importing a feature package starts with the line
`// remove-examples: ...`, so `scripts/remove-examples.sh` deletes it with the examples.

## Screenshot tests (Roborazzi)

`core/designsystem/src/test/java/com/primestrider/kotlinstarter/core/designsystem/screenshot/ControlsScreenshotTest.kt` captures every form control in light and dark.
A plain `test` run does not capture. Reference images live in `core/designsystem/src/test/screenshots/` and are
**recorded only on Linux**, by the manually triggered CI workflow (`record_screenshots` input), because fonts render
differently on Windows and macOS. Never record locally. When references exist, CI runs
`:core:designsystem:verifyRoborazziDebug`.
