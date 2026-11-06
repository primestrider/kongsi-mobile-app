package com.primestrider.kotlinstarter.feature.example.sdk.notifications

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

// Prefixed: SdkDemoRoute's `id` and `amount` arguments land in this same SavedStateHandle.
private const val TITLE_KEY = "notifications_title"
private const val DELAY_KEY = "notifications_delay"
private const val EVENTS_KEY = "notifications_events"
private const val LAST_TAPPED_KEY = "notifications_last_tapped"
private const val MAX_EVENTS = 5
const val DEFAULT_NOTIFICATION_TITLE = "Payment received"
const val DEFAULT_NOTIFICATION_DELAY = 5

/** Adds [line] in front and keeps the newest [MAX_EVENTS]; returns a snapshot of the list. */
fun MutableList<String>.pushEvent(line: String): List<String> {
    add(0, line)
    while (size > MAX_EVENTS) removeAt(lastIndex)
    return toList()
}

data class NotificationsUiState(
    val title: String = DEFAULT_NOTIFICATION_TITLE,
    val delaySeconds: Int = DEFAULT_NOTIFICATION_DELAY,
    val events: List<String> = emptyList(),
)

/** The title, the delay and the event log live in [SavedStateHandle] (rotation, process death). */
@HiltViewModel
class NotificationsViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val clock: () -> Long,
) : ViewModel() {
    @Inject
    constructor(savedStateHandle: SavedStateHandle) : this(savedStateHandle, System::currentTimeMillis)

    init {
        // Tapping a notification makes navigation rebuild this screen with a new ViewModel: carry the log over.
        if (savedStateHandle.get<ArrayList<String>>(EVENTS_KEY) == null && NotificationEvents.history.isNotEmpty()) {
            savedStateHandle[EVENTS_KEY] = ArrayList(NotificationEvents.history)
        }
    }

    val state: StateFlow<NotificationsUiState> =
        combine(
            savedStateHandle.getStateFlow(TITLE_KEY, DEFAULT_NOTIFICATION_TITLE),
            savedStateHandle.getStateFlow(DELAY_KEY, DEFAULT_NOTIFICATION_DELAY),
            savedStateHandle.getStateFlow(EVENTS_KEY, ArrayList<String>()),
            ::NotificationsUiState,
        ).stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            NotificationsUiState(
                title = savedStateHandle[TITLE_KEY] ?: DEFAULT_NOTIFICATION_TITLE,
                delaySeconds = savedStateHandle[DELAY_KEY] ?: DEFAULT_NOTIFICATION_DELAY,
                events = savedStateHandle.get<ArrayList<String>>(EVENTS_KEY).orEmpty(),
            ),
        )

    fun setTitle(title: String) {
        savedStateHandle[TITLE_KEY] = title
    }

    fun setDelay(seconds: Int) {
        savedStateHandle[DELAY_KEY] = seconds
    }

    /** Adds "{time} · {line}" (time in [locale]) to the log, newest first, five at most. */
    fun log(
        line: String,
        locale: Locale = Locale.getDefault(),
    ) {
        val time = DateFormat.getTimeInstance(DateFormat.MEDIUM, locale).format(Date(clock()))
        val events = ArrayList(savedStateHandle.get<ArrayList<String>>(EVENTS_KEY).orEmpty())
        events.pushEvent("$time · $line")
        savedStateHandle[EVENTS_KEY] = events
        NotificationEvents.history.apply {
            clear()
            addAll(events)
        }
    }

    /** The user tapped notification [id]; the same tap is reported again after a rotation, so it is logged once. */
    fun onTapped(
        id: Int,
        label: String,
        locale: Locale = Locale.getDefault(),
    ) {
        if (savedStateHandle.get<Int>(LAST_TAPPED_KEY) == id) return
        savedStateHandle[LAST_TAPPED_KEY] = id
        log(label, locale)
    }
}
