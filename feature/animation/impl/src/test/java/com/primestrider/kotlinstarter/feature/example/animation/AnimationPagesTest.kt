package com.primestrider.kotlinstarter.feature.example.animation

import org.junit.Assert.assertEquals
import org.junit.Test

class AnimationPagesTest {
    @Test
    fun entries_map_one_to_one_to_pages() {
        val entries = animationEntries()
        assertEquals(animationPages.map { it.route }, entries.map { it.route })
        assertEquals(animationPages.map { it.title }, entries.map { it.title })
        assertEquals(animationPages.map { it.subtitle }, entries.map { it.subtitle })
    }

    @Test
    fun routes_are_unique() {
        assertEquals(animationPages.size, animationPages.map { it.route }.toSet().size)
    }
}
