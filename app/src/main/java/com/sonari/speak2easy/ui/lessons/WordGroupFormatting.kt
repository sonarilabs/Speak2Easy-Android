package com.sonari.speak2easy.ui.lessons

import com.sonari.speak2easy.data.remote.dto.ContentGroupItem

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
 * Lesson unlock state — mirrors iOS exactly: trust the API's `isUnlocked` flag, with a local
 * fallback (`previous lesson at >= 80%`) so the UI feels snappy after a session even if the API
 * roundtrip hasn't refreshed yet. The first lesson is always unlocked (backend guarantees this).
 *
 * Earlier versions of this function tried to gate the first lesson by the user's
 * `japanese_level` (e.g. only unlock katakana for intermediate+), but the backend already returns
 * `is_unlocked = true` for every charset's first lesson regardless of level — extra client-side
 * logic was redundant and caused Advanced users to see lessons spuriously unlock.
 */
fun computeUnlocked(
    groups: List<LessonGroup>,
    progress: Map<String, Float>,
    apiUnlocked: Map<String, Boolean>,
): Map<String, Boolean> {
    val flat = groups.flatMap { it.lessons }
    if (flat.isEmpty()) return emptyMap()
    val result = mutableMapOf<String, Boolean>()
    var previousComplete = true  // first lesson always unlocked
    for ((i, lesson) in flat.withIndex()) {
        val key = lesson.progressKey
        val unlocked = if (i == 0) true
                       else (apiUnlocked[key] == true) || previousComplete
        result[key] = unlocked
        previousComplete = (progress[key] ?: 0f) >= LOCAL_UNLOCK_THRESHOLD
    }
    return result
}

/** Matches the backend's `completed_items >= CEIL(total_items * 0.8)` unlock rule. */
private const val LOCAL_UNLOCK_THRESHOLD = 0.8f

/**
 * Curriculum sort order for word-group labels. Mirrors the backend's `ROW_NUMBER OVER (ORDER BY ...)`
 * exactly (content.ts), so on-screen order matches the unlock sequence:
 *   A & Ka → Sa & Ta → Na & Ha → Ma & Ya → Ra/Wa/N → Combined → Voiced → Double Cons → Long Vowels
 * Without this, alphabetical sorting puts "Combined Sounds" right after "A & Ka Row" which makes
 * users think Combined is the next lesson — but the backend won't unlock it until Long Vowels is done.
 */
private fun wordGroupSortOrder(label: String): Int = when {
    label.endsWith("-a-ko") -> 0
    label.endsWith("-sa-to") -> 1
    label.endsWith("-na-ho") -> 2
    label.endsWith("-ma-yo") -> 3
    label.endsWith("-ra-n") -> 4
    label.endsWith("-contracted") -> 10
    label.endsWith("-diacritical") -> 11
    label.endsWith("-double-cons") -> 12
    label.endsWith("-long-vowels") -> 13
    label.startsWith("genki-ch") -> {
        // "genki-ch01-1" → chapter 1, part 1 → 101. Chapter*100 + part keeps natural order.
        val parts = label.removePrefix("genki-ch").split("-")
        val ch = parts.getOrNull(0)?.toIntOrNull() ?: 99
        val pt = parts.getOrNull(1)?.toIntOrNull() ?: 99
        ch * 100 + pt
    }
    else -> 999
}

/** Groups API word-group items into the three display sections, each curriculum-ordered. */
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
    val sortByOrder = compareBy<WordGroupInfo> { wordGroupSortOrder(it.groupLabel) }
    return buildList {
        if (hira.isNotEmpty()) add(WordGroupSection("Hiragana Words", "Core hiragana vocabulary by group", hira.sortedWith(sortByOrder)))
        if (kata.isNotEmpty()) add(WordGroupSection("Katakana Words", "Loanwords & foreign terms", kata.sortedWith(sortByOrder)))
        if (vocab.isNotEmpty()) add(WordGroupSection("Essential Vocabulary", "Curated sets — beginner to advanced", vocab.sortedWith(sortByOrder)))
    }
}
