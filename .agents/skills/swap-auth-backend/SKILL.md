---
name: swap-auth-backend
description: Use when connecting sign-in to a real backend instead of DummyJSON - implementing AuthAdapter (sign-in and token refresh), binding it with @AuthBackend, anonymous login/refresh calls, the per-flavour API URL, and tests for the refresh and expiry flow.
---

# Swap the auth backend

`core:auth` owns the session (encrypted storage, bearer header, single-flight refresh on 401, expiry message). The only
backend-specific piece is `AuthAdapter`. Read `docs/architecture/auth-session.md` first: it explains the refresh rules
this skill relies on.

## Steps

1. **Implement `AuthAdapter`** (`core/auth/src/main/java/com/primestrider/kotlinstarter/core/auth/AuthAdapter.kt`):
   - `signIn(credentials): AuthResult` calls your login endpoint and returns the user and tokens.
   - `refresh(refreshToken): AuthTokens` must use a Retrofit built on `@RefreshRetrofit` (its own client and
     dispatcher; the app client is blocked while a refresh runs). Throw an `IOException` when the server cannot be
     reached; that and an `HttpException` with 5xx or 429 keep the session. Any other failure means the refresh token
     was rejected and signs the user out.
   Reference: `DummyJsonAuthAdapter` in `feature/account/impl` (with `DummyJsonApi`).
2. **Mark login and refresh `@Anonymous`** on the Retrofit interface: they must never carry the bearer token or trigger
   the 401 flow.
3. **Bind it** in a Hilt module of the feature that owns sign-in:
   `@Binds @AuthBackend abstract fun bindAuthAdapter(impl: YourAuthAdapter): AuthAdapter`. The qualifier is required:
   `core:auth` already provides the plain `AuthAdapter` (your `@AuthBackend` one, else `NotConfiguredAuthAdapter`), so an
   unqualified binding fails the build with a duplicate `AuthAdapter` binding. With no `@AuthBackend` binding at all,
   `NotConfiguredAuthAdapter` is used and sign-in fails as "not set up". Replace the
   binding in `ExampleAuthModule` (account feature), or remove the account example and bind yours elsewhere; exactly
   one `@AuthBackend` binding may exist.
4. **Base URL**: set the `prod` (and `dev`) default in `apiUrl(...)` in `app/build.gradle.kts`, or override with
   `api.url` in `local.properties` / `API_URL` in the environment. Only that host receives the bearer token.
5. **Profile data**: adapt `AccountViewModel` / the account screens to your user model if the fields differ.

## Tests

- Adapter against `MockWebServer`: success, wrong credentials (4xx with your error body → `ApiError` message), refresh
  success, refresh rejected (non-IO failure), server unreachable (IOException). Pattern: `DummyJsonAuthAdapterTest`.
- The flow is covered generically by `AuthFlowTest` and `SessionRefresherTest` in `core:auth`; keep them green.
- `AuthAdapterBindingTest` in `:app` asserts which adapter is bound: point it at your class.

## Checklist

- [ ] One `@AuthBackend` binding; `./gradlew test` green including `AuthAdapterBindingTest`.
- [ ] Sign in, kill the app, reopen: still signed in. Expire the access token on the server: the next call refreshes
      once. Revoke the refresh token: the user is signed out with the expiry message.
- [ ] `./gradlew test ktlintCheck lint assembleDevDebug` green.
