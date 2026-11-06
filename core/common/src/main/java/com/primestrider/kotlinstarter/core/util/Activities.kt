package com.primestrider.kotlinstarter.core.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/** The hosting [Activity] of this context, unwrapping [ContextWrapper]s, or null when there is none. */
tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
