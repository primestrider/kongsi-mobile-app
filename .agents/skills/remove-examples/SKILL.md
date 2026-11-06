---
name: remove-examples
description: Use when starting a real app from this starter and the example features (catalog, products, todos, account, settings, devices, animation) should go - runs scripts/remove-examples.sh on a copy first, verifies the result, then applies it, and lists what stays and what to clean up by hand.
---

# Remove the example features

The seven example features are deletable by design: each is its own `:api` + `:impl` pair, and `:app` wires them only
inside `// region example:<feature>` blocks. `scripts/remove-examples.sh` removes exactly those seven and leaves your own
feature modules alone.

## Steps

1. **Commit or stash** your work; start from a clean tree.
2. **Try it on a copy:**
   ```
   git worktree add ../no-examples
   bash scripts/remove-examples.sh ../no-examples
   cd ../no-examples
   ./gradlew test ktlintCheck lint assembleDevDebug assembleProdRelease verifyReleaseKeeps
   ```
   The script deletes `feature/<example>/` and their `include` lines, every `// region example:` block (nested ones
   too), the `:app` tests that exercise examples (tests in a `feature.*` package, tests importing an example module, and
   tests starting with `// remove-examples:`), then runs `ktlintFormat` to drop the imports those blocks used.
   Install it: the app opens on `PlaceholderHomeScreen` with one tab; sign-in is "not configured" until a feature binds
   an `@AuthBackend AuthAdapter` (see `swap-auth-backend`).
3. **Apply it** to the real checkout (`bash scripts/remove-examples.sh`, or `bash scripts/remove-examples.sh --keep .examples`
   to keep the deleted code as git-ignored reference in `.examples/`, see AGENTS.md), or merge the copy's branch. On Windows run the
   script from Git Bash. Delete the worktree afterwards (`git worktree remove ../no-examples`; if Windows keeps files
   locked, stop Gradle first with `./gradlew --stop`).
4. **Installed builds:** the Background Task demo scheduled periodic work named `example-background-sync`. Cancel it
   once in your first release (`WorkManager.getInstance(context).cancelUniqueWork("example-background-sync")`), or
   WorkManager keeps trying to start a worker class that no longer exists.
5. **Docs:** the docs describe the examples. Rewrite `README.md`, `AGENTS.md` and `docs/` for your app, then restore
   `DocsLinksTest` (the script deleted it with the examples) so quoted paths stay correct.

## What stays on purpose

- `core:*` (including `ExamplePageScaffold`/`DemoSection` in `core:ui`, the PiP host, `PermissionGate`).
- WorkManager's on-demand setup in `KotlinStarterApp` and the manifest, the deep-link intent filter on `MainActivity`.
- Flavours, signing, R8, CI and the distribution workflow.

Remove those by hand only if your app does not need them; `docs/examples.md` ("Deleting the section") lists what each
demo needed outside its module.

## Checklist

- [ ] Copy builds and tests green before applying.
- [ ] Your own `feature/<name>` modules and their `include` lines still present, and none of them depends on an example
      module (e.g. `catalog:api` for Home rows): those would no longer compile.
- [ ] Docs rewritten and `DocsLinksTest` back.
