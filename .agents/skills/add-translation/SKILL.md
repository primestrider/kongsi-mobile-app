---
name: add-translation
description: Use when adding or changing user-visible text - strings in the right module for English (values/) and Indonesian (values-in/), naming and prefix rules, placeholders and plurals, reading another module's strings, and the TranslationCompletenessTest that guards them.
---

# Add or change a string

Every module carries its own strings. English is the fallback (`values/`), Indonesian lives in `values-in/`. Read
`docs/architecture/i18n.md` for language switching.

## Steps

1. **Pick the module** that uses the text: the feature module for its screens, `core:designsystem` for reusable
   components (`component_` prefix), `core:ui` for crash/permission UI, `:app` for the shell (tab labels, app name).
2. **Name it** with the owner's prefix (`products_…`, `todos_…`, `auth_…`, `component_…`) and keep the name **unique
   across all modules**: Android merges resources, so a second module defining the same name silently overrides the
   first. `TranslationCompletenessTest` fails on duplicates.
3. **Add it to both files** of that module: `src/main/res/values/strings.xml` and `src/main/res/values-in/strings.xml`,
   same name, same placeholders (`%1$s`, `%1$d`, in the same set). Plurals: a `<plurals>` entry in both files.
   Text that is never translated (e.g. `+62`) gets `translatable="false"` in `values/` only.
4. **Use it** with `stringResource(R.string.<name>)` in Compose, or `UiText.Resource(R.string.<name>)` from a
   ViewModel. `R` is the module's own class (`com.primestrider.kotlinstarter.feature.<name>.impl.R`).
5. **Another module's string** (only from a module that depends on it): import its R under an alias, e.g.
   `import com.primestrider.kotlinstarter.core.designsystem.R as DesignR` and `DesignR.string.component_retry`. A
   feature must not read another feature's strings: copy the text under its own prefix instead.

## Checklist

- [ ] Both locales updated, placeholders identical.
- [ ] `./gradlew test` green: `TranslationCompletenessTest` (in `:app`) reads every module's `values/` and
      `values-in/` and checks keys, placeholders, plurals and name uniqueness.
- [ ] A new language: add `values-<code>/strings.xml` to every module that has strings, add it to `AppLanguage`
      (`core:common`) and extend `TranslationCompletenessTest`; `generateLocaleConfig` in `:app` picks it up.
