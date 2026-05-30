package com.example.speak2easy.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.speak2easy.model.LessonCategory

/**
 * Full Sonari color palette, ported 1:1 from iOS `SonariTheme.swift`.
 * Holds the Material-equivalent semantic colors plus the app's per-category
 * accent/glow colors, which have no corresponding Material 3 slot.
 */
@Immutable
data class SonariColors(
    val background: Color,
    val surfacePrimary: Color,
    val surfaceSecondary: Color,
    val cardBackground: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accent: Color,
    val accentSecondary: Color,
    val buttonText: Color,
    val border: Color,
    val divider: Color,
    val success: Color,
    val error: Color,
    val accentHiragana: Color,
    val accentHiraganaGlow: Color,
    val accentKatakana: Color,
    val accentKatakanaGlow: Color,
    val accentWords: Color,
    val accentWordsGlow: Color,
    val resultCorrectFill: Color,
    val resultCorrectBorder: Color,
    val resultIncorrectFill: Color,
    val resultIncorrectBorder: Color,
) {
    fun accentFor(category: LessonCategory): Color = when (category) {
        LessonCategory.HIRAGANA -> accentHiragana
        LessonCategory.KATAKANA -> accentKatakana
        LessonCategory.WORDS -> accentWords
    }

    fun glowFor(category: LessonCategory): Color = when (category) {
        LessonCategory.HIRAGANA -> accentHiraganaGlow
        LessonCategory.KATAKANA -> accentKatakanaGlow
        LessonCategory.WORDS -> accentWordsGlow
    }
}

val SonariLightColors = SonariColors(
    background = Color(0xFFF1F5F9),
    surfacePrimary = Color(0xFFFFFFFF),
    surfaceSecondary = Color(0xFFE2E8F0),
    cardBackground = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF475569),
    textTertiary = Color(0xFF94A3B8),
    accent = Color(0xFF0891B2),
    accentSecondary = Color(0xFFC026D3),
    buttonText = Color(0xFFFFFFFF),
    border = Color(0xFFCBD5E1),
    divider = Color(0xFFE2E8F0),
    success = Color(0xFF22C55E),
    error = Color(0xFFEF4444),
    accentHiragana = Color(0xFF0891B2),
    accentHiraganaGlow = Color(0xFF22D3EE),
    accentKatakana = Color(0xFFC026D3),
    accentKatakanaGlow = Color(0xFFE879F9),
    accentWords = Color(0xFF4F46E5),
    accentWordsGlow = Color(0xFF818CF8),
    resultCorrectFill = Color(0xFFF0FDF4),
    resultCorrectBorder = Color(0xFF34D399),
    resultIncorrectFill = Color(0xFFFEF2F2),
    resultIncorrectBorder = Color(0xFFFF0000),
)

val SonariDarkColors = SonariColors(
    background = Color(0xFF020617),
    surfacePrimary = Color(0xFF111111),
    surfaceSecondary = Color(0xFF1A1A1A),
    cardBackground = Color(0xFF111111),
    textPrimary = Color(0xFFE5E7EB),
    textSecondary = Color(0xFF8892A4),
    textTertiary = Color(0xFF6B7280),
    accent = Color(0xFF22D3EE),
    accentSecondary = Color(0xFFD946EF),
    buttonText = Color(0xFF020617),
    border = Color(0xFF222222),
    divider = Color(0xFF1F2937),
    success = Color(0xFF22C55E),
    error = Color(0xFFEF4444),
    accentHiragana = Color(0xFF22D3EE),
    accentHiraganaGlow = Color(0xFF06B6D4),
    accentKatakana = Color(0xFFD946EF),
    accentKatakanaGlow = Color(0xFFE879F9),
    accentWords = Color(0xFF6366F1),
    accentWordsGlow = Color(0xFF3C3EF1),
    resultCorrectFill = Color(0xFF111827),
    resultCorrectBorder = Color(0xFF10B981),
    resultIncorrectFill = Color(0xFF111827),
    resultIncorrectBorder = Color(0xFFFF0000),
)

/** Provides the active [SonariColors] down the composition. Defaults to dark. */
val LocalSonariColors = staticCompositionLocalOf { SonariDarkColors }
