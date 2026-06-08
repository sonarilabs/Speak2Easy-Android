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
    data class SentenceLesson(
        val lessonNumber: Int,
        val lessonTitle: String,
        val sentenceCount: Int,
    ) : PracticeSource

    val title: String
        get() = when (this) {
            is Lesson -> info.title
            is WordGroup -> group.title
            is SentenceLesson -> lessonTitle
        }

    val subtitle: String
        get() = when (this) {
            is Lesson -> info.subtitle
            is WordGroup -> group.subtitle
            is SentenceLesson -> "$sentenceCount sentence" + if (sentenceCount == 1) "" else "s"
        }

    val itemCount: Int
        get() = when (this) {
            is Lesson -> info.totalCharacters
            is WordGroup -> group.itemCount
            is SentenceLesson -> sentenceCount
        }

    val headerLabel: String
        get() = when (this) {
            is Lesson -> "LESSON ${info.number}"
            is WordGroup -> "WORD GROUP"
            is SentenceLesson -> "LESSON $lessonNumber"
        }

    val itemCountLabel: String
        get() = when (this) {
            is Lesson -> "${info.totalCharacters} characters"
            is WordGroup -> "${group.itemCount} words"
            is SentenceLesson -> "$sentenceCount sentence" + if (sentenceCount == 1) "" else "s"
        }
}

/** Themed titles for the Sentences track, keyed by lesson_number — mirrors insert-sentences.sql. */
object SentenceCurriculum {
    val titles: Map<Int, String> = mapOf(
        1 to "Greetings", 2 to "Everyday Courtesy", 3 to "Yes, No & Replies", 4 to "Introducing Yourself",
        5 to "Simple Questions", 6 to "Self-Introduction", 7 to "Numbers & Prices", 8 to "This & That",
        9 to "Likes & Dislikes", 10 to "Describing Things", 11 to "Daily Routine", 12 to "Time & Days",
        13 to "Places & Directions", 14 to "Wants & Requests", 15 to "Shopping & Dining",
        16 to "Talking About the Past", 17 to "Asking Politely", 18 to "Reasons & Linking",
        19 to "Weather & Feelings", 20 to "Plans & Invitations", 21 to "Opinions", 22 to "Comparisons",
        23 to "Polite Requests", 24 to "Daily Life", 25 to "Travel",
    )

    fun titleFor(lessonNumber: Int): String = titles[lessonNumber] ?: "Lesson $lessonNumber"
}

/** Session options collected by the options sheet. Mirrors iOS LessonOptionsSheet. */
data class PracticeOptions(
    val isSequential: Boolean = true,
    val isHandsFree: Boolean = false,
    val autoPlayAudio: Boolean = true,
    val isFocusMode: Boolean = false,
)
