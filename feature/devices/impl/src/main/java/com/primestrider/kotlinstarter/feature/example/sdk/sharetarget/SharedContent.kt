package com.primestrider.kotlinstarter.feature.example.sdk.sharetarget

import android.content.Intent
import android.net.Uri
import androidx.core.content.IntentCompat

private const val MAX_IMAGES = 10

/**
 * What another app shared with us. Everything in a share intent comes from outside the app, so this reads only the
 * extras the filters accept (text, one image, several images), trims the text and caps the image count.
 */
data class SharedContent(
    val text: String? = null,
    val subject: String? = null,
    val images: List<Uri> = emptyList(),
) {
    val isEmpty: Boolean get() = text == null && images.isEmpty()

    companion object {
        fun from(intent: Intent): SharedContent {
            val type = intent.type.orEmpty()
            return when {
                intent.action == Intent.ACTION_SEND && type.startsWith("text/") ->
                    SharedContent(
                        text = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim()?.takeIf { it.isNotEmpty() },
                        subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)?.trim()?.takeIf { it.isNotEmpty() },
                    )
                intent.action == Intent.ACTION_SEND && type.startsWith("image/") ->
                    SharedContent(images = listOfNotNull(IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)))
                intent.action == Intent.ACTION_SEND_MULTIPLE && type.startsWith("image/") ->
                    SharedContent(
                        images =
                            IntentCompat
                                .getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                                .orEmpty()
                                .take(MAX_IMAGES),
                    )
                else -> SharedContent()
            }
        }
    }
}
