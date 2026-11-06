package com.primestrider.kotlinstarter.feature.example.components

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kotlinstarter.core.designsystem.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class AppBarsScreenTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun each_demo_button_opens_its_id() {
        val opened = mutableListOf<String>()
        rule.setContent { AppTheme(false) { AppBarsScreen(onBack = {}, onOpenDemo = { opened += it }) } }
        componentDemos.forEach { demo -> rule.onNodeWithTag("open-${demo.id}").performScrollTo().performClick() }
        assertEquals(componentDemos.map { it.id }, opened)
    }
}
