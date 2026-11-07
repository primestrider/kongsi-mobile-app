package com.primestrider.kongsi.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** A background and the colour of the initials on it, for one person's avatar. */
@Immutable
data class PersonTone(
    val container: Color,
    val content: Color,
)

/**
 * Eight clearly different hues for the people on a bill, so their chips read at a glance on every screen. They are
 * pale with dark initials in light theme and deep with pale initials in dark theme; colour is never the only cue,
 * avatars always carry initials too. The butter accent and the status colours are left out on purpose.
 */
private val lightTones =
    listOf(
        PersonTone(Palette.Sky.c100, Palette.Sky.c900),
        PersonTone(Palette.Rose.c100, Palette.Rose.c900),
        PersonTone(Palette.Emerald.c100, Palette.Emerald.c900),
        PersonTone(Palette.Violet.c100, Palette.Violet.c900),
        PersonTone(Palette.Orange.c100, Palette.Orange.c900),
        PersonTone(Palette.Teal.c100, Palette.Teal.c900),
        PersonTone(Palette.Fuchsia.c100, Palette.Fuchsia.c900),
        PersonTone(Palette.Indigo.c100, Palette.Indigo.c900),
    )

private val darkTones =
    listOf(
        PersonTone(Palette.Sky.c800, Palette.Sky.c100),
        PersonTone(Palette.Rose.c800, Palette.Rose.c100),
        PersonTone(Palette.Emerald.c800, Palette.Emerald.c100),
        PersonTone(Palette.Violet.c800, Palette.Violet.c100),
        PersonTone(Palette.Orange.c800, Palette.Orange.c100),
        PersonTone(Palette.Teal.c800, Palette.Teal.c100),
        PersonTone(Palette.Fuchsia.c800, Palette.Fuchsia.c100),
        PersonTone(Palette.Indigo.c800, Palette.Indigo.c100),
    )

/** The tone of the person at [index] (their position on the bill); it repeats after eight people. */
fun personTone(
    index: Int,
    dark: Boolean,
): PersonTone {
    val tones = if (dark) darkTones else lightTones
    return tones[Math.floorMod(index, tones.size)]
}

/** [personTone] for the current theme. */
@Composable
fun personTone(index: Int): PersonTone = personTone(index, dark = MaterialTheme.colorScheme.background.luminance() < 0.5f)
