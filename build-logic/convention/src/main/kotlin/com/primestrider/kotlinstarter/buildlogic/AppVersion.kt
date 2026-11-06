package com.primestrider.kotlinstarter.buildlogic

/** Which part of the version a release raises. */
enum class Bump {
    NONE,
    PATCH,
    MINOR,
    MAJOR,
    ;

    companion object {
        fun of(name: String): Bump =
            entries.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown bump \"$name\"; use one of ${entries.joinToString { it.name.lowercase() }}")
    }
}

/**
 * The app version, `major.minor.patch`, kept once in `gradle.properties` (`version=`). [code] is the Play
 * `versionCode`, `major * 10000 + minor * 100 + patch`, so minor and patch stay below 100 and major below 2100
 * (Play's limit is 2100000000); a version outside that range is rejected instead of producing a colliding code.
 */
data class AppVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
) {
    init {
        require(major in 0 until MAJOR_LIMIT && minor in 0 until PART_LIMIT && patch in 0 until PART_LIMIT) {
            "Version $major.$minor.$patch is out of range: minor and patch must be 0..99, major 0..2099"
        }
    }

    val code: Int get() = major * 10_000 + minor * 100 + patch

    fun bump(part: Bump): AppVersion =
        when (part) {
            Bump.NONE -> this
            Bump.PATCH -> AppVersion(major, minor, patch + 1)
            Bump.MINOR -> AppVersion(major, minor + 1, 0)
            Bump.MAJOR -> AppVersion(major + 1, 0, 0)
        }

    override fun toString(): String = "$major.$minor.$patch"

    companion object {
        private const val PART_LIMIT = 100
        private const val MAJOR_LIMIT = 2100
        private val FORMAT = Regex("""(\d+)\.(\d+)\.(\d+)""")

        fun parse(text: String): AppVersion {
            val match =
                FORMAT.matchEntire(text.trim())
                    ?: throw IllegalArgumentException("Version \"$text\" is not major.minor.patch (e.g. version=1.0.0 in gradle.properties)")
            val (major, minor, patch) = match.destructured
            return AppVersion(major.toInt(), minor.toInt(), patch.toInt())
        }
    }
}
