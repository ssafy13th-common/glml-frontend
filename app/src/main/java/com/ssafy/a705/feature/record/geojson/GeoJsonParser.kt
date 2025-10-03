package com.ssafy.a705.feature.record.geojson

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.JsonArray

object GeoJsonParser {
    data class FeatureCollection(
        val type: String,
        val features: List<Feature>     // 실제 Geometry 목록
    )

    data class Feature(
        val type: String,
        val properties: Map<String, Any>,   // 행정구역명, 지역코드
        val geometry: Geometry
    )

    data class Geometry(
        val type: String,               // Polygon 또는 MultiPolygon
        val coordinates: JsonArray      // Json 배열을 저장
    )

    private val gson = Gson()

    fun parse(json: String): FeatureCollection =
        gson.fromJson(json, FeatureCollection::class.java)
}