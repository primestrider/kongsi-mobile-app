package com.primestrider.kotlinstarter.feature.example.sdk.camera

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kotlinstarter.core.designsystem.theme.AppTheme
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class QrResultHostTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun nothing_shows_until_a_code_is_accepted() {
        val gate = QrResultGate()
        rule.setContent { AppTheme(false) { QrResultHost(gate) } }
        rule.onNodeWithText("QR code scanned").assertDoesNotExist()
    }

    @Test
    fun a_scanned_code_opens_the_sheet_with_its_value() {
        val gate = QrResultGate()
        rule.setContent { AppTheme(false) { QrResultHost(gate) } }
        rule.runOnUiThread { gate.offer("https://example.com/pay?id=7") }
        rule.waitForIdle()
        rule.onNodeWithText("QR code scanned").assertExists()
        rule.onNodeWithText("https://example.com/pay?id=7").assertExists()
        rule.onNodeWithText("Close the sheet to scan again.").assertExists()
    }

    @Test
    fun the_same_code_seen_again_keeps_one_sheet_and_close_lets_it_scan_again() {
        val gate = QrResultGate()
        rule.setContent { AppTheme(false) { QrResultHost(gate) } }
        rule.runOnUiThread {
            gate.offer("A")
            gate.offer("A")
            gate.offer("B")
        }
        rule.waitForIdle()
        rule.onNodeWithText("A").assertExists()
        rule.onNodeWithText("B").assertDoesNotExist()

        rule.onNodeWithText("Close").performClick()
        rule.waitForIdle()
        assertNull(gate.current)
        rule.onNodeWithText("QR code scanned").assertDoesNotExist()
        rule.runOnUiThread { assertTrue(gate.offer("A")) }
        rule.waitForIdle()
        rule.onNodeWithText("QR code scanned").assertExists()
    }
}
