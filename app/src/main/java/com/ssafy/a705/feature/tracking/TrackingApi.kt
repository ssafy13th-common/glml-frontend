package com.ssafy.a705.feature.tracking

import com.ssafy.a705.common.model.BasicResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface TrackingApi {
    // 트래킹 전체 이미지 요청
    @GET("api/v1/trackings")
    suspend fun getTrackingList(): Response<TrackingListResponse>

    @POST("api/v1/trackings")
    suspend fun postTrackingWithThumbnail(
        @Body request: TrackingCreateRequest
    ): Response<TrackingCreateResponse>

    @GET("api/v1/trackings/{tracking-id}")
    suspend fun getTrackingDetail(
        @Path("tracking-id") id: String
    ): Response<TrackingDetailResponse>

    @PATCH("api/v1/trackings/{tracking-id}")
    suspend fun updateTrackingImages(
        @Path("tracking-id") id: String,
        @Body request: TrackingUpdateRequest
    ): Response<BasicResponse>

    @DELETE("api/v1/trackings/{tracking-id}")
    suspend fun deleteTracking(
        @Path("tracking-id") id: String
    ): Response<BasicResponse>
}
