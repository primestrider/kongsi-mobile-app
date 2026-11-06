# Networking

HTTP lives in `core/network`. Features never build their own client.

## Client

`NetworkModule` (`core:network`) provides a singleton `Json` (`ignoreUnknownKeys`, `explicitNulls = false`) and two
clients, both bound to `@ApiBaseUrl HttpUrl` with the kotlinx-serialization converter. `:app` provides the URL from
its flavour's `BuildConfig.API_URL` (`EnvironmentModule`), so `core:network` has no environment of its own:

- **App client** (`OkHttpClient`, `Retrofit`): what features use. It adds every interceptor contributed to the
  `@AppInterceptors` set and the optional `Authenticator`; `core:auth` contributes `AuthInterceptor` (the bearer
  token) and binds `TokenAuthenticator` (refresh on 401, see `auth-session.md`), so `core:network` never depends on
  `core:auth`. Both act only on requests
  whose host is the `API_URL` host, so a call to any other host (an image CDN, a third-party API)
  never carries the token.
- **Refresh client** (`@RefreshClient OkHttpClient`, `@RefreshRetrofit Retrofit`): its own
  dispatcher, no bearer, no authenticator. Only the auth adapter's refresh call uses it.

Both have 30 s connect, read and write timeouts and a 60 s timeout for the whole call. Debug builds
log request and response headers only, never bodies (login and refresh carry passwords and
tokens). The logger is a network interceptor, so it sees the final headers, including
authenticator retries. `Authorization`, `Cookie` and `Set-Cookie` are redacted.

## Base URL

`BuildConfig.API_URL` is resolved at build time per flavour, first match wins:

1. `api.url` in `local.properties`
2. environment variable `API_URL`
3. the flavour's default in `app/build.gradle.kts` (both `dev` and `prod` default to `https://dummyjson.com/`; see `release.md`)

Keep the trailing slash; Retrofit requires it. A blank value counts as unset.

A plain `http://` URL (for example `http://10.0.2.2:3000/` for a local server from the emulator) is blocked by Android unless you add a network security config or cleartext allowance for it.

## Certificate pinning

Pinning makes the app refuse the API host unless its certificate chain contains one of a few known public keys, so a
trusted-but-wrong certificate (a compromised CA, a corporate proxy) cannot read the traffic. It is **off by default**:
DummyJSON rotates its certificates, and a stale pin locks every installed copy out of the API until users update.

`BuildConfig.API_PINS` is a comma-separated list of `sha256/…` pins, resolved like the base URL: `api.pins` in
`local.properties`, else the environment variable `API_PINS`, else empty. `EnvironmentModule` provides it as
`@CertificatePins Set<String>`; when it is not empty `NetworkModule` adds a `CertificatePinner` for the
`@ApiBaseUrl` host to **both** clients (app and refresh). Other hosts (image CDN, third-party APIs) are never pinned.

- **Get the pins:** `openssl s_client -connect api.example.com:443 -servername api.example.com </dev/null | openssl x509 -pubkey -noout | openssl pkey -pubin -outform der | openssl dgst -sha256 -binary | base64`,
  then prefix `sha256/`. Pin the intermediate CA's key (it outlives the leaf), not only the leaf.
- **Always ship a backup pin** (a second CA or a key you hold but have not deployed yet), so a rotation does not lock
  users out. Coordinate every certificate change with the backend team before it happens.
- **Debug proxies** (Charles, mitmproxy) stop working against a pinned build; leave `api.pins` unset locally.
- Tests: `PinningTest` (HTTPS `MockWebServer` with an `okhttp-tls` `HeldCertificate`: matching pin connects, wrong pin
  is refused on both clients, no pins means no pinner).

## `@Anonymous`

Annotate a Retrofit method with `@Anonymous` to send it without the bearer token (login, refresh).
Interceptors check `request.isAnonymous()`.

## Errors

Wrap every call in `safeApiCall(json) { api.something() }`. It returns `ApiResult.Success(data)` or
`ApiResult.Failure(ApiError(message, status, isNetworkError, data))`:

| Cause | `message` | `status` | `isNetworkError` | `data` |
| --- | --- | --- | --- | --- |
| HTTP error | `message` field of a JSON body, else null | HTTP code | false | the body as a `JsonElement` when it is valid JSON, else null |
| `IOException` | null | null | true | null |
| Malformed response | null | null | false | null |

Use `data` for structured error bodies, for example per-field validation errors of a 422.

Cancellation is rethrown, never swallowed.

## Adding an API to a feature

1. Declare a Retrofit interface with `suspend` functions (relative paths).
2. Provide it from a feature Hilt module: `retrofit.create(MyApi::class.java)`.
3. Call it through `safeApiCall` from a repository and map `ApiError` to UI state.
