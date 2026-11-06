package com.primestrider.kotlinstarter.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.Spacing
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews

/** Outlined card of [InfoRow]s from (label, value) pairs; a null or blank value shows an em dash. */
@Composable
fun InfoCard(
    rows: List<Pair<String, String?>>,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = Spacing.s4, vertical = Spacing.s2)) {
            rows.forEach { (label, value) -> InfoRow(label, value?.takeIf { it.isNotBlank() } ?: "—") }
        }
    }
}

@ThemePreviews
@Composable
private fun InfoCardPreview() {
    PreviewTheme {
        InfoCard(rows = listOf("Name" to "Ada Lovelace", "Email" to "ada@example.com", "Phone" to null))
    }
}
