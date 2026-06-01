package com.sonari.speak2easy.ui.lessons

/** The two kana scripts (Words use no script). Mirrors iOS `JapaneseScript`. */
enum class JapaneseScript(val value: String, val displayName: String) {
    HIRAGANA("hiragana", "Hiragana"),
    KATAKANA("katakana", "Katakana"),
}

enum class ContentKind(val value: String) {
    SINGLE_CHARACTER("single_character"),
    DOUBLE_CHARACTER("double_character"),
    WORD("word"),
    SENTENCE("sentence"),
}

data class LessonInfo(
    val number: Int,
    val title: String,
    val subtitle: String,
    val characters: List<String>,
    val unitNumber: Int? = null,
    val apiLessonNumber: Int? = null,
) {
    val totalCharacters: Int get() = characters.size

    /** Key used in progress/unlock maps — must match what the API returns. */
    val progressKey: String
        get() {
            val apiNum = apiLessonNumber ?: number
            return if (unitNumber != null) "$apiNum-$unitNumber" else "$apiNum"
        }
}

data class LessonGroup(
    val title: String,
    val subtitle: String,
    val characterSet: JapaneseScript,
    val contentType: ContentKind,
    val lessons: List<LessonInfo>,
)

data class WordGroupInfo(
    val groupLabel: String,
    val title: String,
    val subtitle: String,
    val contentType: ContentKind,
    val charset: JapaneseScript?,
    val itemCount: Int,
    val difficulty: Int,
)

data class WordGroupSection(
    val title: String,
    val subtitle: String,
    val groups: List<WordGroupInfo>,
)

/** Unified content source for a practice session. Mirrors iOS `PracticeSource`. */
sealed interface PracticeSource {
    data class Lesson(val info: LessonInfo, val characterSet: JapaneseScript) : PracticeSource
    data class WordGroup(val group: WordGroupInfo) : PracticeSource

    val title: String
        get() = when (this) {
            is Lesson -> info.title
            is WordGroup -> group.title
        }

    val subtitle: String
        get() = when (this) {
            is Lesson -> info.subtitle
            is WordGroup -> group.subtitle
        }

    val itemCount: Int
        get() = when (this) {
            is Lesson -> info.totalCharacters
            is WordGroup -> group.itemCount
        }

    val headerLabel: String
        get() = when (this) {
            is Lesson -> "LESSON ${info.number}"
            is WordGroup -> "WORD GROUP"
        }

    val itemCountLabel: String
        get() = when (this) {
            is Lesson -> "${info.totalCharacters} characters"
            is WordGroup -> "${group.itemCount} words"
        }
}

/** Session options collected by the options sheet. Mirrors iOS LessonOptionsSheet. */
data class PracticeOptions(
    val isSequential: Boolean = true,
    val isHandsFree: Boolean = false,
    val autoPlayAudio: Boolean = true,
    val isFocusMode: Boolean = false,
)
