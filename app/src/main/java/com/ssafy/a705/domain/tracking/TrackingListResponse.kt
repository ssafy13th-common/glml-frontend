package com.ssafy.a705.domain.tracking

data class TrackingListResponse(
    val message: String?,
    val data: Data?
) {
    data class Data(
        val trackingImages: List<TrackingImageItem>
    )
}
