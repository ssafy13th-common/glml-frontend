package com.ssafy.a705.tracking

data class TrackingListResponse(
    val message: String?,
    val data: Data?
) {
    data class Data(
        val trackingImages: List<TrackingImageItem>
    )
}
