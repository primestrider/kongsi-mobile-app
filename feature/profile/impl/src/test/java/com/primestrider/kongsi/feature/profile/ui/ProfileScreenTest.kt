package com.primestrider.kongsi.feature.profile.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kongsi.core.datastore.UserProfile
import com.primestrider.kongsi.core.designsystem.theme.AppTheme
import com.primestrider.kongsi.core.i18n.AppLanguage
import com.primestrider.kongsi.core.model.ThemeMode
import com.primestrider.kongsi.core.permission.PermissionStatus
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class ProfileScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private val events = mutableListOf<String>()

    private fun showProfile(profile: UserProfile = UserProfile()) =
        rule.setContent {
            AppTheme(false) {
                ProfileScreen(
                    state = ProfileUiState(profile = profile, themeMode = ThemeMode.DARK, language = AppLanguage.EN),
                    versionName = "1.2.3",
                    actions =
                        ProfileActions(
                            onEditProfile = { events += "edit" },
                            onOpenPaymentAccounts = { events += "accounts" },
                            onOpenPermissions = { events += "permissions" },
                            onOpenAppearance = { events += "appearance" },
                            onOpenAbout = { events += "about" },
                        ),
                )
            }
        }

    @Test
    fun empty_profile_invites_to_add_a_name_and_opens_the_editor() {
        showProfile()
        rule.onNodeWithText("Add your name").assertIsDisplayed()
        rule.onNodeWithText("No phone number yet").performClick()
        assertEquals(listOf("edit"), events)
    }

    @Test
    fun each_row_opens_its_page_and_shows_a_summary() {
        showProfile(UserProfile("Ricky", "081234567890"))
        rule.onNodeWithText("Ricky").assertIsDisplayed()
        rule.onNodeWithText("Dark · English").assertIsDisplayed()
        rule.onNodeWithText("Version 1.2.3").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Bank accounts & e-wallets").performClick()
        rule.onNodeWithText("Permissions").performClick()
        rule.onNodeWithText("Appearance").performClick()
        rule.onNodeWithText("About").performClick()
        assertEquals(listOf("accounts", "permissions", "appearance", "about"), events)
    }

    private fun showPermissions(contacts: PermissionStatus) =
        rule.setContent {
            AppTheme(false) {
                PermissionsScreen(
                    permissions =
                        listOf(
                            PermissionRow(AppPermission.CONTACTS, contacts),
                            // Never the same status as contacts, so each label below is on one row only.
                            PermissionRow(
                                AppPermission.CAMERA,
                                if (contacts == PermissionStatus.GRANTED) PermissionStatus.ASKABLE else PermissionStatus.GRANTED,
                            ),
                        ),
                    onAction = { permission, status -> events += "$permission $status" },
                    onBack = {},
                )
            }
        }

    @Test
    fun contacts_permission_can_be_asked_for() {
        showPermissions(PermissionStatus.ASKABLE)
        rule.onNodeWithText("Not allowed yet").assertIsDisplayed()
        rule.onNodeWithText("Camera").assertIsDisplayed()
        rule.onNodeWithText("Allow").performClick()
        assertEquals(listOf("CONTACTS ASKABLE"), events)
    }

    @Test
    fun camera_permission_can_be_asked_for() {
        showPermissions(PermissionStatus.GRANTED)
        rule.onNodeWithText("Allow").performClick()
        assertEquals(listOf("CAMERA ASKABLE"), events)
    }

    @Test
    fun blocked_permission_points_to_the_system_settings() {
        showPermissions(PermissionStatus.BLOCKED)
        rule.onNodeWithText("Open settings").performClick()
        assertEquals(listOf("CONTACTS BLOCKED"), events)
    }

    @Test
    fun granted_permission_is_managed_in_the_system_settings() {
        showPermissions(PermissionStatus.GRANTED)
        rule.onNodeWithText("Allowed").assertIsDisplayed()
        rule.onNodeWithText("Manage").performClick()
        assertEquals(listOf("CONTACTS GRANTED"), events)
    }

    @Test
    fun appearance_changes_theme_and_language() {
        rule.setContent {
            AppTheme(false) {
                AppearanceScreen(
                    themeMode = ThemeMode.SYSTEM,
                    language = AppLanguage.EN,
                    onThemeModeChange = { events += "theme $it" },
                    onLanguageChange = { events += "language $it" },
                    onBack = {},
                )
            }
        }
        rule.onNodeWithText("Dark").performClick()
        rule.onNodeWithText("Bahasa Indonesia").performClick()
        assertEquals(listOf("theme DARK", "language ID"), events)
    }

    @Test
    fun about_shows_the_version() {
        rule.setContent { AppTheme(false) { AboutScreen(versionName = "1.2.3", onBack = {}) } }
        rule.onNodeWithText("1.2.3").assertIsDisplayed()
    }
}
