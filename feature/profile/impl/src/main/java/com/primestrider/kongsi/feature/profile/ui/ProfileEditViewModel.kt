package com.primestrider.kongsi.feature.profile.ui

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.primestrider.kongsi.core.datastore.UserProfile
import com.primestrider.kongsi.core.datastore.UserProfileRepository
import com.primestrider.kongsi.core.designsystem.component.Messenger
import com.primestrider.kongsi.core.designsystem.component.UiText
import com.primestrider.kongsi.core.util.IndonesianPhone
import com.primestrider.kongsi.feature.profile.impl.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

const val PROFILE_NAME_MAX = 50

private const val NAME_KEY = "name"
private const val PHONE_KEY = "phone"
private const val SUBMITTED_KEY = "submitted"

data class ProfileEditUiState(
    /** False until the stored profile has been read into the form. */
    val loaded: Boolean = false,
    val name: String = "",
    val phone: String = "",
    val submitted: Boolean = false,
    val saving: Boolean = false,
    val closed: Boolean = false,
) {
    @get:StringRes
    val nameError: Int?
        get() = R.string.profile_error_name_long.takeIf { name.trim().length > PROFILE_NAME_MAX }

    /** The phone is optional, but when given it must be an Indonesian mobile number. */
    @get:StringRes
    val phoneError: Int?
        get() = R.string.profile_error_phone.takeIf { phone.isNotBlank() && IndonesianPhone.local(phone) == null }

    val shownNameError: Int? get() = nameError.takeIf { submitted }
    val shownPhoneError: Int? get() = phoneError.takeIf { submitted }
}

/** Edits the user's name and mobile number; the draft lives in [SavedStateHandle] until saved. */
@HiltViewModel
class ProfileEditViewModel
    @Inject
    constructor(
        private val repository: UserProfileRepository,
        private val savedStateHandle: SavedStateHandle,
        private val messenger: Messenger,
    ) : ViewModel() {
        private val _state = MutableStateFlow(ProfileEditUiState())
        val state: StateFlow<ProfileEditUiState> = _state.asStateFlow()

        init {
            val draftName = savedStateHandle.get<String>(NAME_KEY)
            if (draftName != null) {
                _state.value =
                    ProfileEditUiState(
                        loaded = true,
                        name = draftName,
                        phone = savedStateHandle.get<String>(PHONE_KEY).orEmpty(),
                        submitted = savedStateHandle.get<Boolean>(SUBMITTED_KEY) ?: false,
                    )
            } else {
                viewModelScope.launch {
                    val stored = repository.profile.first()
                    update { it.copy(loaded = true, name = stored.name.orEmpty(), phone = stored.phone.orEmpty()) }
                }
            }
        }

        fun onNameChange(value: String) = update { it.copy(name = value) }

        fun onPhoneChange(value: String) = update { it.copy(phone = value) }

        fun save() {
            val current = _state.value
            if (current.saving || !current.loaded) return
            update { it.copy(submitted = true) }
            if (current.nameError != null || current.phoneError != null) return
            update { it.copy(saving = true) }
            viewModelScope.launch {
                try {
                    repository.save(UserProfile(name = current.name, phone = IndonesianPhone.local(current.phone)))
                    messenger.success(UiText.Resource(R.string.profile_saved))
                    update { it.copy(saving = false, closed = true) }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: IOException) {
                    update { it.copy(saving = false) }
                    messenger.error(UiText.Resource(R.string.profile_save_failed))
                }
            }
        }

        private fun update(change: (ProfileEditUiState) -> ProfileEditUiState) {
            _state.update(change)
            val state = _state.value
            if (state.loaded) {
                savedStateHandle[NAME_KEY] = state.name
                savedStateHandle[PHONE_KEY] = state.phone
                savedStateHandle[SUBMITTED_KEY] = state.submitted
            }
        }
    }
