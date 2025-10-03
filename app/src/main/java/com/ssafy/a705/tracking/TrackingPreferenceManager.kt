package com.ssafy.a705.tracking

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/*
Context에 dataStore 확장 프로퍼티 추가
 -> "tracking_prefs"라는 이름으로 Preference DataStore를 생성
 -> 앱 전체에서 context.dataStore로 접근 가능
 */
private val Context.dataStore by preferencesDataStore("tracking_prefs")

class TrackingPreferenceManager(private val context: Context) {

    // DataStore에서 사용할 Key 정의
    companion object {
        val IS_TRACKING = booleanPreferencesKey("is_tracking")
    }

    // 트래킹 여부를 Flow 형태로 관찰
    val isTracking: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[IS_TRACKING] ?: false }

    // 상태 저장
    suspend fun setTracking(isTracking: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[IS_TRACKING] = isTracking
        }
    }
}
