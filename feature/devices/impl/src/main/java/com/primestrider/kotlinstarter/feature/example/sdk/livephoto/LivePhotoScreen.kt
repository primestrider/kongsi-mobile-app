package com.primestrider.kotlinstarter.feature.example.sdk.livephoto

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.primestrider.kotlinstarter.core.designsystem.component.Banner
import com.primestrider.kotlinstarter.core.designsystem.component.BannerVariant
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews
import com.primestrider.kotlinstarter.feature.devices.impl.R
import com.primestrider.kotlinstarter.feature.example.ExamplePageScaffold

/** Live Photos exist only on iOS; this page says so instead of faking the feature. */
@Composable
fun LivePhotoScreen(onBack: () -> Unit) {
    ExamplePageScaffold(
        title = R.string.sdk_live_photo_title,
        subtitle = R.string.sdk_live_photo_subtitle,
        onBack = onBack,
    ) {
        Banner(
            title = stringResource(R.string.sdk_live_photo_banner_title),
            message = stringResource(R.string.sdk_live_photo_banner_message),
            variant = BannerVariant.INFO,
        )
    }
}

@ThemePreviews
@Composable
private fun LivePhotoScreenPreview() {
    PreviewTheme {
        LivePhotoScreen(onBack = {})
    }
}
