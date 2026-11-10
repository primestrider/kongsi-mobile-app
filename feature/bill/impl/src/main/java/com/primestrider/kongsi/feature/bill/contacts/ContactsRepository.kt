package com.primestrider.kongsi.feature.bill.contacts

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Phone
import com.primestrider.kongsi.core.util.IndonesianPhone
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * One phone number of a contact, as offered when adding people to a bill. [phone] is the number as saved in the
 * address book; [international] is its `628…` form, or null when it is not an Indonesian mobile number. [hasWhatsApp]
 * is true when WhatsApp has a profile for this very number.
 */
data class ContactMatch(
    val contactId: Long,
    val name: String,
    val phone: String,
    val international: String?,
    val mobile: Boolean,
    val photoUri: String?,
    val hasWhatsApp: Boolean,
)

/** The address book, read on demand and never copied. */
interface ContactsRepository {
    /**
     * Contacts whose name or number matches [query], one row per number, mobile numbers first within a contact.
     * Needs READ_CONTACTS; without it (or for a blank query) the result is empty.
     */
    suspend fun search(
        query: String,
        limit: Int = 20,
    ): List<ContactMatch>

    /** The number the system contact picker returned (it grants temporary read access), or null when it is gone. */
    suspend fun fromPickerResult(uri: Uri): ContactMatch?
}

/** [ContactsRepository] over `ContactsContract`, queried on [io]. */
class DefaultContactsRepository(
    private val resolver: ContentResolver,
    private val io: CoroutineDispatcher,
) : ContactsRepository {
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : this(context.contentResolver, Dispatchers.IO)

    override suspend fun search(
        query: String,
        limit: Int,
    ): List<ContactMatch> {
        val text = query.trim()
        if (text.isEmpty()) return emptyList()
        return withContext(io) {
            val uri = Uri.withAppendedPath(Phone.CONTENT_FILTER_URI, Uri.encode(text))
            val rows = readPhones(uri) ?: return@withContext emptyList()
            withWhatsApp(arrange(rows)).take(limit)
        }
    }

    override suspend fun fromPickerResult(uri: Uri): ContactMatch? =
        withContext(io) {
            val row = readPhones(uri)?.firstOrNull() ?: return@withContext null
            withWhatsApp(listOf(row)).single()
        }

    /** Phone rows of [uri], or null when the address book cannot be read (permission revoked meanwhile). */
    private fun readPhones(uri: Uri): List<ContactMatch>? =
        try {
            resolver.query(uri, PROJECTION, null, null, null)?.use { cursor -> cursor.rows().toList() } ?: emptyList()
        } catch (e: SecurityException) {
            null
        }

    private fun Cursor.rows(): Sequence<ContactMatch> =
        sequence {
            val id = getColumnIndexOrThrow(Phone.CONTACT_ID)
            val name = getColumnIndexOrThrow(Phone.DISPLAY_NAME_PRIMARY)
            val number = getColumnIndexOrThrow(Phone.NUMBER)
            val type = getColumnIndexOrThrow(Phone.TYPE)
            val photo = getColumnIndexOrThrow(Phone.PHOTO_THUMBNAIL_URI)
            while (moveToNext()) {
                val phone = getString(number)?.takeIf { it.isNotBlank() } ?: continue
                yield(
                    ContactMatch(
                        contactId = getLong(id),
                        name = getString(name).orEmpty(),
                        phone = phone,
                        international = IndonesianPhone.international(phone),
                        mobile = getInt(type) == Phone.TYPE_MOBILE,
                        photoUri = getString(photo),
                        hasWhatsApp = false,
                    ),
                )
            }
        }

    /**
     * Keeps the provider's ranking of contacts, lists each contact's mobile numbers first, and drops a number the
     * contact has twice (the same contact synced from two accounts).
     */
    private fun arrange(rows: List<ContactMatch>): List<ContactMatch> =
        rows
            .groupBy { it.contactId }
            .values
            .flatMap { numbers -> numbers.sortedByDescending { it.mobile } }
            .distinctBy { it.contactId to (it.international ?: it.phone.filter(Char::isDigit)) }

    /** Marks the numbers WhatsApp has a profile for (its rows hold `628…@s.whatsapp.net`). */
    private fun withWhatsApp(matches: List<ContactMatch>): List<ContactMatch> {
        if (matches.none { it.international != null }) return matches
        val ids = matches.map { it.contactId }.distinct()
        val onWhatsApp =
            try {
                resolver
                    .query(
                        ContactsContract.Data.CONTENT_URI,
                        arrayOf(ContactsContract.Data.DATA1),
                        "${ContactsContract.Data.MIMETYPE} = ? AND ${ContactsContract.Data.CONTACT_ID} IN (${ids.joinToString { "?" }})",
                        arrayOf(WHATSAPP_PROFILE) + ids.map { it.toString() },
                        null,
                    )?.use { cursor ->
                        buildSet { while (cursor.moveToNext()) cursor.getString(0)?.substringBefore('@')?.let(::add) }
                    }.orEmpty()
            } catch (e: SecurityException) {
                emptySet()
            }
        return matches.map { it.copy(hasWhatsApp = it.international != null && it.international in onWhatsApp) }
    }

    private companion object {
        val PROJECTION =
            arrayOf(Phone.CONTACT_ID, Phone.DISPLAY_NAME_PRIMARY, Phone.NUMBER, Phone.TYPE, Phone.PHOTO_THUMBNAIL_URI)

        /** The data row WhatsApp adds to a contact for each number that has an account. */
        const val WHATSAPP_PROFILE = "vnd.android.cursor.item/vnd.com.whatsapp.profile"
    }
}
