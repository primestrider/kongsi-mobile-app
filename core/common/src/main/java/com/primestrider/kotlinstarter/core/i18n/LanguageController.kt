package com.primestrider.kotlinstarter.core.i18n

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads and changes the per-app language. Persistence is handled by AppCompat
 * (`autoStoreLocales` in the manifest), so no extra storage is needed.
 */
@Singleton
class LanguageController
    @Inject
    constructor() {
        fun current(): AppLanguage {
            val appTag = AppCompatDelegate.getApplicationLocales().toLanguageTags()
            val deviceTag = LocaleListCompat.getAdjustedDefault()[0]?.toLanguageTag().orEmpty()
            return resolveLanguage(appTag, deviceTag)
        }

        fun set(language: AppLanguage) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag))
        }
    }
