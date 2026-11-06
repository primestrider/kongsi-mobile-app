# Architecture overview

Multi-module app in the style of Google's Now in Android, root package `com.primestrider.kotlinstarter`.

## Modules

- `:app`: application shell. `MainActivity`, `AppViewModel`, the tabs (`MainShell`) and `AppNavHost.kt`, the one place that wires features together. It owns the environment (flavours, `@ApiBaseUrl`).
- `:core:*`: reusable building blocks, one module per responsibility: `common`, `model`, `datastore`, `designsystem`, `network`, `auth`, `ui`, and `testing` for shared test doubles. They know nothing about features.
- `:feature:<name>:api` + `:impl`: user-facing features. `api` holds the `@Serializable` routes others may navigate to; `impl` holds screens, ViewModels, the `data/` layer and Hilt modules. All seven current features are examples and can be removed (`scripts/remove-examples.sh`).
- `build-logic/`: convention plugins, so each module's build file is a few lines.

## Dependency direction (enforced by Gradle)

```
app  ->  feature:*:impl  ->  feature:*:api
 |              |
 +------------> core:*  (core:auth -> core:network, core:datastore, core:designsystem; core:ui -> core:designsystem)
```

`core` never depends on `feature`; an `impl` never depends on another `impl` (only on its `api`); only `:app` sees `impl` modules. Because these are module dependencies, a wrong import does not compile. `core:network` does not know `core:auth`: auth plugs its bearer interceptor into the app client through the `@AppInterceptors` multibinding and binds its 401 refresh as the optional `Authenticator`.

## Screen / Route / ViewModel split

- `XxxScreen(state, callbacks)`: stateless composable. Easy to test with Robolectric and to preview.
- `XxxRoute`: thin composable that obtains the ViewModel via `hiltViewModel()`, collects its `StateFlow`, and passes state and callbacks to the screen.
- `XxxViewModel`: holds UI state as `StateFlow`, talks to repositories (for example DataStore-backed preferences), and is injected by Hilt.

## Error handling

Three levels, from narrow to wide:

1. **Per screen.** A failed load is state, not an exception: ViewModels catch and map failures to `ApiError` (`safeApiCall`, `toApiError`, see `data-loading.md`) and screens render `LoadingState` / `EmptyState` / `ErrorState` (with Retry) from `core/designsystem/component/StateViews.kt`. A 404 for a single item is "not found", with no Retry.
2. **App-wide messages.** Short confirmations and non-blocking failures go through the injected `Messenger` (snackbar shown by `MessengerHost` in the shell), so ViewModels never need a `Context`.
3. **Crashes.** `core/crash/CrashRecorder` is installed in `KotlinStarterApp.attachBaseContext`, before content providers and Hilt start, so crashes during startup are recorded too. On an uncaught exception it stores the class name, a message truncated to 500 characters and the time (no stack trace) with a synchronous `commit()`, then always hands the crash to the previous handler, so the system and any crash reporter still see it and the process still dies. On the next launch `AppViewModel.lastCrash` is non-null and `MainActivity` shows `CrashFallbackScreen` instead of the shell; Continue clears it.

The recorder deliberately does not try to keep the process alive: after an uncaught exception the app's state is unknown, and continuing would hide the bug and risk corrupting data. A crash reporter (Firebase Crashlytics, Sentry) installs its own handler; install it before `CrashRecorder.install()` so the chain is recorder → reporter → system.

Debug builds have Settings → Developer → "Crash the app" to try it.

## Background work

WorkManager (`work-runtime-ktx`) runs deferrable work that must survive the app closing. It is initialised on demand, not at startup: the manifest removes `androidx.work.WorkManagerInitializer` from `androidx.startup.InitializationProvider` (`tools:node="remove"`), and `KotlinStarterApp` implements `Configuration.Provider` with the injected `HiltWorkerFactory`. The first `WorkManager.getInstance(context)` builds WorkManager with that configuration, so app start pays nothing for it.

- Write workers as `@HiltWorker class XxxWorker @AssistedInject constructor(@Assisted context: Context, @Assisted params: WorkerParameters, ...deps) : CoroutineWorker(...)`; Hilt injects the other constructor parameters.
- Use unique work names (`enqueueUniquePeriodicWork(name, UPDATE, ...)`) so a worker is never enqueued twice, and keep what a worker records somewhere a fresh process can read (it may run without any activity).
- Periodic work runs at most every 15 minutes, and the OS decides when (Doze, battery saver, app standby).
- Tests: `TestListenableWorkerBuilder` with a `WorkerFactory` for the worker itself; `WorkManagerTestInitHelper.initializeTestWorkManager(context, config)` (with a `SynchronousExecutor`) for code that enqueues work. `HiltTestApplication` is not a `Configuration.Provider`, so a Hilt test that reaches `WorkManager.getInstance` must initialise the test WorkManager first.

The Background Task demo (`sdk/background/` in `feature/devices/impl`) is the reference implementation (docs/examples.md).

## Picture-in-picture

`core/pip/PictureInPictureHost` (a singleton) holds what to show while `MainActivity` is in picture-in-picture. A screen sets it (Audio & Video sets a `PlayerView` while its video plays) and clears it when it stops or leaves. `MainActivity` wraps `MainShell` in `PictureInPictureFrame`: in PiP with host content, only that content is shown full screen; `MainShell` stays composed underneath (hidden and out of the semantics tree), so the NavHost, its back stack and the screen that set the host are untouched when the window returns to full size. `MainActivity` declares `supportsPictureInPicture` and handles size, layout and orientation changes itself (`configChanges`), so entering and leaving PiP does not recreate it.
