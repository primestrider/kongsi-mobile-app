---
name: add-screen-route
description: Use when adding a screen or page to an existing feature - a @Serializable route in the feature's :api module, the composable destination, navigation between screens and tabs, protected (signed-in only) pages, and deep links - so saved state, deep links and R8 keep working.
---

# Add a screen and its route

Navigation is type-safe Navigation Compose inside a bottom-navigation shell. Read
`docs/architecture/navigation.md` once; this skill is the checklist.

## Steps

1. **Route** in the feature's `:api` module, e.g.
   `feature/products/api/src/main/java/com/primestrider/kotlinstarter/feature/example/ProductsRoutes.kt`:
   `@Serializable data object <Name>Route`, or `data class <Name>Route(val id: Long)` for arguments (primitives,
   strings, nullable with defaults). The serial name is the qualified class name; it is stored in the saved back stack
   and matched by deep links, so **do not rename or move a shipped route** (the examples are pinned by
   `RouteNamesTest`). R8 keeps `*Route` names (`app/proguard-rules.pro`, checked by `./gradlew verifyReleaseKeeps`).
2. **Screen**: stateless `<Name>Screen(state, onBack, …)` on `AppScaffold` (or `ExamplePageScaffold` for demo pages);
   state from a `@HiltViewModel` with one `StateFlow`. Arguments: `savedStateHandle.toRoute<<Name>Route>()` in the
   ViewModel, or `entry.toRoute<…>()` in the destination.
3. **Destination**: add `composable<<Name>Route> { … }` to the feature's `<feature>Destinations(navController)`
   function (e.g. `productsDestinations` in `ProductsNavigation.kt`). `:app` already calls it inside the right tab.
4. **Navigate** with `navController.navigate(<Name>Route(id))`, only to destinations of the current tab: a route of
   another tab's graph would be pushed onto the current stack. Another feature's page: depend on its `:api` and
   navigate to its route (the account feature opens `SettingsRoute` this way).
5. **Protected page** (signed in only): copy the guard of `AccountRoute` in `accountDestinations`
   (`feature/account/impl/src/main/java/com/primestrider/kotlinstarter/feature/example/auth/AccountNavigation.kt`): wait
   for the stored session to load, then replace the page with sign-in when the session is gone without the user
   signing out. Test it like `ProtectedRouteTest`.
6. **Deep link**: `composable<<Name>Route>(deepLinks = listOf(navDeepLink<<Name>Route>(basePath = "$appScheme://example/…")))`,
   with the scheme passed in from `:app` (`BuildConfig.APP_SCHEME`, per flavour; dev is `<scheme>-dev`) as
   `devicesDestinations` does. Code that builds links reads `AppConfig.appScheme(context)` from `core:common`.
7. **Home row** (optional): a `CatalogEntry` contribution, see `add-feature` step 7.

## Tests

- Screen test (Robolectric + Compose) for the states and callbacks.
- A route with arguments: navigate in a test `NavHost` or test the ViewModel with a `SavedStateHandle` built from the
  route.
- Deep link: launch `MainActivity` with an `ACTION_VIEW` intent like `DeepLinkTest`.

## Checklist

- [ ] Route in `:api`, destination in the feature's destinations function, nothing added to `:app` unless it is a new tab.
- [ ] `./gradlew test ktlintCheck lint assembleDevDebug` green; for a release, `assembleProdRelease verifyReleaseKeeps`.
