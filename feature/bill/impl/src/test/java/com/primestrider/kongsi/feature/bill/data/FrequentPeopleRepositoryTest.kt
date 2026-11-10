package com.primestrider.kongsi.feature.bill.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kongsi.feature.bill.model.Person
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class FrequentPeopleRepositoryTest {
    private lateinit var database: KongsiDatabase
    private lateinit var repository: FrequentPeopleRepository
    private var now = 1_000L

    @Before
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), KongsiDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        repository = DefaultFrequentPeopleRepository(database.frequentPersonDao()) { ++now }
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun people_used_more_often_come_first_then_the_most_recent() =
        runTest {
            repository.remember(listOf(Person(0, "Budi", "6281234567890"), Person(0, "Ani")))
            repository.remember(listOf(Person(0, "Budi", "6281234567890")))
            repository.remember(listOf(Person(0, "Citra")))

            assertEquals(listOf("Budi", "Citra", "Ani"), repository.suggestions("").map { it.name })
            assertEquals(2, repository.suggestions("").first().useCount)
        }

    @Test
    fun the_same_number_is_one_person_and_keeps_the_latest_name() =
        runTest {
            repository.remember(listOf(Person(0, "Budi", "6281234567890")))
            repository.remember(listOf(Person(0, "Budi Santoso", "6281234567890")))

            val stored = repository.suggestions("")
            assertEquals(listOf("Budi Santoso"), stored.map { it.name })
            assertEquals(2, stored.single().useCount)
        }

    @Test
    fun without_a_number_the_name_identifies_the_person_ignoring_case() =
        runTest {
            repository.remember(listOf(Person(0, "Ani")))
            repository.remember(listOf(Person(0, "ani ")))

            assertEquals(1, repository.suggestions("").size)
        }

    @Test
    fun suggestions_match_a_name_part_or_digits_of_the_number() =
        runTest {
            repository.remember(listOf(Person(0, "Budi Santoso", "6281234567890"), Person(0, "Ani Lestari", "6285712345678")))

            assertEquals(listOf("Budi Santoso"), repository.suggestions("san").map { it.name })
            assertEquals(listOf("Ani Lestari"), repository.suggestions("0857").map { it.name })
            assertEquals(listOf("Ani Lestari"), repository.suggestions("le").map { it.name })
        }
}
