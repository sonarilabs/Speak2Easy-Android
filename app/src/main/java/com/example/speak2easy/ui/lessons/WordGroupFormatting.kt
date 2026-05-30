package com.example.speak2easy.ui.lessons

import com.example.speak2easy.data.remote.dto.ContentGroupItem

/** Maps API group labels to display titles. Ported from iOS LessonsHomeView/MainTabView. */
private val groupLabelDisplayMap = mapOf(
    "hira-a-ko" to "A & Ka Row",
    "hira-sa-to" to "Sa & Ta Row",
    "hira-na-ho" to "Na & Ha Row",
    "hira-ma-yo" to "Ma & Ya Row",
    "hira-ra-n" to "Ra, Wa & N",
    "hira-contracted" to "Combined Sounds",
    "hira-diacritical" to "Voiced Sounds",
    "hira-double-cons" to "Double Consonants",
    "hira-long-vowels" to "Long Vowels",
    "kata-a-ko" to "A & Ka Row",
    "kata-sa-to" to "Sa & Ta Row",
    "kata-na-ho" to "Na & Ha Row",
    "kata-ma-yo" to "Ma & Ya Row",
    "kata-ra-n" to "Ra, Wa & N",
    "kata-contracted" to "Combined Sounds",
    "kata-diacritical" to "Voiced Sounds",
    "kata-double-cons" to "Double Consonants",
    "kata-long-vowels" to "Long Vowels",
)

fun formatWordGroupTitle(label: String): String {
    groupLabelDisplayMap[label]?.let { return it }
    if (label.startsWith("genki-ch")) {
        val parts = label.removePrefix("genki-ch").split("-")
        val chapter = parts.getOrNull(0)?.toIntOrNull()
        val part = parts.getOrNull(1)?.toIntOrNull()
        if (chapter != null && part != null) return "Chapter $chapter · Part $part"
    }
    val stripped = label.removePrefix("hira-").removePrefix("kata-")
    return stripped.split("-").joinToString(" ") { it.replaceFirstChar(Char::uppercaseChar) }
}

fun difficultyLabel(difficulty: Int): String = when (difficulty) {
    1 -> "Beginner"
    2 -> "Easy"
    3 -> "Intermediate"
    4 -> "Advanced"
    else -> "Expert"
}

/**
 * Lesson unlock state: a lesson is unlocked if the API says so, otherwise if the previous
 * lesson (in flattened order) is 100% complete. The very first lesson is always unlocked.
 * Mirrors iOS lock logic ("API unlock overrides offline logic").
 */
fun computeUnlocked(
    groups: List<LessonGroup>,
    progress: Map<String, Float>,
    apiUnlocked: Map<String, Boolean>,
): Map<String, Boolean> {
    val flat = groups.flatMap { it.lessons }
    val result = mutableMapOf<String, Boolean>()
    var previousComplete = true // first lesson starts unlocked
    for (lesson in flat) {
        val key = lesson.progressKey
        result[key] = apiUnlocked[key] ?: previousComplete
        previousComplete = (progress[key] ?: 0f) >= 1f
    }
    return result
}

/** Groups API word-group items into the three display sections. */
fun buildWordSections(groups: List<ContentGroupItem>): List<WordGroupSection> {
    val hira = mutableListOf<WordGroupInfo>()
    val kata = mutableListOf<WordGroupInfo>()
    val vocab = mutableListOf<WordGroupInfo>()
    for (g in groups) {
        val info = WordGroupInfo(
            groupLabel = g.groupLabel,
            title = formatWordGroupTitle(g.groupLabel),
            subtitle = difficultyLabel(g.difficulty ?: 1),
            contentType = ContentKind.WORD,
            charset = when {
                g.groupLabel.startsWith("hira") -> JapaneseScript.HIRAGANA
                g.groupLabel.startsWith("kata") -> JapaneseScript.KATAKANA
                else -> null
            },
            itemCount = g.itemCount,
            difficulty = g.difficulty ?: 1,
        )
        when {
            g.groupLabel.startsWith("hira") -> hira += info
            g.groupLabel.startsWith("kata") -> kata += info
            else -> vocab += info
        }
    }
    return buildList {
        if (hira.isNotEmpty()) add(WordGroupSection("Hiragana Words", "Core hiragana vocabulary by group", hira))
        if (kata.isNotEmpty()) add(WordGroupSection("Katakana Words", "Loanwords & foreign terms", kata))
        if (vocab.isNotEmpty()) add(WordGroupSection("Essential Vocabulary", "Curated sets — beginner to advanced", vocab))
    }
}
