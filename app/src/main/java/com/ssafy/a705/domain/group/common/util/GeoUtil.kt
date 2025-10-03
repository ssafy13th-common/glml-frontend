package com.ssafy.a705.domain.group.common.util

import android.content.Context
import android.location.Geocoder
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale

object GeoUtil {
    
    /**
     * 대한민국 영역으로 제한된 지오코딩을 수행합니다.
     * @param context Android Context
     * @param place 장소명 (예: "서울역", "부산역", "광화문")
     * @return Pair<latitude, longitude> 또는 null (찾지 못한 경우)
     */
    suspend fun geocodeKoreaOrNull(context: Context, place: String): Pair<Double, Double>? {
        return withContext(Dispatchers.IO) {
            try {
                // Geocoder 지원 여부 확인
                if (!Geocoder.isPresent()) {
                    Log.d("Geocoder", "Geocoder not supported on this device")
                    return@withContext null
                }
                
                val geocoder = Geocoder(context, Locale.KOREA)
                
                // 대한민국 바운딩 박스로 검색 영역 제한
                // lowerLeft: lat=33.0, lon=124.5
                // upperRight: lat=38.7, lon=131.9
                val addresses = geocoder.getFromLocationName(
                    place, 
                    1, 
                    33.0,  // lowerLeft latitude
                    124.5, // lowerLeft longitude
                    38.7,  // upperRight latitude
                    131.9  // upperRight longitude
                )
                
                if (addresses.isNullOrEmpty()) {
                    Log.d("Geocoder", "No results found for: $place")
                    return@withContext null
                }
                
                val address = addresses[0]
                val latitude = address.latitude
                val longitude = address.longitude
                
                Log.d("Geocoder", "result=$latitude,$longitude for $place")
                
                Pair(latitude, longitude)
                
            } catch (e: IOException) {
                Log.e("Geocoder", "IOException during geocoding: ${e.message}")
                null
            } catch (e: Exception) {
                Log.e("Geocoder", "Unexpected error during geocoding: ${e.message}")
                null
            }
        }
    }
}

