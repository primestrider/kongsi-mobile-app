package com.primestrider.kongsi.feature.bill.ui

import com.primestrider.kongsi.feature.bill.data.FrequentPeopleRepository
import com.primestrider.kongsi.feature.bill.data.FrequentPerson
import com.primestrider.kongsi.feature.bill.model.Person

/** In-memory [FrequentPeopleRepository]: suggests [stored] by name and records every [remember] call in [remembered]. */
class FakeFrequentPeopleRepository(
    var stored: List<FrequentPerson> = emptyList(),
) : FrequentPeopleRepository {
    val remembered = mutableListOf<List<Person>>()

    override suspend fun suggestions(
        query: String,
        limit: Int,
    ) = stored.filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }

    override suspend fun remember(people: List<Person>) {
        remembered += people
    }
}
