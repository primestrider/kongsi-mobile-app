package com.primestrider.kongsi.feature.bill.contacts

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Phone
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class ContactsRepositoryTest {
    private val io = StandardTestDispatcher()
    private lateinit var repository: ContactsRepository

    @Before
    fun setUp() {
        FakeContactsProvider.queries.clear()
        Robolectric.setupContentProvider(FakeContactsProvider::class.java, ContactsContract.AUTHORITY)
        repository = DefaultContactsRepository(ApplicationProvider.getApplicationContext<android.content.Context>().contentResolver, io)
    }

    @Test
    fun search_finds_people_by_name_with_one_row_per_number_and_mobile_first() =
        runTest(io) {
            val matches = repository.search("budi")

            assertEquals(
                listOf(
                    ContactMatch(
                        1,
                        "Budi Santoso",
                        "0812-3456-7890",
                        "6281234567890",
                        mobile = true,
                        photoUri = "content://photo/1",
                        hasWhatsApp = true,
                    ),
                    ContactMatch(
                        1,
                        "Budi Santoso",
                        "021 555 1234",
                        null,
                        mobile = false,
                        photoUri = "content://photo/1",
                        hasWhatsApp = false,
                    ),
                ),
                matches,
            )
            // The typed text goes to the provider's filter, which matches names and numbers.
            assertEquals("budi", FakeContactsProvider.queries.first().lastPathSegment)
        }

    @Test
    fun search_by_number_finds_the_contact() =
        runTest(io) {
            val matches = repository.search("0857")

            assertEquals(listOf("Ani Lestari"), matches.map { it.name })
            assertEquals("6285712345678", matches.single().international)
            assertEquals(false, matches.single().hasWhatsApp)
        }

    @Test
    fun the_same_number_twice_on_one_contact_is_one_row() =
        runTest(io) {
            val matches = repository.search("citra")

            assertEquals(listOf("+62 813-1111-2222"), matches.map { it.phone })
        }

    @Test
    fun a_contact_without_a_number_is_not_offered() =
        runTest(io) {
            assertTrue(repository.search("dimas").isEmpty())
        }

    @Test
    fun a_number_from_abroad_is_kept_without_an_international_form() =
        runTest(io) {
            val match = repository.search("eka").single()

            assertEquals("+1 415 555 0100", match.phone)
            assertNull(match.international)
        }

    @Test
    fun blank_search_does_not_query_the_address_book() =
        runTest(io) {
            assertTrue(repository.search("  ").isEmpty())
            assertTrue(FakeContactsProvider.queries.isEmpty())
        }

    @Test
    fun picker_result_is_read_as_one_contact_number() =
        runTest(io) {
            val match = repository.fromPickerResult(Uri.withAppendedPath(Phone.CONTENT_URI, "102"))

            assertEquals(
                ContactMatch(2, "Ani Lestari", "0857 1234 5678", "6285712345678", mobile = true, photoUri = null, hasWhatsApp = false),
                match,
            )
        }

    @Test
    fun picker_result_that_no_longer_exists_is_null() =
        runTest(io) {
            assertNull(repository.fromPickerResult(Uri.withAppendedPath(Phone.CONTENT_URI, "999")))
        }
}

/** An address book for tests: phone rows filtered by name or digits, and WhatsApp profile rows. */
class FakeContactsProvider : ContentProvider() {
    private data class Row(
        val dataId: Long,
        val contactId: Long,
        val name: String,
        val number: String,
        val type: Int,
        val photo: String?,
    )

    private val phones =
        listOf(
            // The home number comes first in the provider; mobile numbers must still be listed first.
            Row(100, 1, "Budi Santoso", "021 555 1234", Phone.TYPE_HOME, "content://photo/1"),
            Row(101, 1, "Budi Santoso", "0812-3456-7890", Phone.TYPE_MOBILE, "content://photo/1"),
            Row(102, 2, "Ani Lestari", "0857 1234 5678", Phone.TYPE_MOBILE, null),
            Row(103, 3, "Citra Dewi", "+62 813-1111-2222", Phone.TYPE_MOBILE, null),
            // Synced twice (phone and SIM account): the same number again.
            Row(104, 3, "Citra Dewi", "081311112222", Phone.TYPE_MOBILE, null),
            Row(105, 5, "Eka Putri", "+1 415 555 0100", Phone.TYPE_MOBILE, null),
            // Dimas (contact 4) has no number, so he has no phone row at all.
        )

    /** Numbers that have a WhatsApp profile row, as WhatsApp writes them. */
    private val whatsApp = listOf("6281234567890@s.whatsapp.net")

    override fun onCreate() = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor {
        queries += uri
        if (uri.toString().startsWith(ContactsContract.Data.CONTENT_URI.toString()) &&
            !uri.toString().startsWith(Phone.CONTENT_URI.toString())
        ) {
            return MatrixCursor(arrayOf(ContactsContract.Data.DATA1)).apply { whatsApp.forEach { addRow(arrayOf(it)) } }
        }
        val rows =
            when {
                uri.toString().startsWith(Phone.CONTENT_FILTER_URI.toString()) -> {
                    val query = uri.lastPathSegment.orEmpty().lowercase()
                    val digits = query.filter(Char::isDigit)
                    phones.filter { row ->
                        row.name
                            .lowercase()
                            .split(" ")
                            .any { it.startsWith(query) } ||
                            (digits.isNotEmpty() && row.number.filter(Char::isDigit).contains(digits))
                    }
                }
                else -> phones.filter { it.dataId.toString() == uri.lastPathSegment }
            }
        val columns = projection ?: emptyArray()
        return MatrixCursor(columns).apply {
            rows.forEach { row ->
                addRow(
                    columns
                        .map<String, Any?> { column ->
                            when (column) {
                                Phone.CONTACT_ID -> row.contactId
                                Phone.DISPLAY_NAME_PRIMARY -> row.name
                                Phone.NUMBER -> row.number
                                Phone.TYPE -> row.type
                                Phone.PHOTO_THUMBNAIL_URI -> row.photo
                                else -> null
                            }
                        }.toTypedArray(),
                )
            }
        }
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
        val queries = mutableListOf<Uri>()
    }
}
