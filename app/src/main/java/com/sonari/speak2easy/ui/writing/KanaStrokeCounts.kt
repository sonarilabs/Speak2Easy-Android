package com.sonari.speak2easy.ui.writing

/**
 * Static stroke-count lookup ported verbatim from iOS `KanaStrokeCounts.swift`.
 * Used for the "X/Y strokes" footer when the backend doesn't include an expected count.
 */
object KanaStrokeCounts {

    val hiragana: Map<String, Int> = mapOf(
        "あ" to 3, "い" to 2, "う" to 2, "え" to 2, "お" to 3,
        "か" to 3, "き" to 4, "く" to 1, "け" to 3, "こ" to 2,
        "さ" to 3, "し" to 1, "す" to 2, "せ" to 3, "そ" to 1,
        "た" to 4, "ち" to 2, "つ" to 1, "て" to 1, "と" to 2,
        "な" to 4, "に" to 3, "ぬ" to 2, "ね" to 2, "の" to 1,
        "は" to 3, "ひ" to 1, "ふ" to 4, "へ" to 1, "ほ" to 4,
        "ま" to 3, "み" to 2, "む" to 3, "め" to 2, "も" to 3,
        "や" to 3, "ゆ" to 2, "よ" to 2,
        "ら" to 2, "り" to 2, "る" to 1, "れ" to 2, "ろ" to 1,
        "わ" to 2, "を" to 3, "ん" to 1,
    )

    val katakana: Map<String, Int> = mapOf(
        "ア" to 2, "イ" to 2, "ウ" to 3, "エ" to 3, "オ" to 3,
        "カ" to 2, "キ" to 3, "ク" to 2, "ケ" to 3, "コ" to 2,
        "サ" to 3, "シ" to 3, "ス" to 2, "セ" to 2, "ソ" to 2,
        "タ" to 3, "チ" to 3, "ツ" to 3, "テ" to 3, "ト" to 2,
        "ナ" to 2, "ニ" to 2, "ヌ" to 2, "ネ" to 4, "ノ" to 1,
        "ハ" to 2, "ヒ" to 2, "フ" to 1, "ヘ" to 1, "ホ" to 4,
        "マ" to 2, "ミ" to 3, "ム" to 2, "メ" to 2, "モ" to 3,
        "ヤ" to 2, "ユ" to 2, "ヨ" to 3,
        "ラ" to 2, "リ" to 2, "ル" to 2, "レ" to 1, "ロ" to 3,
        "ワ" to 2, "ヲ" to 3, "ン" to 2,
    )

    fun countFor(character: String): Int? = hiragana[character] ?: katakana[character]

    fun charactersFor(charset: String): List<String> =
        if (charset.equals("katakana", ignoreCase = true)) katakana.keys.toList() else hiragana.keys.toList()

    fun previous(charset: String, character: String): String? {
        val chars = charactersFor(charset)
        val index = chars.indexOf(character)
        return if (index > 0) chars[index - 1] else null
    }

    fun next(charset: String, character: String): String? {
        val chars = charactersFor(charset)
        val index = chars.indexOf(character)
        return if (index >= 0 && index < chars.lastIndex) chars[index + 1] else null
    }

    fun romanizationFor(character: String): String? = romanization[character]

    private val romanization: Map<String, String> = buildMap {
        val readings = listOf(
            "a", "i", "u", "e", "o",
            "ka", "ki", "ku", "ke", "ko",
            "sa", "shi", "su", "se", "so",
            "ta", "chi", "tsu", "te", "to",
            "na", "ni", "nu", "ne", "no",
            "ha", "hi", "fu", "he", "ho",
            "ma", "mi", "mu", "me", "mo",
            "ya", "yu", "yo",
            "ra", "ri", "ru", "re", "ro",
            "wa", "wo", "n",
        )
        hiragana.keys.zip(readings).forEach { (kana, reading) -> put(kana, reading) }
        katakana.keys.zip(readings).forEach { (kana, reading) -> put(kana, reading) }
    }
}
