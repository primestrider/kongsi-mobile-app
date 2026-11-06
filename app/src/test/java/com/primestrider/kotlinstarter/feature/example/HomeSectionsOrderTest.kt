package com.primestrider.kotlinstarter.feature.example

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kotlinstarter.feature.example.sdk.sdkDemos
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Home as the wired features build it: section order, and every Device APIs demo in registry order. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class HomeSectionsOrderTest {
    @Test
    fun home_sections_are_in_order() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val titles = exampleSections.map { context.getString(it.title) }
        assertEquals(listOf("Features", "Device APIs", "Animations", "Gestures", "Styling"), titles)
        val sdk = exampleSections[1]
        assertEquals(sdkDemos.map { SdkDemoRoute(it.id) }, sdk.entries.map { it.route })
    }
}
