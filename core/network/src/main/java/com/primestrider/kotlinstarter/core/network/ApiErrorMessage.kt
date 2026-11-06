package com.primestrider.kotlinstarter.core.network

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.primestrider.kotlinstarter.core.network.R

/** What to tell the user: a connection hint for network errors, else the server's message or a generic one. */
@Composable
fun ApiError.userMessage(): String =
    if (isNetworkError) {
        stringResource(R.string.common_error_network)
    } else {
        message ?: stringResource(R.string.common_error_generic)
    }
