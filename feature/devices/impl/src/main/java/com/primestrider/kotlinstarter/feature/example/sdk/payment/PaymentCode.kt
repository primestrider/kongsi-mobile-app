package com.primestrider.kotlinstarter.feature.example.sdk.payment

import java.util.Locale
import kotlin.random.Random

private const val GROUP_COUNT = 3
private const val GROUP_RANGE = 10_000

/** A payment code: three groups of four random digits, space separated, leading zeros kept ("0042 7310 0005"). */
fun paymentCode(random: Random): String =
    List(GROUP_COUNT) { String.format(Locale.ROOT, "%04d", random.nextInt(GROUP_RANGE)) }.joinToString(" ")
