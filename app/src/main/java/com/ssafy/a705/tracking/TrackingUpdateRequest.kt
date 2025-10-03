package com.ssafy.a705.tracking

data class TrackingUpdateRequest(
    val images: List<String>,
    val thumbnailImage: String
)