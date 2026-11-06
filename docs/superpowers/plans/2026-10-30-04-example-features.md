# Example Features Implementation Plan (Plan 4 of 6)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Port the RN example features: a Products catalog (infinite list with search against DummyJSON, product detail), an offline Todos list persisted in Room, and a crash fallback, all built on the existing components, networking and Messenger.

**Architecture:** Products use Paging 3 (`PagingSource` over a Retrofit API, `Pager` in a repository, debounced query in the ViewModel, `collectAsLazyPagingItems` in the screen). Todos introduce `core/database` (Room) with the todo table owned by the example feature. A `CrashRecorder` notes an uncaught crash synchronously and the next cold start shows a fallback screen before the shell. Errors everywhere map to `ApiError` through one shared function.

**Tech Stack:** Paging 3 (`paging-runtime`, `paging-compose`), Room (`room-runtime`, `room-ktx`, `room-compiler` via KSP, `room-testing` not needed), Retrofit/OkHttp (existing), Coil 3 (existing), Hilt, Compose M3 (`PullToRefreshBox`), Robolectric.

**Spec:** `docs/superpowers/specs/2026-10-30-kotlin-android-starter-design.md` §4 (Repository + Flow, Paging 3, Room), §5.2 (error state per screen + global crash handler with a fallback screen), §6 (Products, Todos)
**Reference source:** `D:\RN\rn-expo-boilerplate\src\app\(public)\example\features\{products\index,products\[id],todos}.tsx`, `src\features\example\stores\todos.store.ts`, `src\features\example\languages\example.{en,id}.ts`, `src\shared\components\AppErrorBoundary.tsx`

## Global Constraints

- Branch `feat/foundation`. New dependencies (latest stable, via `libs.versions.toml`): `androidx.paging:paging-runtime`, `androidx.paging:paging-compose`, `androidx.room:room-runtime`, `room-ktx`, `room-compiler` (KSP). Nothing else.
- `core/*` never imports `feature/*`. The example's Room database (`ExampleDatabase`) therefore lives in `feature/example/todos` with its entity and DAO, so deleting `feature/example` deletes it (see Task 3).
- DummyJSON product endpoints are public: annotate them `@Anonymous`. Page size **20**; search debounce **350 ms**; next page while `skip + loaded < total`.
- Use components from `core/designsystem/component` (`AppScaffold`, `SearchField`, `LoadingState`, `EmptyState`, `ErrorState`, `Skeleton`, `Banner`, `AppButton`, `AppTextField`, `ConfirmDialog`, `AppBottomSheet`, `InfoRow`, `SectionHeader`, `Avatar`), `LocalMessenger`/`Messenger` for transient feedback.
- All HTTP/IO failures surface as `ApiError`; network → `auth_error_network` text ("Cannot reach the server. Check your connection."), otherwise the server message or `auth_error_generic`.
- Strings in `values/` and `values-in/`, copied from RN `example.en.ts`/`utils.en.ts` where they exist.
- Tests: MockWebServer for APIs, Room `inMemoryDatabaseBuilder` under Robolectric, `@Config(sdk = [36])`, in-memory DataStore fake for DataStore; no real network.
- Proof command: `./gradlew testDebugUnitTest ktlintCheck assembleDebug`.

## Review Focus

1. **Typing quickly in search** → one request after a 350 ms pause; a whitespace-only query shows the full list (no `/search` call); clearing the query returns to the full list. Test in Task 1.
2. **Reaching the end of the list** → no further page request once `skip + loaded >= total`; an empty search result shows the empty state, not an endless spinner. Test in Task 1.
3. **Connection drops while scrolling** → already loaded products stay on screen; the bottom shows an error row with Retry that loads the next page. Test in Task 1.
4. **Product that no longer exists (404)** → "That product no longer exists." with no retry loop, distinct from a network error which offers Retry. Test in Task 2.
5. **Todos: blank or 101-character task, rotation mid-typing** → rejected with the RN messages; typed text and filter survive rotation and process death. Test in Task 3.

## File Structure

```
core/network/ApiError.kt                 # + Throwable.toApiError(json)                (T1)
core/util/Formatters.kt                  # formatCurrency, formatPercent               (T2)

core/crash/{CrashRecorder,CrashFallbackScreen}.kt                                     (T4)
feature/example/products/
  ProductsApi.kt, ProductsPagingSource.kt, ProductsRepository.kt                      (T1)
  ProductsViewModel.kt, ProductsScreen.kt                                             (T1)
  ProductDetailViewModel.kt, ProductDetailScreen.kt                                   (T2)
feature/example/todos/
  TodoEntity.kt, TodoDao.kt, ExampleDatabase.kt, TodosModule.kt                      (T3)
  TodosRepository.kt, TodosViewModel.kt, TodosScreen.kt                               (T3)
docs/architecture/{data-loading,storage-database}.md
```

---

### Task 1: Products list with paging and search

**Files:**
- Modify: `gradle/libs.versions.toml`, `app/build.gradle.kts` (paging deps), `core/network/ApiError.kt` (extract mapping), `feature/example/ExampleRoutes.kt` (`ProductsRoute`, `ProductDetailRoute(id: Long)`), `ExampleCatalog.kt` (Features: Products first, then Todos (T3), then Form), `app/navigation/AppNavHost.kt` (`homeGraph`), strings ×2
- Create: `feature/example/products/{ProductsApi,ProductsPagingSource,ProductsRepository,ProductsViewModel,ProductsScreen}.kt`, `docs/architecture/data-loading.md`
- Test: `core/network/ToApiErrorTest.kt`, `feature/example/products/{ProductsPagingSourceTest,ProductsViewModelTest,ProductsScreenTest}.kt`

**Interfaces:**
- Produces:
  - `fun Throwable.toApiError(json: Json): ApiError` in `ApiError.kt` — the mapping `safeApiCall` already does (HttpException → status/message/data, IOException → network, else generic); `safeApiCall` now delegates to it. Paging errors use it too.
  - `@Serializable data class ProductDto(val id: Long, val title: String, val description: String, val category: String, val price: Double, val discountPercentage: Double, val rating: Double, val stock: Int, val brand: String? = null, val thumbnail: String, val images: List<String> = emptyList(), val reviews: List<ReviewDto> = emptyList())`, `@Serializable data class ReviewDto(val rating: Int, val comment: String, val reviewerName: String, val date: String)`, `@Serializable data class ProductPage(val products: List<ProductDto>, val total: Int, val skip: Int, val limit: Int)`.
  - `interface ProductsApi { @Anonymous @GET("products") suspend fun products(@Query("limit") limit: Int, @Query("skip") skip: Int): ProductPage; @Anonymous @GET("products/search") suspend fun search(@Query("q") query: String, @Query("limit") limit: Int, @Query("skip") skip: Int): ProductPage; @Anonymous @GET("products/{id}") suspend fun product(@Path("id") id: Long): ProductDto }` provided from the app `Retrofit` in a feature Hilt module.
  - `class ProductsPagingSource(api: ProductsApi, query: String) : PagingSource<Int, ProductDto>` — key = skip (starts 0), loadSize = `PAGE_SIZE` (20) for every page (set `initialLoadSize` = 20 in the `PagingConfig`), blank query → `products`, else `search(query.trim())`; `nextKey = (skip + page.products.size).takeIf { it < page.total }`, `prevKey = null`; exceptions → `LoadResult.Error`; `getRefreshKey` → null (always restart from the top).
  - `class ProductsRepository @Inject constructor(api: ProductsApi) { fun pager(query: String): Flow<PagingData<ProductDto>>; suspend fun product(id: Long): ProductDto }`.
  - `@HiltViewModel class ProductsViewModel @Inject constructor(repository, savedStateHandle) { val query: StateFlow<String>; fun onQueryChange(String); val products: Flow<PagingData<ProductDto>> }` — query stored in `SavedStateHandle` (key `query`); products = query.map(trim).debounce(350).distinctUntilChanged().flatMapLatest(repository::pager).cachedIn(viewModelScope). (Debounce applies from the first emission onward; the first load starts after 350 ms is acceptable — or use `debounce { if (it == initial) 0 else 350 }` so the first page loads immediately: **do that**.)
  - `@Composable fun ProductsScreen(query: String, onQueryChange: (String) -> Unit, products: LazyPagingItems<ProductDto>, onOpen: (Long) -> Unit, onBack: () -> Unit)` — `AppScaffold("Products", onBack)`, subtitle, `SearchField`, `PullToRefreshBox` (`products.refresh()`); states from `products.loadState`: refresh Loading with no items → 6 skeleton rows (80dp `Skeleton` + 3 bars); refresh Error with no items → `ErrorState(message, onRetry = products::retry)`; NotLoading and itemCount 0 → `EmptyState("No products found", "Try a different search term.")`; append Loading → centered `CircularProgressIndicator` row; append Error → row with message + `AppButton("Try again", TEXT)` calling `retry()`. Row: Coil `AsyncImage` thumbnail 64dp rounded, title (1 line, ellipsis), brand or category (muted), price (`formatCurrency` from T2 — **for T1 use a temporary `"$%.2f"`; T2 swaps it**), stock "N in stock" / "Out of stock" (error color when 0). Row click → `onOpen(id)` with `Role.Button`.
- Strings (en / id): `products_title` Products / Produk; `products_subtitle` "Paging 3, infinite scroll and search against a live API" / "Paging 3, gulir tanpa batas, dan pencarian ke API sungguhan"; `products_search_placeholder` "Search products" / "Cari produk"; `products_empty_title` "No products found" / "Produk tidak ditemukan"; `products_empty_description` "Try a different search term." / "Coba kata kunci lain."; `products_error_title` "Could not load products" / "Gagal memuat produk"; `products_stock` "%1$d in stock" / "Stok %1$d"; `products_out_of_stock` "Out of stock" / "Stok habis"; `common_action_retry` "Try again" / "Coba lagi".

- [ ] **Step 1: Write failing tests**: `ToApiErrorTest` — HttpException 404 `{"message":"x"}` → `ApiError("x", 404, false, data)`; IOException → network; other → generic (`safeApiCall` tests stay green). `ProductsPagingSourceTest` (MockWebServer; JSON fixtures with `total`) — refresh with blank query requests `/products?limit=20&skip=0`; query `"  phone "` requests `/products/search?q=phone&limit=20&skip=0`; page with total 45, skip 40, 5 items → `nextKey == null` (Review Focus 2); skip 0, 20 items, total 45 → `nextKey == 20`; 500 → `LoadResult.Error`. `ProductsViewModelTest` (TestDispatcher, fake repository recording queries): typing "p","ph","pho" within 100 ms each → after 350 ms exactly one `pager("pho")` beyond the initial `pager("")`; `"   "` → no new pager call (trimmed equals ""); query survives a new ViewModel built from the same `SavedStateHandle` (Review Focus 1). `ProductsScreenTest` (with `LazyPagingItems` from `flowOf(PagingData.from(list, sourceLoadStates))` or a fake `PagingData` with explicit `LoadStates`): items + append `LoadState.Error(IOException())` → items still shown and "Try again" visible in the footer (Review Focus 3); refresh NotLoading with 0 items → "No products found"; refresh Error, 0 items → error title + Retry; clicking a row calls `onOpen(id)`.
- [ ] **Step 2: Run** `./gradlew testDebugUnitTest --tests "*ToApiErrorTest" --tests "*Products*"` → FAIL (unresolved).
- [ ] **Step 3: Implement**; register routes, add Products to the Features section (ProductDetailRoute composable can be a placeholder until T2).
- [ ] **Step 4: Run** full proof command → PASS.
- [ ] **Step 5: Docs**: `data-loading.md` — Repository + Flow pattern, Paging 3 setup (PagingSource keying by skip, `cachedIn`, mapping `LoadState` to the state components), `toApiError`, debounced search; point to the Products example.
- [ ] **Step 6: Commit** `feat(example): add Products list with paging and debounced search`

---

### Task 2: Product detail

**Files:**
- Create: `core/util/Formatters.kt`, `feature/example/products/{ProductDetailViewModel,ProductDetailScreen}.kt`
- Modify: `ProductsScreen.kt` (use `formatCurrency`), `AppNavHost.kt` (`ProductDetailRoute`), strings ×2
- Test: `core/util/FormattersTest.kt`, `feature/example/products/{ProductDetailViewModelTest,ProductDetailScreenTest}.kt`

**Interfaces:**
- Consumes: `ProductsRepository.product(id)`, `ProductDto`, `toApiError` (T1); `Messenger`, components.
- Produces:
  - `fun formatCurrency(amount: Double, currencyCode: String = "USD", locale: Locale = Locale.getDefault()): String` (`NumberFormat.getCurrencyInstance(locale)` with `currency = Currency.getInstance(currencyCode)`); `fun formatPercent(fraction: Double, locale: Locale = Locale.getDefault()): String` (0 decimals for whole values, else 1).
  - `sealed interface ProductDetailState { data object Loading; data class Loaded(val product: ProductDto); data object NotFound; data class Failed(val error: ApiError) }`.
  - `@HiltViewModel class ProductDetailViewModel @Inject constructor(savedStateHandle, repository, json, messenger)` — `id = savedStateHandle.toRoute<ProductDetailRoute>().id`; `id <= 0` → `NotFound` without calling the repository; otherwise load: 404 → `NotFound`, other failure → `Failed`; `fun retry()`; `fun addToCart(quantity: Int)` → `messenger.success(products_added_to_cart)` (no real cart; quantity clamped 1..stock).
  - `@Composable fun ProductDetailScreen(state: ProductDetailState, onRetry: () -> Unit, onAddToCart: (Int) -> Unit, onBack: () -> Unit)` — `AppScaffold(title = product title or "Loading…", onBack)`; Loading → `LoadingState`; NotFound → `EmptyState("That product no longer exists.")` (no retry); Failed → `ErrorState` with Retry; Loaded → thumbnail (Coil, 16:9, rounded), title, price via `formatCurrency`, discount `Badge` with `formatPercent(discountPercentage / 100)` when > 0, `PrimaryTabRow` Overview / Specs / Reviews (selected tab saveable): overview = description; specs = `InfoRow`s brand (or "-"), category, rating (1 decimal), stock; reviews = list (reviewer name, `rating/5` badge, comment, date) or "No reviews yet"; bottom `AppButton("Add to cart", fullWidth)` (disabled when stock 0) → `AppBottomSheet("Add to cart")` with a quantity stepper (−/+ `IconButton`s, value 1..stock) and `AppButton("Confirm")`.
- Strings (en / id): `products_loading` "Loading…" / "Memuat…"; `products_not_found` "That product no longer exists." / "Produk ini sudah tidak tersedia."; `products_tab_overview` Overview / Ringkasan; `products_tab_specs` Specs / Spesifikasi; `products_tab_reviews` Reviews / Ulasan; `products_brand` Brand / Merek; `products_category` Category / Kategori; `products_rating` Rating / Rating; `products_stock_label` Stock / Stok; `products_no_reviews` "No reviews yet" / "Belum ada ulasan"; `products_add_to_cart` "Add to cart" / "Tambah ke keranjang"; `products_quantity` Quantity / Jumlah; `products_confirm` Confirm / Konfirmasi; `products_added_to_cart` "Added to cart" / "Ditambahkan ke keranjang"; `products_cd_increase` / `products_cd_decrease` "Increase quantity" / "Tambah jumlah", "Decrease quantity" / "Kurangi jumlah".

- [ ] **Step 1: Write failing tests**: `FormattersTest` — `formatCurrency(9.99, "USD", Locale.US) == "$9.99"`; `formatCurrency(1500.0, "IDR", Locale("in","ID"))` contains "Rp" and "1.500"; `formatPercent(0.1258, Locale.US) == "12.6%"`; `formatPercent(0.1, Locale.US) == "10%"`. `ProductDetailViewModelTest` (MockWebServer or fake repository): id 0 → `NotFound`, repository untouched; 404 → `NotFound` (Review Focus 4); IOException → `Failed(isNetworkError)`, `retry()` then success → `Loaded`; `addToCart(99)` with stock 5 → one SUCCESS message. `ProductDetailScreenTest` — `NotFound` shows "That product no longer exists." and no "Try again"; `Failed(network)` shows the network message and "Try again"; `Loaded` with no reviews: Reviews tab shows "No reviews yet"; stock 0 → "Add to cart" disabled; stepper cannot go below 1 or above stock.
- [ ] **Step 2: Run** `./gradlew testDebugUnitTest --tests "*FormattersTest" --tests "*ProductDetail*"` → FAIL.
- [ ] **Step 3: Implement**; switch the list price to `formatCurrency`.
- [ ] **Step 4: Run** full proof command → PASS.
- [ ] **Step 5: Commit** `feat(example): add product detail with tabs and add-to-cart sheet`

---

### Task 3: Todos persisted in Room

**Files:**
- Modify: `gradle/libs.versions.toml`, `app/build.gradle.kts` (Room + KSP `room.schemaLocation` arg → `app/schemas`), `ExampleRoutes.kt` (`TodosRoute`), `ExampleCatalog.kt`, `AppNavHost.kt`, strings ×2
- Create: `feature/example/todos/{TodoEntity,TodoDao,ExampleDatabase,TodosModule,TodosRepository,TodosViewModel,TodosScreen}.kt`, `docs/architecture/storage-database.md`
- Test: `feature/example/todos/{TodoDaoTest,TodosViewModelTest,TodosScreenTest}.kt`

**Interfaces:**
- **Ruling (database location):** the Room database class lives in `feature/example/todos` as `ExampleDatabase` (name `example.db`), so `core` never references example entities and deleting `feature/example` deletes it. Apps add their own `@Database` in their feature or a `core/database` later; `storage-database.md` explains the pattern. (This replaces the Global Constraints' tentative `core/database` line.)
- Produces:
  - `@Entity(tableName = "todos") data class TodoEntity(@PrimaryKey val id: String, val title: String, val done: Boolean, val createdAt: Long)`.
  - `@Dao interface TodoDao { @Query("SELECT * FROM todos ORDER BY createdAt DESC") fun observeAll(): Flow<List<TodoEntity>>; @Insert suspend fun insert(todo: TodoEntity); @Query("UPDATE todos SET done = NOT done WHERE id = :id") suspend fun toggle(id: String); @Query("DELETE FROM todos WHERE id = :id") suspend fun delete(id: String); @Query("DELETE FROM todos WHERE done = 1") suspend fun deleteCompleted(): Int }`.
  - `@Database(entities = [TodoEntity::class], version = 1, exportSchema = true) abstract class ExampleDatabase : RoomDatabase() { abstract fun todoDao(): TodoDao }`; `TodosModule` provides it (`Room.databaseBuilder(..., "example.db")`) and the DAO.
  - `class TodosRepository @Inject constructor(dao: TodoDao, clock: () -> Long = System::currentTimeMillis)` with `todos: Flow<List<TodoEntity>>`, `suspend fun add(title: String)` (trimmed, id = UUID), `toggle`, `remove`, `clearCompleted(): Int`. (Provide the clock via constructor default; Hilt sees an `@Inject` secondary constructor without it.)
  - `enum class TodoFilter { ALL, ACTIVE, DONE }`; `fun validateTodoTitle(title: String): Int?` — trimmed length < 3 → `todos_validation_min` ("Write at least 3 characters"), > 100 → `todos_validation_max` ("Keep it under 100 characters"), else null.
  - `data class TodosUiState(val input: String, val inputError: Int?, val filter: TodoFilter, val todos: List<TodoEntity>, val done: Int, val total: Int)`; `@HiltViewModel class TodosViewModel @Inject constructor(repository, savedStateHandle, messenger)` with `state: StateFlow<TodosUiState>`, `onInputChange`, `add()` (validate on submit, then live; success clears input and sends `todos_added`), `toggle(id)`, `remove(id)` (sends `todos_removed`), `clearCompleted()` (sends `todos_cleared` when > 0), `setFilter`. `input` and `filter` live in `SavedStateHandle` (Review Focus 5).
  - `@Composable fun TodosScreen(state: TodosUiState, onInputChange, onAdd, onToggle: (String) -> Unit, onRemove: (String) -> Unit, onClearCompleted, onFilterChange: (TodoFilter) -> Unit, onBack)` — `AppTextField` (placeholder "What needs doing?", error text, `ImeAction.Done` → add) + `AppButton("Add")`; progress "%1$d of %2$d done"; `SingleChoiceSegmentedButtonRow` All/Active/Done; list rows (`Checkbox` row toggleable with `Role.Checkbox`, title struck through when done, delete `IconButton` with content description) — delete asks `ConfirmDialog("Delete this task?", "This cannot be undone.", destructive)`; `AppButton("Clear completed", TEXT)` enabled when done > 0; empty → `EmptyState("Nothing to do", "Add your first task above. It survives a restart.")`.
- Strings (en / id) from RN `todos`: `todos_title` Todos / Tugas; `todos_subtitle` "Room on device — this one works offline" / "Room di perangkat — bisa dipakai offline"; `todos_placeholder` "What needs doing?" / "Apa yang perlu dikerjakan?"; `todos_validation_min` / `todos_validation_max` (above; id: "Tulis minimal 3 karakter", "Maksimal 100 karakter"); `todos_add` Add / Tambah; `todos_clear_completed` "Clear completed" / "Hapus yang selesai"; `todos_filter_all` All / Semua; `todos_filter_active` Active / Aktif; `todos_filter_done` Done / Selesai; `todos_progress` "%1$d of %2$d done" / "%1$d dari %2$d selesai"; `todos_empty_title` "Nothing to do" / "Tidak ada tugas"; `todos_empty_description` "Add your first task above. It survives a restart." / "Tambahkan tugas pertamamu di atas. Tetap tersimpan walau aplikasi ditutup."; `todos_remove_title` "Delete this task?" / "Hapus tugas ini?"; `todos_remove_description` "This cannot be undone." / "Tindakan ini tidak bisa dibatalkan."; `todos_added` "Task added" / "Tugas ditambahkan"; `todos_removed` "Task deleted" / "Tugas dihapus"; `todos_cleared` "Completed tasks cleared" / "Tugas selesai dihapus"; `todos_cd_delete` "Delete %1$s" / "Hapus %1$s".

- [ ] **Step 1: Write failing tests**: `TodoDaoTest` (Robolectric, `Room.inMemoryDatabaseBuilder`, `allowMainThreadQueries`) — insert 2 → `observeAll` newest first; `toggle` flips; `deleteCompleted` returns count and leaves active ones. `TodosViewModelTest` (real repository on an in-memory DB or a fake DAO; Turbine on messenger) — add `"  ab "` → error `todos_validation_min`, nothing stored; `"x".repeat(101)` → `todos_validation_max`; valid → stored trimmed, input cleared, one `todos_added`; filter ACTIVE hides done items; progress counts; input and filter restored from the same `SavedStateHandle` in a new ViewModel (Review Focus 5); `clearCompleted` with none done sends no message. `TodosScreenTest` — empty list shows "Nothing to do"; delete icon opens the dialog and only "Delete" calls `onRemove`; IME Done on the field calls `onAdd`; "Clear completed" disabled when nothing is done.
- [ ] **Step 2: Run** `./gradlew testDebugUnitTest --tests "*Todo*"` → FAIL.
- [ ] **Step 3: Implement**; route + Features entry (order: Products, Todos, Form); `app/schemas` committed.
- [ ] **Step 4: Run** full proof command → PASS.
- [ ] **Step 5: Docs**: `storage-database.md` — DataStore (preferences, theme, session) vs Room (structured lists), where a database lives, migrations (`exportSchema`, `app/schemas`, add a `Migration` when bumping `version`), testing with in-memory Room.
- [ ] **Step 6: Commit** `feat(example): add Todos persisted in Room`

---

### Task 4: Crash fallback

**Files:**
- Create: `core/crash/{CrashRecorder,CrashFallbackScreen}.kt`
- Modify: `KotlinStarterApp.kt` (install recorder), `app/AppViewModel.kt` (+ `lastCrash`), `app/MainActivity.kt` (fallback before shell), `feature/example/settings/SettingsScreen.kt` (+ debug-only "Developer" section with "Crash the app"), strings ×2, `docs/architecture/overview.md` (error handling section)
- Test: `core/crash/CrashRecorderTest.kt`, `core/crash/CrashFallbackScreenTest.kt`, `app/AppViewModelTest.kt` (update), `feature/example/settings/SettingsScreenTest.kt` (update)

**Interfaces:**
- Produces:
  - `data class CrashReport(val exceptionClass: String, val message: String?, val timestamp: Long)`.
  - `@Singleton class CrashRecorder @Inject constructor(@ApplicationContext context: Context)` — `fun install()` wraps `Thread.getDefaultUncaughtExceptionHandler()`: on a crash it writes the report to `SharedPreferences("crash", MODE_PRIVATE)` with `commit()` (synchronous: the process is dying) inside a `try/catch(Throwable)` that never throws, then **always** calls the previous handler (the system still reports and kills the process); `fun lastCrash(): CrashReport?`; `fun clear()`. Messages are truncated to 500 chars; no stack trace stored.
  - `AppViewModel.lastCrash: StateFlow<CrashReport?>` (read once at start), `fun dismissCrash()` (clears).
  - `@Composable fun CrashFallbackScreen(report: CrashReport, onContinue: () -> Unit)` — full screen: `ErrorState`-like layout with title "Something went wrong" (RN `fallback.errorTitle`), body "The app closed unexpectedly last time. You can continue where you left off." / Indonesian, a `SectionHeader("Details")` + `InfoRow`s (error type = simple class name, time formatted), `AppButton("Continue", fullWidth)` → `onContinue`.
  - `MainActivity`: when `lastCrash != null`, show `CrashFallbackScreen` instead of `MainShell` (inside the theme + messenger provider); Continue → `dismissCrash()`.
  - Settings: when `BuildConfig.DEBUG`, a "Developer" section with `AppButton("Crash the app", DESTRUCTIVE)` → `throw IllegalStateException("Test crash from Settings")` on the main thread. Hidden in release.
- Strings (en / id): `crash_title` "Something went wrong" / "Terjadi kesalahan"; `crash_body` "The app closed unexpectedly last time. You can continue where you left off." / "Aplikasi tertutup tiba-tiba sebelumnya. Kamu bisa melanjutkan."; `crash_details` Details / Detail; `crash_type` "Error type" / "Jenis error"; `crash_time` Time / Waktu; `crash_continue` Continue / Lanjutkan; `settings_developer` Developer / Pengembang; `settings_crash` "Crash the app" / "Buat aplikasi crash".

- [ ] **Step 1: Write failing tests**: `CrashRecorderTest` (Robolectric) — after `install()`, invoking the installed handler with `IllegalStateException("boom")` stores a report (class `java.lang.IllegalStateException`, message "boom") and calls the previous handler exactly once (a fake previous handler set before `install`); a message of 2,000 chars is stored truncated to 500; if writing fails (e.g. a `SharedPreferences` that throws — inject a factory) the previous handler is still called; `clear()` removes it. `CrashFallbackScreenTest` — shows title, error type "IllegalStateException", and "Continue" calls `onContinue`. `AppViewModelTest` — a stored report is exposed by `lastCrash`; `dismissCrash()` clears it and the recorder. `SettingsScreenTest` — "Crash the app" visible in debug builds (unit tests run debug).
- [ ] **Step 2: Run** `./gradlew testDebugUnitTest --tests "*Crash*" --tests "*AppViewModelTest" --tests "*SettingsScreenTest"` → FAIL.
- [ ] **Step 3: Implement**; install in `KotlinStarterApp.onCreate` before anything else.
- [ ] **Step 4: Run** full proof command → PASS.
- [ ] **Step 5: Docs**: `overview.md` "Error handling" — per-screen states (`LoadingState`/`EmptyState`/`ErrorState`, `ApiError`), app-wide messages (`Messenger`), the crash fallback and why it does not try to keep the process alive.
- [ ] **Step 6: Manual check** (if `adb devices` lists a device): Products scroll to the end, search "phone", airplane mode while scrolling then Retry; open a product, add to cart; Todos add/toggle/delete, kill and relaunch → still there; Settings → Crash the app → relaunch shows the fallback, Continue → app.
- [ ] **Step 7: Commit** `feat(app): record crashes and show a fallback on the next launch`
