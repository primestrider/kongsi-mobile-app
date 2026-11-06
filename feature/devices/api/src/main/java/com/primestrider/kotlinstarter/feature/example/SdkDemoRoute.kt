package com.primestrider.kotlinstarter.feature.example

import kotlinx.serialization.Serializable

/** Every "Device APIs" demo shares this one destination; [id] picks the demo from `sdkDemos`. */
@Serializable
data class SdkDemoRoute(
    val id: String,
    /** Set only by the Payment Code deep link (`.../sdk/payment-code?amount=50000`). */
    val amount: Long? = null,
)
