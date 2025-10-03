package com.ssafy.a705.feature.record.map.data.repository

import android.util.Log
import com.ssafy.a705.feature.record.map.data.source.MapRemoteDataSource
import com.ssafy.a705.feature.record.map.data.remote.dto.MapUpdateRequest
import com.ssafy.a705.feature.record.map.domain.repository.MapRepository
import javax.inject.Inject

class MapRepositoryImpl @Inject constructor(
    private val remote: MapRemoteDataSource
) : MapRepository {
    override suspend fun getMapColors(): Map<String, String> {
        val list = remote.fetchColors()

        return list.associate { dto ->
            dto.locationCode to (
                    if (dto.locationColor.startsWith("#")) dto.locationColor
                    else "#${dto.locationColor}"
                    )
        }
    }

    override suspend fun updateMapColor(code: String, rawColor: String) {
        val color = rawColor.trim().removePrefix("#").removePrefix("0x").removePrefix("0X")
        val newColor = if (color.length == 8) color.drop(2) else color;
        Log.d("MapRepo", "color: $newColor")
        remote.patchColor(code.toLong(), newColor)
    }
}