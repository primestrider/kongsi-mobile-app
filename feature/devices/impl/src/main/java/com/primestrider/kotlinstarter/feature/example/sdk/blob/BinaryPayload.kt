package com.primestrider.kotlinstarter.feature.example.sdk.blob

import java.io.ByteArrayOutputStream

/** An immutable byte payload with a MIME [type]: the Android counterpart of a JavaScript `Blob`. */
class BinaryPayload(
    val bytes: ByteArray,
    val type: String,
) {
    val size: Int get() = bytes.size

    /** The bytes decoded as UTF-8. */
    fun text(): String = bytes.toString(Charsets.UTF_8)

    /** Bytes [start, end), both clamped to the payload; the slice keeps the type. */
    fun slice(
        start: Int,
        end: Int,
    ): BinaryPayload {
        val from = start.coerceIn(0, size)
        val to = end.coerceIn(from, size)
        return BinaryPayload(bytes.copyOfRange(from, to), type)
    }

    /** Two-digit lowercase hex per byte, separated by spaces. */
    fun hex(): String = bytes.joinToString(" ") { "%02x".format(it.toInt() and 0xFF) }

    companion object {
        fun of(
            text: String,
            type: String,
        ) = BinaryPayload(text.toByteArray(Charsets.UTF_8), type)

        /** Concatenates [parts], each a [BinaryPayload] or a [String] (UTF-8), into one payload of [type]. */
        fun combine(
            parts: List<Any>,
            type: String,
        ): BinaryPayload {
            val out = ByteArrayOutputStream()
            parts.forEach { part ->
                when (part) {
                    is BinaryPayload -> out.write(part.bytes)
                    is String -> out.write(part.toByteArray(Charsets.UTF_8))
                    else -> throw IllegalArgumentException("Unsupported part: ${part::class.simpleName}")
                }
            }
            return BinaryPayload(out.toByteArray(), type)
        }
    }
}
