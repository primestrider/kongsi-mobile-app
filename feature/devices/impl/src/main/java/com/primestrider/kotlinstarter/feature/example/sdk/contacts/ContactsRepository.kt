package com.primestrider.kotlinstarter.feature.example.sdk.contacts

import android.content.ContentResolver
import android.net.Uri
import android.provider.ContactsContract
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** One address book entry: [name] may be missing, [phone] is the first number if there is one. */
data class ContactRow(
    val name: String?,
    val phone: String?,
)

/** What the Contacts demo needs from the address book; the ViewModel depends on this, not on a ContentResolver. */
interface ContactsSource {
    suspend fun count(): Int

    suspend fun search(
        name: String,
        limit: Int = DEFAULT_LIMIT,
    ): List<ContactRow>

    companion object {
        const val DEFAULT_LIMIT = 10
    }
}

/** Reads the address book with `ContactsContract`. Every query runs on [io]; a missing permission reads as empty. */
class ContactsRepository(
    private val contentResolver: ContentResolver,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : ContactsSource {
    override suspend fun count(): Int =
        withContext(io) {
            safely(0) {
                contentResolver
                    .query(
                        ContactsContract.Contacts.CONTENT_URI,
                        arrayOf(ContactsContract.Contacts._ID),
                        null,
                        null,
                        null,
                    )?.use {
                        it.count
                    } ?: 0
            }
        }

    /** Contacts whose name contains [name] (all of them when it is blank), by name, at most [limit]. */
    override suspend fun search(
        name: String,
        limit: Int,
    ): List<ContactRow> =
        withContext(io) {
            safely(emptyList()) {
                val filter = name.trim()
                val selection = if (filter.isEmpty()) null else "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} LIKE ?"
                val args = if (filter.isEmpty()) null else arrayOf("%$filter%")
                val contacts = mutableListOf<Pair<Long, String?>>()
                contentResolver
                    .query(
                        ContactsContract.Contacts.CONTENT_URI,
                        arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME_PRIMARY),
                        selection,
                        args,
                        "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} COLLATE NOCASE ASC",
                    )?.use { cursor ->
                        while (contacts.size < limit && cursor.moveToNext()) {
                            contacts += cursor.getLong(0) to cursor.getString(1)?.takeIf { it.isNotBlank() }
                        }
                    }
                contacts.map { (id, displayName) -> ContactRow(displayName, firstPhone(id)) }
            }
        }

    /** The name and first number of the contact the system picker returned. */
    suspend fun picked(contact: Uri): ContactRow? =
        withContext(io) {
            // Each query on its own: a refused phone lookup must not throw away the name that was already read.
            val name =
                safely(null) {
                    contentResolver
                        .query(contact, arrayOf(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY), null, null, null)
                        ?.use { if (it.moveToFirst()) it.getString(0) else null }
                }
            // The picker grants access to this contact's data directory even without READ_CONTACTS.
            val phone =
                safely(null) {
                    contentResolver
                        .query(
                            Uri.withAppendedPath(contact, ContactsContract.Contacts.Data.CONTENT_DIRECTORY),
                            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                            "${ContactsContract.Data.MIMETYPE} = ?",
                            arrayOf(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE),
                            null,
                        )?.use { if (it.moveToFirst()) it.getString(0) else null }
                }
            if (name == null && phone == null) null else ContactRow(name, phone)
        }

    private fun firstPhone(contactId: Long): String? =
        contentResolver
            .query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                arrayOf(contactId.toString()),
                null,
            )?.use { if (it.moveToFirst()) it.getString(0) else null }

    private inline fun <T> safely(
        fallback: T,
        block: () -> T,
    ): T =
        try {
            block()
        } catch (e: SecurityException) {
            fallback
        }
}
