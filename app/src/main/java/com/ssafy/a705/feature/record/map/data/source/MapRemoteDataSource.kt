package com.ssafy.a705.feature.record.map.data.source

import com.ssafy.a705.feature.record.map.data.remote.api.MapApi
import com.ssafy.a705.feature.record.map.data.remote.dto.MapColors
import com.ssafy.a705.feature.record.map.data.remote.dto.MapUpdateRequest
import javax.inject.Inject

class MapRemoteDataSource @Inject constructor(
    private val api: MapApi
) {
    suspend fun fetchColors(): List<MapColors> =
        api.getMapColors().data?.colors.orEmpty()

    suspend fun patchColor(code: Long, rgb: String) =
        api.patchLocationColor(code, MapUpdateRequest(rgb))
}