package com.ssafy.a705.feature.tracking

data class TrackingCreateRequest(
    val tracks: List<TrackingSnapshot>,
    val thumbnailImage: String
)

data class TrackingCreateResponse(
    val message: String?,
    val data: CreateData?
) {
    data class CreateData(val trackingId: String)
}