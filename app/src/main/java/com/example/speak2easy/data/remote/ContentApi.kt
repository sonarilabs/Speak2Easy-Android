package com.example.speak2easy.data.remote

import com.example.speak2easy.data.remote.dto.ContentGroupsResponse
import com.example.speak2easy.data.remote.dto.ContentItem
import com.example.speak2easy.data.remote.dto.ContentItemsResponse
import com.example.speak2easy.data.remote.dto.LessonContentResponse
import com.example.speak2easy.data.remote.dto.LessonsResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ContentApi {
    @GET("lessons")
    suspend fun getLessons(
        @Query("charset") charset: String? = null,
        @Query("content_type") contentType: String? = null,
    ): LessonsResponse

    @GET("content/lessons/{lessonNumber}")
    suspend fun getLessonContent(
        @Path("lessonNumber") lessonNumber: Int,
        @Query("charset") charset: String,
        @Query("content_type") contentType: String? = null,
        @Query("unit_number") unitNumber: Int? = null,
    ): LessonContentResponse

    @GET("content/hiragana")
    suspend fun getHiraganaChart(): List<ContentItem>

    @GET("content/katakana")
    suspend fun getKatakanaChart(): List<ContentItem>

    @GET("content/groups")
    suspend fun getContentGroups(
        @Query("content_type") contentType: String? = null,
        @Query("charset") charset: String? = null,
    ): ContentGroupsResponse

    @GET("content/items")
    suspend fun getContentItems(
        @Query("content_type") contentType: String? = null,
        @Query("charset") charset: String? = null,
        @Query("group_label") groupLabel: String? = null,
        @Query("limit") limit: Int = 200,
        @Query("offset") offset: Int = 0,
    ): ContentItemsResponse
}
