# Navigation

The app uses Navigation Compose with type-safe routes (kotlinx.serialization) inside a bottom-navigation shell.

## Shell and tabs

`app/navigation/MainShell.kt` is the app's root composable (set by `MainActivity`). It is a `Scaffold` holding `AppNavigationSuite` (`core/designsystem/component/`), which puts the tabs in a bottom `AppNavigationBar` on compact width (phones in portrait) and in a `NavigationRail` at the start from medium width (tablets, foldables, phones in landscape), chosen from `currentWindowAdaptiveInfo()`. Its content is one `NavHost` with a nested graph per tab:

| Tab (`TopLevelTab`) | Graph | Start route | Contents |
| --- | --- | --- | --- |
| Home | `HomeGraph` | `HomeRoute` | example index, Form, styling pages |
| Components | `ComponentsGraph` | `ComponentsHomeRoute` | component example pages |
| Profile | `ProfileGraph` | `ProfileRoute` | Profile hub, Settings, sign-in, protected Account |

Behavior:

- Switching tabs uses `navigate(graph) { popUpTo(start) { saveState = true }; launchSingleTop = true; restoreState = true }`, so each tab keeps its own back stack.
- Re-selecting the current tab pops it to its start route.
- Back at the root of a non-Home tab switches to Home and restores Home's saved stack (a `BackHandler` composed after the `NavHost`, so Home's stale saved state is never resurrected later); back at Home's root leaves the app.
- The `NavHost` consumes the inset the bar or rail already pads for, so screens use `AppScaffold` normally without a double navigation-bar gap.

## Adaptive layouts

- **Shell:** bar or rail, see above (`AppNavigationSuiteTest` checks both with Robolectric `qualifiers = "w840dp-h900dp"`).
- **List-detail:** `ProductsListDetail` (`feature/products/impl/src/main/java/com/primestrider/kotlinstarter/feature/example/products/ui/ProductsListDetail.kt`) uses Material's `ListDetailPaneScaffold`. When the window fits two panes (`navigator.scaffoldDirective.maxHorizontalPartitions >= 2`), tapping a product shows it beside the list; on narrower windows it is just `ProductsScreen` and a tap navigates to `ProductDetailRoute` as before. The detail's ViewModel takes the id through assisted injection (`hiltViewModel<ProductDetailViewModel, ProductDetailViewModel.Factory>(key = "product-$id") { it.create(id) }`), so the same ViewModel serves the page and the pane.
- **Sizes inside a screen:** derive from the window, not the device type, e.g. the camera preview's `cameraPreviewHeight(screenHeightDp)` keeps the controls visible in landscape.

## Route objects

Each destination is a `@Serializable` object or data class:

- `HomeRoute` and the graph objects live in `app/navigation/AppNavHost.kt`.
- Feature routes live in the feature's `:api` module (e.g. `feature/products/api/src/main/java/com/primestrider/kotlinstarter/feature/example/ProductsRoutes.kt`). They keep the package `com.primestrider.kotlinstarter.feature.example`, because a route's serial name is its qualified class name and the saved back stack and deep links store it (`RouteNamesTest`); R8 keeps the names too (`verifyReleaseKeeps`).

## Registering a destination

1. Add a `@Serializable` route object to the feature's `:api` module.
2. Register `composable<MyRoute> { ... }` in the feature's `NavGraphBuilder.<feature>Destinations(navController)` function. `:app` calls that function inside the graph of the tab the feature belongs to (`homeGraph` or `featureGraphs` in `AppNavHost.kt`, inside a `// region example:<feature>` block for the examples). Navigate only to destinations of the current tab (or switch tabs first): navigating into another tab's graph pushes the page onto the current stack and the selected tab flips to the declaring tab.
3. To list it on Home, contribute a `CatalogEntry(title, subtitle, route, sectionId, order)` with `@Provides @IntoSet` from the feature's Hilt module; a feature that owns a Home section also contributes its `CatalogSection`. `catalog:impl` renders whatever the wired features contribute, so removing a feature removes its rows.
4. A new top-level tab means a `TopLevelTab` added to `topLevelTabs`, a graph object and its nested graph in `featureGraphs`. With only one tab the bottom bar hides; without a feature providing Home, `PlaceholderHomeScreen` shows.

## Screen / Route split

- `XxxScreen(state, callbacks)` is stateless and tested with Robolectric.
- The `composable<Route>` block is the thin "route" layer: it obtains the `hiltViewModel()`, collects state with `collectAsStateWithLifecycle()`, and wires callbacks (including `navController.popBackStack()` for back).

## Adding a protected graph later

Wrap protected destinations in a nested graph, e.g. `navigation<ProtectedGraph>(startDestination = ...)`, and redirect to sign-in (`navigate { popUpTo(...) }`) from an auth state exposed by a ViewModel. Public routes (sign-in, onboarding) stay outside the tabs.

## Profile graph and the protected route pattern

`ProfileRoute` (start) reads the session and shows either a sign-in prompt or the signed-in user's row, plus a Settings row. From it: `SignInRoute`, `AccountRoute` (protected) and `SettingsRoute`.

To protect a route, collect the session and `loaded` in its `composable<...>` block and redirect when the session is lost. Wait for `loaded`: after process death the route is restored before the stored session has been read, and a null session then only means "not known yet". A deliberate sign-out is told apart from a lost session by the ViewModel (`signingOut` flag plus a one-shot `signedOut` event):

```kotlin
composable<AccountRoute> {
    val session by viewModel.session.collectAsStateWithLifecycle()
    val signingOut by viewModel.signingOut.collectAsStateWithLifecycle()
    val loaded by viewModel.loaded.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.signedOut.collect { navController.popBackStack(ProfileRoute, inclusive = false) }
    }
    LaunchedEffect(loaded, session == null, signingOut) {
        if (loaded && session == null && !signingOut) navController.navigate(SignInRoute) { popUpTo<AccountRoute> { inclusive = true } }
    }
    session?.let { AccountScreen(session = it, ...) }
}
```

- **Expiry (or any session lost without the user asking):** Account is replaced by sign-in (popped inclusive), so back from sign-in never returns to it.
- **User sign-out:** returns to the Profile hub, which then shows the signed-out prompt; back does not return to Account.

After a successful sign-in, `SignInRoute` navigates to `AccountRoute` with `popUpTo<SignInRoute> { inclusive = true }`.

## Page transitions

Navigation Compose animates every navigation with four lambdas, each an `AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition` or `ExitTransition`: `enterTransition` and `exitTransition` for navigating forward (the new page enters, the old one exits), and `popEnterTransition` and `popExitTransition` for going back.

- **NavHost-level:** passed to `NavHost(...)`, they apply to every destination that does not set its own. Leave out the pop pair and they repeat the forward pair.
- **Per destination:** passed to `composable<Route>(enterTransition = { ... }, ...)`, they override the NavHost defaults for that destination. The lambdas run when a transition starts, so they can read state (the transitions demo reads the chosen pattern there) or inspect `initialState`/`targetState`.
- **The app's own defaults:** the app `NavHost` in `app/navigation/MainShell.kt` sets no transitions, so every page uses Navigation Compose's `DefaultNavTransitions` (a 700 ms fade in and out). Change the app-wide motion there, not per page; give a single destination its own lambdas only when it needs to differ.
- **Predictive back:** the system turns it on by default for apps targeting Android 16 (targetSdk 36) or higher; apps targeting 33 to 35 opt in with `android:enableOnBackInvokedCallback="true"` on `<application>`, and the gesture animations show on Android 14+. This app targets 37, so it needs no flag. `NavHost` then drives the pop transitions from the back gesture: the outgoing page follows the finger and a cancelled gesture restores it. A `BackHandler` that is enabled above a `NavHost` takes the press instead and replaces that seek with a plain pop, so enable one only while it has something of its own to close.
- **Shared elements:** wrap the `NavHost` in `SharedTransitionLayout` and give the matching composables `Modifier.sharedBounds` or `Modifier.sharedElement` with the same key, passing each destination's `AnimatedVisibilityScope` (`this` inside `composable { }`).

Animations > Page transitions (`animation/transitions/` in `feature/animation/impl`) shows all of this on a nested `NavHost` with six Material patterns, container transform through `sharedBounds`, and back that pops the nested page before leaving; the app's `NavHost` transitions are left unchanged.

## Deep links: scheme config and `navDeepLink`

The example feature opens screens from outside the app with `<scheme>://example/...`.

- **Scheme:** `appScheme` in `app/build.gradle.kts`: `app.scheme` in `local.properties`, else env `APP_SCHEME`, else `kotlinstarter`. It reaches the manifest as the `${appScheme}` placeholder and the code as `BuildConfig.APP_SCHEME`, so one value drives both.
- **Manifest:** `MainActivity` has a `VIEW` intent filter (`DEFAULT` + `BROWSABLE`, scheme `${appScheme}`, host `example`) and `launchMode="singleTop"`, so a link delivered to the running activity arrives in `onNewIntent` instead of creating a second instance. That does not keep the back stack for links from outside the app: browsers, other apps, `adb shell am start` and notification taps send the intent with `FLAG_ACTIVITY_NEW_TASK`, and `NavController.handleDeepLink` answers such an intent by restarting the task (`FLAG_ACTIVITY_CLEAR_TASK`) with a back stack built from the graph. Whatever the user had open is gone, and back from the target returns to Home. Only an intent without `NEW_TASK` (one started from inside the app) is navigated to on top of the current back stack.
- **Graph:** a destination declares its links with the type-safe builder, which derives the path and query from the route class:

  ```kotlin
  composable<SdkDemoRoute>(
      deepLinks = listOf(navDeepLink<SdkDemoRoute>(basePath = "${BuildConfig.APP_SCHEME}://example/sdk")),
  ) { ... }
  ```

  `.../sdk/payment-code?amount=50000` fills `SdkDemoRoute(id = "payment-code", amount = 50000)`. Fields with defaults (`amount: Long? = null`) are optional query parameters. The back stack under the target is built from its graph (Home, then the screen), so back returns to Home.
- **Already running:** `MainActivity` creates the `NavHostController` and forwards `onNewIntent` to it with `addOnNewIntentListener { navController.handleDeepLink(it) }`. The first launch is handled by the NavHost itself.
- **Reading the argument:** a ViewModel gets route fields from its `SavedStateHandle` (`savedStateHandle["amount"]` or `savedStateHandle.toRoute<SdkDemoRoute>()`), which survives rotation and process death.
- **Plain URI patterns:** a route without fields can use `navDeepLink { uriPattern = "$appScheme://example/todos" }` (`todosDestinations`); the Todos home-screen widget opens the app with that link.
- **Try it:** `adb shell am start -a android.intent.action.VIEW -d "kotlinstarter://example/sdk/payment-code?amount=50000"`.
