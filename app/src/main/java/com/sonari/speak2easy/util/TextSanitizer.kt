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

    /**
     * Cleans a city-name input: keeps letters / space / hyphen / apostrophe / period
     * (covers "St. Louis", "Fort Worth", "Winston-Salem", "O'Fallon"). Strips digits,
     * control chars, and stray punctuation. Collapses internal whitespace. Capped at
     * [MAX_CITY_CHARS] which matches the backend's 100-char field limit.
     */
    fun cleanCity(raw: String): String {
        val filtered = raw.filter { ch ->
            ch.isLetter() || ch == ' ' || ch == '-' || ch == '\'' || ch == '.'
        }
        return filtered.replace(Regex("\\s+"), " ").trimStart().take(MAX_CITY_CHARS)
    }

    const val MAX_CITY_CHARS = 100
    const val MIN_CITY_CHARS = 3

    /** True if [city] is acceptable as a typed-in "Other" city — letters only, ≥3 chars. */
    fun isValidCity(city: String): Boolean {
        val trimmed = city.trim()
        if (trimmed.length < MIN_CITY_CHARS) return false
        // After cleanCity strips digits/symbols, length should be unchanged. If not, the
        // user typed disallowed characters that we silently dropped — reject so the
        // CONTINUE button stays disabled until they fix the input.
        return cleanCity(trimmed).length == trimmed.length
    }

    /** Strip control chars + NUL + leading/trailing whitespace; cap to [maxLen]. */
    fun cleanFreeText(raw: String, maxLen: Int = 100): String =
        raw.filter { it >= ' ' }.trim().take(maxLen)

    /**
     * Cleans multiline user text while preserving readable line breaks. Removes NUL and other
     * control characters, normalizes CRLF/CR to LF, caps blank-line runs, and caps to [maxLen].
     * HTML/SQL escaping still belongs on the backend at the final output/query sink.
     */
    fun cleanMultilineText(raw: String, maxLen: Int): String {
        val normalized = raw.replace("\r\n", "\n").replace('\r', '\n')
        val filtered = normalized.filter { ch -> ch == '\n' || ch >= ' ' }
        return filtered
            .replace(Regex("[ \\t]+\\n"), "\n")
            .replace(Regex("\\n{4,}"), "\n\n\n")
            .take(maxLen)
    }
}
