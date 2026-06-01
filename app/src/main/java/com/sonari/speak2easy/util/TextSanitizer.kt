package com.sonari.speak2easy.util

/**
 * Tiny shared sanitizer for free-text inputs that ride to the backend (display name, city,
 * referral code, etc.). kotlinx.serialization already JSON-escapes everything, so this is
 * about stripping characters the backend's email/HTML rendering layer doesn't want to see
 * (NULs, control chars, stray newlines) and enforcing per-field character classes.
 */
object TextSanitizer {

    /**
     * Cleans a human name input: keeps letters / space / hyphen / apostrophe / period,
     * strips digits, control chars, NUL, and other punctuation. Collapses internal whitespace.
     * Hard-capped at 20 chars to fit nicely on display surfaces (profile card, lesson header, etc.).
     */
    fun cleanName(raw: String): String {
        val filtered = raw.filter { ch ->
            ch.isLetter() || ch == ' ' || ch == '-' || ch == '\'' || ch == '.'
        }
        return filtered.replace(Regex("\\s+"), " ").trimStart().take(MAX_NAME_CHARS)
    }

    const val MAX_NAME_CHARS = 20

    /** Strip control chars + NUL + leading/trailing whitespace; cap to [maxLen]. */
    fun cleanFreeText(raw: String, maxLen: Int = 100): String =
        raw.filter { it >= ' ' }.trim().take(maxLen)
}
