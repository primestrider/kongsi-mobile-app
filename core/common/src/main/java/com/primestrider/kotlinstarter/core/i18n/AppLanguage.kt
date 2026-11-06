package com.primestrider.kotlinstarter.core.i18n

enum class AppLanguage(
    val tag: String,
    val nativeName: String,
) {
    EN("en", "English"),
    ID("id", "Bahasa Indonesia"),
    ;

    companion object {
        val fallback = EN
    }
}

/**
 * Explicit app locale wins, then the device language, then [AppLanguage.fallback].
 * Only the language subtag is compared; the legacy `in` code maps to `id`.
 */
fun resolveLanguage(
    appLocaleTag: String?,
    deviceTag: String,
): AppLanguage = match(appLocaleTag) ?: match(deviceTag) ?: AppLanguage.fallback

private fun match(tag: String?): AppLanguage? {
    if (tag.isNullOrBlank()) return null
    val language =
        tag
            .substringBefore('-')
            .substringBefore('_')
            .lowercase()
            .let { if (it == "in") "id" else it }
    return AppLanguage.entries.firstOrNull { it.tag == language }
}
