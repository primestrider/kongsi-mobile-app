---
name: release-build
description: Use when preparing or shipping a release - version bumps and versionCode rules, a signed local prod release, the R8 route check, verifying the signature, the device smoke test, the baseline profile, and distributing to testers through the Firebase App Distribution workflow.
---

# Build and ship a release

Background: `docs/architecture/release.md` (versioning, flavours, signing, R8, baseline profile, CI) and
`docs/firebase-app-distribution-setup.md` (the one-time distribution setup).

## Version

- The version is `version=major.minor.patch` in `gradle.properties`; `versionCode = major*10000 + minor*100 + patch`.
- `./gradlew -q printVersion` shows it; `./gradlew bumpVersion -Pbump=patch|minor|major` raises it (only the
  `version=` line changes). Minor and patch stay below 100: after `x.y.99`, bump the minor.
- On `main` the distribution workflow bumps for you from the merged branch prefix (`feat/` → minor, anything else →
  patch); bump by hand only for local releases, and never on a branch the workflow will also bump.

## Local signed release

1. Put the keystore values in `local.properties` (never committed): `android.keystore.path`,
   `android.keystore.password`, `android.key.alias`, `android.key.password`. Without them the release is debug-signed:
   fine for testing R8, never for shipping.
2. Build and check R8 kept the navigation routes:
   ```
   ./gradlew assembleProdRelease verifyReleaseKeeps
   ```
   The APK is `app/build/outputs/apk/prod/release/app-prod-release.apk`; keep
   `app/build/outputs/mapping/prodRelease/mapping.txt` to read release stack traces.
3. Verify the signature: `apksigner verify --print-certs <apk>` (from the SDK's `build-tools`); it must not say
   `CN=Android Debug`.
4. **Smoke test on a device** (minified builds can fail where debug builds do not): install the prod release, open Home,
   a list and a detail page, sign in and out, open a deep link, rotate a screen, and leave the app in the background
   and come back.

## Baseline profile (when startup matters)

With a device or emulator (API 28+) attached: `./gradlew :app:generateProdReleaseBaselineProfile`, commit the generated
profile, and compare with `./gradlew :baselineprofile:connectedProdBenchmarkReleaseAndroidTest`.

## Distribute to testers

- Merging to `main` runs `.github/workflows/deploy-android.yml`: tests, bump + `chore(release)` commit, signed
  `assembleProdRelease`, signature check, upload to Firebase App Distribution, tag `vX.Y.Z`.
- Manual run: **Actions → Distribute Android → Run workflow**; from a branch it never bumps (pipeline test).
- First time: follow `docs/firebase-app-distribution-setup.md` (Firebase app, keystore, `app-distribution`
  environment, secrets, variables, Actions write permission).

## Checklist

- [ ] `./gradlew test ktlintCheck lint assembleDevDebug` green on the release commit.
- [ ] `assembleProdRelease verifyReleaseKeeps` green and the APK signed with the release key.
- [ ] Device smoke test done on the minified prod build.
- [ ] Mapping file kept with the release.
- [ ] If `API_PINS` is set: it includes a backup pin and matches the live certificate chain (`docs/architecture/networking.md`).
