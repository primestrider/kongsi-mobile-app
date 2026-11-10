package com.primestrider.kongsi.feature.bill.share

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.net.toUri
import com.primestrider.kongsi.core.util.IndonesianPhone

/**
 * Sends a bill message through WhatsApp with plain Android intents, so no SDK or key is needed. WhatsApp is preferred,
 * then WhatsApp Business; without either, the system share sheet opens. Seeing those apps needs the `<queries>` entries
 * in this module's manifest.
 */
object WhatsAppSharer {
    private val WHATSAPP_PACKAGES = listOf("com.whatsapp", "com.whatsapp.w4b")

    /**
     * The intent for [message]: with an Indonesian mobile [phone], that person's chat (`wa.me`) with the message typed
     * in; without one, WhatsApp's own chat and group picker; without WhatsApp, the share sheet.
     */
    fun intentFor(
        context: Context,
        message: String,
        phone: String?,
    ): Intent {
        val whatsApp = WHATSAPP_PACKAGES.firstOrNull { context.packageManager.isInstalled(it) } ?: return chooser(message)
        val number = phone?.let(IndonesianPhone::international)
        return if (number != null) {
            Intent(Intent.ACTION_VIEW, "https://wa.me/$number?text=${Uri.encode(message)}".toUri()).setPackage(whatsApp)
        } else {
            sendText(message).setPackage(whatsApp)
        }
    }

    /** Opens [intentFor]; should WhatsApp refuse it after all, the share sheet opens instead. */
    fun share(
        context: Context,
        message: String,
        phone: String?,
    ) {
        try {
            context.startActivity(intentFor(context, message, phone).forContext(context))
        } catch (e: ActivityNotFoundException) {
            context.startActivity(chooser(message).forContext(context))
        }
    }

    private fun sendText(message: String) = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, message)

    private fun chooser(message: String): Intent = Intent.createChooser(sendText(message), null)

    /** Started from outside an activity, an activity needs a task of its own. */
    private fun Intent.forContext(context: Context): Intent = if (context is Activity) this else addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    private fun PackageManager.isInstalled(packageName: String): Boolean =
        try {
            getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
}
