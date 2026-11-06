package com.primestrider.kotlinstarter.feature.example.sdk.blob

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.primestrider.kotlinstarter.core.designsystem.component.AppButton
import com.primestrider.kotlinstarter.core.designsystem.component.AppTextField
import com.primestrider.kotlinstarter.core.designsystem.component.ButtonVariant
import com.primestrider.kotlinstarter.core.designsystem.component.InfoCard
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.Spacing
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews
import com.primestrider.kotlinstarter.feature.devices.impl.R
import com.primestrider.kotlinstarter.feature.example.ExamplePageScaffold
import com.primestrider.kotlinstarter.feature.example.sdk.SdkSection

private const val INITIAL_CONTENT = """{"trx":"TRX-001","amount":50000,"status":"PAID"}"""
private const val JSON_TYPE = "application/json"
private const val TEXT_TYPE = "text/plain"
private const val SLICE_END = 8

/** What the result card shows; the combined payload is rebuilt from the base on demand, never stored or saved. */
private enum class Detail { NONE, TEXT, BYTES, COMBINED }

private val payloadSaver =
    Saver<BinaryPayload?, List<Any>>(
        save = { it?.let { payload -> listOf(payload.bytes, payload.type) } },
        restore = { BinaryPayload(it[0] as ByteArray, it[1] as String) },
    )

/** Build a [BinaryPayload] from text, then read it back as text, slice its first bytes or combine it with itself. */
@Composable
fun BlobScreen(onBack: () -> Unit) {
    var content by rememberSaveable { mutableStateOf(INITIAL_CONTENT) }
    var base by rememberSaveable(stateSaver = payloadSaver) { mutableStateOf<BinaryPayload?>(null) }
    var detail by rememberSaveable { mutableStateOf(Detail.NONE) }

    ExamplePageScaffold(title = R.string.sdk_blob_title, subtitle = R.string.sdk_blob_subtitle, onBack = onBack) {
        SdkSection(R.string.sdk_blob_section_title, R.string.sdk_blob_section_description) {
            AppTextField(value = content, onValueChange = { content = it }, label = stringResource(R.string.sdk_blob_content))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                AppButton(
                    stringResource(R.string.sdk_blob_create),
                    onClick = {
                        base = BinaryPayload.of(content, JSON_TYPE)
                        detail = Detail.NONE
                    },
                )
                AppButton(
                    stringResource(R.string.sdk_blob_text),
                    onClick = { detail = Detail.TEXT },
                    variant = ButtonVariant.OUTLINED,
                    enabled = base != null,
                )
                AppButton(
                    stringResource(R.string.sdk_blob_slice),
                    onClick = {
                        detail = Detail.BYTES
                    },
                    variant = ButtonVariant.OUTLINED,
                    enabled = base != null,
                )
                AppButton(
                    stringResource(R.string.sdk_blob_combine),
                    onClick = {
                        detail = Detail.COMBINED
                    },
                    variant = ButtonVariant.OUTLINED,
                    enabled = base != null,
                )
            }
            base?.let { created ->
                // Slice and Combine always work on the created payload, so repeating them never grows it.
                val shown = if (detail == Detail.COMBINED) combined(created) else created
                val rows =
                    buildList {
                        add(stringResource(R.string.sdk_blob_size) to stringResource(R.string.sdk_blob_size_value, shown.size))
                        add(stringResource(R.string.sdk_blob_type) to shown.type)
                        when (detail) {
                            Detail.TEXT, Detail.COMBINED -> add(stringResource(R.string.sdk_blob_result_text) to shown.text())
                            Detail.BYTES -> add(stringResource(R.string.sdk_blob_result_bytes) to created.slice(0, SLICE_END).hex())
                            Detail.NONE -> Unit
                        }
                    }
                InfoCard(rows)
            }
        }
    }
}

/** `[payload, "\n", payload]` as plain text: twice the payload plus the newline. */
internal fun combined(payload: BinaryPayload): BinaryPayload = BinaryPayload.combine(listOf(payload, "\n", payload), TEXT_TYPE)

@ThemePreviews
@Composable
private fun BlobScreenPreview() {
    PreviewTheme {
        BlobScreen(onBack = {})
    }
}
