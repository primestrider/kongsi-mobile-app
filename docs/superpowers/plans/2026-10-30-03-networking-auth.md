# Networking & Auth Implementation Plan (Plan 3 of 6)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Port the RN boilerplate's networking and auth: Retrofit/OkHttp with normalized errors, an encrypted persisted session, bearer tokens with a single-flight 401 refresh, a backend-agnostic `AuthAdapter` with a DummyJSON implementation, sign-in, and a protected Account page in the Profile tab.

**Architecture:** `core/network` owns HTTP (OkHttp client, Retrofit, `ApiError`/`ApiResult`, `@Anonymous`). `core/auth` owns the session (models, `SessionRepository` persisted in an encrypted DataStore, `AuthAdapter` seam, `SessionRefresher`, `AuthInterceptor`, `TokenAuthenticator`, expiry announcement). `feature/example/auth` holds the DummyJSON adapter, sign-in, the Profile hub and the Account page. Replacing the backend means binding another `AuthAdapter`; nothing in `core` changes.

**Tech Stack:** Retrofit 3 + kotlinx-serialization converter, OkHttp 5 (+ logging interceptor in debug), MockWebServer 3 (tests), Android Keystore AES/GCM, DataStore Preferences, Coil 3 (avatar images), Hilt, Compose, Robolectric.

**Spec:** `docs/superpowers/specs/2026-10-30-kotlin-android-starter-design.md` §4 (Retrofit + OkHttp, Interceptor for token, Authenticator for 401), §5.1 (Auth & sesi), §5.3 (konfigurasi), §6 (Protected Account)
**Reference source:** `D:\RN\rn-expo-boilerplate\src\features\auth\**`, `src\plugins\axios\**`, `src\features\example\services\{api,auth.adapter}.ts`, `src\app\sign-in.tsx`, `src\app\(protected)\account.tsx`

## Global Constraints

- Branch `feat/foundation`. Toolchain unchanged. New dependencies (latest stable, via `libs.versions.toml`): `retrofit`, `converter-kotlinx-serialization`, `okhttp`, `logging-interceptor`, `mockwebserver3` (testImplementation), `coil-compose`, `coil-network-okhttp`. Nothing else.
- `core/*` never imports `feature/*`; the DummyJSON adapter and its Hilt binding live in `feature/example`.
- Session behavior must match RN (spec §5.1):
  - Status is "signed in" iff tokens exist.
  - Session is read before the first frame (splash waits), so a signed-in cold start never flashes sign-in.
  - Every request sends `Authorization: Bearer <accessToken>` unless its Retrofit method is annotated `@Anonymous`.
  - On 401 of an authenticated request: refresh once (concurrent 401s share one refresh), retry once; still 401 or refresh fails → sign out with reason `EXPIRED` → one "session expired" message (not repeated on cold start).
  - All HTTP failures surface as `ApiError(message, status, isNetworkError)`.
  - Tokens are stored encrypted with an Android Keystore key (spec §5.1 call).
- API base URL: `BuildConfig.API_URL`, from `local.properties` key `api.url`, else env `API_URL`, else `https://dummyjson.com/`. (APP_NAME/APP_SCHEME of spec §5.3 come with the plans that need them.)
- DummyJSON: `POST auth/login {username, password, expiresInMins?}` → user fields + `accessToken`, `refreshToken`; `POST auth/refresh {refreshToken, expiresInMins?}` → `{accessToken, refreshToken}`; `GET auth/me` (bearer) → user. Demo account `emilys` / `emilyspass`.
- Strings in `values/` and `values-in/`; copy from RN `example.en.ts` / `utils.en.ts` where it exists.
- Tests: no real network (MockWebServer); no Keystore in JVM tests (fake cipher); in-memory DataStore fake for writes (Windows rule); Robolectric `@Config(sdk = [36])`.
- Proof command: `./gradlew testDebugUnitTest ktlintCheck assembleDebug`.

## Review Focus

1. **Many requests meet a 401 together** → exactly one refresh call; every request is retried with the new token. Test in Task 3.
2. **Refresh itself fails (refresh token expired, 401 on `auth/refresh`)** → signed out once with `EXPIRED`, one "session expired" message, no retry loop; the caller gets an `ApiError` with status 401. Test in Task 3.
3. **Stored session cannot be decrypted** (backup restored on a new phone, Keystore key gone, corrupted bytes) → app starts signed out, the bad record is cleared, no crash. Test in Task 2.
4. **Sign-in while offline / wrong password** → clear message (network message vs. server message), loading stops, form stays filled. Test in Task 4.
5. **Session expires while the Account page is open** → user lands on Sign in (Account popped), not on an empty Account page. Test in Task 5.

## File Structure

```
core/network/
  NetworkModule.kt      # Json, OkHttpClient, Retrofit (Hilt)          (T1, T3 adds interceptor/authenticator)
  ApiError.kt           # ApiError, ApiResult, safeApiCall               (T1)
  Anonymous.kt          # @Anonymous + Request.isAnonymous()             (T1)
core/auth/
  Session.kt            # AuthUser, AuthTokens, Session, SignOutReason, SignInCredentials, AuthResult (T2)
  SessionCipher.kt      # SessionCipher, KeystoreSessionCipher           (T2)
  SessionRepository.kt  # encrypted DataStore persistence + state        (T2)
  AuthAdapter.kt        # backend seam                                   (T3)
  SessionRefresher.kt   # single-flight refresh                          (T3)
  AuthInterceptor.kt, TokenAuthenticator.kt                              (T3)
  SessionExpiryAnnouncer.kt                                              (T3)
feature/example/auth/
  DummyJsonApi.kt, DummyJsonAuthAdapter.kt, ExampleAuthModule.kt        (T4)
  SignInViewModel.kt, SignInScreen.kt                                    (T4)
  ProfileScreen.kt, AccountViewModel.kt, AccountScreen.kt                (T5)
core/designsystem/component/Avatar.kt   # + imageUrl via Coil            (T5)
docs/architecture/{networking,auth-session}.md
```

---

### Task 1: HTTP foundation and normalized errors

**Files:**
- Modify: `gradle/libs.versions.toml`, `app/build.gradle.kts` (deps; `buildConfigField("String", "API_URL", …)` resolved as in Global Constraints), `AndroidManifest.xml` (`INTERNET`)
- Create: `core/network/{NetworkModule,ApiError,Anonymous}.kt`, `docs/architecture/networking.md`
- Test: `core/network/SafeApiCallTest.kt`, `core/network/AnonymousTest.kt`

**Interfaces:**
- Produces:
  - `data class ApiError(val message: String?, val status: Int? = null, val isNetworkError: Boolean = false)`.
  - `sealed interface ApiResult<out T> { data class Success<T>(val data: T) : ApiResult<T>; data class Failure(val error: ApiError) : ApiResult<Nothing> }`.
  - `suspend fun <T> safeApiCall(json: Json, block: suspend () -> T): ApiResult<T>` — `HttpException` → status + `message` field of a JSON body `{"message": "..."}` (null if body is not JSON or has no `message`); `IOException` → `isNetworkError = true`, message null; `SerializationException` → status null, message null; `CancellationException` is rethrown.
  - `@Target(AnnotationTarget.FUNCTION) @Retention(AnnotationRetention.RUNTIME) annotation class Anonymous`; `fun okhttp3.Request.isAnonymous(): Boolean` — reads `tag(retrofit2.Invocation::class.java)?.method()?.isAnnotationPresent(Anonymous::class.java)`.
  - `NetworkModule` (`@Module @InstallIn(SingletonComponent)`): `@Provides @Singleton fun json(): Json` (`ignoreUnknownKeys = true`, `explicitNulls = false`); `fun okHttpClient(...)`: timeouts 30 s, `HttpLoggingInterceptor` level `BODY` only when `BuildConfig.DEBUG`, header `Authorization` redacted; `fun retrofit(client, json): Retrofit` with `baseUrl(BuildConfig.API_URL)` and the kotlinx-serialization converter. Task 3 adds the auth interceptor and authenticator here.
- `networking.md`: client setup, `@Anonymous`, `safeApiCall` + `ApiError`, base URL configuration (local.properties / env), how a feature adds a Retrofit interface.

- [ ] **Step 1: Write failing tests** (`MockWebServer` started per test, Retrofit built on it with a test interface `GET item` / `@Anonymous GET open`): `SafeApiCallTest` — 200 `{"id":1}` → `Success`; 404 `{"message":"Not found"}` → `Failure(ApiError("Not found", 404))`; 500 body `oops` → `Failure(ApiError(null, 500))`; server shut down before the call → `Failure` with `isNetworkError = true`; 200 with malformed JSON → `Failure(ApiError(null, null))`. `AnonymousTest` — the request recorded for the `@Anonymous` method has `isAnonymous() == true`, the other `false` (inspect via an interceptor that captures `chain.request()`).
- [ ] **Step 2: Run** `./gradlew testDebugUnitTest --tests "*SafeApiCallTest" --tests "*AnonymousTest"` → FAIL (unresolved).
- [ ] **Step 3: Implement** deps, BuildConfig field, permission, the three files, docs.
- [ ] **Step 4: Run** full proof command → PASS.
- [ ] **Step 5: Commit** `feat(network): add Retrofit/OkHttp setup with normalized ApiError`

---

### Task 2: Encrypted, persisted session

**Files:**
- Create: `core/auth/{Session,SessionCipher,SessionRepository}.kt`, `core/storage/SessionDataStore` provider in `DataStoreModule.kt` (qualifier `@SessionStore`, file `session`), `docs/architecture/auth-session.md` (storage part)
- Modify: `app/AppViewModel.kt` (+ `ready`), `app/MainActivity.kt` (splash waits for `ready`), `res/xml/backup_rules.xml` + `data_extraction_rules.xml` (exclude `datastore/session.preferences_pb`)
- Test: `core/auth/SessionRepositoryTest.kt`, `core/auth/FakeSessionCipher.kt` (test source), update `app/AppViewModelTest.kt`

**Interfaces:**
- Produces:
  - `@Serializable data class AuthUser(val id: Long, val username: String, val email: String, val firstName: String, val lastName: String, val image: String? = null) { val fullName: String get() = "$firstName $lastName".trim() }`; `@Serializable data class AuthTokens(val accessToken: String, val refreshToken: String)`; `@Serializable data class Session(val user: AuthUser, val tokens: AuthTokens)`; `enum class SignOutReason { USER, EXPIRED }`; `data class SignInCredentials(val username: String, val password: String)`.
  - `interface SessionCipher { fun encrypt(plain: ByteArray): ByteArray; fun decrypt(cipher: ByteArray): ByteArray }` (the test seam: JVM tests have no Keystore); `class KeystoreSessionCipher @Inject constructor() : SessionCipher` — AES/GCM/NoPadding, 256-bit key alias `kotlinstarter_session`, generated in `AndroidKeyStore` on first use, IV (12 bytes) prepended to the ciphertext. Bound in a `SessionModule` (`@Binds`).
  - `@Singleton class SessionRepository @Inject constructor(@SessionStore dataStore: DataStore<Preferences>, cipher: SessionCipher, json: Json, @ApplicationScope scope: CoroutineScope)` with:
    - `val session: StateFlow<Session?>`, `val loaded: StateFlow<Boolean>` (true after the first read), `val signOutReason: StateFlow<SignOutReason?>`;
    - `val isSignedIn: Boolean get() = session.value != null`; `fun accessToken(): String?`;
    - `suspend fun signIn(session: Session)` (clears `signOutReason`), `suspend fun updateTokens(tokens: AuthTokens)` (no-op when signed out), `suspend fun signOut(reason: SignOutReason)` (clears storage, sets reason), `fun clearSignOutReason()`.
    - Storage: key `auth.session`, value Base64 of `cipher.encrypt(json(session))`. Any decrypt/parse failure → treated as signed out and the key is removed. `signOutReason` is never persisted.
  - `@Qualifier annotation class ApplicationScope` + provider of a `SupervisorJob() + Dispatchers.Default` scope (in `core/storage` or a new `core/common/CoroutinesModule.kt`); `@Qualifier annotation class SessionStore`.
  - `AppViewModel.ready: StateFlow<Boolean>` = theme loaded && `sessionRepository.loaded`; `MainActivity` keeps the splash while `!ready`.
- `auth-session.md` (storage section): what is stored, encryption, why backups exclude it, failure handling.

- [ ] **Step 1: Write failing tests** (in-memory DataStore fake, `FakeSessionCipher` = XOR with a constant byte so ciphertext differs from plaintext, `TestScope` as application scope): `SessionRepositoryTest` — fresh store → `loaded` becomes true, `session == null`; `signIn(s)` → `session == s`, raw stored value is not the JSON text (it is encrypted); a new repository on the same store restores `s`; `updateTokens(t)` replaces tokens and persists; `signOut(EXPIRED)` → `session == null`, stored key removed, `signOutReason == EXPIRED`; `clearSignOutReason()` → null; a store pre-filled with `"not-base64!!"` → `session == null`, `loaded == true`, key removed (Review Focus 3); a cipher whose `decrypt` throws `GeneralSecurityException` → same outcome. `AppViewModelTest` — `ready` false until both theme and session have loaded.
- [ ] **Step 2: Run** `./gradlew testDebugUnitTest --tests "*SessionRepositoryTest" --tests "*AppViewModelTest"` → FAIL.
- [ ] **Step 3: Implement**; backup exclusions; docs.
- [ ] **Step 4: Run** full proof command → PASS.
- [ ] **Step 5: Commit** `feat(auth): add encrypted persisted session and gate splash on it`

---

### Task 3: Bearer tokens, single-flight refresh, expiry

**Files:**
- Create: `core/auth/{AuthAdapter,SessionRefresher,AuthInterceptor,TokenAuthenticator,SessionExpiryAnnouncer}.kt`
- Modify: `core/network/NetworkModule.kt` (add interceptor + authenticator), `KotlinStarterApp.kt` (start the announcer), `docs/architecture/auth-session.md` (request + refresh sections), strings ×2
- Test: `core/auth/AuthFlowTest.kt` (MockWebServer end-to-end), `core/auth/SessionRefresherTest.kt`, `core/auth/SessionExpiryAnnouncerTest.kt`

**Interfaces:**
- Consumes: `SessionRepository`, `Session`, `AuthTokens`, `SignOutReason` (T2); `isAnonymous()`, `ApiError`, `safeApiCall` (T1); `Messenger`, `UiText` (plan 2b).
- Produces:
  - `data class AuthResult(val user: AuthUser, val tokens: AuthTokens)`; `interface AuthAdapter { suspend fun signIn(credentials: SignInCredentials): AuthResult; suspend fun refresh(refreshToken: String): AuthTokens }` — the only backend-specific piece (spec §5.1).
  - `@Singleton class SessionRefresher @Inject constructor(repository: SessionRepository, adapter: AuthAdapter) { suspend fun refresh(failedAccessToken: String?): String? }` — under a `Mutex`: if the stored access token already differs from `failedAccessToken` (another caller refreshed meanwhile), return it without calling the adapter; if signed out return null; else call `adapter.refresh`, `updateTokens`, return the new access token; any exception → if still signed in `signOut(EXPIRED)`, return null.
  - `class AuthInterceptor @Inject constructor(repository: SessionRepository) : Interceptor` — adds `Authorization: Bearer …` when a token exists and the request is not `@Anonymous`.
  - `class TokenAuthenticator @Inject constructor(refresher: Provider<SessionRefresher>) : Authenticator` — returns null (give up) when the request is `@Anonymous`, has no bearer header, or `response.priorResponse != null` (already retried once); otherwise `runBlocking { refresher.get().refresh(failedToken) }` and rebuilds the request with the new bearer, or null. A `Provider` breaks the client → adapter → client cycle.
  - `@Singleton class SessionExpiryAnnouncer @Inject constructor(repository: SessionRepository, messenger: Messenger, @ApplicationScope scope: CoroutineScope) { fun start() }` — on `signOutReason == EXPIRED`: `messenger.error(UiText.Resource(R.string.auth_session_expired))` then `clearSignOutReason()`. `KotlinStarterApp.onCreate` calls `start()` (field-injected).
- Strings: `auth_session_expired` "Your session has expired. Please sign in again." / "Sesi kamu sudah berakhir. Silakan masuk lagi."

- [ ] **Step 1: Write failing tests**: `AuthFlowTest` (MockWebServer; real OkHttp with `AuthInterceptor` + `TokenAuthenticator`; a fake `AuthAdapter` counting `refresh` calls; repository signed in with tokens `a1`/`r1`):
  - authenticated GET carries `Authorization: Bearer a1`; an `@Anonymous` call carries no header;
  - 401 then 200 → the retried request carries `Bearer a2` (fake refresh returns `a2`/`r2`), result `Success`, repository tokens are `a2`/`r2`;
  - 10 concurrent calls each first answered 401 then 200 (dispatcher keyed on the bearer: `a1` → 401, `a2` → 200) → `refreshCount == 1`, all 10 `Success` (Review Focus 1);
  - refresh throws → caller gets `Failure(ApiError(_, 401))`, `session == null`, `signOutReason == EXPIRED`, exactly one refresh attempt even with 3 concurrent callers (Review Focus 2);
  - 401 on the retried request too → no third request reaches the server (`requestCount == 2`).
  `SessionRefresherTest` — stale `failedAccessToken` returns current token without calling the adapter; signed out returns null without calling it. `SessionExpiryAnnouncerTest` — `signOut(EXPIRED)` produces exactly one `ERROR` message with `auth_session_expired` (Turbine on `messenger.messages`) and resets the reason; `signOut(USER)` produces none.
- [ ] **Step 2: Run** `./gradlew testDebugUnitTest --tests "*AuthFlowTest" --tests "*SessionRefresherTest" --tests "*SessionExpiryAnnouncerTest"` → FAIL.
- [ ] **Step 3: Implement**; wire into `NetworkModule`; start the announcer; docs (request flow diagram in text: interceptor → 401 → authenticator → refresher → retry/sign-out).
- [ ] **Step 4: Run** full proof command → PASS (the app module needs an `AuthAdapter` binding to build: Task 4 adds the DummyJSON one — for this task add it in Task 4's file `feature/example/auth/ExampleAuthModule.kt` with a minimal `DummyJsonAuthAdapter` stub that throws `NotImplementedError`, replaced in Task 4).
- [ ] **Step 5: Commit** `feat(auth): add bearer interceptor, single-flight 401 refresh and expiry message`

---

### Task 4: DummyJSON adapter and sign-in

**Files:**
- Create: `feature/example/auth/{DummyJsonApi,DummyJsonAuthAdapter,SignInViewModel,SignInScreen}.kt`
- Modify: `feature/example/auth/ExampleAuthModule.kt` (real adapter), `feature/example/ExampleRoutes.kt` (`SignInRoute`), `app/navigation/AppNavHost.kt` (`SignInRoute` in `profileGraph`), strings ×2
- Test: `feature/example/auth/DummyJsonAuthAdapterTest.kt` (MockWebServer), `SignInViewModelTest.kt`, `SignInScreenTest.kt`

**Interfaces:**
- Consumes: `AuthAdapter`, `AuthResult`, `SignInCredentials`, `SessionRepository.signIn`, `Session` (T2/T3); `safeApiCall`, `ApiError`, `@Anonymous`, `Retrofit` (T1); components `AppScaffold`, `AppTextField`, `PasswordTextField`, `AppButton`, `Banner`, `LocalMessenger` (plan 2b).
- Produces:
  - `interface DummyJsonApi { @Anonymous @POST("auth/login") suspend fun login(@Body body: LoginRequest): LoginResponse; @Anonymous @POST("auth/refresh") suspend fun refresh(@Body body: RefreshRequest): TokenResponse; @GET("auth/me") suspend fun me(): UserResponse }` with `@Serializable` DTOs (`LoginRequest(username, password, expiresInMins: Int? = null)`, `RefreshRequest(refreshToken, expiresInMins: Int? = null)`, `LoginResponse(id, username, email, firstName, lastName, image, accessToken, refreshToken)`, `TokenResponse(accessToken, refreshToken)`, `UserResponse(id, username, email, firstName, lastName, image)`); provided from `ExampleAuthModule` via `retrofit.create()`.
  - `class DummyJsonAuthAdapter @Inject constructor(api: DummyJsonApi, settings: DemoTokenSettings) : AuthAdapter` — maps DTOs to `AuthResult`/`AuthTokens`; passes `expiresInMins = 1` to login and refresh when `settings.shortLivedTokens` is true (demo of the refresh flow).
  - `@Singleton class DemoTokenSettings @Inject constructor() { var shortLivedTokens: Boolean = false }` (in-memory, example only).
  - `data class SignInUiState(val username: String = "", val password: String = "", val shortLived: Boolean = false, val errors: Map<SignInField, Int> = emptyMap(), val submitAttempted: Boolean = false, val loading: Boolean = false, val error: ApiError? = null)`, `enum class SignInField { USERNAME, PASSWORD }`.
  - `fun validateSignIn(username: String, password: String): Map<SignInField, Int>` — username trimmed length < 3 → `auth_validation_username_min`; password length < 6 → `auth_validation_password_min` (RN `signInSchema`).
  - `@HiltViewModel class SignInViewModel @Inject constructor(adapter: AuthAdapter, repository: SessionRepository, demo: DemoTokenSettings, messenger: Messenger) : ViewModel() { val state: StateFlow<SignInUiState>; val signedIn: Flow<Unit>; fun onUsernameChange(String); fun onPasswordChange(String); fun onShortLivedChange(Boolean); fun fillDemo(); fun submit() }` — validate on submit then live (same pattern as the form example); valid → `loading`, `safeApiCall { adapter.signIn(...) }`; success → `repository.signIn(Session(...))`, `messenger.success(auth_signed_in)`, emit `signedIn`; failure → `error = it`, `loading = false`, fields untouched; a second `submit()` while loading is ignored. `fillDemo()` sets `emilys` / `emilyspass`.
  - `@Composable fun SignInScreen(state: SignInUiState, onUsernameChange: (String) -> Unit, onPasswordChange: (String) -> Unit, onShortLivedChange: (Boolean) -> Unit, onFillDemo: () -> Unit, onSubmit: () -> Unit, onBack: () -> Unit)` — `AppScaffold` (title "Sign in"), subtitle, demo card (`OutlinedCard`: "Demo account", "DummyJSON accepts these credentials.", `emilys / emilyspass`, "Fill the form" button), error `Banner(variant = ERROR, title = "Could not sign in", message = network → `auth_error_network` else `error.message ?: auth_error_generic`)`, username/password fields, a checkbox row "Expire tokens after 1 minute (to try the refresh flow)", `AppButton("Sign in", loading = state.loading, fullWidth = true)`.
  - Route: `composable<SignInRoute>` collects `signedIn` and navigates to `AccountRoute` (Task 5) with `popUpTo<SignInRoute> { inclusive = true }`; until Task 5 exists, it pops back.
- Strings (en / id), from RN where available: `auth_sign_in_title` Sign in / Masuk; `auth_sign_in_subtitle` "Retrofit, OkHttp and an encrypted token in DataStore" / "Retrofit, OkHttp, dan token terenkripsi di DataStore"; `auth_demo_title` Demo account / Akun demo; `auth_demo_description` "DummyJSON accepts these credentials." / "DummyJSON menerima kredensial ini."; `auth_demo_fill` Fill the form / Isi formulir; `auth_field_username` Username / Nama pengguna; `auth_field_password` Password / Kata sandi; `auth_validation_username_min` "Username must be at least 3 characters" / "Nama pengguna minimal 3 karakter"; `auth_validation_password_min` "Password must be at least 6 characters" / "Kata sandi minimal 6 karakter"; `auth_short_lived` "Expire tokens after 1 minute (to try the refresh flow)" / "Token kedaluwarsa setelah 1 menit (untuk mencoba alur refresh)"; `auth_error_title` Could not sign in / Gagal masuk; `auth_error_network` "Cannot reach the server. Check your connection." / "Tidak bisa terhubung ke server. Periksa koneksimu."; `auth_error_generic` "Something went wrong. Please try again." / "Terjadi kesalahan. Silakan coba lagi."; `auth_signed_in` Welcome back / Selamat datang kembali.

- [ ] **Step 1: Write failing tests**: `DummyJsonAuthAdapterTest` (MockWebServer serving the DummyJSON JSON shapes) — `signIn` posts `{"username":"emilys","password":"emilyspass"}` to `/auth/login` without an `Authorization` header and maps all fields; with `shortLivedTokens = true` the body contains `"expiresInMins":1`; `refresh("r1")` posts to `/auth/refresh`. `SignInViewModelTest` (fake `AuthAdapter`, real `SessionRepository` on fakes): submit with `"ab"`/`"12345"` → both errors, adapter not called; fix username → its error disappears live; valid submit → `loading` then session stored, one `SUCCESS` message, one `signedIn` emission; adapter throws `IOException` → `error.isNetworkError`, `loading == false`, username still set (Review Focus 4); adapter throws `HttpException` 400 `{"message":"Invalid credentials"}` → `error.message == "Invalid credentials"`; double submit → adapter called once. `SignInScreenTest` — network error state shows "Cannot reach the server. Check your connection."; "Fill the form" calls `onFillDemo`; loading state → sign-in button not enabled.
- [ ] **Step 2: Run** `./gradlew testDebugUnitTest --tests "*DummyJsonAuthAdapterTest" --tests "*SignIn*"` → FAIL.
- [ ] **Step 3: Implement**, replacing the Task 3 stub.
- [ ] **Step 4: Run** full proof command → PASS.
- [ ] **Step 5: Commit** `feat(example): add DummyJSON auth adapter and sign-in screen`

---

### Task 5: Profile hub, protected Account, avatar images

**Files:**
- Create: `feature/example/auth/{ProfileScreen,AccountViewModel,AccountScreen}.kt`
- Modify: `app/navigation/AppNavHost.kt` (`profileGraph`: start `ProfileRoute`; `SignInRoute`, `AccountRoute`, `SettingsRoute`), `app/navigation/MainShell.kt` (`TopLevelTab.PROFILE.startRoute = ProfileRoute`), `feature/example/ExampleRoutes.kt`, `core/designsystem/component/Avatar.kt` (+ `imageUrl`), `docs/architecture/{auth-session,navigation,components}.md`, `AGENTS.md`, strings ×2
- Test: `feature/example/auth/ProfileScreenTest.kt`, `AccountScreenTest.kt`, `AccountViewModelTest.kt`, `app/ProtectedRouteTest.kt`, update `core/designsystem/component/AvatarTest.kt`, `app/MainShellTest.kt` (Profile root text)

**Interfaces:**
- Consumes: `SessionRepository` (T2), `DummyJsonApi.me()` (T4), `safeApiCall` (T1), components (2b).
- Produces:
  - Routes: `ProfileRoute`, `AccountRoute` (protected), `SignInRoute` (T4), `SettingsRoute`.
  - `@Composable fun ProfileScreen(session: Session?, onSignIn: () -> Unit, onOpenAccount: () -> Unit, onOpenSettings: () -> Unit)` — `AppScaffold(title = "Profile")`; signed out: `EmptyState(title = auth_profile_signed_out_title "You are not signed in", description = auth_profile_signed_out_body, actionLabel = "Sign in")`; signed in: a clickable row with `Avatar(name, imageUrl = user.image, status = ONLINE)`, full name, email → `onOpenAccount`; always a `ListItem` "Settings" → `onOpenSettings`.
  - `@HiltViewModel class AccountViewModel @Inject constructor(repository: SessionRepository, api: DummyJsonApi, json: Json, messenger: Messenger) { val session: StateFlow<Session?>; val profile: StateFlow<ProfileLoad>; fun loadProfile(); fun signOut() }` with `sealed interface ProfileLoad { Idle; Loading; Loaded(user: UserResponse); Failed(error: ApiError) }`; `signOut()` → `repository.signOut(USER)` + `messenger.info(auth_signed_out)`.
  - `@Composable fun AccountScreen(session: Session, profile: ProfileLoad, onLoadProfile: () -> Unit, onSignOut: () -> Unit, onBack: () -> Unit)` — `AppScaffold(title = "Account", onBack)`, subtitle "A screen only a signed-in session can reach", `Avatar(XLARGE, imageUrl)`, name, email, `SectionHeader("Server profile")` + `AppButton("Load from /auth/me", loading = profile is Loading)`; `Loaded` → `InfoRow`s (id, username, email); `Failed` → `Banner(ERROR)`; `ConfirmDialog` before sign-out ("Sign out?", destructive).
  - Guard: `composable<AccountRoute>` collects `session`; when it is null it navigates `navigate(SignInRoute) { popUpTo<AccountRoute> { inclusive = true } }` (covers sign-out and expiry; Review Focus 5). `ProfileRoute` reads `session` and decides the content; sign-in success navigates to `AccountRoute` popping `SignInRoute`.
  - `Avatar(name, …, imageUrl: String? = null)` — when `imageUrl` is non-null uses Coil `SubcomposeAsyncImage` clipped to the shape, showing the initials while loading and on error; `AvatarGroup` unchanged.
- Strings (en / id): `auth_profile_signed_out_title` "You are not signed in" / "Kamu belum masuk"; `auth_profile_signed_out_body` "Sign in to see your account." / "Masuk untuk melihat akunmu."; `auth_account_title` Account / Akun; `auth_account_subtitle` "A screen only a signed-in session can reach" / "Layar yang hanya bisa dibuka setelah masuk"; `auth_server_profile` "Server profile" / "Profil dari server"; `auth_load_profile` "Load from /auth/me" / "Muat dari /auth/me"; `auth_sign_out` Sign out / Keluar; `auth_sign_out_confirm_title` "Sign out?" / "Keluar dari akun?"; `auth_sign_out_confirm_body` "You will need to sign in again to see your account." / "Kamu perlu masuk lagi untuk melihat akunmu."; `auth_signed_out` Signed out / Berhasil keluar; info labels `auth_info_id` ID, `auth_info_username` (reuse `auth_field_username`), `auth_info_email` Email.

- [ ] **Step 1: Write failing tests**: `ProfileScreenTest` — `session = null` shows "You are not signed in" and "Sign in" calls `onSignIn`; with a session shows the full name and clicking it calls `onOpenAccount`; "Settings" calls `onOpenSettings`. `AccountScreenTest` — shows name and email; `Loaded` shows the three info rows; `Failed(ApiError(isNetworkError = true))` shows the network message; sign-out asks for confirmation and only the confirm button calls `onSignOut`. `AccountViewModelTest` (MockWebServer for `me`) — `loadProfile` → `Loading` then `Loaded`; 500 → `Failed`; `signOut()` clears the session and emits one `INFO` message. `ProtectedRouteTest` (Hilt + `MainShell`, fake session signed in via `SessionRepository`): open Profile → account row → Account shown; call `repository.signOut(EXPIRED)` from the test → "Sign in" screen title displayed and back from it does not return to Account (Review Focus 5). `AvatarTest` — `imageUrl = null` still shows initials; with an unreachable URL the initials stay visible. `MainShellTest` — Profile root now shows "You are not signed in" (update the existing assertion).
- [ ] **Step 2: Run** `./gradlew testDebugUnitTest --tests "*ProfileScreenTest" --tests "*Account*" --tests "*ProtectedRouteTest" --tests "*AvatarTest" --tests "*MainShellTest"` → FAIL.
- [ ] **Step 3: Implement** screens, ViewModel, routes, guard, Coil avatar.
- [ ] **Step 4: Run** full proof command → PASS.
- [ ] **Step 5: Docs**: finish `auth-session.md` (guard, sign-out, expiry message, swapping the backend: implement `AuthAdapter`, bind it instead of `DummyJsonAuthAdapter`, keep `@Anonymous` on login/refresh); `navigation.md` (Profile graph + protected route pattern); `components.md` (Avatar `imageUrl`); `AGENTS.md` (core/network, core/auth, where the adapter is bound).
- [ ] **Step 6: Manual check** on emulator/device: sign in with the demo account; with "Expire tokens after 1 minute" on, wait 1 min and tap "Load from /auth/me" → loads (refresh happened); toggle airplane mode and sign in → network message; sign out from Account → back to Profile; kill and relaunch while signed in → no sign-in flash.
- [ ] **Step 7: Commit** `feat(example): add Profile hub, protected Account and avatar images`
