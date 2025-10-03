package com.ssafy.a705.global.imageS3

data class ImagePresignRequest(
    val fileNames: List<String>,
    val domain: String
)

data class ImagePresignResponse(
    val message: String?,
    val data: ImagePresignPayload?
)

data class ImagePresignPayload(
    val presignedUrls: List<PresignedData>
)

data class PresignedData(
    val fileName: String,
    val presignedUrl: String
)