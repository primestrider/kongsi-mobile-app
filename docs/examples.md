# Examples

The example features (`feature/*`) are demo code and can be removed without touching `core/*` (`scripts/remove-examples.sh`). This page covers the "Device APIs" Home section, the `devices` feature (`feature/devices/impl`, package `com.primestrider.kotlinstarter.feature.example.sdk`; paths below are relative to that package): small, real Android screens that exercise one platform API each.

## The Home list

`HomeScreen` shows the sections from `exampleSections` (`ExampleCatalog.kt`) as cards. `ExampleSection(title, description, entries, icon)` requires an `icon`: it appears in the card header and on the section's filter chip. With more than one section the title scrolls with the list, while a search box and a chip row (an "All" chip, then one chip per section, chosen by the section's title string id) stay pinned above the cards, so the list keeps room in landscape or with the keyboard open. A visible "N results" line under the chips (a polite live region) appears while a query or chip is active. Search matches entry titles and subtitles (substring) in the app language, plus the section title (by word start, so one letter does not match every section), and keeps the whole section when its title matches. Chips and search appear only when there is more than one section. A card's badge reads "N/M" (shown out of the section's total) only when the query or chip cut that section down; otherwise it shows the plain count.

## The Device APIs section

The section sits on the Home tab between Features and Styling. Its entries come from one registry, `sdkDemos` in `sdk/SdkDemos.kt`; `DevicesCatalogModule` contributes them to Home:

```kotlin
SdkDemo("clipboard", R.string.sdk_clipboard_title, R.string.sdk_clipboard_subtitle) { ClipboardScreen(it) }
```

All demos share one navigation destination, `SdkDemoRoute(id)`. `SdkDemoScreen` looks the id up in the registry and shows the demo; an unknown id shows an empty state with a back arrow. The Home section and the `homeGraph` entry are built from the registry, so a demo never needs its own route.

### Adding a demo

1. Write a stateless `XxxScreen(onBack: () -> Unit)` in `sdk/<name>/` on top of `ExamplePageScaffold`; use `SdkSection` (a `DemoSection` plus a muted description) and `InfoCard` for label/value data.
2. Add `sdk_<name>_` strings to `values/strings.xml` and `values-in/strings.xml` (`TranslationCompletenessTest` enforces both).
3. Append a `SdkDemo("kebab-id", title, subtitle) { XxxScreen(it) }` entry to `sdkDemos`. The order of the list is the order on Home.
4. Declare any permission or `<queries>` entry in `AndroidManifest.xml`, and test the demo's logic (pure functions or a ViewModel); add a Robolectric test where the UI has logic-driven states.
5. If the demo has a ViewModel, prefix its `SavedStateHandle` keys with the demo name (`background_interval`, `notifications_title`). The route arguments `id` and `amount` of `SdkDemoRoute` land in every demo ViewModel's `SavedStateHandle`, so a key named `id` or `amount` would read, or overwrite, the navigation argument.

### PermissionGate

`core/permission/PermissionGate` wraps content that needs runtime permissions:

```kotlin
PermissionGate(permissions = listOf(Manifest.permission.CAMERA), reason = "The camera scans the code.") { Scanner() }
```

It shows the content once **all** listed permissions are granted (`requireAll = false`: once any one is; the prompt still asks for all, as location needs). Until then it shows an outlined card with the reason and a "Grant access" button. When the system will no longer show the prompt (the permission was requested before and no rationale is offered, see `permissionStatus`), the card says "Enable it in Settings to continue." and the button opens the app's settings page. Which permissions were requested is stored in `SharedPreferences("permissions")` so "blocked" survives a restart, and the state is re-read on every `ON_RESUME`, so granting access in Settings and coming back shows the content.

Use it for any demo that needs a runtime permission, instead of requesting permissions by hand.

For a permission asked for when a button is pressed (no gate card), use `rememberPermissionRequest(permission) { granted -> ... }`: `launch()` records the request in the same store as the gate before prompting, and `status` turns `BLOCKED` after a denial the system will not repeat, so you can offer `openAppSettings(context)` (the Image & Document Picker does this for its Camera button).

### Deleting the section

The whole section is the `:feature:devices:api` and `:feature:devices:impl` modules: its manifest (permissions, `<queries>`, the `FileProvider`, the notification receiver), resources, strings, `DevicesDatabase` and dependencies all live there. To remove only this section, delete `feature/devices/`, its two `include` lines in `settings.gradle.kts` and the `// region example:devices` blocks in `app/build.gradle.kts` and `AppNavHost.kt`, then run `./gradlew ktlintFormat` (it drops the now unused imports). Run `bash scripts/remove-examples.sh` (on a copy first, see AGENTS.md) to remove every example feature at once. The deep-link `<intent-filter>` with `${appScheme}` on `MainActivity` stays in `:app`: it is app infrastructure, harmless without demos.

Before deleting, cancel the Background Task demo's periodic work on installed builds (`WorkManager.getInstance(context).cancelUniqueWork("example-background-sync")`, once, for example in an app-update migration), or WorkManager keeps trying to start a `SyncWorker` class that no longer exists (it fails and is dropped, but it is noise). WorkManager itself stays in `:app` (`Configuration.Provider` with the `HiltWorkerFactory` in `KotlinStarterApp`, and the manifest block that removes `androidx.work.WorkManagerInitializer`): it is app infrastructure for on-demand initialisation with Hilt-injected workers. Drop `work-runtime-ktx`, `hilt-work` and the `androidx.hilt.compiler` ksp dependency from `app/build.gradle.kts` together with that code only if no worker is left. The picture-in-picture plumbing also stays: `core/pip/` in `core:ui` (`PictureInPictureHost`, `PictureInPictureFrame`), the host injected in `MainActivity`, and `android:supportsPictureInPicture="true"` and `android:configChanges` on `MainActivity`.

Tests go with the demos: they live in `feature/devices/impl/src/test/`. Three `:app` tests also depend on demo code and carry the `// remove-examples:` marker: `DeepLinkTest` opens Payment Code through `<scheme>://example/sdk/payment-code` and `DevicesManifestTest` checks the merged permissions, so delete them with the section or point them at your own deep link and permissions; `CrashDeepLinkTest` and `PictureInPictureModeTest` exercise `:app` and `core:ui` plumbing and stay.

## Demos

### Clipboard & Haptics (`clipboard`)

- **Purpose:** copy a referral code, paste the clipboard into a field, and try the haptic effects.
- **Android API:** `ClipboardManager` (`setPrimaryClip`, `primaryClip`, `OnPrimaryClipChangedListener`), `View.performHapticFeedback`.
- **Permissions:** none.
- **API-level fallback:** the success and error haptics use `CONFIRM` and `REJECT` on API 30+, and `KEYBOARD_TAP` and `LONG_PRESS` below.
- **Files:** `sdk/clipboard/ClipboardScreen.kt`.

### Device & Network (`device`)

- **Purpose:** live connectivity (type, connected, internet reachable) and build constants (brand, model, OS, memory, performance class).
- **Android API:** `ConnectivityManager.registerDefaultNetworkCallback`, `NetworkCapabilities`, `Build`, `ActivityManager.MemoryInfo`, `UiModeManager`, `Settings.Global.AIRPLANE_MODE_ON`.
- **Permissions:** `ACCESS_NETWORK_STATE` (normal, granted at install).
- **API-level fallback:** "Performance class" needs API 31 (`Build.VERSION.MEDIA_PERFORMANCE_CLASS`); older devices, and devices that declare none, show "—".
- **Files:** `sdk/device/DeviceScreen.kt`, `sdk/device/NetworkStatus.kt`.

### Share & Print (`share`)

- **Purpose:** render an e-receipt (merchant, amount) into a one-page A4 PDF, then share it through the system sheet or print it.
- **Android API:** `PdfDocument`, `FileProvider` (authority `${applicationId}.files`), `Intent.ACTION_SEND` with `Intent.createChooser`, `PrintManager.print` with a `PrintDocumentAdapter`.
- **Permissions:** none.
- **API-level fallback:** none needed; all of it exists on API 24. If no app can handle the share (`ActivityNotFoundException`) or printing fails, the screen shows "Something went wrong."
- **Files:** `sdk/share/ShareScreen.kt`, `sdk/share/Receipt.kt` (amount parsing, reference), `sdk/share/ReceiptPdf.kt` (page content, PDF writer, print adapter), `res/xml/file_paths.xml`, the `<provider>` in `AndroidManifest.xml`. Robolectric has no native PDF backend, so only the page content is unit tested; the rendering is checked on a device.

### Room transactions (`sqlite`)

- **Purpose:** an offline transaction history. Pick a kind (top-up, payment, transfer) and an amount, add it, and watch the balance; it survives restarts.
- **Android API:** Room (`@Entity`, `@Dao` with `Flow`, a `Migration`).
- **Permissions:** none.
- **API-level fallback:** not applicable.
- **Files:** `sdk/sqlite/` (`TransactionEntity`, `TransactionDao`, `DevicesDatabase`, `TransactionsViewModel`, `TransactionsScreen`). The table lives in the devices feature's own `DevicesDatabase` (`devices.db`, version 1); until the modular split it lived in `ExampleDatabase`, whose `MIGRATION_2_3` drops it there. See "Where a database lives" in [storage-database.md](architecture/storage-database.md).

### Image Compression & Files (`files`)

- **Purpose:** pick a photo, resize it to a maximum width and re-encode it as JPEG (shows the saving); download a sample PDF into the cache directory and list or clear it.
- **Android API:** Photo Picker (`ActivityResultContracts.PickVisualMedia`), `BitmapFactory` (`inSampleSize`), `ExifInterface` (rotation), `Bitmap.createScaledBitmap`, OkHttp.
- **Permissions:** none. The Photo Picker needs no storage permission.
- **API-level fallback:** the Photo Picker falls back to the system document picker on devices without it (down to API 24).
- **Files:** `sdk/files/` (`ImageCompressor`, `Downloads`, `FilesViewModel`, `FilesScreen`); the `exifinterface` dependency in `libs.versions.toml`.

### App Info & Store Review (`app-info`)

- **Purpose:** show the installed package's info and request the Play in-app review sheet, with the store page as a fallback.
- **Android API:** `PackageManager.getPackageInfo`, `PackageInfoCompat.getLongVersionCode`, Play Core `ReviewManager` (`review-ktx`), `market://` and `https://play.google.com` intents.
- **Permissions:** none.
- **API-level fallback:** `getPackageInfo` uses `PackageInfoFlags` on API 33+ and the older overload below. The review flow needs Google Play; without it the screen says "In-app review is not available here."
- **Files:** `sdk/appinfo/AppInfoScreen.kt`.

### Live Photo (`live-photo`)

- **Purpose:** Live Photos are an iOS feature; the page explains that instead of faking it.
- **Android API:** none.
- **Permissions:** none.
- **API-level fallback:** not applicable.
- **Files:** `sdk/livephoto/LivePhotoScreen.kt`.

### Binary data (`blob`)

- **Purpose:** the counterpart of a JavaScript `Blob`: create a byte payload from text, read it back as text, slice its first 8 bytes as hex, or combine it with itself.
- **Android API:** none; plain Kotlin (`ByteArray`, UTF-8).
- **Permissions:** none.
- **API-level fallback:** not applicable.
- **Files:** `sdk/blob/BinaryPayload.kt`, `sdk/blob/BlobScreen.kt`.

### System Bars (`status-bar`)

- **Purpose:** change the status bar icon style (Auto, Light icons, Dark icons, Inverted) and hide it, for this screen only.
- **Android API:** `WindowCompat.getInsetsController` (`isAppearanceLightStatusBars`, `hide`/`show` of `WindowInsetsCompat.Type.statusBars()`).
- **Permissions:** none.
- **API-level fallback:** none needed; the compat controller covers API 24 and up. Leaving the screen restores the previous icon style and visibility.
- **Files:** `sdk/statusbar/StatusBarScreen.kt`, `sdk/statusbar/SystemBarStyle.kt` (the style-to-appearance rule).

### Background Music (`music`)

- **Purpose:** play, pause and stop a bundled 10-second track that keeps playing with the app in the background or the screen locked, with controls in the media notification and on the lock screen.
- **Android API:** a **foreground service** of type `mediaPlayback`: Media3 `MediaSessionService` with an `ExoPlayer` (`MusicService`), controlled from the screen through a `MediaController` connected with a `SessionToken`. Media3 posts the media notification and moves the service in and out of the foreground as playback starts and pauses; audio focus and "becoming noisy" (headphones unplugged) are handled by the player.
- **Permissions:** `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_MEDIA_PLAYBACK` (normal, granted at install). The notification itself needs `POST_NOTIFICATIONS` on Android 13+; without it playback still runs, only the notification is hidden.
- **API-level fallback:** none needed (Media3 covers API 24+); the service type is declared in the manifest for Android 14+.
- **Files:** `sdk/music/MusicService.kt`, `sdk/music/MusicController.kt` (controller + `MusicState`), `sdk/music/MusicScreen.kt`, `res/raw/sdk_music_sample.wav` (generated, no licence).

### Share Target (`share-target`)

- **Purpose:** appear in other apps' share sheets and show what they share: text (with its subject) and up to 10 images. The Device APIs page explains it and shares a sample text through the chooser so you can pick Kotlin Starter yourself.
- **Android API:** intent filters on `ShareReceiverActivity` for `ACTION_SEND` (`text/plain`, `image/*`) and `ACTION_SEND_MULTIPLE` (`image/*`); `SharedContent.from(intent)` reads `EXTRA_TEXT`, `EXTRA_SUBJECT` and `EXTRA_STREAM` (`IntentCompat` for the typed parcelable reads). Images are content URIs the sender grants for as long as the receiving activity lives; Coil loads them.
- **Why its own activity:** sharing opens one small page on top of the sending app (back returns there) instead of the whole app and its back stack. It is `exported` because other apps start it, so its input is untrusted: unknown actions or types and missing extras give an empty state.
- **Permissions:** none.
- **API-level fallback:** none needed.
- **Files:** `sdk/sharetarget/SharedContent.kt`, `sdk/sharetarget/ShareReceiverActivity.kt`, `sdk/sharetarget/SharedContentScreen.kt`; filters in `feature/devices/impl/src/main/AndroidManifest.xml`.

### In-App Update (`in-app-update`)

- **Purpose:** Play's **flexible** update: ask Play whether a newer version exists, show Play's confirmation, download in the background while the app keeps working (progress bar), then "Install and restart" when the user chooses.
- **Android API:** `AppUpdateManager` (`app-update-ktx`): `appUpdateInfo`, `startUpdateFlowForResult(info, launcher, AppUpdateOptions(FLEXIBLE))` with an `ActivityResultLauncher<IntentSenderRequest>`, an `InstallStateUpdatedListener` for progress, `completeUpdate()`. The immediate (blocking) type is the same call with `AppUpdateType.IMMEDIATE`.
- **Play only:** an app not installed from Play (debug builds, sideloaded APKs) gets an error and the page says so. Try the real flow with Play internal app sharing (install an older build, then open a newer link), or in tests with `FakeAppUpdateManager` (`AppUpdateViewModelTest`).
- **Permissions:** none.
- **Files:** `sdk/appupdate/AppUpdateViewModel.kt` (states, `AppUpdateModule`), `sdk/appupdate/AppUpdateScreen.kt`.

### Play Integrity (`play-integrity`)

- **Purpose:** request a Play Integrity token bound to one action (`transfer:amount=50000`) and show a short preview of it, with what the server must do next.
- **Android API:** `StandardIntegrityManager`: `prepareIntegrityToken` once (with the Cloud project number), then `request` with a **request hash** (SHA-256 of the action, `requestHash()`), so a token captured for one request cannot be replayed for another. Tasks are awaited with `kotlinx-coroutines-play-services`.
- **Setup:** link a Google Cloud project in Play Console > App integrity and put its **project number** in `play.integrity.project` (`local.properties`) or `PLAY_INTEGRITY_PROJECT`; it becomes `BuildConfig.PLAY_INTEGRITY_PROJECT` of `feature/devices/impl`. With the default `0` the page shows "Not configured" and makes no call.
- **The verdict is the server's job:** the token is encrypted and signed by Google; send it with the request and let your backend decode it (Play Integrity API `decodeIntegrityToken`) and check `requestDetails.requestHash` and the app and device verdicts. A check on the device can be patched out, so the app never decides on its own.
- **Permissions:** none (needs the Play Store and Play services on the device).
- **Files:** `sdk/integrity/IntegrityViewModel.kt` (states, `requestHash`, `PlayIntegrityTokens`, `IntegrityModule`), `sdk/integrity/IntegrityScreen.kt`.

### Blur & Gradients (`effects`)

- **Purpose:** a wallet card with a linear gradient at any angle (three palettes), a balance hidden behind a blur with an intensity slider, and a 3×3 mesh gradient whose middle point and smoothing you can change.
- **Android API:** `Brush.linearGradient`, `Modifier.blur` (API 31+), an AGSL `RuntimeShader` drawn with `ShaderBrush` (API 33+).
- **Permissions:** none.
- **API-level fallback:** below API 31 there is no blur: a translucent scrim covers the balance and the caption "Blur needs Android 12; showing a scrim instead." shows. Below API 33 the mesh is a plain linear gradient with the caption "Mesh gradients need Android 13; showing a linear gradient instead."
- **Files:** `sdk/effects/GradientMath.kt` (angle to offsets, intensity to blur radius), `sdk/effects/MeshGradient.kt` (the shader), `sdk/effects/EffectsScreen.kt`.

### Web Browser & Linking (`web-browser`)

- **Purpose:** open a page in a Custom Tab themed with the app colors, hand `https` / `tel:` / `mailto:` / `geo:` URIs to other apps, and show the deep link that opens Payment Code from outside the app.
- **Android API:** `androidx.browser` (`CustomTabsIntent`, `CustomTabColorSchemeParams`), `Intent.ACTION_VIEW`, a manifest deep link.
- **Permissions:** none. Android 11+ package visibility needs the `<queries>` entries in the manifest.
- **API-level fallback:** not applicable. `ActivityNotFoundException` is caught everywhere: a missing handler shows a warning banner, and a Custom Tab with no Custom Tabs browser falls back to a plain `ACTION_VIEW`.
- **Files:** `sdk/browser/ExternalApps.kt` (`ExternalApp`, `openExternal`, `openCustomTab`, `paymentDeepLink`), `sdk/browser/WebBrowserScreen.kt`.
- **Try the link:** `adb shell am start -a android.intent.action.VIEW -d "kotlinstarter://example/sdk/payment-code?amount=50000"` (the scheme is `appScheme`, see below).

### Payment Code (`payment-code`)

- **Purpose:** a payment code (3 groups of 4 digits, with a QR code of it above the digits for the cashier) that refreshes every 60 s, on a screen that is at full brightness, stays awake and blocks screenshots while it is open. The deep link `.../sdk/payment-code?amount=50000` also shows the amount.
- **Android API:** ZXing `QRCodeWriter` (`com.google.zxing:core`, rendered to an `ImageBitmap` per code), `Window.attributes.screenBrightness`, `FLAG_KEEP_SCREEN_ON`, `FLAG_SECURE`, `Activity.registerScreenCaptureCallback` (API 34+).
- **Permissions:** `DETECT_SCREEN_CAPTURE` (normal, granted at install).
- **API-level fallback:** screenshot detection needs API 34; below it the caption "Screenshot detection needs Android 14." shows. The three window treatments work on every API level.
- **Behavior:** `WindowTreatments` restores only what it changed: a flag the window already had stays set, and brightness returns to its earlier value, when a switch goes off or the screen closes. The amount reaches `PaymentCodeViewModel` through the navigation `SavedStateHandle` (`amount` argument of `SdkDemoRoute`); it comes from outside the app, so a negative value is ignored as if absent.
- **Files:** `sdk/payment/PaymentCode.kt`, `PaymentQr.kt` (`paymentQrMatrix`, `paymentQrBitmap`), `PaymentCodeViewModel.kt`, `WindowTreatments.kt`, `PaymentCodeScreen.kt`.

### Location (`location`)

- **Purpose:** the current position once or live (every 5 s, 5 m minimum distance) at low, balanced or high accuracy, with latitude, longitude, accuracy and time; a one-off fix is reverse-geocoded into street, district, city, region, postal code and country.
- **Android API:** `FusedLocationProviderClient` (`getCurrentLocation`, `requestLocationUpdates` inside `ForegroundWatchEffect`, a `LifecycleStartEffect` keyed on the watch switch and the accuracy, so live updates run in the foreground only and stop when the app goes to the background), `Geocoder`.
- **Permissions:** `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION`, requested together through `PermissionGate(requireAll = false)`: either one opens the demo. With approximate access only, High accuracy runs at balanced power (`effectivePriority`). A fix that does not arrive within 15 s shows "Could not get a location."
- **API-level fallback:** `Geocoder.GeocodeListener` needs API 33; below it `getFromLocation` runs on `Dispatchers.IO`. When `Geocoder.isPresent()` is false the address rows show "—" and the caption "No geocoder on this device." shows.
- **Files:** `sdk/location/LocationFormat.kt` (`formatCoordinate`, `formatAccuracy`), `sdk/location/ForegroundWatch.kt`, `sdk/location/LocationScreen.kt`.

### Notifications (`notifications`)

- **Purpose:** post a local notification now or after a delay (5 s, 30 s, 1 min), choose whether it shows while the app is open, cancel everything scheduled, and see a log of the last five events (scheduled, received, tapped). Tapping a notification opens this demo through its deep link.
- **Android API:** `NotificationManagerCompat`, a `NotificationChannel` with id `default`, `AlarmManager.set(ELAPSED_REALTIME_WAKEUP, ...)` (inexact, so no exact-alarm permission), a `BroadcastReceiver`, `ProcessLifecycleOwner` (to know whether the app is open).
- **Permissions:** `POST_NOTIFICATIONS` (runtime permission from Android 13).
- **API-level fallback:** below Android 13 there is nothing to ask for; the gate passes and the status row reads "granted (not needed before Android 13)", or "denied" when the user turned the app's notifications off in Settings (`areNotificationsEnabled()`). The channel is only created from Android 8.
- **Layout:** the Status row (granted / denied / not requested), the form and the Events log show before the grant; a `PermissionGate` under the Status row asks for access and "Schedule" stays disabled until it is granted.
- **Behavior:** "Now" follows the same rule as an alarm that fires while the app is open: with "Show while app is open" off it is not posted, and either way it is logged as "Received". the ids of the scheduled notifications are kept in `SharedPreferences("sdk_notifications")`, so the "n pending" count survives rotation and process death; an id whose alarm is gone (device restart) is dropped. The "Show while app is open" switch lives in the same preferences, because `NotificationReceiver` reads it. The title, the delay and the log are in the ViewModel `SavedStateHandle` under `notifications_*` keys; a tap makes navigation rebuild the screen, so the log is also carried to the next ViewModel through `NotificationEvents.history`.
- **Files:** `sdk/notifications/NotificationScheduler.kt`, `NotificationReceiver.kt`, `NotificationEvents.kt`, `NotificationsViewModel.kt`, `NotificationsScreen.kt`, and `res/drawable/sdk_ic_notification.xml` (the small icon).

### Contacts (`contacts`)

- **Purpose:** the system contact picker (name and first phone number of the one contact the user shares), and a search of the address book by name (250 ms debounce, 10 rows) once `READ_CONTACTS` is granted.
- **Android API:** `ActivityResultContracts.PickContact`, `ContactsContract`. All queries run on `Dispatchers.IO` in `ContactsRepository`.
- **Permissions:** none for the picker; `READ_CONTACTS` for the search section only (`PermissionGate` around that section).
- **API-level fallback:** not applicable. A missing contacts app (`ActivityNotFoundException`) shows "Contact picker failed."
- **Files:** `sdk/contacts/ContactsRepository.kt` (also `ContactsSource`, `ContactRow`), `ContactsViewModel.kt` (query in `SavedStateHandle` under `contacts_query`), `ContactsScreen.kt`.

### Calendar (`calendar`)

- **Purpose:** add one of three preset bookings (flight, doctor, movie) at 09:00 tomorrow, in 3 days or next week to the first writable calendar, optionally with a reminder an hour before, and list the events of the next 7 days.
- **Android API:** `CalendarContract` (`Calendars`, `Events`, `Reminders`, `Instances`) through `CalendarRepository`, on `Dispatchers.IO`.
- **Permissions:** `READ_CALENDAR` and `WRITE_CALENDAR`, as a gate around the whole screen.
- **API-level fallback:** none needed. `bookingStart` uses `java.util.Calendar`, because `java.time` needs API 26. A device without a writable calendar shows "No writable calendar on this device."
- **Files:** `sdk/calendar/Bookings.kt` (presets, `bookingStart`, `bookingEnd`, `loadUpcoming`: a calendar that cannot be read shows "Could not read the calendar." instead of crashing), `CalendarRepository.kt`, `CalendarScreen.kt`.

### Camera & QR Scanner (`camera`)

- **Purpose:** a CameraX preview that scans QR codes (the value opens in a sheet) or takes photos, with flash, torch, zoom and lens flip.
- **Android API:** CameraX (`Preview`, `ImageCapture` at JPEG quality 70, `ImageAnalysis` with `STRATEGY_KEEP_ONLY_LATEST`, `PreviewView`), ML Kit barcode scanning (QR only, bundled model).
- **Permissions:** `CAMERA`, asked by a whole-screen `PermissionGate`; `<uses-feature android:required="false"/>` for `android.hardware.camera` and `android.hardware.camera.autofocus` keep the app installable on devices without a camera.
- **API-level fallback:** none needed; CameraX supports API 24.
- **Files:** `sdk/camera/` (`QrResultGate`, `QrAnalyzer`, `CameraController`, `CameraRotation`, `CameraScreen`). `MainActivity` handles rotation itself, so `DeviceRotationEffect` (an `OrientationEventListener`) passes the device rotation to `CameraController.setTargetRotation`, which updates the bound `ImageCapture` and `ImageAnalysis`; photos come out upright after the phone is turned. `QrResultGate` is held in a ViewModel and accepts one value at a time until the sheet is closed, so a code the camera keeps seeing opens one sheet. Frames are analysed on a single-thread executor; `CameraController.release()` unbinds the camera, closes the detector and shuts that executor down. Photos go to `cacheDir/camera/` (the `cache-path` entry in `res/xml/file_paths.xml`). Only the gate, the result sheet and the rotation mapping are unit tested; the camera is checked on a device.

### Image & Document Picker (`image-picker`)

- **Purpose:** pick images or videos from the Photo Picker (one or many) or take a photo, re-encode images as JPEG at a chosen quality, optionally crop a single image to a centered square; pick documents filtered by MIME type.
- **Android API:** `PickVisualMedia` / `PickMultipleVisualMedia`, `TakePicture` into a `FileProvider` URI (authority `${applicationId}.files`), `OpenMultipleDocuments`, `ContentResolver`, `BitmapFactory` (`inJustDecodeBounds`), `MediaMetadataRetriever`, `ImageCompressor`.
- **Permissions:** none for the Photo Picker and documents. `CAMERA` is requested only when "Camera" is pressed (`rememberPermissionRequest`); a denial shows "Camera permission was denied.", and a blocked permission adds "Enable it in Settings to continue." with an Open Settings button.
- **API-level fallback:** the Photo Picker falls back to the system document picker on devices without it (down to API 24); a launch with no matching app (`ActivityNotFoundException`) shows an error.
- **Files:** `sdk/picker/` (`PickedMedia`, `SquareCrop`, `PickerViewModel`, `ImagePickerScreen`); results are written to `cacheDir/picker/`.

### Biometric & Secure Store (`biometric`)

- **Purpose:** what the device can verify with (hardware, enrolment, strength, Keystore), a plain "Verify it's me" prompt, and a 6-digit transaction PIN that is stored encrypted and can only be read back after biometric verification.
- **Android API:** `androidx.biometric` (`BiometricManager.canAuthenticate` for `BIOMETRIC_WEAK` / `BIOMETRIC_STRONG`, `BiometricPrompt` with a `CryptoObject`), Android Keystore (AES/GCM key `example_pin_key`, `setUserAuthenticationRequired(true)`, invalidated by new enrolments).
- **Permissions:** `USE_BIOMETRIC` (normal, granted at install).
- **API-level fallback:** the "Allow device passcode fallback" switch needs Android 11 (`setUserAuthenticationParameters(0, BIOMETRIC_STRONG or DEVICE_CREDENTIAL)` on the key); below it the switch is disabled with "Passcode fallback with a protected key needs Android 11." and the key is biometric only.
- **Behavior:** Save also authenticates ("Confirm to save your PIN"), because the key only encrypts after a prompt. The key is created on the first save with the switch's value and reused while a PIN exists; Delete removes the PIN and the key, so the next save picks up the switch again. A biometric change invalidates the key (`KeyPermanentlyInvalidatedException`): the PIN is deleted and the status reads "Biometrics changed" with "The PIN was removed." below it. Prompt errors (cancel, lockout, nothing enrolled) show "Not verified" with the system message below it; Keystore failures show "Secure storage failed" with the remedy, never a crash. Every status banner keeps a short one-line title and puts longer text in the wrapping message, so nothing is ellipsised. While the "Stored PIN" status shows, the window has `FLAG_SECURE` (no screenshots, a blank Recents thumbnail), and going to the background (`ON_STOP`) clears that status. The PIN text lives in plain `remember` state, never in `rememberSaveable` or the `SavedStateHandle`; `BiometricViewModel` keeps only the switch (`biometric_fallback`) and the in-memory status.
- **Files:** `sdk/biometric/PinRules.kt` (`isValidPin`), `PinVault.kt` (`PinVault`, `PinStore` / `PrefsPinStore` over `SharedPreferences("example_pin")`: Base64 ciphertext and IV), `BiometricViewModel.kt` (also `biometricSupport`), `BiometricScreen.kt`. Robolectric has no AndroidKeyStore, so only the preferences layout and the ViewModel are unit tested; the Keystore path is checked on a device.

### Audio & Video (`media`)

- **Purpose:** record a voice note and play it back with its progress; play a muted, looping video that can continue in picture-in-picture.
- **Video source:** `BigBuckBunny_320x180.mp4` from the ExoPlayer test media bucket (the reference URL on `d23dyxeqlo5psv.cloudfront.net` answers 403); a load failure shows "Could not load the video." and Play video retries.
- **Android API:** `MediaRecorder` (AAC in MPEG-4, `cacheDir/voice/note.m4a`, wrapped by `VoiceRecorder`), Media3 `ExoPlayer` (one for the note, one for the video with `REPEAT_MODE_ONE` and volume 0) and `PlayerView` (`RESIZE_MODE_ZOOM`, no controller), `Activity.enterPictureInPictureMode` (16:9) through `core/pip`.
- **Permissions:** `RECORD_AUDIO`, through a `PermissionGate` around the voice note section only (the video works without it; a blocked permission shows the gate's "Open Settings").
- **API-level fallback:** picture-in-picture needs Android 8; below it the button is hidden and "Picture-in-picture needs Android 8." shows. A device without the PiP feature shows "Picture-in-picture is not available on this device.".
- **Behavior:** while the video plays, the screen sets `PictureInPictureHost` to a `PlayerView` of the same player; in PiP, `MainActivity` shows only that content and the inline view lets go of the player, so they never compete for it. Leaving the screen clears the host and releases the recorder and both players. Closing the PiP window (or leaving the app) stops the activity, which pauses playback and stops a recording.
- **Files:** `sdk/media/MediaFormat.kt` (`formatClock`), `VoiceRecorder.kt`, `MediaScreen.kt`; `core/pip/PictureInPicture.kt`.

### Background Task (`background-task`)

- **Purpose:** turn a periodic sync on or off (every 15 min, 30 min or hour), see the work state and what the worker recorded, and run it once now in debug builds.
- **Android API:** WorkManager (`enqueueUniquePeriodicWork("example-background-sync", UPDATE, ...)`, `cancelUniqueWork`, `getWorkInfosForUniqueWorkFlow`), a `@HiltWorker` `CoroutineWorker`.
- **Permissions:** none.
- **API-level fallback:** not applicable; Android runs periodic work at most every 15 minutes, whatever the API level.
- **Behavior:** `SyncWorker` stores the last run and the run count in `SharedPreferences("example_sync")` (`SyncLog`, a `@Singleton`, so its synchronized `record` serialises workers running at once). The interval is in the `SavedStateHandle` under `background_interval` (with no saved choice, the screen shows the interval of the enqueued work); the switch reflects whether the unique work is enqueued. "Run now" (a `OneTimeWorkRequest` of the same worker) exists only when `BuildConfig.DEBUG`. See docs/architecture/overview.md, Background work.
- **Files:** `sdk/background/SyncLog.kt`, `SyncWorker.kt`, `BackgroundSyncViewModel.kt`, `BackgroundSyncScreen.kt`.

### Deep link scheme

The scheme is `appScheme`: `app.scheme` in `local.properties`, else the `APP_SCHEME` environment variable, else `kotlinstarter`. `app/build.gradle.kts` passes it to the manifest as the `${appScheme}` placeholder and to code as `BuildConfig.APP_SCHEME`. The deep link is `<scheme>://example/sdk/<demo id>` (Payment Code also takes `?amount=<IDR>`). Deep-link arguments come from outside the app (any app, web page or `adb` can fire the link), so validate them before use, as `PaymentCodeViewModel` does with a negative `amount`; see docs/architecture/navigation.md.

## Todos home-screen widget

`feature/todos/impl/src/main/java/com/primestrider/kotlinstarter/feature/example/todos/widget/TodosWidget.kt` is a **Glance** app widget: Compose-style code (`androidx.glance.*` composables only) that Glance turns into `RemoteViews` for the launcher. It shows the number of open todos and the three newest, and a tap opens Todos through `<scheme>://example/todos`.

- **Data:** a widget is not created by Hilt, so `provideGlance` gets `TodosRepository` through a Hilt `@EntryPoint` (`TodosWidgetEntryPoint`).
- **Updates:** no polling (`updatePeriodMillis="0"`); `DefaultTodosRepository` calls `TodosWidgetUpdater.update()` (`TodosWidget().updateAll(context)`) after every write, so the widget changes as soon as a todo does.
- **Declared in** `feature/todos/impl/src/main/AndroidManifest.xml` (`TodosWidgetReceiver` + `res/xml/todos_widget_info.xml`); deleting the feature deletes the widget.
- **Tests:** `TodosWidgetTest` (`runGlanceAppWidgetUnitTest`), `TodosRepositoryTest` (every write updates), `DeepLinkTest` (the link opens Todos).
- **Try it:** long-press the home screen > Widgets > Kotlin Starter > Todos.

## The Animations section

The section sits on the Home tab after Device APIs. It shows motion patterns well-known apps use, each as a live example with a title, one sentence on when to use it and the Compose API behind it (`AnimExample` in `feature/example/animation/`). Pages are listed in `animationPages` (`AnimationPages.kt`), in the order they appear on Home; each page has its own route (`AnimMicroRoute`, `AnimCardsRoute`, `AnimListsRoute`, `AnimTransitionsRoute`). Nothing here changes the app's own navigation transitions.

- **Page transitions** (`animation/transitions/`): a demo frame hosts its own `NavHost` (routes `DemoListRoute` and `DemoDetailRoute(itemId)`) inside a `SharedTransitionLayout`. Pick one of six Material patterns (`TransitionPattern`: container transform, shared axis X/Y/Z, fade through, fade; durations, distances and scales in `MotionSpec`), then open "Mountain Trip", "City Lights" or "Ocean Days": both destinations set `enterTransition`/`exitTransition`/`popEnterTransition`/`popExitTransition` from the chosen pattern's `enter(forward, ...)`/`exit(forward, ...)`, and container transform adds `Modifier.sharedBounds` keyed `item-<id>` on the card and the detail page. Shared axis X mirrors in RTL. "Open" is ignored until the list entry is `RESUMED` again (no double navigation), and the detail's Back only pops while it is on top. Back pops the demo detail first (the nested `NavHost`'s own back handling, which follows the predictive back gesture); on the demo list that handler is disabled, so back reaches the app's `NavHost`, which leaves the page with its own predictive back. The pattern (`rememberSaveable`) and the open detail (the nested controller's saved back stack) survive recreation. Below the frame, one card per pattern says when to use it, and a predictive back example uses `PredictiveBackHandler` to shrink and shift a preview panel with the gesture (closes on commit, springs back on cancel); below Android 14 it shows "Predictive back needs Android 14." and back simply closes the panel. See "Page transitions" in docs/architecture/navigation.md.
- **Cards** (`animation/cards/`): press feedback (`collectIsPressedAsState` + `spring()`: the card scales to 0.96 while pressed), a carousel (`HorizontalPager` with `carouselScale`/`carouselAlpha` applied in `graphicsLayer` from `getOffsetDistanceInPages`), expand and collapse (`Modifier.animateContentSize()` and a rotating chevron), swipe to archive (`SwipeToDismissBox` with an Archive button per card as the non-gesture alternative, an Undo snackbar and a "Reset" empty state; `ArchiveState` keeps a stack so undo re-inserts each card at its original index, clamped to the list) and a flip card (`graphicsLayer { rotationY; cameraDistance }`, back face past 90 degrees).
- **Lists & loading** (`animation/lists/`): skeleton loading (`Skeleton(shimmer = true)` and `SkeletonText(shimmer = true)` from the design system sweep a light `Brush.horizontalGradient` band, animated by `rememberInfiniteTransition` and mirrored in RTL, for about 1.5 s after "Reload"; then the rows replace them and the transition leaves composition), an animated list (`LazyColumn` + `Modifier.animateItem()` with Add, Shuffle and per-row up, down and remove buttons; `DemoListState` hands out ids that are never reused and saves its order) and a collapsing header (a fixed-height frame with its own scroll; `collapseFraction` shrinks the hero in a `layout` modifier, `parallaxOffset` shifts the image in `graphicsLayer`, and the title fades into a small toolbar title).
- **Micro-interactions** (`animation/micro/`): a like button that pops with a spring, a counter that slides up or down, a tab indicator that slides and resizes, and a content swap with `AnimatedContent` + `togetherWith`.

### Adding an animation page

1. Add a `@Serializable data object` route to `feature/animation/api/src/main/java/com/primestrider/kotlinstarter/feature/example/AnimationRoutes.kt`.
2. Write `XxxScreen(onBack)` on top of `ExamplePageScaffold`, composed of `AnimExample(title, usage, api) { ... }` cards. Keep user-chosen state in `rememberSaveable`.
3. Add an `AnimationPage(route, title, subtitle)` entry to `animationPages` in its place in the order.
4. Add `composable<XxxRoute> { XxxScreen(onBack = back) }` to `animationDestinations` in `animation/AnimationNavigation.kt`.
5. Add `anim_<page>_` strings to the module's `values/strings.xml` and `values-in/strings.xml`.

Examples respect "Remove animations": Compose scales every animation by the system animator duration scale, so each example still reaches its end state, and no state is shown by motion alone.

### Deleting the Animations section

The section is the `:feature:animation:api` and `:feature:animation:impl` modules. Delete `feature/animation/`, its `include` lines and the `// region example:animation` blocks in `app/build.gradle.kts` and `AppNavHost.kt`, run `./gradlew ktlintFormat`, drop the last paragraph of "Page transitions" in `docs/architecture/navigation.md`, and update the section order in `HomeSectionsOrderTest` and the `exampleSections` helper in `:app`'s tests. Run `bash scripts/remove-examples.sh` (on a copy first, see AGENTS.md) to remove every example feature at once.

## The Gestures section

The section sits on the Home tab after Animations. Each page is a list of live gesture examples. Every example is a `DemoSection(title, usage, api, file)` (from `core:ui`), so it shows when to use it, the Compose API behind it, and the file to open (`feature/gestures/impl`, package `com.primestrider.kotlinstarter.feature.example.gestures`). The pages are listed in `gesturePages` (`GesturePages.kt`) in Home order, and each has its own route in `GestureRoutes.kt`. Inline gesture surfaces have a fixed height and consume their pointer input, so the page's vertical scroll never steals a drag that starts on them. Every gesture-only action also has a button or a TalkBack action.

- **Tap** (`TapScreen.kt`):
  - `Modifier.clickable` with a tap counter.
  - `Modifier.combinedClickable`: click, long press (with `onLongClickLabel`, offered to TalkBack) and double tap, with a log of the last five events.
  - `pointerInput { detectTapGestures() }`, which marks where the finger touched and shows the pressed state while held (`onPress` + `tryAwaitRelease`). A single tap is reported once the double-tap window has passed.
- **Drag** (`DragScreen.kt`):
  - A one-axis handle (`Modifier.draggable`, value 0–100, plus `setProgress` semantics for TalkBack).
  - A free 2D drag (`detectDragGestures`, with `change.consume()` so the page does not scroll and the object clamped to the area).
  - A sheet that snaps between Collapsed, Half and Expanded (`AnchoredDraggableState` + `Modifier.anchoredDraggable`, saved with `AnchoredDraggableState.Saver()`, with buttons as the non-gesture alternative). The sheet is only as tall as its visible part, so its touch area matches what is on screen.
  - A list reordered by long press then drag (`detectDragGesturesAfterLongPress` with `List.moved`), with up/down buttons for every row.
- **Transform** (`TransformScreen.kt`):
  - A photo stand-in you pinch to zoom (`MIN_ZOOM` 1× to `MAX_ZOOM` 4×), pan and rotate (`detectTransformGestures`).
  - Pan is clamped by `clampPan`, so the zoomed content never leaves a gap at the edges and cannot move at 1×.
  - Double tap toggles between 1× and `DOUBLE_TAP_ZOOM` 2.5× (`toggleZoom`).
  - Zoom in, Zoom out and Reset buttons are the non-gesture alternative.
  - A rotation-only example of the higher-level `Modifier.transformable`.
- **Swipe & fling** (`SwipeScreen.kt`):
  - Three menus (Food, Drinks, Desserts) side by side: swipe left for the next menu, right for the previous one, or tap a tab (`PrimaryTabRow` synced with `HorizontalPager(rememberPagerState())`). The pager owns the page, so the tab row only reads `currentPage` and calls `animateScrollToPage`; the page survives rotation.
  - A row you swipe left to reveal Archive and Delete (`anchoredDraggable` between Closed and Open). TalkBack gets a "Show actions" custom action instead.
  - A stack of five cards: drag one and it tilts with the finger. On release, `swipeOutcome` sends it off when it travelled 40% of the width or was flung faster than 1500 px/s, and springs it back otherwise (`detectDragGestures` + `VelocityTracker` + `Animatable`). Keep and Skip buttons do the same, and "Start over" refills the stack.
  - A puck that keeps gliding after release and slows down, bounded by its area (`Animatable.animateDecay(exponentialDecay())`).
- **Scroll** (`ScrollScreen.kt`):
  - A dial driven by `Modifier.scrollable` that turns scroll deltas into a 0–100 value. It returns only the delta it used, so the page scrolls on at either end, and has − / + buttons and `setProgress` semantics.
  - A collapsing header made with a `NestedScrollConnection`, whose height math is in `headerHeightAfter`. It shrinks in `onPreScroll` before the list moves, and grows back in `onPostScroll` once the list is at its top.
  - Two lists side by side, with and without the overscroll effect (`LazyColumn(overscrollEffect = rememberOverscrollEffect() / null)`).
- **Advanced pointer** (`PointerScreen.kt`):
  - A multi-touch visualiser that reads raw events (`awaitEachGesture { awaitPointerEvent() }`) and draws a ring with its id for every pressed pointer.
  - A finger-drawing canvas (`detectDragGestures` + `Canvas` paths). Strokes are saved as flat float arrays, so they survive rotation and process death.
  - A conflict demo where a child consumes the up event, so the parent's `detectTapGestures` ignores the tap. Switch "Child consumes" off and both count the tap.

### Adding a gesture page

1. Add a `@Serializable data object` route to `feature/gestures/api/src/main/java/com/primestrider/kotlinstarter/feature/example/GestureRoutes.kt`.
2. Write `XxxScreen(onBack)` on top of `ExamplePageScaffold`, made of `DemoSection(title, usage, api, file) { ... }` blocks. Keep user-chosen state in `rememberSaveable`.
3. Add a `GesturePage(route, title, subtitle)` entry to `gesturePages` in its place in the order.
4. Add `composable<XxxRoute> { XxxScreen(onBack = back) }` to `gestureDestinations` in `GesturesNavigation.kt`.
5. Add `gesture_<page>_` strings to the module's `values/strings.xml` and `values-in/strings.xml`.

### Deleting the Gestures section

The section is made of the `:feature:gestures:api` and `:feature:gestures:impl` modules. To delete it:
1. Delete `feature/gestures/` and its `include` lines.
2. Delete the `// region example:gestures` blocks in `app/build.gradle.kts` and `AppNavHost.kt`.
3. Run `./gradlew ktlintFormat`.
4. Update the section order in `HomeSectionsOrderTest`, the `exampleSections` helper in `:app`'s tests, and the gesture routes in `RouteNamesTest`.

`bash scripts/remove-examples.sh` removes it together with the other examples.

## Physical-device checklist

Robolectric covers the logic; these need real hardware (or a well-equipped emulator) before a release:

- **Camera & QR:** scan a QR code end to end (one sheet per code), take a photo, and try torch, flash and lens flip.
- **Camera rotation:** rotate the phone to landscape (and back), then capture a photo: it is oriented correctly.
- **Payment Code QR:** open Payment Code on one device and scan its QR with the Camera demo on another (or a photo of it): the sheet shows the same 12 digits.
- **Image & Document Picker:** the Camera button (grant, deny, then deny until blocked: the Open Settings route), video picks show a frame thumbnail, and documents of each MIME filter.
- **Biometric:** with a fingerprint or face enrolled, Verify, Save and Reveal the PIN; cancel a prompt; trigger lockout; enrol a new fingerprint and check the PIN is removed with "Biometrics changed" (and "The PIN was removed."); on Android 10 or older the fallback switch is disabled.
- **Picture-in-picture:** play the video, enter PiP (only the video shows), return to full screen (the screen and back stack are as they were), close the PiP window (playback stops).
- **Audio & Video:** record and play back a voice note; deny the microphone until blocked and check Open Settings.
- **Contacts:** the picker returns the chosen contact's name and number.
- **Calendar:** the added booking appears in the Calendar app, with its reminder.
- **Location:** the 15 s timeout path ("Could not get a location.") with location off or indoors.
- **Notifications:** on Android 12 or older (no runtime permission), notifications post and the status reads "granted (not needed before Android 13)".
- **Clipboard & Haptics:** paste from another app and feel each haptic.
- **System Bars:** dark theme with Inverted, and the style after a rotation.
- **Todos widget:** add it to the home screen, add and complete todos in the app (the widget follows), tap it (Todos opens), and resize it.
- **Share Target:** share a link from Chrome, one photo and several photos from the gallery: each opens the page, back returns to the sender.
- **In-App Update:** with internal app sharing, install an older version and open the page: Update, download progress, Install and restart.
- **Play Integrity:** with a project number set and the app from Play (internal testing), request a token and decode it on a server.
- **Background Music:** play, lock the screen (lock-screen controls work), leave the app (keeps playing), pause and swipe the app away from Recents (the service stops), and on Android 13+ with notifications denied (plays without a notification).
- **Payment Code:** brightness goes to full and returns; the screenshot banner on Android 14+.
- **Web Browser & Linking:** Custom Tabs and each external app (phone, mail, maps).
- **Predictive back gesture on the transitions demo:** on Android 14+, open an item on Animations > Page transitions and swipe back slowly from the edge: the detail follows the finger, cancelling restores it and releasing returns to the list (the page stays); do the same with the predictive back preview panel.
- **Indonesian:** switch the app language to Bahasa Indonesia and walk the demos.
- **Rotation and process death:** rotate each demo (`MainActivity` handles rotation itself since the PiP `configChanges`, so state stays put), then use "Don't keep activities" or `adb shell am kill` with the app in the background to check what survives a recreation.
