package com.primestrider.kotlinstarter.feature.example.sdk.sharetarget

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.primestrider.kotlinstarter.core.designsystem.component.AppButton
import com.primestrider.kotlinstarter.core.designsystem.component.EmptyState
import com.primestrider.kotlinstarter.core.designsystem.component.InfoCard
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.Spacing
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews
import com.primestrider.kotlinstarter.feature.devices.impl.R
import com.primestrider.kotlinstarter.feature.example.ExamplePageScaffold
import com.primestrider.kotlinstarter.feature.example.sdk.SdkSection

/** What [ShareReceiverActivity] received: the text and subject, the images as thumbnails, or an empty state. */
@Composable
fun SharedContentScreen(
    content: SharedContent,
    onBack: () -> Unit,
) {
    ExamplePageScaffold(title = R.string.sdk_share_target_received, subtitle = R.string.sdk_share_target_subtitle, onBack = onBack) {
        if (content.isEmpty) {
            EmptyState(
                title = stringResource(R.string.sdk_share_target_empty_title),
                description = stringResource(R.string.sdk_share_target_empty_description),
                icon = Icons.Filled.Share,
            )
        }
        content.text?.let { text ->
            SdkSection(R.string.sdk_share_target_text, R.string.sdk_share_target_text_description) {
                InfoCard(listOf(stringResource(R.string.sdk_share_target_subject) to content.subject))
                Text(text, style = MaterialTheme.typography.bodyLarge)
            }
        }
        if (content.images.isNotEmpty()) {
            SdkSection(R.string.sdk_share_target_images, R.string.sdk_share_target_images_description) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s2), verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                    content.images.forEach { uri ->
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(96.dp).clip(RoundedCornerShape(12.dp)),
                        )
                    }
                }
            }
        }
    }
}

/** The Device APIs entry: explains the share target and offers a share to try it with. */
@Composable
fun ShareTargetScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    ExamplePageScaffold(title = R.string.sdk_share_target_title, subtitle = R.string.sdk_share_target_subtitle, onBack = onBack) {
        SdkSection(R.string.sdk_share_target_how_title, R.string.sdk_share_target_how_description) {
            AppButton(stringResource(R.string.sdk_share_target_try), onClick = { shareSample(context) })
        }
    }
}

private fun shareSample(context: Context) {
    val send =
        Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.sdk_share_target_sample_subject))
            .putExtra(Intent.EXTRA_TEXT, context.getString(R.string.sdk_share_target_sample_text))
    context.startActivity(Intent.createChooser(send, null))
}

@ThemePreviews
@Composable
private fun SharedContentScreenPreview() {
    PreviewTheme {
        SharedContentScreen(SharedContent(text = "https://developer.android.com", subject = "Android docs"), onBack = {})
    }
}

@ThemePreviews
@Composable
private fun ShareTargetScreenPreview() {
    PreviewTheme { ShareTargetScreen(onBack = {}) }
}
