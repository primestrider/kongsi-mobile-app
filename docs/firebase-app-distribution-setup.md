# Firebase App Distribution setup

`.github/workflows/deploy-android.yml` builds the signed `prod` release and sends it to testers through Firebase App
Distribution, on every merge to `main` and on demand. This guide lists everything the workflow needs that cannot live
in the repository: a Firebase app, a release keystore, and a GitHub environment with secrets.

## Summary

On each run the workflow:

1. decides the version bump (below);
2. runs `./gradlew test`;
3. decodes the keystore and checks the Firebase service-account key;
4. raises `version=` in `gradle.properties` (`./gradlew bumpVersion`) and pushes `chore(release): vX.Y.Z [skip ci]`;
5. builds `./gradlew assembleProdRelease verifyReleaseKeeps` and fails if the APK is signed with the debug key;
6. uploads the APK (and the R8 mapping) as a workflow artifact, distributes it, and tags `vX.Y.Z`.

## Versions rise from the branch name

| PR branch prefix | Bump | Example |
| --- | --- | --- |
| `feat/`, `feature/` | minor | 1.2.3 → 1.3.0 |
| `fix/`, `refactor/`, `chore/`, `docs/`, any other | patch | 1.2.3 → 1.2.4 |
| none (manual run) | your choice: `none`, `patch`, `minor`, `major` | |

- **Major has no prefix on purpose:** a major release is a decision, so it is only available from **Run workflow**.
- **Where the prefix comes from:** the branch of the pull request that contains the merged commit (squash and rebase
  merges keep no branch name in the message). A direct push to `main` uses the conventional commit type of its subject
  (`feat(ui): …` → minor).
- **Written back to `main`:** the version commit is pushed before the build, so a failed build costs one version number
  but two builds never share a `versionCode`. `[skip ci]` and a job guard stop the commit from triggering the workflow.
- **`versionCode` follows the version:** `major * 10000 + minor * 100 + patch` (see `docs/architecture/release.md`).
- **Manual runs from a branch never bump:** they build and distribute the current version, so the pipeline can be
  tested before a merge without recording a version that `main` never had.

## What you need

- A Firebase project where you are an owner.
- A GitHub repository with Actions enabled and admin access to its settings.
- `keytool` (comes with the JDK).

## Part A — Firebase

### A1. Register the Android app

Firebase Console → **Project settings** → **Your apps** → **Add app** → Android. The package name must be exactly the
`prod` application id from `app/build.gradle.kts`:

```
com.primestrider.kotlinstarter
```

It is case-sensitive and cannot be changed after registration. Skip downloading `google-services.json` and adding the
SDK: uploading to App Distribution needs neither. Copy the **App ID** from the app's general settings; it looks like
`1:123456789012:android:a1b2c3d4e5f6a7b8` (not the project ID `name-abc12`, not the project number).

To distribute the `dev` flavour as well, register `com.primestrider.kotlinstarter.dev` as a second app.

### A2. Enable App Distribution and create a tester group

Firebase Console → **Release & Monitor** → **App Distribution** → **Get started**. In **Testers & Groups**, create a
group (e.g. `internal`), note its **alias** (the workflow uses the alias, not the display name) and add testers.

### A3. Create a service-account key

Firebase Console → **Project settings** → **Service accounts** → **Generate new private key**. The downloaded file must
contain `"type": "service_account"`.

> `google-services.json` is **not** a service-account key. It has `project_info` and `client` and no `type`; pasting it
> into the secret gives `Failed to authenticate, have you run firebase login?`. The workflow checks the key before the
> build and names the problem instead.

### A4. Give the service account its role

Google Cloud Console → **IAM & Admin** → **IAM** → the service account → **Edit** → add the role
**Firebase App Distribution Admin**. The role belongs to the account, not to the key file, so a key generated before the
role was added keeps working.

## Part B — Release keystore

Run this yourself (it is interactive and secret), in a folder **outside** the repository:

```powershell
mkdir $HOME\keystores -Force
cd $HOME\keystores
keytool -genkeypair -v -keystore release.keystore -alias upload `
  -keyalg RSA -keysize 2048 -validity 10000 -storetype PKCS12
```

With PKCS12 the key password equals the keystore password. Then copy the keystore as base64 straight to the clipboard:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("$HOME\keystores\release.keystore")) | Set-Clipboard
```

On macOS or Linux: `openssl base64 -A -in ~/keystores/release.keystore`.

> **Back the keystore up outside GitHub.** Without it, testers cannot update the app (they must uninstall first), and it
> is the key you would later use for Play.

For local signed builds, the same values go into `local.properties` (see `docs/architecture/release.md`).

## Part C — GitHub

### C1. Environment

Repository → **Settings** → **Environments** → **New environment**, named exactly `app-distribution`. A different name
makes every secret below read as an empty string, silently.

### C2. Environment secrets

| Secret | Value |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | the base64 text from Part B |
| `ANDROID_KEYSTORE_PASSWORD` | the keystore password |
| `ANDROID_KEY_ALIAS` | `upload` |
| `ANDROID_KEY_PASSWORD` | the keystore password (PKCS12) |
| `FIREBASE_SERVICE_ACCOUNT_JSON` | the whole key file from A3, `{` to `}` (secrets accept several lines) |

### C3. Repository variables

| Variable | Required | Value |
| --- | --- | --- |
| `FIREBASE_ANDROID_APP_ID` | yes | the App ID from A1 |
| `FIREBASE_TESTER_GROUPS` | yes | group aliases from A2, comma-separated |
| `API_URL` | no | overrides the `prod` API base URL for the build; compiled into the APK, so not a secret |

### C4. Deployment branches

For the first test keep **All branches**. Once the pipeline works, restrict the environment to `main` (**Deployment
branches and tags** → **Selected branches**), so the keystore can only be used from `main`. A job from another branch
then does not start at all, visibly.

### C5. Let Actions push to `main`

- **Settings → Actions → General → Workflow permissions → Read and write permissions**, or **Commit version bump** fails
  with `403`.
- If a ruleset or branch protection requires pull requests on `main`, add `github-actions[bot]` to its bypass list.

If you do not want automatic commits on `main`, run the workflow only by hand with `bump: none` and raise the version
yourself with `./gradlew bumpVersion`.

## Testing before a merge

Push the workflow on a branch, open **Actions → Distribute Android → Run workflow**, pick that branch and `bump: none`.
The run tests, builds, verifies the signature and distributes the current version without touching `main`. Check that
the tester got the build, then merge.

## After cloning this starter

- **Change in code:** the application id (`namespace`/`applicationId` in `app/build.gradle.kts`) before registering the
  Firebase app; the `prod` API URL; the deep-link scheme (`app.scheme`).
- **Create per project:** the Firebase app and tester group, a new keystore, the `app-distribution` environment, its
  secrets and the two variables.
- **No change needed:** the workflow itself and the secret names.

## Troubleshooting

- **`Failed to authenticate, have you run firebase login?`** — the service-account secret is not a service-account key
  (often `google-services.json`), or the account lacks the role from A4. The key check step usually catches the first.
- **Every secret is empty** — the environment name is not exactly `app-distribution`.
- **The job does not start** — the branch is not allowed by the environment's deployment branches (C4).
- **`Commit version bump` fails with `403` or `protected branch hook declined`** — see C5.
- **`Commit version bump` fails with `Updates were rejected`** — someone pushed to `main` during the run. Re-run the
  workflow; it is never rebased automatically, so untested changes cannot slip into the build.
- **`Permission denied` on `./gradlew`** — the workflow sets the executable bit; if you copied steps elsewhere, keep
  `chmod +x gradlew`.
- **Signature check fails with `CN=Android Debug`** — the keystore secret or one of the passwords is missing or wrong,
  so the build fell back to the debug key. Check the four `ANDROID_*` secrets.
- **Firebase `403`** — the service account lacks **Firebase App Distribution Admin**, or the App ID belongs to another
  project.
