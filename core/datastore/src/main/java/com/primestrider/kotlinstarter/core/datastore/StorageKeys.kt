package com.primestrider.kotlinstarter.core.datastore

import androidx.datastore.preferences.core.stringPreferencesKey

object StorageKeys {
    val themeMode = stringPreferencesKey("app.theme")
}
