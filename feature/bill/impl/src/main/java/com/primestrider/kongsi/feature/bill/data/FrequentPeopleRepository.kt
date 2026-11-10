package com.primestrider.kongsi.feature.bill.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import com.primestrider.kongsi.core.util.IndonesianPhone
import com.primestrider.kongsi.feature.bill.model.Person
import javax.inject.Inject

/**
 * Someone the user split a bill with before, suggested first next time (also without the contacts permission).
 * [phone] is the `628…` form; without one, the name identifies the person.
 */
@Entity(tableName = "frequent_people")
data class FrequentPersonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val name: String,
    val phone: String?,
    val lastUsedAt: Long,
    val useCount: Int,
)

@Dao
interface FrequentPersonDao {
    /** Most used first, then most recent. */
    @Query("SELECT * FROM frequent_people ORDER BY useCount DESC, lastUsedAt DESC, id")
    suspend fun all(): List<FrequentPersonEntity>

    @Upsert
    suspend fun upsert(person: FrequentPersonEntity)
}

/** A remembered person, as offered on the People step. */
data class FrequentPerson(
    val id: Long,
    val name: String,
    val phone: String?,
    val useCount: Int,
)

interface FrequentPeopleRepository {
    /**
     * Remembered people whose name has a word starting with [query], or whose number contains its digits (typed as
     * `0857…` or `62857…`); everyone for a blank query. Most used first, then most recent.
     */
    suspend fun suggestions(
        query: String,
        limit: Int = 8,
    ): List<FrequentPerson>

    /** Counts one more bill with each of [people] (by number, or by name when there is none). */
    suspend fun remember(people: List<Person>)
}

/** [FrequentPeopleRepository] on Room; [clock] stamps `lastUsedAt` and is replaceable in tests. */
class DefaultFrequentPeopleRepository(
    private val dao: FrequentPersonDao,
    private val clock: () -> Long,
) : FrequentPeopleRepository {
    @Inject
    constructor(dao: FrequentPersonDao) : this(dao, System::currentTimeMillis)

    override suspend fun suggestions(
        query: String,
        limit: Int,
    ): List<FrequentPerson> {
        val text = query.trim().lowercase()
        val digits = text.filter(Char::isDigit)
        return dao
            .all()
            .filter { row ->
                text.isEmpty() ||
                    row.name
                        .lowercase()
                        .split(' ')
                        .any { it.startsWith(text) } ||
                    (digits.isNotEmpty() && row.phone != null && (row.phone.contains(digits) || "0${row.phone.drop(2)}".contains(digits)))
            }.take(limit)
            .map { FrequentPerson(it.id, it.name, it.phone, it.useCount) }
    }

    override suspend fun remember(people: List<Person>) {
        val stored = dao.all()
        val now = clock()
        people.filter { it.name.isNotBlank() }.forEach { person ->
            val phone = person.phone?.let(IndonesianPhone::international)
            val name = person.name.trim()
            val existing =
                stored.firstOrNull { row ->
                    if (phone != null) row.phone == phone else row.phone == null && row.name.equals(name, ignoreCase = true)
                }
            dao.upsert(
                FrequentPersonEntity(
                    id = existing?.id ?: 0,
                    name = name,
                    phone = phone,
                    lastUsedAt = now,
                    useCount = (existing?.useCount ?: 0) + 1,
                ),
            )
        }
    }
}
