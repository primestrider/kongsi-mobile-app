package com.primestrider.kotlinstarter.feature.example.sdk.contacts

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.annotation.Config

/** Answers the contact URI with [name]; its `data` sub-URI (the phone lookup) throws unless [phone] is set. */
class FakeContactsProvider : ContentProvider() {
    override fun onCreate() = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor {
        if (uri.lastPathSegment == "data") {
            val number = phone ?: throw SecurityException("no READ_CONTACTS")
            return MatrixCursor(arrayOf("data1")).apply { addRow(arrayOf(number)) }
        }
        return MatrixCursor(arrayOf("display_name")).apply { name?.let { addRow(arrayOf(it)) } }
    }

    override fun getType(uri: Uri): String? = null

    override fun insert(
        uri: Uri,
        values: ContentValues?,
    ): Uri? = null

    override fun delete(
        uri: Uri,
        selection: String?,
        selectionArgs: Array<out String>?,
    ) = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ) = 0

    companion object {
        var name: String? = null
        var phone: String? = null
    }
}

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class ContactsRepositoryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val contact = Uri.parse("content://com.android.contacts/contacts/lookup/abc/1")

    private fun picked(
        name: String?,
        phone: String?,
    ): ContactRow? {
        FakeContactsProvider.name = name
        FakeContactsProvider.phone = phone
        val controller = Robolectric.buildContentProvider(FakeContactsProvider::class.java).create("com.android.contacts")
        try {
            return runBlocking { ContactsRepository(context.contentResolver, Dispatchers.Unconfined).picked(contact) }
        } finally {
            controller.shutdown()
        }
    }

    @Test
    fun a_refused_phone_lookup_keeps_the_name() {
        assertEquals(ContactRow("Budi", null), picked("Budi", phone = null))
    }

    @Test
    fun name_and_phone_are_both_read_when_allowed() {
        assertEquals(ContactRow("Budi", "0812"), picked("Budi", phone = "0812"))
    }

    @Test
    fun nothing_readable_is_a_failure() {
        assertNull(picked(null, phone = null))
    }
}
