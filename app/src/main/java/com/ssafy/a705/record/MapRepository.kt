package com.ssafy.a705.record

import android.util.Log
import javax.inject.Inject

class MapRepository @Inject constructor(
    private val api: MapApi
) {
    suspend fun getMapColors(): Map<String, String> {
        val resp = api.getMapColors()
        val list = resp.data?.colors.orEmpty()
        return list.associate { dto ->
            dto.locationCode to (
                    if (dto.locationColor.startsWith("#")) dto.locationColor
                    else "#${dto.locationColor}"
                    )
        }
    }

    suspend fun updateMapColor(code: String, rawColor: String) {
        val color = rawColor.trim().removePrefix("#").removePrefix("0x").removePrefix("0X")
        val newColor = color.substring(2);
        Log.d("MapRepo", "color: $newColor")
        api.patchLocationColor(code.toLong(), MapUpdateRequest(newColor))
    }
}