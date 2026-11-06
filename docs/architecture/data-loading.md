# Data loading

How screens get remote data. The Products example (`feature/example/products/`) is the reference implementation.

## Layers

```
ProductsApi (Retrofit)  →  ProductsRepository  →  ProductsViewModel  →  ProductsScreen
   suspend calls            Flow / suspend         StateFlow / Flow       stateless, renders states
```

- **API interface:** Retrofit `suspend` functions returning DTOs (`@Serializable`). Public endpoints are marked `@Anonymous` so no bearer token is attached. Provide it from the app `Retrofit` in the feature's Hilt module (`ProductsModule`).
- **Repository:** the only class that talks to the API. It exposes a `Flow` (lists, paging) or `suspend` functions (single objects). ViewModels never see Retrofit.
- **ViewModel:** turns user input into flows and keeps what must survive process death in `SavedStateHandle`.
- **Screen:** stateless; maps load states to the shared state components (`Skeleton`, `ErrorState`, `EmptyState`).

## Errors

`core/network/ApiError.kt` normalizes every failure:

| Use | Function |
| --- | --- |
| Wrap a call and get a result type | `safeApiCall(json) { api.call() }` → `ApiResult.Success` / `ApiResult.Failure(ApiError)` |
| Map a failure you already caught | `throwable.toApiError(json)`: HTTP → status, server `message`, JSON body; I/O → `isNetworkError`; anything else → generic |
| Pass an `ApiError` through a `Throwable`-only API (Paging) | `ApiException(error)`; read it back with `throwable.asApiError()` |
| Text for the user | `apiError.userMessage()` (composable): connection hint, else server message, else a generic message |

Coroutine cancellation is always rethrown, never turned into an error.

## Paging 3

`ProductsPagingSource` is keyed by DummyJSON's `skip` offset:

- First key `0`, every page `PAGE_SIZE` (20) items (`PagingConfig(pageSize = 20, initialLoadSize = 20)`; without `initialLoadSize` the first page would be 3× and the offsets would still line up only by luck).
- `nextKey = skip + received` while that is below `total` and the page was not empty; `prevKey = null` (the list only grows downwards).
- `getRefreshKey` returns `null`: pull-to-refresh and a new search always start from the top.
- Failures become `LoadResult.Error(ApiException(e.toApiError(json)))`.

The repository builds a new `Pager` per query; the ViewModel ends the chain with `cachedIn(viewModelScope)` so rotation and recomposition do not reload pages.

### Mapping `LoadState` to UI

In `ProductsScreen`, collected with `collectAsLazyPagingItems()`:

| Condition | Shown |
| --- | --- |
| Items present | the list (and pull-to-refresh spinner while `refresh` is `Loading`) |
| `refresh` `Loading`, no items | skeleton rows |
| `refresh` `Error`, no items | `ErrorState` with Retry → `retry()` |
| `refresh` `NotLoading`, no items | `EmptyState` |
| `append` `Loading` | spinner row at the end |
| `append` `Error` | message + "Try again" row at the end → `retry()`; loaded items stay |

## Offline-first

The full list (blank query) is cached in the feature's own Room database, `products.db`, so the last list loaded still shows without a connection:

```mermaid
flowchart LR
    API[DummyJSON] -->|ProductsRemoteMediator| DB[(products.db)]
    DB -->|ProductDao.pagingSource| Pager --> Screen[ProductsScreen]
```

- The screen pages **only from the database** (`Pager(config, remoteMediator = ProductsRemoteMediator(...)) { dao.pagingSource() }`). The mediator fetches pages from the network and writes them: `REFRESH` loads offset 0 and replaces the table in one transaction, `APPEND` continues from the offset stored in `remote_keys`, `PREPEND` has nothing to do.
- The table is replaced only after the first page arrives, so a failed refresh keeps the cached rows on screen and the error shows beside them (`refresh` is `Error` with items present).
- `ProductEntity.position` keeps the server's order; image URLs and reviews are stored as JSON text through `ProductConverters`.
- Searches stay online (`ProductsPagingSource`): caching every query would grow the database without bound.
- `product(id)` asks the network first and, on an `IOException` (no connection), answers from the cached row; other failures (a 404, a server error) are not hidden by stale data.
- The database is only a cache, so its builder uses `fallbackToDestructiveMigration(dropAllTables = true)`: after a schema change it is rebuilt from the network instead of migrated. Data the user created (todos) needs real migrations instead (see `ExampleDatabase`).

Tests: `ProductsRemoteMediatorTest` (in-memory Room + `FakeProductsApi`: refresh replaces, append continues and ends, offline refresh is an `Error` that keeps the cache) and `ProductsRepositoryTest` (cached rows offline, search stays online, detail falls back to the cache).

## Debounced search

```kotlin
val query = savedStateHandle.getStateFlow("query", "")
val products = query
    .map { it.trim() }
    .debounce { if (it == initialQuery) 0L else 350L } // first load is immediate
    .distinctUntilChanged()                           // "phone " after "phone" does not reload
    .flatMapLatest(repository::pager)                 // a new query cancels the old pager
    .cachedIn(viewModelScope)
```

The query lives in `SavedStateHandle`, so the search text and its results come back after process death.

## Testing

- **PagingSource:** call `load(PagingSource.LoadParams.Refresh(key, PAGE_SIZE, false))` against `MockWebServer` and assert the request URL and the returned keys (`ProductsPagingSourceTest`).
- **ViewModel:** subclass the repository to record queries; drive time with `StandardTestDispatcher` + `advanceTimeBy` (`ProductsViewModelTest`).
- **Screen:** feed `flowOf(PagingData.from(items, loadStates)).collectAsLazyPagingItems()` to reach any load state without a network (`ProductsScreenTest`).
- **Robolectric caveat:** `LazyPagingItems` delivers pages on Compose's process-wide `AndroidUiDispatcher.Main`, which an earlier test can leave stuck (Robolectric clears the main looper queue while a dispatch is scheduled). Paging screen tests call `resetAndroidUiDispatcher()` (`testing/`) in `@Before` and wait for content with `rule.waitUntil`.

## Single objects

`ProductDetailViewModel` shows the shape for one item: a `sealed interface` state (`Loading`, `Loaded`, `NotFound`, `Failed(ApiError)`), the id passed in through an `@AssistedFactory` (so the same ViewModel serves the detail page and the detail pane beside the list on wide screens), an invalid id answered as `NotFound` without a request, HTTP 404 mapped to `NotFound` (no Retry: retrying cannot help) and every other failure to `Failed` with Retry.

## Formatting

`core/util/Formatters.kt`: `formatCurrency(amount, currencyCode, locale)` and `formatPercent(fraction, locale)`. In composables pass `currentLocale()` (the app language from `LocalConfiguration`), not `Locale.getDefault()`, so a per-app language change reformats prices.
