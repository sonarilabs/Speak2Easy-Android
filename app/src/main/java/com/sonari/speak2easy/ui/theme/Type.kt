package com.sonari.speak2easy.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Typography ported from iOS `SonariFonts`. Most of the UI uses a monospaced
 * family for its tech-forward look; the hero heading and plain caption use the
 * default family.
 */
object SonariFonts {
    val heroLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Black, fontSize = 34.sp)
    val monoLarge = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 24.sp)
    val monoMedium = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
    val monoSmall = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium, fontSize = 14.sp)
    val monoCaption = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium, fontSize = 12.sp)
    val monoTiny = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
    val bodyRegular = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Normal, fontSize = 14.sp)
    val caption = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 12.sp)
}

/** Material 3 Typography mapped onto the Sonari styles for default component text. */
val SonariTypography = Typography(
    displayLarge = SonariFonts.heroLarge,
    titleLarge = SonariFonts.monoLarge,
    titleMedium = SonariFonts.monoMedium,
    bodyLarge = SonariFonts.bodyRegular,
    bodyMedium = SonariFonts.monoSmall,
    labelMedium = SonariFonts.monoCaption,
    labelSmall = SonariFonts.monoTiny,
)
