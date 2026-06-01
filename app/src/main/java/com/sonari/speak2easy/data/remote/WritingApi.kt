package com.sonari.speak2easy.data.remote

import com.sonari.speak2easy.data.remote.dto.EvaluationRequest
import com.sonari.speak2easy.data.remote.dto.EvaluationResponse
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path

interface WritingApi {

    /**
     * Returns the raw AnimCJK SVG body. We declare `ResponseBody` so Retrofit's JSON
     * converter doesn't try to parse it — the repository reads `.string()` itself.
     */
    @Headers("Accept: image/svg+xml")
    @GET("{charset}/svg/{character}")
    suspend fun getStrokeSvg(
        @Path("charset") charset: String,
        @Path(value = "character", encoded = false) character: String,
    ): ResponseBody

    @POST("{charset}/evaluate")
    suspend fun evaluate(
        @Path("charset") charset: String,
        @Body request: EvaluationRequest,
    ): EvaluationResponse
}
