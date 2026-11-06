package com.primestrider.kotlinstarter.feature.example.sdk.files

import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.primestrider.kotlinstarter.core.designsystem.component.AppButton
import com.primestrider.kotlinstarter.core.designsystem.component.Banner
import com.primestrider.kotlinstarter.core.designsystem.component.BannerVariant
import com.primestrider.kotlinstarter.core.designsystem.component.ButtonVariant
import com.primestrider.kotlinstarter.core.designsystem.component.InfoCard
import com.primestrider.kotlinstarter.core.designsystem.component.LabeledSlider
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.Spacing
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews
import com.primestrider.kotlinstarter.core.util.currentLocale
import com.primestrider.kotlinstarter.feature.devices.impl.R
import com.primestrider.kotlinstarter.feature.example.ExamplePageScaffold
import com.primestrider.kotlinstarter.feature.example.sdk.SdkSection
import java.util.Locale
import kotlin.math.roundToInt

private const val MIN_WIDTH = 480f
private const val MAX_WIDTH = 1920f
private const val WIDTH_STEP = 120
private const val MIN_QUALITY = 0.1f
private const val QUALITY_STEPS = 8

/** Shrink a photo before upload, and manage files downloaded into the cache directory. */
@Composable
fun FilesScreen(
    onBack: () -> Unit,
    viewModel: FilesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let(viewModel::pick) }
    FilesContent(
        state = state,
        onMaxWidth = viewModel::setMaxWidth,
        onQuality = viewModel::setQuality,
        onPick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        onCompress = viewModel::compress,
        onDownload = viewModel::download,
        onClearDownloads = viewModel::clearDownloads,
        onBack = onBack,
    )
}

@Composable
internal fun FilesContent(
    state: FilesUiState,
    onMaxWidth: (Int) -> Unit,
    onQuality: (Float) -> Unit,
    onPick: () -> Unit,
    onCompress: () -> Unit,
    onDownload: () -> Unit,
    onClearDownloads: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val locale = currentLocale()
    ExamplePageScaffold(title = R.string.sdk_files_title, subtitle = R.string.sdk_files_subtitle, onBack = onBack) {
        SdkSection(R.string.sdk_files_compress_title, R.string.sdk_files_compress_description) {
            LabeledSlider(
                label = stringResource(R.string.sdk_files_max_width),
                value = state.maxWidth.toFloat(),
                onValueChange = { onMaxWidth(snapWidth(it)) },
                valueRange = MIN_WIDTH..MAX_WIDTH,
                steps = ((MAX_WIDTH - MIN_WIDTH) / WIDTH_STEP).roundToInt() - 1,
                valueText = "${state.maxWidth} px",
            )
            LabeledSlider(
                label = stringResource(R.string.sdk_files_quality),
                value = state.quality,
                onValueChange = onQuality,
                valueRange = MIN_QUALITY..1f,
                steps = QUALITY_STEPS,
                valueText = quality(state.quality, locale),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                AppButton(stringResource(R.string.sdk_files_pick), onClick = onPick, variant = ButtonVariant.OUTLINED)
                AppButton(
                    stringResource(R.string.sdk_files_compress),
                    onClick = onCompress,
                    enabled = state.picked != null,
                    loading = state.compressing,
                )
            }
            Text(
                stringResource(R.string.sdk_files_caption, state.maxWidth, quality(state.quality, locale)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (state.compressFailed) {
                Banner(stringResource(R.string.sdk_files_compress_failed), variant = BannerVariant.ERROR)
            }
            state.result?.let { result ->
                AsyncImage(
                    model = result.file,
                    contentDescription = stringResource(R.string.sdk_files_preview),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                )
                InfoCard(
                    listOf(
                        stringResource(R.string.sdk_files_original) to
                            stringResource(R.string.sdk_files_dimensions, result.originalWidth, result.originalHeight),
                        stringResource(R.string.sdk_files_original_size) to Formatter.formatShortFileSize(context, result.originalBytes),
                        stringResource(R.string.sdk_files_compressed) to
                            stringResource(R.string.sdk_files_dimensions, result.width, result.height),
                        stringResource(R.string.sdk_files_compressed_size) to Formatter.formatShortFileSize(context, result.bytes),
                        stringResource(R.string.sdk_files_saving) to
                            stringResource(R.string.sdk_files_saving_value, savingPercent(result.originalBytes, result.bytes)),
                    ),
                )
            }
        }
        SdkSection(R.string.sdk_files_downloads_title, R.string.sdk_files_downloads_description) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                AppButton(stringResource(R.string.sdk_files_download), onClick = onDownload, loading = state.downloading)
                AppButton(stringResource(R.string.sdk_files_clear), onClick = onClearDownloads, variant = ButtonVariant.TEXT)
            }
            if (state.downloadFailed) {
                Banner(stringResource(R.string.sdk_files_download_failed), variant = BannerVariant.ERROR)
            }
            if (state.downloads.isEmpty()) {
                Text(
                    stringResource(R.string.sdk_files_no_files),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                InfoCard(state.downloads.map { it.name to Formatter.formatShortFileSize(context, it.length()) })
            }
        }
    }
}

private fun snapWidth(value: Float): Int = (value / WIDTH_STEP).roundToInt() * WIDTH_STEP

private fun quality(
    value: Float,
    locale: Locale,
): String = String.format(locale, "%.1f", value)

@ThemePreviews
@Composable
private fun FilesScreenPreview() {
    PreviewTheme {
        FilesContent(
            state = FilesUiState(),
            onMaxWidth = {},
            onQuality = {},
            onPick = {},
            onCompress = {},
            onDownload = {},
            onClearDownloads = {},
            onBack = {},
        )
    }
}
