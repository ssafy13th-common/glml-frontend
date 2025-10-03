package com.ssafy.a705.record

import com.google.gson.annotations.SerializedName

data class MapResponse (
    val message: String?,
    val data: MapData?
)

data class MapData(
    val colors: List<MapColors> = emptyList()
)

data class MapColors(
    @SerializedName("LocationCode") val locationCode: String,
    @SerializedName("LocationColor") val locationColor: String
)