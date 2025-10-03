package com.ssafy.a705.tracking

data class TrackingDetailResponse(
    val message: String?,
    val data: DetailData?
) {
    data class DetailData(
        val trackingId: String,
        val trackPoints: List<TrackingSnapshot>,
        val images: List<String>,
        val createdAt: String,
        val modifiedAt: String
    )
}