package com.primestrider.kotlinstarter.feature.example.components

import androidx.lifecycle.ViewModel
import com.primestrider.kotlinstarter.core.designsystem.component.MessageVariant
import com.primestrider.kotlinstarter.core.designsystem.component.Messenger
import com.primestrider.kotlinstarter.core.designsystem.component.UiMessage
import com.primestrider.kotlinstarter.core.designsystem.component.UiText
import com.primestrider.kotlinstarter.feature.catalog.impl.R
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.primestrider.kotlinstarter.core.designsystem.R as DesignR

/** Shows that a ViewModel raises app-wide messages by injecting [Messenger]; no Context needed. */
@HiltViewModel
class FeedbackViewModel
    @Inject
    constructor(
        private val messenger: Messenger,
    ) : ViewModel() {
        fun saveSucceeded() = messenger.success(UiText.Resource(R.string.feedback_saved))

        fun saveFailed() =
            messenger.show(
                UiMessage(
                    text = UiText.Resource(R.string.feedback_save_failed),
                    variant = MessageVariant.ERROR,
                    actionLabel = UiText.Resource(DesignR.string.component_retry),
                    onAction = ::saveSucceeded,
                ),
            )
    }
