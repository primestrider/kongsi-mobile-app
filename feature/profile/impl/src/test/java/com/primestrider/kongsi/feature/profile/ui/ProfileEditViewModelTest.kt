package com.primestrider.kongsi.feature.profile.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kongsi.core.datastore.UserProfile
import com.primestrider.kongsi.core.datastore.UserProfileRepository
import com.primestrider.kongsi.core.designsystem.component.Messenger
import com.primestrider.kongsi.feature.profile.impl.R
import com.primestrider.kongsi.testing.InMemoryPreferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileEditViewModelTest {
    private val main = StandardTestDispatcher()
    private val messenger = Messenger()
    private val repository = UserProfileRepository(InMemoryPreferencesDataStore())
    private val created = mutableListOf<ProfileEditViewModel>()

    @Before
    fun setUp() = Dispatchers.setMain(main)

    @After
    fun tearDown() {
        created.forEach { it.viewModelScope.cancel() }
        Dispatchers.resetMain()
    }

    private fun viewModel(handle: SavedStateHandle = SavedStateHandle()) =
        ProfileEditViewModel(repository, handle, messenger).also { created += it }

    @Test
    fun loads_the_stored_profile() =
        runTest(main) {
            repository.save(UserProfile("Ricky", "081234567890"))
            val viewModel = viewModel()
            assertFalse(viewModel.state.value.loaded)
            advanceUntilIdle()
            assertEquals("Ricky", viewModel.state.value.name)
            assertEquals("081234567890", viewModel.state.value.phone)
        }

    @Test
    fun saves_name_and_normalised_phone_then_closes() =
        runTest(main) {
            val viewModel = viewModel()
            advanceUntilIdle()
            viewModel.onNameChange(" Ricky Tan ")
            viewModel.onPhoneChange("+62 812-3456-7890")
            viewModel.save()
            advanceUntilIdle()
            assertEquals(UserProfile("Ricky Tan", "081234567890"), repository.profile.first())
            assertTrue(viewModel.state.value.closed)
        }

    @Test
    fun phone_is_optional_but_must_be_valid_when_given() =
        runTest(main) {
            val viewModel = viewModel()
            advanceUntilIdle()
            viewModel.onNameChange("Ricky")
            viewModel.onPhoneChange("12345")
            viewModel.save()
            advanceUntilIdle()
            assertEquals(R.string.profile_error_phone, viewModel.state.value.shownPhoneError)
            assertEquals(UserProfile(), repository.profile.first())

            viewModel.onPhoneChange("")
            assertNull(viewModel.state.value.shownPhoneError)
            viewModel.save()
            advanceUntilIdle()
            assertEquals(UserProfile("Ricky", null), repository.profile.first())
        }

    @Test
    fun draft_survives_process_death() =
        runTest(main) {
            val handle = SavedStateHandle()
            val before = viewModel(handle)
            advanceUntilIdle()
            before.onNameChange("Draft name")
            val after = viewModel(handle)
            assertTrue(after.state.value.loaded)
            assertEquals("Draft name", after.state.value.name)
        }
}
