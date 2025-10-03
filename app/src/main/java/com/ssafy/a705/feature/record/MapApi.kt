package com.ssafy.a705.feature.record

import com.ssafy.a705.feature.model.resp.BasicResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path

interface MapApi {
    @GET("api/v1/locations")
    suspend fun getMapColors(): MapResponse

    @PATCH("api/v1/locations/{location-code}")
    suspend fun patchLocationColor(
        @Path("location-code") code: Long,
        @Body body: MapUpdateRequest
    ): BasicResponse
}
