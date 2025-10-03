package com.ssafy.a705.domain.tracking

data class TrackingUpdateRequest(
    val images: List<String>,
    val thumbnailImage: String
)