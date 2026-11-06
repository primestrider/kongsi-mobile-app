package com.primestrider.kotlinstarter.feature.example.sdk.contacts

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

// Prefixed: SdkDemoRoute's `id` and `amount` arguments land in this same SavedStateHandle.
private const val QUERY_KEY = "contacts_query"
private const val SEARCH_DEBOUNCE_MILLIS = 250L

data class ContactsUiState(
    val query: String = "",
    /** Null until the address book has been counted. */
    val count: Int? = null,
    val rows: List<ContactRow> = emptyList(),
    /** False until the first search has answered, so "No contacts found." does not flash on open. */
    val loaded: Boolean = false,
)

/** Address book search: the text is kept across process death and each pause in typing runs one search. */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class ContactsViewModel(
    source: ContactsSource,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    @Inject
    constructor(
        @ApplicationContext context: Context,
        savedStateHandle: SavedStateHandle,
    ) : this(ContactsRepository(context.contentResolver), savedStateHandle)

    private val query: StateFlow<String> = savedStateHandle.getStateFlow(QUERY_KEY, "")

    // The first (restored or empty) query runs at once; typing waits for a pause.
    private val initialQuery = query.value.trim()

    private val rows: Flow<List<ContactRow>?> =
        query
            .map { it.trim() }
            .debounce { if (it == initialQuery) 0L else SEARCH_DEBOUNCE_MILLIS }
            .distinctUntilChanged()
            .mapLatest<String, List<ContactRow>?> { source.search(it) }
            .onStart { emit(null) }

    private val count: Flow<Int?> = flow<Int?> { emit(source.count()) }.onStart { emit(null) }

    val state: StateFlow<ContactsUiState> =
        combine(query, count, rows) { text, total, found ->
            ContactsUiState(text, total, found.orEmpty(), loaded = found != null)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, ContactsUiState(query = query.value))

    fun onQueryChange(value: String) {
        savedStateHandle[QUERY_KEY] = value
    }
}
