package com.ssafy.a705.record

import com.ssafy.a705.model.resp.BasicResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface RecordApi {
    @POST("api/v1/diaries")
    suspend fun createDiary(
        @Body body: RecordCreateRequest
    ): RecordCreateResponse

    @GET("api/v1/diaries")
    suspend fun getDiaries(
        @Query("cursorId") cursorId: Long?,       // null이면 첫 페이지
        @Query("size") size: Int,
        @Query("locationCode") locationCode: Int?
    ): DiaryListResponse

    @GET("api/v1/diaries/{diary-id}")
    suspend fun getDiaryDetail(
        @Path("diary-id") id: Long
    ): DiaryDetailResponse

    @PUT("api/v1/diaries/{diary-id}")
    suspend fun updateDiary(
        @Path("diary-id") id: Long,
        @Body body: RecordUpdateRequest
    ): BasicResponse

    @DELETE("api/v1/diaries/{diary-id}")
    suspend fun deleteDiary(
        @Path("diary-id") id: Long
    ): BasicResponse
}