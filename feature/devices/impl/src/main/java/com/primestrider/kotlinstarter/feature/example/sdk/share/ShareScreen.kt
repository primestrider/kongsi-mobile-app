package com.primestrider.kotlinstarter.feature.example.sdk.share

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.print.PrintManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.core.content.FileProvider
import com.primestrider.kotlinstarter.core.designsystem.component.AppButton
import com.primestrider.kotlinstarter.core.designsystem.component.AppTextField
import com.primestrider.kotlinstarter.core.designsystem.component.Banner
import com.primestrider.kotlinstarter.core.designsystem.component.BannerVariant
import com.primestrider.kotlinstarter.core.designsystem.component.ButtonVariant
import com.primestrider.kotlinstarter.core.designsystem.component.InfoCard
import com.primestrider.kotlinstarter.core.designsystem.component.SelectField
import com.primestrider.kotlinstarter.core.designsystem.component.SelectOption
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.Spacing
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews
import com.primestrider.kotlinstarter.core.util.currentLocale
import com.primestrider.kotlinstarter.feature.devices.impl.R
import com.primestrider.kotlinstarter.feature.example.ExamplePageScaffold
import com.primestrider.kotlinstarter.feature.example.sdk.SdkSection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.Locale

private val MERCHANTS = listOf("Kopi Kenangan", "PLN Token", "Telkomsel Pulsa")
private const val INITIAL_AMOUNT = "25000"

/** Renders an e-receipt as a PDF, then shares it through the system sheet or hands it to the print dialog. */
@Composable
fun ShareScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val locale = currentLocale()
    val scope = rememberCoroutineScope()
    var merchant by rememberSaveable { mutableStateOf(MERCHANTS.first()) }
    var amount by rememberSaveable { mutableStateOf(INITIAL_AMOUNT) }
    var fileName by rememberSaveable { mutableStateOf<String?>(null) }
    var pages by rememberSaveable { mutableStateOf<Int?>(null) }
    var failed by rememberSaveable { mutableStateOf(false) }

    val labels =
        ReceiptLabels(
            heading = stringResource(R.string.sdk_share_pdf_heading),
            merchant = stringResource(R.string.sdk_share_merchant),
            reference = stringResource(R.string.sdk_share_pdf_reference),
            total = stringResource(R.string.sdk_share_pdf_total),
        )
    val chooserTitle = stringResource(R.string.sdk_share_chooser)
    val printJob = stringResource(R.string.sdk_share_print_job)

    fun run(action: (Context, File, Int) -> Unit) {
        failed = false
        scope.launch {
            try {
                val (file, pageCount) =
                    withContext(
                        Dispatchers.IO,
                    ) { renderReceipt(context, merchant, parseAmount(amount), locale, labels) }
                action(context, file, pageCount)
            } catch (e: CancellationException) {
                throw e
            } catch (e: ActivityNotFoundException) {
                failed = true
            } catch (e: IOException) {
                failed = true
            } catch (e: IllegalStateException) {
                failed = true
            } catch (e: IllegalArgumentException) {
                failed = true
            }
        }
    }

    ExamplePageScaffold(title = R.string.sdk_share_title, subtitle = R.string.sdk_share_subtitle, onBack = onBack) {
        SdkSection(R.string.sdk_share_receipt_title, R.string.sdk_share_receipt_description) {
            SelectField(
                label = stringResource(R.string.sdk_share_merchant),
                options = MERCHANTS.map { SelectOption(it, it) },
                selected = merchant,
                onSelect = { merchant = it },
            )
            AppTextField(
                value = amount,
                onValueChange = { amount = it },
                label = stringResource(R.string.sdk_share_amount),
                keyboardType = KeyboardType.Number,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                AppButton(
                    stringResource(R.string.sdk_share_share),
                    onClick = {
                        run { ctx, file, pageCount ->
                            fileName = file.name
                            pages = pageCount
                            ctx.startActivity(shareChooser(ctx, file, chooserTitle))
                        }
                    },
                )
                AppButton(
                    stringResource(R.string.sdk_share_print),
                    onClick = {
                        run { ctx, file, _ ->
                            val manager = ctx.getSystemService(Context.PRINT_SERVICE) as PrintManager
                            manager.print(printJob, PdfPrintAdapter(file), null)
                        }
                    },
                    variant = ButtonVariant.OUTLINED,
                )
            }
            if (failed) Banner(stringResource(R.string.sdk_share_error), variant = BannerVariant.ERROR)
            val name = fileName
            if (name != null) {
                InfoCard(
                    listOf(
                        stringResource(R.string.sdk_share_file) to name,
                        stringResource(R.string.sdk_share_pages) to pages?.toString(),
                    ),
                )
            }
        }
    }
}

/** Writes the receipt into `cache/receipts`, replacing earlier ones, and returns the PDF. */
private fun renderReceipt(
    context: Context,
    merchant: String,
    amount: Long,
    locale: Locale,
    labels: ReceiptLabels,
): Pair<File, Int> {
    val now = System.currentTimeMillis()
    val dir = File(context.cacheDir, "receipts")
    dir.listFiles().orEmpty().forEach { it.delete() }
    val receipt = Receipt(merchant, amount, receiptReference(now), now)
    val file = File(dir, "${receipt.reference}.pdf")
    return file to receipt.writePdf(file, locale, labels)
}

private fun shareChooser(
    context: Context,
    file: File,
    title: String,
): Intent {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    val send =
        Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri(file.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    return Intent.createChooser(send, title)
}

@ThemePreviews
@Composable
private fun ShareScreenPreview() {
    PreviewTheme {
        ShareScreen(onBack = {})
    }
}
