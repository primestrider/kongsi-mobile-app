package com.primestrider.kongsi.feature.bill.ui

import android.net.Uri
import com.primestrider.kongsi.feature.bill.contacts.ContactMatch
import com.primestrider.kongsi.feature.bill.contacts.ContactsRepository

/** An address book holding [matches]: search finds them by name, the picker returns the first; [searched] records queries. */
class FakeContactsRepository(
    private val matches: List<ContactMatch>,
) : ContactsRepository {
    val searched = mutableListOf<String>()

    override suspend fun search(
        query: String,
        limit: Int,
    ): List<ContactMatch> {
        searched += query
        return matches.filter { it.name.contains(query, ignoreCase = true) }
    }

    override suspend fun fromPickerResult(uri: Uri): ContactMatch? = matches.firstOrNull()
}
