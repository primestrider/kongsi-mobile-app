package com.primestrider.kotlinstarter.core.datastore

import org.junit.Assert.assertEquals
import org.junit.Test

/** Upgrading from the single-module app must find the same DataStore files, or settings and the session are lost. */
class DataStoreFileNamesTest {
    @Test
    fun datastore_file_names_unchanged() {
        assertEquals("settings", DataStoreFiles.SETTINGS)
        assertEquals("session", DataStoreFiles.SESSION)
    }
}
