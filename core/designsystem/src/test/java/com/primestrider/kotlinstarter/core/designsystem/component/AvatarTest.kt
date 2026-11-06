package com.primestrider.kotlinstarter.core.designsystem.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kotlinstarter.core.designsystem.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class AvatarTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun shows_initials() {
        rule.setContent { AppTheme(false) { Avatar("Ada Lovelace") } }
        rule.onNodeWithText("AL", useUnmergedTree = true).assertExists()
    }

    @Test
    fun null_image_url_still_shows_initials() {
        rule.setContent { AppTheme(false) { Avatar("Ada Lovelace", imageUrl = null) } }
        rule.onNodeWithText("AL", useUnmergedTree = true).assertExists()
    }

    @Test
    fun unreachable_image_url_keeps_initials() {
        rule.setContent { AppTheme(false) { Avatar("Ada Lovelace", imageUrl = "http://127.0.0.1:1/missing.png") } }
        rule.waitForIdle()
        rule.onNodeWithText("AL", useUnmergedTree = true).assertExists()
    }

    @Test
    fun status_is_announced() {
        rule.setContent { AppTheme(false) { Avatar("Ada Lovelace", status = AvatarStatus.ONLINE) } }
        rule.onNode(hasContentDescription("Online", substring = true)).assertExists()
    }

    @Test
    fun group_shows_max_and_overflow() {
        val names = listOf("Ada Lovelace", "Grace Hopper", "Alan Turing", "Margaret Hamilton", "Linus Torvalds", "Barbara Liskov")
        rule.setContent { AppTheme(false) { AvatarGroup(names, max = 4) } }
        rule.onNodeWithText("+2", useUnmergedTree = true).assertExists()
        listOf("AL", "GH", "AT", "MH").forEach { rule.onNodeWithText(it, useUnmergedTree = true).assertExists() }
        rule.onAllNodesWithText("LT", useUnmergedTree = true).assertCountEquals(0)
        rule.onNode(hasContentDescription("2 more")).assertExists()
    }

    @Test
    fun group_width_accounts_for_overlap() {
        val names = listOf("Ada Lovelace", "Grace Hopper", "Alan Turing")
        rule.setContent { AppTheme(false) { AvatarGroup(names, size = AvatarSize.MEDIUM, modifier = Modifier.testTag("g")) } }
        // Three 40dp avatars overlapping by 10dp: 40 + 30 + 30 = 100dp, not 120dp.
        rule.onNodeWithTag("g").assertWidthIsEqualTo(100.dp)
    }
}
