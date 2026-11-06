package com.primestrider.kotlinstarter.architecture

// remove-examples: exercises the example features, so scripts/remove-examples.sh deletes it with them.

import com.primestrider.kotlinstarter.feature.example.AccountRoute
import com.primestrider.kotlinstarter.feature.example.ActionsRoute
import com.primestrider.kotlinstarter.feature.example.AnimCardsRoute
import com.primestrider.kotlinstarter.feature.example.AnimListsRoute
import com.primestrider.kotlinstarter.feature.example.AnimMicroRoute
import com.primestrider.kotlinstarter.feature.example.AnimTransitionsRoute
import com.primestrider.kotlinstarter.feature.example.AppBarsRoute
import com.primestrider.kotlinstarter.feature.example.AppearanceRoute
import com.primestrider.kotlinstarter.feature.example.CarouselRoute
import com.primestrider.kotlinstarter.feature.example.ColorsRoute
import com.primestrider.kotlinstarter.feature.example.ComponentDemoRoute
import com.primestrider.kotlinstarter.feature.example.ComponentsHomeRoute
import com.primestrider.kotlinstarter.feature.example.DisplayRoute
import com.primestrider.kotlinstarter.feature.example.FeedbackRoute
import com.primestrider.kotlinstarter.feature.example.FormExampleRoute
import com.primestrider.kotlinstarter.feature.example.GestureDragRoute
import com.primestrider.kotlinstarter.feature.example.GesturePointerRoute
import com.primestrider.kotlinstarter.feature.example.GestureScrollRoute
import com.primestrider.kotlinstarter.feature.example.GestureSwipeRoute
import com.primestrider.kotlinstarter.feature.example.GestureTapRoute
import com.primestrider.kotlinstarter.feature.example.GestureTransformRoute
import com.primestrider.kotlinstarter.feature.example.InputsRoute
import com.primestrider.kotlinstarter.feature.example.LayoutRoute
import com.primestrider.kotlinstarter.feature.example.OverlaysRoute
import com.primestrider.kotlinstarter.feature.example.PickersRoute
import com.primestrider.kotlinstarter.feature.example.ProductDetailRoute
import com.primestrider.kotlinstarter.feature.example.ProductsRoute
import com.primestrider.kotlinstarter.feature.example.ProfileRoute
import com.primestrider.kotlinstarter.feature.example.SdkDemoRoute
import com.primestrider.kotlinstarter.feature.example.SettingsRoute
import com.primestrider.kotlinstarter.feature.example.SignInRoute
import com.primestrider.kotlinstarter.feature.example.SizingRoute
import com.primestrider.kotlinstarter.feature.example.SpacingRoute
import com.primestrider.kotlinstarter.feature.example.ThemeRoute
import com.primestrider.kotlinstarter.feature.example.TodosRoute
import com.primestrider.kotlinstarter.feature.example.TypographyRoute
import kotlinx.serialization.serializer
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Type-safe navigation stores routes by serial name in the saved back stack and deep-link patterns. Moving a
 * route to another module must not change it, or a restored back stack no longer decodes.
 */
class RouteNamesTest {
    @Test
    fun restored_back_stack_survives_route_move() {
        val names =
            listOf(
                serializer<SettingsRoute>(),
                serializer<ColorsRoute>(),
                serializer<TypographyRoute>(),
                serializer<SpacingRoute>(),
                serializer<SizingRoute>(),
                serializer<LayoutRoute>(),
                serializer<AppearanceRoute>(),
                serializer<ThemeRoute>(),
                serializer<ActionsRoute>(),
                serializer<InputsRoute>(),
                serializer<DisplayRoute>(),
                serializer<OverlaysRoute>(),
                serializer<FormExampleRoute>(),
                serializer<ComponentsHomeRoute>(),
                serializer<FeedbackRoute>(),
                serializer<SignInRoute>(),
                serializer<ProfileRoute>(),
                serializer<AccountRoute>(),
                serializer<ProductsRoute>(),
                serializer<ProductDetailRoute>(),
                serializer<TodosRoute>(),
                serializer<SdkDemoRoute>(),
                serializer<AnimMicroRoute>(),
                serializer<AnimCardsRoute>(),
                serializer<AnimListsRoute>(),
                serializer<AnimTransitionsRoute>(),
                serializer<GestureTapRoute>(),
                serializer<GestureDragRoute>(),
                serializer<GestureTransformRoute>(),
                serializer<GestureSwipeRoute>(),
                serializer<GestureScrollRoute>(),
                serializer<GesturePointerRoute>(),
                serializer<CarouselRoute>(),
                serializer<AppBarsRoute>(),
                serializer<ComponentDemoRoute>(),
                serializer<PickersRoute>(),
            ).map { it.descriptor.serialName.substringAfterLast('.') to it.descriptor.serialName }
        names.forEach { (simple, serialName) ->
            assertEquals("com.primestrider.kotlinstarter.feature.example.$simple", serialName)
        }
        assertEquals(36, names.size)
    }
}
