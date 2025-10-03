package com.ssafy.a705.network

import com.ssafy.a705.group.latecheck.ApiEnvelope
import com.ssafy.a705.group.latecheck.EmptyData
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.POST
import retrofit2.http.Path

interface LiveLocationStatusApi {
    @POST("api/v1/group/{group-id}/live-location/status")
    suspend fun enable(@Path("group-id") groupId: Long): Response<ApiEnvelope<EmptyData>>

    @DELETE("api/v1/group/{group-id}/live-location/status")
    suspend fun disable(@Path("group-id") groupId: Long): Response<ApiEnvelope<EmptyData>>
}
