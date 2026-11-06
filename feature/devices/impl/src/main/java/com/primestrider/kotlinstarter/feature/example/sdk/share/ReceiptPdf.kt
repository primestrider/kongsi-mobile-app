package com.primestrider.kotlinstarter.feature.example.sdk.share

import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import com.primestrider.kotlinstarter.core.util.formatCurrency
import java.io.File
import java.text.DateFormat
import java.util.Date
import java.util.Locale

private const val PAGE_WIDTH = 595
private const val PAGE_HEIGHT = 842
private const val MARGIN = 48f

/** Texts printed on the receipt, so the caller can pass translated ones. */
data class ReceiptLabels(
    val heading: String = "Payment receipt",
    val merchant: String = "Merchant",
    val reference: String = "Reference",
    val total: String = "Total",
)

/** The text of the receipt page, formatted for [locale]. */
internal data class ReceiptContent(
    val heading: String,
    val date: String,
    val rows: List<Pair<String, String>>,
    val totalLabel: String,
    val total: String,
)

internal fun Receipt.content(
    locale: Locale,
    labels: ReceiptLabels,
) = ReceiptContent(
    heading = labels.heading,
    date = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale).format(Date(paidAt)),
    rows = listOf(labels.merchant to merchant, labels.reference to reference),
    totalLabel = labels.total,
    total = formatCurrency(amount.toDouble(), "IDR", locale, fractionDigits = 0),
)

/** Renders this receipt as a one-page A4 PDF into [file] and returns the page count. */
fun Receipt.writePdf(
    file: File,
    locale: Locale,
    labels: ReceiptLabels = ReceiptLabels(),
): Int {
    val content = content(locale, labels)
    val document = PdfDocument()
    try {
        val page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create())
        val canvas = page.canvas
        val heading =
            Paint().apply {
                textSize = 24f
                typeface = Typeface.DEFAULT_BOLD
                isAntiAlias = true
            }
        val body =
            Paint().apply {
                textSize = 14f
                isAntiAlias = true
            }
        val bold = Paint(body).apply { typeface = Typeface.DEFAULT_BOLD }
        val right = PAGE_WIDTH - MARGIN
        var y = 90f

        canvas.drawText(content.heading, MARGIN, y, heading)
        y += 28f
        canvas.drawText(content.date, MARGIN, y, body)
        y += 48f
        content.rows.forEach { (label, value) ->
            canvas.drawText(label, MARGIN, y, body)
            canvas.drawText(value, right - body.measureText(value), y, body)
            y += 28f
        }
        canvas.drawText(content.totalLabel, MARGIN, y, bold)
        canvas.drawText(content.total, right - bold.measureText(content.total), y, bold)

        document.finishPage(page)
        file.parentFile?.mkdirs()
        file.outputStream().use { document.writeTo(it) }
        return document.pages.size
    } finally {
        document.close()
    }
}

/** Hands an already written PDF [file] to the system print dialog. */
class PdfPrintAdapter(
    private val file: File,
) : PrintDocumentAdapter() {
    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes?,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback,
        extras: Bundle?,
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback.onLayoutCancelled()
            return
        }
        val info =
            PrintDocumentInfo
                .Builder(file.name)
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                .build()
        callback.onLayoutFinished(info, true)
    }

    override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback,
    ) {
        try {
            file.inputStream().use { input ->
                ParcelFileDescriptor.AutoCloseOutputStream(destination).use { input.copyTo(it) }
            }
            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: java.io.IOException) {
            callback.onWriteFailed(e.message)
        }
    }
}
