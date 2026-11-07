package com.primestrider.kongsi.feature.profile

import kotlinx.serialization.Serializable

// Routes of the profile feature. The Profile page itself is a tab of the app shell, not a route; each of its rows
// opens one of these pages.

/** Edits the user's own name and mobile number. */
@Serializable
data object ProfileEditRoute

/** The runtime permissions the app uses, with their status. */
@Serializable
data object PermissionsRoute

/** Theme and language. */
@Serializable
data object AppearanceRoute

/** App version and info. */
@Serializable
data object AboutRoute
