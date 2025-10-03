package com.ssafy.a705.record.geojson

import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import com.google.android.gms.common.util.Hex
import com.google.gson.JsonArray
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelTextBuilder
import com.kakao.vectormap.shape.MapPoints
import com.kakao.vectormap.shape.Polygon
import com.kakao.vectormap.shape.PolygonOptions
import com.kakao.vectormap.shape.PolygonStyles
import com.kakao.vectormap.shape.PolygonStylesSet
import com.ssafy.a705.record.geojson.GeoJsonParser.Feature

object GeoJsonDrawer {

    fun drawFeature(map: KakaoMap, feature: Feature, color: Int, code: String?, isShow: Boolean) {
        val geom = feature.geometry
        val name = feature.properties["ADMIN_NAME"]?.toString() ?: "이름없음"
        val regionCode = feature.properties["ADMIN_CODE"]?.toString() ?: "코드없음"

        if (code != null || isShow) {
            map.shapeManager?.layer?.allPolygons
                ?.filter { it.tag == name }
                ?.forEach { polygon ->
                    Log.d("PolygonRemove", "Removing polygon with tag: ${polygon.tag}")
                    map.shapeManager?.layer?.remove(polygon)
                }
        }


        when (geom.type) {
            "Polygon" -> {
                geom.coordinates.asJsonArray.forEachIndexed { idx, ringEl ->
                    val ring = ringEl.asJsonArray
                    val path = parseRing(ring)
                    if (idx == 0) {
                        // 외부 경계: 시계 방향 그대로
                        drawPolygon(map, path, name, color, regionCode, isShow)
                    } else {
                        // 내부 구멍: 반시계 방향으로 뒤집어서 구멍 역할
                        drawPolygon(map, path.reversed(), name, color, regionCode, isShow)
                    }
                }
            }

            "MultiPolygon" -> {
                val multiPoly = geom.coordinates.asJsonArray
                for (polygon in multiPoly) {
                    val rings = polygon.asJsonArray
                    for ((idx, ringEl) in rings.withIndex()) {
                        val ring = ringEl.asJsonArray
                        val path = parseRing(ring)
                        if (idx == 0) {
                            // 외부 링: 시계 방향
                            drawPolygon(map, path, name, color, regionCode, isShow)
                        } else {
                            // 내부 링(구멍): 반시계 방향으로 뒤집어서
                            drawPolygon(map, path.reversed(), name, color, regionCode, isShow)
                        }
                    }
                }
            }
        }
    }


    fun drawSidoFeature(map: KakaoMap, feature: Feature, color: Int, isShow: Boolean) {
        val geom = feature.geometry
        val name = feature.properties["CTP_KOR_NM"]?.toString() ?: "이름없음"
        val regionCode = feature.properties["CTPRVN_CD"]?.toString() ?: "코드없음"

        when (geom.type) {
            "Polygon" -> {
                geom.coordinates.asJsonArray.forEachIndexed { idx, ringEl ->
                    val ring = ringEl.asJsonArray
                    val path = parseRing(ring)
                    if (idx == 0) {
                        // 외부 경계: 시계 방향 그대로
                        drawPolygon(map, path, name + "_", color, regionCode, isShow)
                    } else {
                        // 내부 구멍: 반시계 방향으로 뒤집어서 구멍 역할
                        drawPolygon(map, path.reversed(), name + "_", color, regionCode, isShow)
                    }
                }
            }

            "MultiPolygon" -> {
                val multiPoly = geom.coordinates.asJsonArray
                for (polygon in multiPoly) {
                    val rings = polygon.asJsonArray
                    for ((idx, ringEl) in rings.withIndex()) {
                        val ring = ringEl.asJsonArray
                        val path = parseRing(ring)
                        if (idx == 0) {
                            // 외부 링: 시계 방향
                            drawPolygon(map, path, name + "_", color, regionCode, isShow)
                        } else {
                            // 내부 링(구멍): 반시계 방향으로 뒤집어서
                            drawPolygon(map, path.reversed(), name + "_", color, regionCode, isShow)
                        }
                    }
                }
            }
        }
    }

    private fun drawPolygon(
        map: KakaoMap,
        path: List<LatLng>,
        name: String,
        color: Int,
        regeionCode: String,
        isShow: Boolean,

    ): Polygon? {
        val closedPath = if (path.first() != path.last()) path + path.first() else path
        val points = MapPoints.fromLatLng(closedPath)
        val strokeColor =
            if (isShow) 0xB3D3D3D3.toInt() else 0x00FFFFFF.toInt() // true면 테두리 그려주고 false면 테두리 안그려줌

        // 폴리곤 생성 및 태그 등록
        val polygon = map.shapeManager?.layer?.addPolygon(
            PolygonOptions.from(points)
                .setTag(name)
                .setZOrder(20)
                .setStylesSet(
                    PolygonStylesSet.from(
                        PolygonStyles.from(
                            color,
                            2f,
                            strokeColor
                        )
                    )
                )
        )

        // 라벨 추가
        val center = getCentroidOfOuterRing(closedPath)
        val fontSize = 30

        Log.d("COLOR", "${isShow} ${strokeColor}")
        if (isShow) {
            Log.d("COLOR_INFO", "보여주세요")
            map.labelManager?.layer?.addLabel(
                LabelOptions.from(regeionCode, center)      // Label의 ID는 고유해야 하므로 지역 코드로 지정
                    .setTexts(                       // LabelTextBuilder 타입을 넘겨 줌
                        LabelTextBuilder()
                            .setTexts(name)             // 실제로 표시할 텍스트
                    )
                    .setRank(10)
                    .setStyles(
                        LabelStyle.from()
                            .setTextStyles(
                                fontSize,                 // 폰트 크기(px)
                                0xCC000000.toInt(),     // 글자색
                                0,              // 테두리 두께(px)
                                0x00000000     // 테두리 색
                            )
                            .setApplyDpScale(true)      // dp → px 변환 적용
                            .setPadding(fontSize.toFloat())      // 상하좌우 여백(px, dp 스케일 적용)
                    )
            )
        } else {
            map.labelManager?.layer?.removeAll()
        }

        return polygon;
    }

    // 외곽 폴리곤만 이용해서 라벨 위치 결정
    private fun getCentroidOfOuterRing(path: List<LatLng>): LatLng {
        val cleanedPath = if (path.first() != path.last()) path + path.first() else path

        var signedArea = 0.0
        var cx = 0.0
        var cy = 0.0

        for (i in 0 until cleanedPath.size - 1) {
            val x0 = cleanedPath[i].longitude
            val y0 = cleanedPath[i].latitude
            val x1 = cleanedPath[i + 1].longitude
            val y1 = cleanedPath[i + 1].latitude

            val a = x0 * y1 - x1 * y0
            signedArea += a
            cx += (x0 + x1) * a
            cy += (y0 + y1) * a
        }

        signedArea *= 0.5
        if (signedArea == 0.0) {
            val avgLat = cleanedPath.map { it.latitude }.average()
            val avgLng = cleanedPath.map { it.longitude }.average()
            return LatLng.from(avgLat, avgLng)
        }

        cx /= (6 * signedArea)
        cy /= (6 * signedArea)
        return LatLng.from(cy, cx)
    }


    private fun parseRing(ring: JsonArray): List<LatLng> {
        val path = ring.map { coordEl ->
            val arr = coordEl.asJsonArray
            LatLng.from(arr[1].asDouble, arr[0].asDouble)
        }

        // 시작점과 끝점이 다르면 다시 추가
        return if (path.first() != path.last()) path + path.first() else path
    }

}