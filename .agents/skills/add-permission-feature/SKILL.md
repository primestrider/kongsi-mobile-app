---
name: add-permission-feature
description: Use when a feature needs a runtime permission or a device API (camera, location, contacts, notifications, microphone, biometrics, files) - manifest entries in the feature's own module, PermissionGate or rememberPermissionRequest, denied and blocked states, API-level fallbacks, and tests.
---

# Add a feature that needs a permission

The Device APIs demos (`feature/devices/impl`, documented per demo in `docs/examples.md`) are working references for
every common permission: copy the closest one.

## Steps

1. **Manifest in the feature module.** Declare `<uses-permission>`, `<uses-feature android:required="false">`,
   `<queries>`, providers and receivers in the feature's `src/main/AndroidManifest.xml`, as
   `feature/devices/impl/src/main/AndroidManifest.xml` does, never in `:app`'s manifest. Receivers and providers need
   fully qualified class names. They merge into the app and disappear with the feature.
2. **Ask through the shared UI** in `core:ui` (`core/permission/`):
   - `PermissionGate(permissions = listOf(…), reason = …) { granted content }` for a screen or section that only makes sense with
     the permission: one card handles never-asked, denied and blocked (with an "Open settings" button).
   - `rememberPermissionRequest(permission) { granted -> … }` for a permission asked when a button is pressed; its
     `status` turns `BLOCKED` after a denial the system will not show again, then offer `openAppSettings(context)`.
   Do not call the Activity Result APIs directly: the shared code records requests so "blocked" can be told apart from
   "never asked".
3. **API levels.** `minSdk` is 24. Check `Build.VERSION.SDK_INT` before newer APIs and give a fallback (e.g.
   `POST_NOTIFICATIONS` only exists from 33; below that notifications are allowed by default). Say the fallback in the
   UI, as the demos do.
4. **Keep the logic testable:** platform calls behind a small class or interface, the decisions (formatting, state
   machine) in pure functions or the ViewModel.
5. **Strings** for rationale, denied and blocked texts in both locales.
6. **Foreground services** need a type on Android 14+: declare `android:foregroundServiceType` on the `<service>` and
   its matching normal permission (`FOREGROUND_SERVICE` plus e.g. `FOREGROUND_SERVICE_MEDIA_PLAYBACK`,
   `..._LOCATION`, `..._DATA_SYNC`). For media, use Media3's `MediaSessionService`, which handles the notification and
   the foreground promotion (`sdk/music/MusicService.kt`); for deferrable work prefer WorkManager.

## Tests

- ViewModel / pure logic tests, without hardware (the demos' tests are the pattern).
- `PermissionGate` states are already covered in `core:ui`; test your screen's granted content and its denied text.
- A merged-manifest test in `:app` if the permission is essential, like
  `app/src/test/java/com/primestrider/kotlinstarter/architecture/DevicesManifestTest.kt`.
- Real hardware behaviour: add a line to the physical-device checklist at the end of `docs/examples.md`.

## Checklist

- [ ] Nothing permission-related added to `app/src/main/AndroidManifest.xml`.
- [ ] Denied and blocked paths reachable without the permission.
- [ ] `./gradlew test ktlintCheck lint assembleDevDebug` green (lint flags missing permission checks).
