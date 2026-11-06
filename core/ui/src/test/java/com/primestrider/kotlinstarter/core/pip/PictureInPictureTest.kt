package com.primestrider.kotlinstarter.core.pip

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class PictureInPictureTest {
    @Test
    fun set_then_clear_leaves_no_content() {
        val host = PictureInPictureHost()
        host.set { Text("video") }
        host.clear()
        assertNull(host.content.value)
    }

    @Test
    fun a_second_set_replaces_the_first() {
        val host = PictureInPictureHost()
        val first: @Composable () -> Unit = { Text("first") }
        val second: @Composable () -> Unit = { Text("second") }
        host.set(first)
        host.set(second)
        assertSame(second, host.content.value)
    }
}
