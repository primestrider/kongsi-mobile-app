package com.primestrider.kongsi.core.datastore

import androidx.datastore.preferences.core.stringPreferencesKey

object StorageKeys {
    val themeMode = stringPreferencesKey("app.theme")

    /** The user's own name and mobile number (Profile), used as "Me" in bills and for e-wallet accounts. */
    val profileName = stringPreferencesKey("profile.name")
    val profilePhone = stringPreferencesKey("profile.phone")
}
