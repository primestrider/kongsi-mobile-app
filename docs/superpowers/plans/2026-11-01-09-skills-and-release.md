# Project Skills, Versioning and Distribution Implementation Plan (plan 6)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Finish the original spec: one-source app versioning with a derived `versionCode`, a GitHub Actions pipeline that bumps the version and distributes the signed release to Firebase App Distribution, its setup guide, and nine project skills that teach humans and AI agents the repeated tasks of this starter.

**Architecture:** The version lives in `gradle.properties` (`version=1.0.0`). A small `AppVersion` type in `build-logic` parses, bumps and derives `versionCode`; `:app` reads it, and a `bumpVersion` Gradle task rewrites the property, so the workflow needs no Node or sed for versioning. Skills are tool-neutral `SKILL.md` files under `.agents/skills/<name>/` (as in the RN repo), listed in `AGENTS.md`, and checked by tests.

**Tech Stack:** Gradle (Kotlin DSL, build-logic + JUnit), GitHub Actions, firebase-tools, Markdown.

**Spec:** `docs/superpowers/specs/2026-10-30-kotlin-android-starter-design.md` §7 (release) and §9.1 (skills); the CI part already shipped in plan 08 (`docs/superpowers/specs/2026-10-31-architecture-modularisation-design.md`).

## Global Constraints

- Version: `version=1.0.0` in `gradle.properties`; `versionCode = major*10000 + minor*100 + patch` (spec §7); minor and patch stay below 100, major below 2100 (Play's versionCode limit 2100000000).
- Workflow `deploy-android.yml`: trigger push to `main` + manual; bump from the PR branch prefix (`feat`/`feature` → minor, everything else → patch, major only manually); bump only on `main`; tests before the bump; version commit `chore(release): vX [skip ci]` pushed before the build; release must be signed with the release key (fail on `CN=Android Debug`); tag `vX` after a successful distribution; GitHub environment `app-distribution`.
- Secrets/vars (names fixed, from the RN pipeline): secrets `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`, `FIREBASE_SERVICE_ACCOUNT_JSON`; variables `FIREBASE_ANDROID_APP_ID`, `FIREBASE_TESTER_GROUPS`, optional `API_URL`. The build already reads `ANDROID_KEYSTORE_PATH` etc. (plan 08, `release.md`).
- Docs and skills in English; every path they quote must exist (`DocsLinksTest`, extended to `.agents/skills/`).
- Skills: one folder per skill, `SKILL.md` with YAML frontmatter `name` (= folder name) and `description` (when to use it, one paragraph); steps reference real files and real commands; no copy of content that `docs/architecture/*` already explains — link it.
- Proof per task: `gradlew.bat test ktlintCheck lint assembleDevDebug` plus, for build-logic changes, `gradlew.bat -p build-logic test`.

## Review Focus

1. **Version at the limits:** `1.99.99` + patch → `1.100.0` must be rejected (it would collide with `2.0.0`'s code space); pinned by `bump_past_99_is_rejected` (Task 1).
2. **A malformed `version` property** (`1.0`, `v1.0.0`, blank) fails the build with a clear message instead of producing versionCode 0; pinned by `parse_rejects_malformed_versions` (Task 1).
3. **The workflow on a fork or a branch run:** manual run off `main` never bumps (warning instead); pinned by reading the job's script in `DeployWorkflowTest.bump_only_on_main` (Task 2).
4. **Debug-signed APK reaching testers** when secrets are missing: the verify step must fail; pinned by `DeployWorkflowTest.fails_on_debug_signature` (Task 2).
5. **Skills drifting from the code:** a renamed file or task breaks a skill silently; pinned by `DocsLinksTest` scanning skills and `SkillsTest.every_skill_has_frontmatter_matching_its_folder` (Task 3).

---

### Task 1: One-source version and `bumpVersion`

**Files:**
- Create: `build-logic/convention/src/main/kotlin/com/primestrider/kotlinstarter/buildlogic/AppVersion.kt`, `BumpVersionTask.kt`; `build-logic/convention/src/test/kotlin/com/primestrider/kotlinstarter/buildlogic/AppVersionTest.kt`
- Modify: `build-logic/convention/build.gradle.kts` (JUnit test deps), `gradle.properties` (`version=1.0.0`), `app/build.gradle.kts` (versionName/versionCode from `AppVersion`, register `bumpVersion` and `printVersion` on the root project or `:app`), `docs/architecture/release.md` (Versioning section)

**Interfaces:**
- `data class AppVersion(val major: Int, val minor: Int, val patch: Int)` with `companion fun parse(text: String): AppVersion` (throws `IllegalArgumentException` naming the bad value), `fun bump(part: Bump): AppVersion`, `val code: Int`, `override fun toString() = "$major.$minor.$patch"`; `enum class Bump { NONE, PATCH, MINOR, MAJOR }`.
- Gradle: `./gradlew bumpVersion -Pbump=patch|minor|major` rewrites the `version=` line of `gradle.properties` (keeps every other line); `./gradlew -q printVersion` prints the version.

- [ ] **Step 1: Failing tests** in `AppVersionTest`: `code_is_major_minor_patch` (`1.2.3` → 10203, `0.0.1` → 1, `20.99.99` → 209999); `bump_minor_resets_patch` (`1.2.3`+MINOR = `1.3.0`), `bump_major_resets_minor_and_patch`, `bump_none_is_identity`; `bump_past_99_is_rejected` (`1.2.99`+PATCH and `1.99.0`+MINOR throw); `parse_rejects_malformed_versions` (`1.0`, `v1.0.0`, `""`, `1.0.0-beta`, `1.100.0`).
- [ ] **Step 2:** `gradlew.bat -p build-logic test` → FAIL (unresolved).
- [ ] **Step 3:** Implement; wire `:app` and the tasks.
- [ ] **Step 4:** `gradlew.bat -p build-logic test` → PASS; `gradlew.bat -q printVersion` → `1.0.0`; on a scratch copy of the repo `bumpVersion -Pbump=minor` → `gradle.properties` shows `version=1.1.0` and nothing else changed (git diff); full proof → green; `aapt`-free check: `app/build/intermediates/merged_manifest/devDebug/**/AndroidManifest.xml` has `versionCode="10000"` and `versionName="1.0.0-dev"`.
- [ ] **Step 5:** Commit `build: read the app version from gradle.properties and derive versionCode, add bumpVersion`.

### Task 2: Distribution workflow and Firebase setup guide

**Files:**
- Create: `.github/workflows/deploy-android.yml`, `docs/firebase-app-distribution-setup.md`, `app/src/test/java/com/primestrider/kotlinstarter/architecture/DeployWorkflowTest.kt`
- Modify: `docs/architecture/release.md` (Distribution section linking the guide), `README.md` (one line), `.gitignore` if needed.

Workflow steps (adapted from the RN pipeline, no Node/prebuild): checkout (full history) → setup-java 17 → setup-gradle → resolve bump (PR branch prefix via `gh api`, else commit subject; manual input; not on `main` → `none` with a warning) → `./gradlew test` → decode keystore to `$RUNNER_TEMP` and export `ANDROID_KEYSTORE_PATH` → validate the Firebase service-account JSON (type, private_key, client_email, project_id; using `jq`) → `./gradlew bumpVersion -Pbump=$BUMP` and read `./gradlew -q printVersion` → commit + push `chore(release): v$VERSION [skip ci]` (only when bumped) → `./gradlew assembleProdRelease verifyReleaseKeeps` with signing env vars → `apksigner verify --print-certs`, fail on `CN=Android Debug` → upload the APK artifact → `npx --yes firebase-tools@15 appdistribution:distribute` with app id, groups and release notes → tag `v$VERSION` (only when bumped). Job guard against loops (`!startsWith(head_commit.message, 'chore(release):')`), `concurrency` without cancel, `permissions: contents: write, pull-requests: read`, `environment: app-distribution`.

- [ ] **Step 1: Failing test** `DeployWorkflowTest` (plain JUnit in `:app`, reads `../.github/workflows/deploy-android.yml` as text and parses it with a minimal YAML-free check): `bump_only_on_main` (the bump script contains the `GITHUB_REF_NAME" != "main"` guard and sets `BUMP=none`); `fails_on_debug_signature` (contains `CN=Android Debug` and `exit 1` in the verify step); `version_commit_skips_ci` (`[skip ci]` and the `chore(release):` loop guard); `tag_after_distribution` (the tag step comes after the distribute step); `uses_existing_secret_names` (every secret named in Global Constraints appears).
- [ ] **Step 2:** Run → FAIL (file missing).
- [ ] **Step 3:** Write the workflow (comments explain each guard, as in the RN file) and the setup guide (English; parts: summary, versioning rules, Firebase app + App Distribution + tester group + service account with the *Firebase App Distribution Admin* role, release keystore creation with `keytool` and base64, GitHub environment + secrets + variables + deployment branches + Actions write permission, testing a manual run from a branch, what to change after cloning, troubleshooting: auth error, empty secrets, 403 on push, rejected push, permission denied on gradlew, debug signature, Firebase 403).
- [ ] **Step 4:** Validate YAML with `npx --yes js-yaml`; run the test → PASS; full proof → green.
- [ ] **Step 5:** Commit `ci: add the Firebase App Distribution workflow with automatic version bumps`.

### Task 3: Skills — structure, test, and the first four skills

**Files:**
- Create: `.agents/skills/{add-feature,add-screen-route,add-api-endpoint,add-translation}/SKILL.md`, `app/src/test/java/com/primestrider/kotlinstarter/architecture/SkillsTest.kt`
- Modify: `app/src/test/java/com/primestrider/kotlinstarter/architecture/DocsLinksTest.kt` (also scan `.agents/skills/**/*.md`), `AGENTS.md` (Skills section: table of all nine with one line each; replaces "Added in plan 6"), `docs/architecture/testing.md` (SkillsTest line).

Content rules per skill: frontmatter; "When to use"; numbered steps naming exact files, Gradle plugins, Hilt annotations and commands; "Checklist" (tests to write, `./gradlew test ktlintCheck lint assembleDevDebug`); "See also" links into `docs/architecture/`.
- `add-feature`: new `feature/<name>/{api,impl}` with the conventions, `settings.gradle.kts` includes, `data/` (interface repository + `Default…` + `@Binds`, DTO → model mapper), `ui/` (stateless Screen + ViewModel with one `StateFlow`), `NavGraphBuilder.<name>Destinations`, `:app` wiring (no `region example:` markers for real features), optional `CatalogEntry`, strings in both locales, own manifest for permissions, tests (mapper, repository fake, ViewModel, screen), lint baseline note.
- `add-screen-route`: route in `:api` (package rules for serial names; `RouteNamesTest` for examples), destination in the feature's destinations function, tab rules (navigate only within a tab), protected pages following the Account guard in `accountDestinations`, deep links via `navDeepLink` with the scheme passed from `:app`.
- `add-api-endpoint`: Retrofit interface in `data/`, `@Anonymous` for public calls, `@Serializable` DTO + mapper, `safeApiCall` + `ApiError`, repository method, MockWebServer test with `TestApi` patterns, R8 note (DTOs keep serializers via the library rules; check `verifyReleaseKeeps` and a release run).
- `add-translation`: strings per module in `values/` and `values-in/`, prefix rules, unique names across modules, placeholders, plurals, `TranslationCompletenessTest`, R alias imports for another module's strings.

- [ ] **Step 1: Failing test** `SkillsTest`: `every_skill_has_frontmatter_matching_its_folder` (each `.agents/skills/*/SKILL.md` starts with `---`, has `name: <folder>` and a non-blank `description:`, closes `---`); `agents_md_lists_every_skill` (each folder name appears in `AGENTS.md`); `there_are_at_least_four_skills` (raised to nine in Task 4).
- [ ] **Step 2:** Run → FAIL. **Step 3:** Write the four skills + AGENTS.md section. **Step 4:** Tests (incl. DocsLinksTest over skills) → PASS; full proof → green. **Step 5:** Commit `docs: add project skills for features, routes, API endpoints and translations`.

### Task 4: The remaining five skills

**Files:** `.agents/skills/{add-component-example,add-permission-feature,swap-auth-backend,remove-examples,release-build}/SKILL.md`; `SkillsTest` floor → 9.
- `add-component-example`: catalog `components/` pages, `DemoSection`, form controls only from `core:designsystem` (`NoRawMaterialControlsTest`), strings, Robolectric test next to the page's test; a new reusable component goes to `core/designsystem/component` + `components.md` + a Roborazzi entry.
- `add-permission-feature`: permissions in the feature's own manifest, `PermissionGate` / `rememberPermissionRequest` (`core:ui`), API-level fallbacks, `DevicesManifestTest`-style merged-manifest test, demos in `docs/examples.md` as references.
- `swap-auth-backend`: implement `AuthAdapter` (`signIn`, `refresh` on `@RefreshRetrofit`, IOException vs HTTP rules), bind with `@Binds @AuthBackend`, `@Anonymous` login/refresh, base URL per flavour, tests modelled on `AuthFlowTest`/`DummyJsonAuthAdapterTest`; link `auth-session.md`.
- `remove-examples`: run `scripts/remove-examples.sh` on a worktree copy, check `test ktlintCheck lint assembleDevDebug assembleProdRelease verifyReleaseKeeps`, apply, then rewrite docs and restore `DocsLinksTest`; cancel the demo's WorkManager job on installed builds; what stays (PiP, WorkManager setup, deep-link filter).
- `release-build`: versioning rules, `bumpVersion`, local signed release with `local.properties`, `assembleProdRelease verifyReleaseKeeps`, `apksigner`, device smoke list, baseline profile generation, the distribution workflow and its setup guide.

- [ ] **Step 1:** Raise `SkillsTest` floor to 9 → FAIL. **Step 2:** Write the five skills. **Step 3:** Tests → PASS; full proof → green. **Step 4:** Commit `docs: add skills for component examples, permissions, auth backends, removing examples and releases`.
