package com.primestrider.kotlinstarter.feature.example.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.CarouselItemScope
import androidx.compose.material3.carousel.HorizontalCenteredHeroCarousel
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.HorizontalUncontainedCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.Spacing
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews
import com.primestrider.kotlinstarter.feature.catalog.impl.R
import com.primestrider.kotlinstarter.feature.example.DemoSection
import com.primestrider.kotlinstarter.feature.example.ExamplePageScaffold

private const val FILE = "CarouselScreen.kt"

private const val ITEMS = 8

/** Colour blocks stand in for photos, so the page needs no images or network. */
private val swatches =
    listOf(Color(0xFF5B8DEF), Color(0xFFE5866B), Color(0xFF55B48C), Color(0xFFB07CD8), Color(0xFFE0B54A), Color(0xFF4FB3BF))

/** Material 3 carousels plus the lazy grids from the "Lists and grids" guide. */
@Composable
fun CarouselScreen(onBack: () -> Unit) {
    ExamplePageScaffold(R.string.example_components_carousel_title, R.string.example_components_carousel_subtitle, onBack) {
        CarouselsExample()
        GridsExample()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CarouselItemScope.Swatch(
    index: Int,
    @StringRes label: Int,
) {
    Box(
        Modifier
            .fillMaxSize()
            .maskClip(MaterialTheme.shapes.extraLarge)
            .background(swatches[index % swatches.size]),
        contentAlignment = Alignment.BottomStart,
    ) {
        Text(stringResource(label, index + 1), color = Color.White, modifier = Modifier.padding(Spacing.s3))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CarouselsExample() {
    val carouselModifier = Modifier.fillMaxWidth().height(200.dp)
    DemoSection(
        R.string.components_carousel_multi,
        R.string.components_carousel_multi_usage,
        "HorizontalMultiBrowseCarousel(rememberCarouselState())",
        FILE,
    ) {
        HorizontalMultiBrowseCarousel(
            state = rememberCarouselState { ITEMS },
            preferredItemWidth = 186.dp,
            modifier = carouselModifier.testTag("multiBrowseCarousel"),
            itemSpacing = Spacing.s2,
        ) { Swatch(it, R.string.components_carousel_item) }
    }
    DemoSection(
        R.string.components_carousel_uncontained,
        R.string.components_carousel_uncontained_usage,
        "HorizontalUncontainedCarousel",
        FILE,
    ) {
        HorizontalUncontainedCarousel(
            state = rememberCarouselState { ITEMS },
            itemWidth = 186.dp,
            modifier = carouselModifier.testTag("uncontainedCarousel"),
            itemSpacing = Spacing.s2,
        ) { Swatch(it, R.string.components_carousel_card) }
    }
    DemoSection(R.string.components_carousel_hero, R.string.components_carousel_hero_usage, "HorizontalCenteredHeroCarousel", FILE) {
        HorizontalCenteredHeroCarousel(
            state = rememberCarouselState { ITEMS },
            modifier = carouselModifier.testTag("heroCarousel"),
            itemSpacing = Spacing.s2,
        ) { Swatch(it, R.string.components_carousel_hero_item) }
    }
}

@Composable
private fun GridsExample() {
    DemoSection(
        R.string.components_grid_staggered,
        R.string.components_grid_staggered_usage,
        "LazyVerticalStaggeredGrid(StaggeredGridCells.Fixed(2))",
        FILE,
    ) {
        // Fixed height: a lazy grid cannot measure itself inside the page's vertical scroll.
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            modifier = Modifier.fillMaxWidth().height(360.dp).testTag("staggeredGrid"),
            verticalItemSpacing = Spacing.s2,
            horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
        ) {
            items(20) { index ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height((96 + (index % 3) * 48).dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(swatches[index % swatches.size]),
                    contentAlignment = Alignment.BottomStart,
                ) {
                    Text(
                        stringResource(R.string.components_grid_tile, index + 1),
                        color = Color.White,
                        modifier = Modifier.padding(Spacing.s2),
                    )
                }
            }
        }
    }
    DemoSection(
        R.string.components_grid_horizontal,
        R.string.components_grid_horizontal_usage,
        "LazyHorizontalGrid(GridCells.Fixed(2))",
        FILE,
    ) {
        LazyHorizontalGrid(
            rows = GridCells.Fixed(2),
            modifier = Modifier.fillMaxWidth().height(200.dp).testTag("horizontalGrid"),
            horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
            verticalArrangement = Arrangement.spacedBy(Spacing.s2),
        ) {
            items(16) { index ->
                Box(
                    Modifier
                        .width(140.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(horizontal = Spacing.s4),
                    contentAlignment = Alignment.Center,
                ) { Text(stringResource(R.string.components_grid_cell, index + 1), color = MaterialTheme.colorScheme.onSecondaryContainer) }
            }
        }
    }
}

@ThemePreviews
@Composable
private fun CarouselScreenPreview() {
    PreviewTheme {
        CarouselScreen(onBack = {})
    }
}
