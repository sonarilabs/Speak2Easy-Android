package com.sonari.speak2easy.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import com.sonari.speak2easy.R
import com.sonari.speak2easy.ui.theme.SonariFonts
import com.sonari.speak2easy.ui.theme.SonariTheme

/**
 * Mirrors the iOS `MascotAsset` enum (Views/Components/MascotView.swift).
 * Only the 8 mascots iOS actually uses are imported — extend on demand.
 */
enum class Mascot(@DrawableRes val resId: Int) {
    Studying(R.drawable.mascot_studying),
    Cheering(R.drawable.mascot_cheering),
    Thinking(R.drawable.mascot_thinking),
    Practicing(R.drawable.mascot_practicing),
    Success(R.drawable.mascot_success),
    Sleepy(R.drawable.mascot_sleepy),
    Relaxing(R.drawable.mascot_relaxing),
    HappyWave(R.drawable.mascot_happy_wave),
}

/** Plain mascot illustration, decorative by default (matches iOS `isDecorative = true`). */
@Composable
fun MascotImage(
    mascot: Mascot,
    height: Dp,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(mascot.resId),
        contentDescription = null,
        modifier = modifier.height(height),
        contentScale = ContentScale.Fit,
    )
}

/**
 * Mascot + two-line text in a surface card. Mirrors iOS `MascotCallout`
 * (Views/Components/MascotView.swift:57).
 */
@Composable
fun MascotCallout(
    mascot: Mascot,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    mascotHeight: Dp = 96.dp,
) {
    val c = SonariTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(c.surfaceSecondary)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MascotImage(mascot = mascot, height = mascotHeight)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
            Text(title, style = SonariFonts.monoSmall, color = c.textPrimary)
            Text(body, style = SonariFonts.monoTiny, color = c.textSecondary)
        }
    }
}
