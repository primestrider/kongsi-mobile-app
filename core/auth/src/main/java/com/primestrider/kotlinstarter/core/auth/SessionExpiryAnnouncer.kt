package com.primestrider.kotlinstarter.core.auth

import com.primestrider.kotlinstarter.core.auth.R
import com.primestrider.kotlinstarter.core.common.ApplicationScope
import com.primestrider.kotlinstarter.core.designsystem.component.Messenger
import com.primestrider.kotlinstarter.core.designsystem.component.UiText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Shows one "session expired" message per [SignOutReason.EXPIRED] sign-out. Call [start] once. */
@Singleton
class SessionExpiryAnnouncer
    @Inject
    constructor(
        private val repository: SessionRepository,
        private val messenger: Messenger,
        @ApplicationScope private val scope: CoroutineScope,
    ) {
        fun start() {
            scope.launch {
                repository.signOutReason.collect { reason ->
                    if (reason == SignOutReason.EXPIRED) {
                        messenger.error(UiText.Resource(R.string.core_auth_session_expired))
                        repository.clearSignOutReason()
                    }
                }
            }
        }
    }
