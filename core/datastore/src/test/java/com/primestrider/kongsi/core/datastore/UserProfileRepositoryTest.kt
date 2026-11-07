package com.primestrider.kongsi.core.datastore

import com.primestrider.kongsi.testing.InMemoryPreferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class UserProfileRepositoryTest {
    @Test
    fun empty_until_saved() =
        runTest {
            assertEquals(UserProfile(), UserProfileRepository(InMemoryPreferencesDataStore()).profile.first())
        }

    @Test
    fun saves_trimmed_values_and_blank_clears_them() =
        runTest {
            val repository = UserProfileRepository(InMemoryPreferencesDataStore())
            repository.save(UserProfile(name = "  Ricky ", phone = "081234567890"))
            assertEquals(UserProfile("Ricky", "081234567890"), repository.profile.first())
            repository.save(UserProfile(name = "Ricky", phone = "   "))
            assertEquals(UserProfile("Ricky", null), repository.profile.first())
        }
}
