package com.primestrider.kotlinstarter.feature.example.sdk.location

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.compose.LifecycleStartEffect

/**
 * Runs [subscribe] while [watch] is on and the screen's lifecycle is at least STARTED, i.e. in the foreground
 * only. The function [subscribe] returns unsubscribes; it runs when the app goes to the background, [watch]
 * turns off, [key] changes (then [subscribe] runs again) or the screen leaves composition.
 */
@Composable
fun ForegroundWatchEffect(
    watch: Boolean,
    key: Any?,
    subscribe: () -> () -> Unit,
) {
    val latest by rememberUpdatedState(subscribe)
    LifecycleStartEffect(watch, key) {
        val unsubscribe = if (watch) latest() else null
        onStopOrDispose { unsubscribe?.invoke() }
    }
}
