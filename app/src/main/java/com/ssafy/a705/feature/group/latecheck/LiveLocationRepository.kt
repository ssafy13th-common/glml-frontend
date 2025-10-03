package com.ssafy.a705.feature.group.latecheck

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import com.ssafy.a705.common.network.sign.SessionManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LiveLocationRepository @Inject constructor(
    private val webSocketClient: LiveLocationWebSocketClient,
    private val sessionManager: SessionManager // SessionManager 추가
) {
    companion object {
        private const val TAG = "LiveLocationRepository"
        private const val LOCATION_UPDATE_INTERVAL = 10000L // 10초
    }

    private var locationManager: LocationManager? = null
    private var locationListener: LocationListener? = null
    private var currentGroupId: Long? = null
    private var lastLocation: Location? = null
    private var locationJob: Job? = null

    // 위치 데이터 스트림
    private val _locations = MutableStateFlow<Map<String, LiveLocationReceived>>(emptyMap())
    val locations: StateFlow<Map<String, LiveLocationReceived>> = _locations.asStateFlow()

    // 지각비 데이터 스트림
    private val _lateFees = MutableStateFlow<Map<String, Int>>(emptyMap())
    val lateFees: StateFlow<Map<String, Int>> = _lateFees.asStateFlow()

    // 연결 상태
    val connectionState: StateFlow<WebSocketConnectionState> = webSocketClient.connectionState

    // 현재 사용자의 이메일 정보
    private val currentUserEmail: String?
        get() = sessionManager.load()?.email

    fun start(groupId: Long, context: Context, jwt: String): Boolean {
        Log.d(TAG, "=== 위치 공유 시작 ===")
        Log.d(TAG, "그룹 ID: $groupId")
        Log.d(TAG, "JWT 토큰: ${jwt.take(20)}...")
        Log.d(TAG, "현재 사용자 이메일: $currentUserEmail")

        // 1. 위치 권한 확인
        if (!checkLocationPermission(context)) {
            Log.e(TAG, "❌ 위치 권한 없음")
            return false
        }
        Log.d(TAG, "✅ 위치 권한 확인됨")

        // 2. 웹소켓 연결
        Log.d(TAG, "🔌 웹소켓 연결 시작")
        webSocketClient.connect(jwt)

        // 3. 위치 수집 시작
        Log.d(TAG, "📍 위치 수집 시작")
        startLocationCollection(context, groupId)

        // 4. 수신 데이터 처리
        Log.d(TAG, "📡 수신 데이터 처리 시작")
        startDataCollection()

        currentGroupId = groupId
        Log.d(TAG, "=== 위치 공유 시작 완료 ===")
        return true
    }

    fun stop() {
        Log.d(TAG, "위치 공유 종료")
        locationJob?.cancel()
        webSocketClient.close()
        currentGroupId = null
        _locations.value = emptyMap()
        _lateFees.value = emptyMap()
    }

    private fun checkLocationPermission(context: Context): Boolean {
        return context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun startLocationCollection(context: Context, groupId: Long) {
        Log.d(TAG, "위치 수집 시작")

        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // 마지막 알려진 위치 즉시 전송
        lastLocation = try {
            locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        } catch (e: SecurityException) {
            Log.e(TAG, "위치 권한 오류로 마지막 위치를 가져올 수 없음", e)
            null
        }

        lastLocation?.let { location ->
            Log.d(TAG, "📍 마지막 알려진 위치: (${location.latitude}, ${location.longitude})")
            Log.d(TAG, "📤 웹소켓으로 위치 전송 시작")
            webSocketClient.sendLocation(groupId, location.latitude, location.longitude)
            Log.d(TAG, "📤 웹소켓 위치 전송 완료")

            // 자신의 위치도 _locations에 저장 (현재 사용자만)
            val myLocation = LiveLocationReceived(
                groupId = groupId,
                memberEmail = currentUserEmail ?: "me@local.com", // 실제 이메일 사용
                latitude = location.latitude,
                longitude = location.longitude,
                timestamp = TimeFmt.nowKst(),
                lateFee = 0 // 기본값
            )
            Log.d(TAG, "💾 내 위치 데이터 생성: ${myLocation.memberEmail}")

            _locations.value = _locations.value.toMutableMap().apply {
                // 현재 사용자 이메일로만 저장 (다른 사용자와 충돌 방지)
                currentUserEmail?.let { email ->
                    put(email, myLocation)
                    Log.d(TAG, "✅ 현재 사용자 위치를 이메일 키로 저장: $email")
                } ?: run {
                    // 이메일이 없는 경우에만 fallback 키 사용
                    put("me", myLocation)
                    put("current_user", myLocation)
                    Log.d(TAG, "⚠️ 이메일 없음, fallback 키 사용")
                }
            }
            Log.d(TAG, "💾 자신의 위치 저장 완료: (${location.latitude}, ${location.longitude})")
            Log.d(TAG, "📊 _locations 크기: ${_locations.value.size}")
            Log.d(TAG, "📊 _locations 키들: ${_locations.value.keys}")
        } ?: run {
            Log.w(TAG, "⚠️ 마지막 알려진 위치 없음")
        }

        // 위치 리스너 설정
        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                Log.d(TAG, "📍 위치 변경 감지: (${location.latitude}, ${location.longitude})")
                lastLocation = location
                currentGroupId?.let { groupId ->
                    Log.d(TAG, "📤 웹소켓으로 위치 변경 전송 시작")
                    webSocketClient.sendLocation(groupId, location.latitude, location.longitude)
                    Log.d(TAG, "📤 웹소켓 위치 변경 전송 완료")

                    // 자신의 위치도 _locations에 저장 (현재 사용자만)
                    val myLocation = LiveLocationReceived(
                        groupId = groupId,
                        memberEmail = currentUserEmail ?: "me@local.com", // 실제 이메일 사용
                        latitude = location.latitude,
                        longitude = location.longitude,
                        timestamp = TimeFmt.nowKst(),
                        lateFee = 0 // 기본값
                    )
                    Log.d(TAG, "💾 위치 변경 시 내 위치 데이터 생성: ${myLocation.memberEmail}")

                    _locations.value = _locations.value.toMutableMap().apply {
                        // 현재 사용자 이메일로만 저장 (다른 사용자와 충돌 방지)
                        currentUserEmail?.let { email ->
                            put(email, myLocation)
                            Log.d(TAG, "✅ 위치 변경 시 현재 사용자 위치를 이메일 키로 저장: $email")
                        } ?: run {
                            // 이메일이 없는 경우에만 fallback 키 사용
                            put("me", myLocation)
                            put("current_user", myLocation)
                            Log.d(TAG, "⚠️ 위치 변경 시 이메일 없음, fallback 키 사용")
                        }
                    }
                    Log.d(TAG, "💾 위치 변경 시 자신의 위치 저장: (${location.latitude}, ${location.longitude})")
                    Log.d(TAG, "📊 _locations 크기: ${_locations.value.size}")
                }
            }

            @Suppress("DEPRECATION")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        // 위치 업데이트 요청
        try {
            Log.d(TAG, "📍 GPS 위치 업데이트 요청 시작")
            locationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                LOCATION_UPDATE_INTERVAL,
                0f,
                locationListener!!
            )
            Log.d(TAG, "📍 Network 위치 업데이트 요청 시작")
            locationManager?.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                LOCATION_UPDATE_INTERVAL,
                0f,
                locationListener!!
            )
            Log.d(TAG, "✅ 위치 업데이트 요청 완료 (${LOCATION_UPDATE_INTERVAL}ms 간격)")
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ 위치 권한 오류", e)
        }
    }

    private fun startDataCollection() {
        locationJob = CoroutineScope(Dispatchers.IO).launch {
            Log.d(TAG, "📡 WebSocket 수신 데이터 처리 시작")

            // WebSocket에서 수신한 데이터 처리
            webSocketClient.incoming.collect { locationData ->
                Log.d(TAG, "=== 📡 WebSocket 수신 데이터 처리 ===")
                Log.d(TAG, "📧 수신된 멤버 이메일: ${locationData.memberEmail}")
                Log.d(TAG, "📍 수신된 위치: (${locationData.latitude}, ${locationData.longitude})")
                Log.d(TAG, "💰 수신된 지각비: ${locationData.lateFee}원")
                Log.d(TAG, "⏰ 수신된 시간: ${locationData.timestamp}")
                Log.d(TAG, "🆔 그룹 ID: ${locationData.groupId}")
                Log.d(TAG, "👤 현재 사용자 이메일: $currentUserEmail")
                Log.d(TAG, "❓ 수신된 데이터가 현재 사용자 데이터인가?: ${locationData.memberEmail == currentUserEmail}")

                // 이메일에서 닉네임 추출
                val nickname = locationData.memberEmail.substringBefore("@")
                Log.d(TAG, "🏷️ 추출된 닉네임: $nickname")

                // 위치 데이터 저장 (이메일을 우선 키로 사용)
                val beforeSize = _locations.value.size
                _locations.value = _locations.value.toMutableMap().apply {
                    put(locationData.memberEmail, locationData)
                    Log.d(TAG, "✅ 이메일 키로 위치 데이터 저장: ${locationData.memberEmail}")

                    // 닉네임은 보조 키로만 사용 (이메일이 없는 경우)
                    if (locationData.memberEmail.contains("@")) {
                        put(nickname, locationData)
                        Log.d(TAG, "✅ 닉네임 키로 위치 데이터 저장: $nickname")
                    } else {
                        Log.d(TAG, "⚠️ 이메일 형식이 아님, 닉네임 키 저장 건너뜀")
                    }
                }

                // 지각비 데이터 저장 (이메일을 우선 키로 사용)
                val beforeLateFeeSize = _lateFees.value.size
                _lateFees.value = _lateFees.value.toMutableMap().apply {
                    put(locationData.memberEmail, locationData.lateFee)
                    Log.d(TAG, "✅ 이메일 키로 지각비 데이터 저장: ${locationData.memberEmail} = ${locationData.lateFee}원")

                    // 닉네임은 보조 키로만 사용 (이메일이 없는 경우)
                    if (locationData.memberEmail.contains("@")) {
                        put(nickname, locationData.lateFee)
                        Log.d(TAG, "✅ 닉네임 키로 지각비 데이터 저장: $nickname = ${locationData.lateFee}원")
                    }
                }

                Log.d(TAG, "💾 데이터 저장 완료")
                Log.d(TAG, "📊 _locations 크기: ${beforeSize} → ${_locations.value.size}")
                Log.d(TAG, "📊 _lateFees 크기: ${beforeLateFeeSize} → ${_lateFees.value.size}")
                Log.d(TAG, "📊 _locations 키들: ${_locations.value.keys}")
                Log.d(TAG, "📊 _lateFees 키들: ${_lateFees.value.keys}")
                Log.d(TAG, "=== 📡 WebSocket 수신 데이터 처리 완료 ===")
            }
        }
    }

    fun cleanup() {
        Log.d(TAG, "위치 수집 정리")
        locationJob?.cancel()
        locationListener?.let { listener ->
            locationManager?.removeUpdates(listener)
        }
        locationManager = null
        locationListener = null
    }
}
