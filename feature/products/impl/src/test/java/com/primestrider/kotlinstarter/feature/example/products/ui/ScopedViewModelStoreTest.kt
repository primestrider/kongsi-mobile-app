package com.primestrider.kotlinstarter.feature.example.products.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** One product's ViewModels live only while that product is in the pane. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class ScopedViewModelStoreTest {
    @get:Rule
    val rule = createComposeRule()

    class Probe : ViewModel() {
        var cleared = false

        override fun onCleared() {
            cleared = true
        }
    }

    @Test
    fun changing_the_key_clears_the_previous_view_models() {
        var id by mutableLongStateOf(1L)
        val seen = mutableListOf<Probe>()
        rule.setContent {
            ScopedViewModelStore(key = id) {
                val probe: Probe = viewModel()
                if (seen.lastOrNull() !== probe) seen += probe
            }
        }
        rule.runOnIdle { id = 2L }
        rule.waitForIdle()
        assertEquals(2, seen.size)
        assertTrue(seen[0].cleared)
        assertFalse(seen[1].cleared)
    }
}
