package com.ssafy.a705.feature.group.latecheck

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.ssafy.a705.common.navigation.MainActivity
import com.ssafy.a705.R
import com.ssafy.a705.common.network.sign.SessionManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class LiveLocationService : Service() {

    companion object {
        private const val TAG = "LiveLocationService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "live_location_channel"
        private const val CHANNEL_NAME = "실시간 위치 공유"
        private const val LOCATION_UPDATE_INTERVAL = 10000L // 10초
        const val ACTION_LOCATION_UPDATE = "com.ssafy.a705.LIVE_LOCATION_UPDATE"
        const val ACTION_WEBSOCKET_UPDATE = "com.ssafy.a705.WEBSOCKET_UPDATE"

        // 싱글톤 인스턴스
        @Volatile
        private var instance: LiveLocationService? = null

        fun getInstance(): LiveLocationService? = instance

        fun start(context: Context, groupId: Long, jwt: String) {
            val intent = Intent(context, LiveLocationService::class.java).apply {
                putExtra("groupId", groupId)
                putExtra("jwt", jwt)
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, LiveLocationService::class.java)
            context.stopService(intent)
        }
    }

    private lateinit var webSocketClient: LiveLocationWebSocketClient
    private lateinit var sessionManager: SessionManager

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
    private val _connectionState = MutableStateFlow<WebSocketConnectionState>(WebSocketConnectionState.Idle)
    val connectionState: StateFlow<WebSocketConnectionState> = _connectionState.asStateFlow()

    // 현재 사용자의 이메일 정보
    private val currentUserEmail: String?
        get() = sessionManager.load()?.email

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "=== 🔧 LiveLocationService 생성 ===")

        // 싱글톤 인스턴스 설정
        instance = this

        // 의존성 직접 생성
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        webSocketClient = LiveLocationWebSocketClient(okHttpClient)
        sessionManager = SessionManager(this)

        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "=== 🚀 LiveLocationService 시작 ===")

        val groupId = intent?.getLongExtra("groupId", -1L) ?: -1L
        val jwt = intent?.getStringExtra("jwt") ?: ""

        if (groupId != -1L && jwt.isNotEmpty()) {
            Log.d(TAG, "🔧 서비스 시작 파라미터: groupId=$groupId, jwt=${jwt.take(20)}...")
            startLocationSharing(groupId, jwt)
        } else {
            Log.e(TAG, "❌ 서비스 시작 파라미터 누락: groupId=$groupId, jwt=${jwt.take(20)}...")
            stopSelf()
            return START_NOT_STICKY
        }

        // 서비스가 강제 종료되면 자동으로 재시작
        return START_STICKY
    }

    private fun startLocationSharing(groupId: Long, jwt: String) {
        Log.d(TAG, "=== 📍 위치 공유 시작 ===")
        Log.d(TAG, "그룹 ID: $groupId")
        Log.d(TAG, "JWT 토큰: ${jwt.take(20)}...")
        Log.d(TAG, "현재 사용자 이메일: $currentUserEmail")

        currentGroupId = groupId

        // 1. Foreground Service 시작
        startForeground(NOTIFICATION_ID, createNotification("위치 공유 중..."))

        // 2. 웹소켓 연결
        Log.d(TAG, "🔌 웹소켓 연결 시작")
        webSocketClient.connect(jwt)

        // 3. 위치 수집 시작
        Log.d(TAG, "📍 위치 수집 시작")
        startLocationCollection(groupId)

        // 4. 수신 데이터 처리
        Log.d(TAG, "📡 수신 데이터 처리 시작")
        startDataCollection()

        Log.d(TAG, "=== 📍 위치 공유 시작 완료 ===")
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "실시간 위치 공유 알림"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "✅ 알림 채널 생성 완료")
        }
    }

    private fun createNotification(content: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("실시간 위치 공유")
            .setContentText(content)
            .setSmallIcon(R.drawable.location)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()
    }

    private fun updateNotification(content: String) {
        val notification = createNotification(content)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
        Log.d(TAG, "📱 알림 업데이트: $content")
    }

    private fun startLocationCollection(groupId: Long) {
        Log.d(TAG, "📍 위치 수집 시작")

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        // 권한 체크
        val fine = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fine && !coarse) {
            Log.e(TAG, "❌ 위치 권한 없음")
            return
        }

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
                memberEmail = currentUserEmail ?: "me@local.com",
                latitude = location.latitude,
                longitude = location.longitude,
                timestamp = TimeFmt.nowKst(),
                lateFee = 0
            )
            Log.d(TAG, "💾 내 위치 데이터 생성: ${myLocation.memberEmail}")

            _locations.value = _locations.value.toMutableMap().apply {
                currentUserEmail?.let { email ->
                    put(email, myLocation)
                    Log.d(TAG, "✅ 현재 사용자 위치를 이메일 키로 저장: $email")
                } ?: run {
                    put("me", myLocation)
                    put("current_user", myLocation)
                    Log.d(TAG, "⚠️ 이메일 없음, fallback 키 사용")
                }
            }
            Log.d(TAG, "💾 자신의 위치 저장 완료: (${location.latitude}, ${location.longitude})")
            Log.d(TAG, "📊 _locations 크기: ${_locations.value.size}")

            // 알림 업데이트
            updateNotification("위치 전송 중... (${location.latitude}, ${location.longitude})")

            // 브로드캐스트로 위치 업데이트 전송
            broadcastLocationUpdate(myLocation)
        } ?: run {
            Log.w(TAG, "⚠️ 마지막 알려진 위치 없음")
            updateNotification("위치 정보 수집 중...")
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
                        memberEmail = currentUserEmail ?: "me@local.com",
                        latitude = location.latitude,
                        longitude = location.longitude,
                        timestamp = TimeFmt.nowKst(),
                        lateFee = 0
                    )
                    Log.d(TAG, "💾 위치 변경 시 내 위치 데이터 생성: ${myLocation.memberEmail}")

                    _locations.value = _locations.value.toMutableMap().apply {
                        currentUserEmail?.let { email ->
                            put(email, myLocation)
                            Log.d(TAG, "✅ 위치 변경 시 현재 사용자 위치를 이메일 키로 저장: $email")
                        } ?: run {
                            put("me", myLocation)
                            put("current_user", myLocation)
                            Log.d(TAG, "⚠️ 위치 변경 시 이메일 없음, fallback 키 사용")
                        }
                    }
                    Log.d(TAG, "💾 위치 변경 시 자신의 위치 저장: (${location.latitude}, ${location.longitude})")
                    Log.d(TAG, "📊 _locations 크기: ${_locations.value.size}")

                    // 알림 업데이트
                    updateNotification("위치 업데이트: (${location.latitude}, ${location.longitude})")

                    // 브로드캐스트로 위치 업데이트 전송
                    broadcastLocationUpdate(myLocation)
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

            webSocketClient.incoming.collect { locationData ->
                Log.d(TAG, "=== 📡 WebSocket 수신 데이터 처리 ===")
                Log.d(TAG, "📧 수신된 멤버 이메일: ${locationData.memberEmail}")
                Log.d(TAG, "📍 수신된 위치: (${locationData.latitude}, ${locationData.longitude})")
                Log.d(TAG, "💰 수신된 지각비: ${locationData.lateFee}원")
                Log.d(TAG, "⏰ 수신된 시간: ${locationData.timestamp}")
                Log.d(TAG, "🆔 그룹 ID: ${locationData.groupId}")
                Log.d(TAG, "👤 현재 사용자 이메일: $currentUserEmail")
                Log.d(TAG, "❓ 수신된 데이터가 현재 사용자 데이터인가?: ${locationData.memberEmail == currentUserEmail}")

                val nickname = locationData.memberEmail.substringBefore("@")
                Log.d(TAG, "🏷️ 추출된 닉네임: $nickname")

                val beforeSize = _locations.value.size
                _locations.value = _locations.value.toMutableMap().apply {
                    put(locationData.memberEmail, locationData)
                    Log.d(TAG, "✅ 이메일 키로 위치 데이터 저장: ${locationData.memberEmail}")

                    if (locationData.memberEmail.contains("@")) {
                        put(nickname, locationData)
                        Log.d(TAG, "✅ 닉네임 키로 위치 데이터 저장: $nickname")
                    } else {
                        Log.d(TAG, "⚠️ 이메일 형식이 아님, 닉네임 키 저장 건너뜀")
                    }
                }

                val beforeLateFeeSize = _lateFees.value.size
                _lateFees.value = _lateFees.value.toMutableMap().apply {
                    put(locationData.memberEmail, locationData.lateFee)
                    Log.d(TAG, "✅ 이메일 키로 지각비 데이터 저장: ${locationData.memberEmail} = ${locationData.lateFee}원")

                    if (locationData.memberEmail.contains("@")) {
                        put(nickname, locationData.lateFee)
                        Log.d(TAG, "✅ 닉네임 키로 지각비 데이터 저장: $nickname = ${locationData.lateFee}원")
                    }
                }

                Log.d(TAG, "💾 데이터 저장 완료")
                Log.d(TAG, "📊 _locations 크기: ${beforeSize} → ${_locations.value.size}")
                Log.d(TAG, "📊 _lateFees 크기: ${beforeLateFeeSize} → ${_lateFees.value.size}")

                // 브로드캐스트로 WebSocket 데이터 전송
                broadcastWebSocketUpdate(locationData)

                Log.d(TAG, "=== 📡 WebSocket 수신 데이터 처리 완료 ===")
            }
        }
    }

    private fun broadcastLocationUpdate(location: LiveLocationReceived) {
        val intent = Intent(ACTION_LOCATION_UPDATE).apply {
            putExtra("groupId", location.groupId)
            putExtra("memberEmail", location.memberEmail)
            putExtra("latitude", location.latitude)
            putExtra("longitude", location.longitude)
            putExtra("timestamp", location.timestamp)
            putExtra("lateFee", location.lateFee)
        }
        intent.setPackage(packageName)
        sendBroadcast(intent)
        Log.d(TAG, "📡 위치 업데이트 브로드캐스트 전송: ${location.memberEmail}")
    }

    private fun broadcastWebSocketUpdate(location: LiveLocationReceived) {
        val intent = Intent(ACTION_WEBSOCKET_UPDATE).apply {
            putExtra("groupId", location.groupId)
            putExtra("memberEmail", location.memberEmail)
            putExtra("latitude", location.latitude)
            putExtra("longitude", location.longitude)
            putExtra("timestamp", location.timestamp)
            putExtra("lateFee", location.lateFee)
        }
        intent.setPackage(packageName)
        sendBroadcast(intent)
        Log.d(TAG, "📡 WebSocket 업데이트 브로드캐스트 전송: ${location.memberEmail}")
    }

    fun stopLocationSharing() {
        Log.d(TAG, "=== 🛑 위치 공유 종료 ===")

        locationJob?.cancel()
        locationListener?.let { listener ->
            locationManager?.removeUpdates(listener)
        }
        webSocketClient.close()

        locationManager = null
        locationListener = null
        currentGroupId = null
        _locations.value = emptyMap()
        _lateFees.value = emptyMap()

        // Foreground Service 종료
        stopForeground(true)
        stopSelf()

        Log.d(TAG, "=== 🛑 위치 공유 종료 완료 ===")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "=== 🔧 LiveLocationService 소멸 ===")

        // 싱글톤 인스턴스 정리
        instance = null

        stopLocationSharing()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
