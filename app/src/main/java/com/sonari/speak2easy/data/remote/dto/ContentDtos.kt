package com.sonari.speak2easy.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ContentItem(
    val contentId: String = "",
    val contentText: String = "",
    val contentLength: Int? = null,
    val romanization: String = "",
    val pronunciationGuide: String? = null,
    val pronunciationIpa: String? = null,
    val difficultyLevel: Int? = null,
    val lessonNumber: Int? = null,
    val sortOrder: Int? = null,
    val isCompound: Boolean? = null,
    val englishTranslation: String? = null,
    val hiraganaReading: String? = null,
    val groupLabel: String? = null,
)

@Serializable
data class LessonsResponse(
    val charset: String? = null,
    val categoryAccessible: Boolean? = null,
    val contentType: String? = null,
    val lessons: List<LessonProgress> = emptyList(),
    val wordGroups: List<WordGroupProgress>? = null,
)

@Serializable
data class LessonProgress(
    val lessonNumber: Int = 0,
    val unitNumber: Int? = null,
    val charsetDisplayName: String? = null,
    val totalItems: Int = 0,
    val completedItems: Int = 0,
    val isUnlocked: Boolean = false,
) {
    val progressKey: String get() = if (unitNumber != null) "$lessonNumber-$unitNumber" else "$lessonNumber"
    val progress: Float get() = if (totalItems > 0) completedItems.toFloat() / totalItems.toFloat() else 0f
}

@Serializable
data class WordGroupProgress(
    val groupLabel: String = "",
    val totalItems: Int = 0,
    val completedItems: Int = 0,
    val isUnlocked: Boolean = false,
)

@Serializable
data class ContentGroupsResponse(
    val groups: List<ContentGroupItem> = emptyList(),
)

@Serializable
data class ContentGroupItem(
    val groupLabel: String = "",
    val contentType: String = "",
    val charset: String? = null,
    val unitNumber: Int? = null,
    val difficulty: Int? = null,
    val itemCount: Int = 0,
)

@Serializable
data class ContentItemsResponse(
    // count/limit/offset are ignored (they arrive as String-or-Int; we only need items).
    val items: List<ContentItem> = emptyList(),
)

@Serializable
data class LessonContentResponse(
    val lesson: LessonContentMetadata,
    val items: List<ContentItem> = emptyList(),
)

@Serializable
data class LessonContentMetadata(
    val lessonNumber: Int = 0,
    val unitNumber: Int? = null,
    val charset: String = "",
    val contentType: String = "",
    val itemCount: Int = 0,
    val difficultyMin: Int? = null,
    val difficultyMax: Int? = null,
)
