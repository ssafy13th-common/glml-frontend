package com.ssafy.a705.imageS3

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ImageApi {
    @POST("api/v1/images/presigned-urls")
    suspend fun getPresignedUrls(
        @Body request: ImagePresignRequest
    ): Response<ImagePresignResponse>
}