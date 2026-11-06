# 8. Environment config: the `.env` of an Android app

How this repository handles per-environment values (API URL, certificate pins, signing keys), explained for someone
who knows `.env` from Node.js. Chapter 5 introduces flavours; this chapter follows one value from where you set it to
where the code reads it.

## The one idea that differs from Node

In Node, `.env` is read **at runtime**: `dotenv` loads it into `process.env` when the server starts. A phone has no
`process.env` you control, so on Android the values are read **at build time** by Gradle and compiled into the APK as
constants of a generated `BuildConfig` class. Changing a value means building again.

| Node.js | This repository |
| --- | --- |
| `.env` (git-ignored) | `local.properties` (git-ignored) |
| `process.env.X` set by CI | an environment variable such as `API_URL` |
| `.env.development` / `.env.production` | the `dev` / `prod` product flavours |
| `process.env.API_URL` in code | `BuildConfig.API_URL` |
| `dotenv` | `localOrEnv` in `AndroidConfig.kt` |

## Following one value

```mermaid
flowchart LR
    lp[local.properties<br/>api.url=...] --> loe[localOrEnv]
    env[env var<br/>API_URL=...] --> loe
    def[default in<br/>app/build.gradle.kts] --> loe
    loe --> bcf[buildConfigField<br/>per flavour]
    bcf --> bc[BuildConfig.API_URL<br/>generated, in the APK]
    bc --> em[EnvironmentModule<br/>@ApiBaseUrl]
    em --> net[Retrofit / OkHttp<br/>in core/network]
```

### 1. Resolve the value

`build-logic/convention/src/main/kotlin/com/primestrider/kotlinstarter/buildlogic/AndroidConfig.kt`:

```kotlin
/** `local.properties` [key], else environment variable [env], else [default]; blank values count as absent. */
fun Project.localOrEnv(key: String, env: String, default: String): String {
    val local = java.util.Properties().apply {
        rootProject.file("local.properties").takeIf { it.exists() }?.reader()?.use { load(it) }
    }
    return local.getProperty(key)?.takeIf { it.isNotBlank() }
        ?: providers.environmentVariable(env).orNull?.takeIf { it.isNotBlank() }
        ?: default
}
```

The lookup order is `local.properties`, then the environment variable, then the default. On your machine you edit
`local.properties`. CI has no such file, so the environment variable is used.

### 2. Compile it into the build

`app/build.gradle.kts` writes the value into each flavour:

```kotlin
fun apiUrl(default: String): String = localOrEnv("api.url", "API_URL", default)

productFlavors {
    create("dev") {
        dimension = "environment"
        applicationIdSuffix = ".dev"
        buildConfigField("String", "API_URL", javaString(apiUrl("https://dummyjson.com/")))
    }
    create("prod") {
        dimension = "environment"
        buildConfigField("String", "API_URL", javaString(apiUrl("https://dummyjson.com/")))
    }
}
```

`buildConfigField(type, name, value)` adds `public static final String API_URL = "...";` to the generated
`BuildConfig`. The value is pasted in as Java source, so `javaString` quotes and escapes strings. Without it the
build fails on a stray `"`.

### 3. Read it in one place

`app/src/main/java/com/primestrider/kotlinstarter/app/di/EnvironmentModule.kt` is the only code that reads
`BuildConfig.API_URL`:

```kotlin
fun apiBaseUrl(): HttpUrl = BuildConfig.API_URL.toHttpUrl()
```

Hilt passes the value to `core/network` as `@ApiBaseUrl`. Core and feature modules never touch `BuildConfig`, so they
do not know which environment they run in, and tests can provide their own URL.

> **In React/Vue:** this is like reading `import.meta.env.VITE_API_URL` once in `src/config.ts` and importing the
> config everywhere else, instead of sprinkling `import.meta.env` across components.

## The values that exist today

| `local.properties` | Environment variable | Default | Used for |
| --- | --- | --- | --- |
| `api.url` | `API_URL` | `https://dummyjson.com/` | API base URL; keep the trailing slash |
| `api.pins` | `API_PINS` | empty (no pinning) | certificate pins; see `networking.md` |
| `app.scheme` | `APP_SCHEME` | `kotlinstarter` | deep-link scheme (`dev` adds `-dev`) |
| `play.integrity.project` | `PLAY_INTEGRITY_PROJECT` | `0` | Play Integrity demo; see `feature/devices/impl/build.gradle.kts` |
| `android.keystore.path` and friends | `ANDROID_KEYSTORE_PATH` and friends | none (debug-signed) | release signing; see `release.md` |

## Everyday use

Point your local build at another server by adding lines to `local.properties` in the repository root:

```properties
api.url=https://staging.example.com/
```

From a shell or CI, set the environment variable for one build:

```bash
API_URL=https://api.example.com/ ./gradlew assembleProdRelease
```

In GitHub Actions the values come from repository secrets passed as `env:`. See the signing steps in
`.github/workflows/deploy-android.yml`.

Choose the environment by choosing the variant: `assembleDevDebug` / `assembleProdRelease` on the command line, or the
Build Variants panel in Android Studio. `dev` has its own application id, so it installs next to `prod`.

## Adding a new value

Example: an analytics key.

1. Add the field to `app/build.gradle.kts`. Put it in `defaultConfig` if the value is the same for every flavour, or
   in each flavour if it differs:

   ```kotlin
   defaultConfig {
       buildConfigField("String", "ANALYTICS_KEY", javaString(localOrEnv("analytics.key", "ANALYTICS_KEY", "")))
   }
   ```

2. Provide it from `EnvironmentModule` (with a qualifier like `@ApiBaseUrl`) rather than reading `BuildConfig` from a
   feature module.
3. Add the value to the table above and, if CI needs it, to the workflow's `env:` as a secret.

A module that only needs a value for itself (like `feature/devices/impl/build.gradle.kts`) can declare the field in its
own build file with `buildFeatures { buildConfig = true }`. It then gets its own `BuildConfig` in its own package.

## Security: the APK is public

Everything in `BuildConfig` ends up inside the APK, and anyone can unpack an APK. Treat `BuildConfig` like a frontend
bundle:

- **Safe:** URLs, feature flags, public keys, certificate pins, analytics or crash-reporting ids that the vendor marks
  as client-side.
- **Not safe:** server API keys, client secrets, database passwords. Keep those on a backend and have the app call it.

`local.properties` stays out of git so that machine paths and signing passwords are never committed. It does not hide
values from people who have the built app.

## Why not a `.env` file and a plugin?

Google's `secrets-gradle-plugin` reads a properties file into `BuildConfig`, which is the closest thing to `dotenv`.
This repository does the same job in a few lines with `localOrEnv`, and it also supports environment variables for CI.
Adding the plugin would add a dependency without adding a feature.

Back to the [index](README.md).
