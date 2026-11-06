---
name: add-api-endpoint
description: Use when calling a new backend endpoint - a Retrofit method and @Serializable DTO in the feature's data/ package, mapping to a model, error handling with safeApiCall and ApiError, anonymous versus authenticated calls, and MockWebServer tests.
---

# Add an API endpoint

Networking lives in `core:network` (Retrofit + OkHttp + kotlinx serialization); auth plugs in from `core:auth`. Read
`docs/architecture/networking.md` for the client and `docs/architecture/data-loading.md` for loading states.

## Steps

1. **DTO** in the feature's `data/` package: `@Serializable data class <Name>Dto(...)`. The shared `Json` ignores
   unknown keys and omits explicit nulls, so declare only the fields you use and give optional ones defaults.
2. **Retrofit method** on the feature's interface (create one if needed, e.g.
   `feature/products/impl/src/main/java/com/primestrider/kotlinstarter/feature/example/products/data/ProductsApi.kt`):
   `suspend fun <name>(@Path("id") id: Long): <Name>Dto`. Paths are relative (no leading `/`) to the `@ApiBaseUrl`.
   Mark public calls `@Anonymous`: they then never carry the bearer token nor trigger the 401 refresh. Without it,
   requests to the API host get `Authorization: Bearer …` while signed in.
3. **Provide** the interface once: `@Provides @Singleton fun <name>Api(retrofit: Retrofit) = retrofit.create(…)` in the
   feature's Hilt module.
4. **Map** to a model in `data/` (`fun <Name>Dto.to<Name>()`); the repository returns models, never DTOs
   (`ProductsArchitectureTest` shows how to pin that).
5. **Errors**: in a ViewModel or repository, wrap the call: `when (val r = safeApiCall(json) { api.<name>() }) { is
   ApiResult.Success -> …; is ApiResult.Failure -> r.error }`. `ApiError` carries `message` (the server's), `status`,
   `isNetworkError` and the JSON body (`data`). Show it with `ApiError.userMessage()` from `core:network`
   (localized fallbacks). Paging: `LoadResult.Error` + `throwable.toApiError(json)`.
6. **Login and token refresh** endpoints belong to the `AuthAdapter`, built on `@RefreshRetrofit` (see `swap-auth-backend`).

## Tests

- DTO → model mapper test.
- Endpoint: a `MockWebServer` and a real Retrofit on its URL, asserting the request path/query and the parsed result,
  and the error mapping for a 4xx with a JSON body and for a dropped connection. Patterns: `SafeApiCallTest` and
  `TestApi` (`core:testing`), `ProductsPagingSourceTest`. Add
  `testImplementation(libs.okhttp.mockwebserver3)` and `testImplementation(libs.retrofit.converter.kotlinx.serialization)`.
- ViewModel: a fake repository returning success and failure.

## Checklist

- [ ] `@Anonymous` decided on purpose for every new method.
- [ ] No DTO type in `ui/`.
- [ ] Release: R8 keeps serializers through the library's rules; still run `./gradlew assembleProdRelease
      verifyReleaseKeeps` and try the call in the prod release on a device before shipping.
- [ ] `./gradlew test ktlintCheck lint assembleDevDebug` green.
