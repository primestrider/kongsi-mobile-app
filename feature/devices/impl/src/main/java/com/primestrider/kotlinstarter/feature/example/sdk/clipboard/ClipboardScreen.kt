package com.primestrider.kotlinstarter.feature.example.sdk.clipboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import com.primestrider.kotlinstarter.core.designsystem.component.AppButton
import com.primestrider.kotlinstarter.core.designsystem.component.AppTextField
import com.primestrider.kotlinstarter.core.designsystem.component.ButtonVariant
import com.primestrider.kotlinstarter.core.designsystem.component.InfoCard
import com.primestrider.kotlinstarter.core.designsystem.component.SegmentedChoice
import com.primestrider.kotlinstarter.core.designsystem.component.SelectField
import com.primestrider.kotlinstarter.core.designsystem.component.SelectOption
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews
import com.primestrider.kotlinstarter.feature.devices.impl.R
import com.primestrider.kotlinstarter.feature.example.ExamplePageScaffold
import com.primestrider.kotlinstarter.feature.example.sdk.SdkSection

private const val REFERRAL_CODE = "SUPER-7K2Q9"

private enum class Impact(
    val feedback: Int,
    val label: Int,
) {
    LIGHT(HapticFeedbackConstants.VIRTUAL_KEY, R.string.sdk_clipboard_impact_light),
    MEDIUM(HapticFeedbackConstants.KEYBOARD_TAP, R.string.sdk_clipboard_impact_medium),
    HEAVY(HapticFeedbackConstants.LONG_PRESS, R.string.sdk_clipboard_impact_heavy),
    SOFT(HapticFeedbackConstants.CONTEXT_CLICK, R.string.sdk_clipboard_impact_soft),
    RIGID(HapticFeedbackConstants.CLOCK_TICK, R.string.sdk_clipboard_impact_rigid),
}

private enum class Outcome(
    val label: Int,
) {
    SUCCESS(R.string.sdk_clipboard_outcome_success),
    WARNING(R.string.sdk_clipboard_outcome_warning),
    ERROR(R.string.sdk_clipboard_outcome_error),
}

/** Copy and paste with the system clipboard, plus the haptic effects the view can perform. */
@Composable
fun ClipboardScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    val clipboard = remember(context) { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }
    var field by rememberSaveable { mutableStateOf("") }
    var lastPasted by rememberSaveable { mutableStateOf<String?>(null) }
    var changes by remember { mutableIntStateOf(0) }
    var impact by rememberSaveable { mutableStateOf(Impact.MEDIUM) }
    var outcome by rememberSaveable { mutableStateOf(Outcome.SUCCESS) }

    val outcomeLabels = Outcome.entries.associateWith { stringResource(it.label) }

    DisposableEffect(clipboard) {
        val listener = ClipboardManager.OnPrimaryClipChangedListener { changes++ }
        clipboard.addPrimaryClipChangedListener(listener)
        onDispose { clipboard.removePrimaryClipChangedListener(listener) }
    }

    ExamplePageScaffold(
        title = R.string.sdk_clipboard_title,
        subtitle = R.string.sdk_clipboard_subtitle,
        onBack = onBack,
    ) {
        SdkSection(R.string.sdk_clipboard_copy_title, R.string.sdk_clipboard_copy_description) {
            Text(REFERRAL_CODE, style = MaterialTheme.typography.titleLarge)
            AppButton(
                stringResource(R.string.sdk_clipboard_copy),
                onClick = {
                    clipboard.setPrimaryClip(ClipData.newPlainText("referral", REFERRAL_CODE))
                    view.hapticSuccess()
                },
            )
        }
        SdkSection(R.string.sdk_clipboard_paste_title, R.string.sdk_clipboard_paste_description) {
            AppTextField(value = field, onValueChange = { field = it }, label = stringResource(R.string.sdk_clipboard_field))
            AppButton(
                stringResource(R.string.sdk_clipboard_paste),
                onClick = {
                    val text = clipboard.readText(context)
                    if (text == null) {
                        view.hapticWarning()
                    } else {
                        field = text
                        lastPasted = text
                        view.hapticSelection()
                    }
                },
                variant = ButtonVariant.OUTLINED,
            )
            Text(
                stringResource(R.string.sdk_clipboard_changes, changes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            lastPasted?.let { InfoCard(listOf(stringResource(R.string.sdk_clipboard_last_pasted) to it)) }
        }
        SdkSection(R.string.sdk_clipboard_haptics_title, R.string.sdk_clipboard_haptics_description) {
            SelectField(
                label = stringResource(R.string.sdk_clipboard_impact_label),
                options = Impact.entries.map { SelectOption(it, stringResource(it.label)) },
                selected = impact,
                onSelect = {
                    impact = it
                    view.hapticSelection()
                },
            )
            AppButton(stringResource(R.string.sdk_clipboard_impact), onClick = { view.performHapticFeedback(impact.feedback) })
            SegmentedChoice(
                options = Outcome.entries,
                selected = outcome,
                onSelect = {
                    outcome = it
                    view.hapticSelection()
                },
                label = { outcomeLabels.getValue(it) },
            )
            AppButton(
                stringResource(R.string.sdk_clipboard_notify),
                onClick = {
                    when (outcome) {
                        Outcome.SUCCESS -> view.hapticSuccess()
                        Outcome.WARNING -> view.hapticWarning()
                        Outcome.ERROR -> view.hapticError()
                    }
                },
            )
        }
    }
}

private fun ClipboardManager.readText(context: Context): String? =
    primaryClip
        ?.takeIf { it.itemCount > 0 }
        ?.getItemAt(0)
        ?.coerceToText(context)
        ?.toString()
        ?.takeIf { it.isNotEmpty() }

private fun View.hapticSelection() {
    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
}

private fun View.hapticSuccess() {
    performHapticFeedback(
        if (Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.R
        ) {
            HapticFeedbackConstants.CONFIRM
        } else {
            HapticFeedbackConstants.KEYBOARD_TAP
        },
    )
}

private fun View.hapticWarning() {
    performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
}

private fun View.hapticError() {
    performHapticFeedback(
        if (Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.R
        ) {
            HapticFeedbackConstants.REJECT
        } else {
            HapticFeedbackConstants.LONG_PRESS
        },
    )
}

@ThemePreviews
@Composable
private fun ClipboardScreenPreview() {
    PreviewTheme {
        ExamplePageScaffold(title = R.string.sdk_clipboard_title, subtitle = R.string.sdk_clipboard_subtitle, onBack = {}) {
            SdkSection(R.string.sdk_clipboard_copy_title, R.string.sdk_clipboard_copy_description) {
                Text(REFERRAL_CODE, style = MaterialTheme.typography.titleLarge)
                AppButton(stringResource(R.string.sdk_clipboard_copy), onClick = {})
            }
            SdkSection(R.string.sdk_clipboard_paste_title, R.string.sdk_clipboard_paste_description) {
                AppTextField(value = "", onValueChange = {}, label = stringResource(R.string.sdk_clipboard_field))
                AppButton(stringResource(R.string.sdk_clipboard_paste), onClick = {})
            }
        }
    }
}
