package com.primestrider.kotlinstarter.feature.example

import kotlinx.serialization.Serializable

// Routes of the catalog feature. They keep the `feature.example` package: the serial name (the package-qualified
// class name) is what the saved back stack stores, so moving modules must not change it.

/** Root of the Components tab. */
@Serializable
data object ComponentsHomeRoute

@Serializable
data object ActionsRoute

@Serializable
data object InputsRoute

@Serializable
data object DisplayRoute

@Serializable
data object OverlaysRoute

@Serializable
data object FeedbackRoute

@Serializable
data object PickersRoute

@Serializable
data object AppBarsRoute

/** A full-screen component demo opened from the App bars page; [id] is a `ComponentDemo.id`. */
@Serializable
data class ComponentDemoRoute(
    val id: String,
)

@Serializable
data object CarouselRoute

@Serializable
data object FormExampleRoute

@Serializable
data object ColorsRoute

@Serializable
data object TypographyRoute

@Serializable
data object SpacingRoute

@Serializable
data object SizingRoute

@Serializable
data object LayoutRoute

@Serializable
data object AppearanceRoute

@Serializable
data object ThemeRoute
