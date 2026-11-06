package com.primestrider.kotlinstarter.feature.example.sdk.sharetarget

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.primestrider.kotlinstarter.core.designsystem.theme.AppTheme

/**
 * The share target: other apps' share sheets list "Kotlin Starter" for text and images (see the intent filters in
 * this feature's manifest). It is its own small activity, so sharing opens just this page on top of the sending app
 * instead of the whole app; back returns to the sender.
 */
class ShareReceiverActivity : ComponentActivity() {
    private var content by mutableStateOf(SharedContent())

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        content = SharedContent.from(intent)
        setContent {
            AppTheme(darkTheme = isSystemInDarkTheme()) {
                SharedContentScreen(content = content, onBack = ::finish)
            }
        }
    }

    /** A second share while this page is on top (singleTop) replaces what is shown. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        content = SharedContent.from(intent)
    }
}
