package com.ssafy.a705.feature.tracking

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.ssafy.a705.R
import com.ssafy.a705.common.navigation.MainActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class TrackingService : Service() {

    companion object {
        private const val CHANNEL_ID = "location_channel"
        const val ACTION_LOCATION_UPDATE = "com.ssafy.a705.ACTION_LOCATION_UPDATE"


        fun start(context: Context) {
            val intent = Intent(context, TrackingService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, TrackingService::class.java)
            context.stopService(intent)
        }
    }

    private lateinit var fusedClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    // 마지막으로 반영된 위치
    private var lastKept: Location? = null

    // 거리 컷 : m 단위
    private val minWalk = 10f
    private val minVehicle = 30f

    // 스파이크 의심점을 1개까지 보류해두었다가 다음 점과 비교해서 확인
    private var suspect: Location? = null

    // 최근 채택점 리스트 : 차량 이동 여부/방향 안정성 판단용
    private val window = ArrayDeque<Location>()

    // 튐/차량 판단 임계값(필요시 조절)
    private val JUMP_SPEED = 15f       // m/s 이상이면 "너무 빠름"으로 의심 (약 54km/h)
    private val VEHICLE_SPEED = 10f    // m/s 이상이 연속되면 차량 이동으로 간주 (약 36km/h)
    private val GOOD_ACC = 15f         // m 이하면 품질 양호
    private val JUMP_BAD_ACC = 30f     // m 이상이면 품질 불량으로 의심
    private val BEARING_STABLE = 25f   // 연속 샘플의 방향 변화 평균이 이하면 "안정"

    override fun onCreate() {
        super.onCreate()

        fusedClient = LocationServices.getFusedLocationProviderClient(this)

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5_000L                     // 위치 갱신 요청 간격
        ).apply {
            setMinUpdateIntervalMillis(2_000L)      // 위치 업데이트 최소 간격
            setMinUpdateDistanceMeters(minWalk)     // 10m 이상 이동해야 좌표 반영
        }.build()

        // 위치 갱신 콜백
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (location in result.locations) {
                    handleNewLocation(location)
                }
            }
        }

        // 알림 채널 만들기 : ForegroundService는 알림 필수
        createNotificationChannel()
        startForeground(1, createNotification())

        // 권한 체크를 사전에 해서 SecurityException 최소화
        val fine = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fine || coarse) {
            try {
                fusedClient.requestLocationUpdates(request, locationCallback, mainLooper)
            } catch (e: SecurityException) {
                Log.e("TrackingService", "위치 권한 없음: ${e.message}")
            }
        } else {
            Log.e("TrackingService", "필수 위치 권한이 없어서 위치 업데이트를 요청하지 않음")
        }
    }

    // 서비스가 종료돼도 자동 재시작하도록 명시
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isTrackingEnabled()) {
            cancelRestartAlarm()         // 잡혀있던 재시작 예약 제거
            stopSelf()
            return START_NOT_STICKY      // 시스템이 다시 살리지 않도록
        }
        return START_STICKY
    }

    // 사용자가 앱 자체를 스와이프로 종료해도 1.5초 뒤 자동 복구
    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!isTrackingEnabled()) {
            super.onTaskRemoved(rootIntent)  // 사용자가 끈 상태면 아무것도 안 함
            return
        }
        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val restartIntent = Intent(this, TrackingService::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pi = PendingIntent.getService(this, 1001, restartIntent, flags)

        val triggerAt = System.currentTimeMillis() + 1_500L

        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)

        super.onTaskRemoved(rootIntent)
    }

    // 재시작 알람 취소
    private fun cancelRestartAlarm() {
        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                (PendingIntent.FLAG_IMMUTABLE)
        val pi = PendingIntent.getService(this, 1001, Intent(this, TrackingService::class.java), flags)
        am.cancel(pi)
    }

    // DataStore의 isTracking 조회
    private fun isTrackingEnabled(): Boolean = runBlocking {
        TrackingPreferenceManager(this@TrackingService).isTracking.first()
    }

    private fun handleNewLocation(loc: Location) {
        // 정확도 컷
        if (loc.accuracy > 20f) {
            Log.d("TrackingService", "정확도 낮음(${loc.accuracy}m) -> 무시")
            return
        }

        // 최소 이동거리 컷
        lastKept?.let { kept ->
            val moved = kept.distanceTo(loc)
            val minDist = currentMinDistance()
            if (moved < minDist) {
                Log.d("TrackingService", "${"%.1f".format(moved)}m < ${"%.1f".format(minDist)}m -> 스킵(가까움)")
                return
            }
        }

        // GPS 에러 의심 : 마지막 채택점 대비 순간 속도 계산
        lastKept?.let { prev ->
            // 시간차는 elapsedRealtimeNanos 기반(시계 변경 영향 최소화)
            val dt = kotlin.math.max(
                0.001f,
                (loc.elapsedRealtimeNanos - prev.elapsedRealtimeNanos) / 1e9f
            )
            val dist = prev.distanceTo(loc)
            val v = dist / dt // m/s

            // 속도/방향/정확도로 스파이크 여부 판단
            val speedAccOk = if (loc.hasSpeedAccuracy())
                loc.speedAccuracyMetersPerSecond <= 3f else true
            val bearingOk = if (prev.hasBearing() && loc.hasBearing())
                bearingDelta(prev.bearing, loc.bearing) <= 90f else true

            val isSpike = v > JUMP_SPEED && (!speedAccOk || loc.accuracy > JUMP_BAD_ACC || !bearingOk)

            if (isSpike) {
                // 첫 의심이면 일단 보류
                if (suspect == null) {
                    suspect = loc
                    Log.d("TrackingService", "고속 의심점 보류 v=${"%.1f".format(v)} m/s, acc=${loc.accuracy}")
                    return
                } else {
                    // 직전 보류점 + 현재 점이 같은 추세면 차량으로 인정
                    val s = suspect!!
                    val d1 = prev.distanceTo(s)
                    val d2 = s.distanceTo(loc)
                    
                    // 유턴의 경우 느린 속도로 이동하므로 필터링 통과됨
                    val sameDir =
                        prev.hasBearing() && s.hasBearing() && loc.hasBearing() &&
                                bearingDelta(prev.bearing, s.bearing) < 45f &&
                                bearingDelta(s.bearing, loc.bearing) < 45f

                    val confirmVehicle = (d1 + d2) > 200f && sameDir &&
                            (s.accuracy < GOOD_ACC && loc.accuracy < GOOD_ACC)

                    if (!confirmVehicle) {
                        Log.d("TrackingService", "단발 스파이크로 판정(누적 ${(d1 + d2).toInt()}m, sameDir=$sameDir) -> 폐기")
                        suspect = null
                        return
                    } else {
                        // 차량으로 인정: 보류점부터 채택
                        Log.d("TrackingService", "차량 이동으로 인정 → 보류점부터 채택")
                        accept(s)   // 의심점 먼저 반영
                        suspect = null
                    }
                }
            } else {
                // 고속 스파이크가 아니면 보류 초기화
                suspect = null
            }
        }

        // 3) 최종 채택
        accept(loc)
    }

    // 현재 최소 이동거리 : 차량 추정 시 상향
    private fun currentMinDistance(): Float {
        return if (isVehicleNow()) {
            minVehicle
        } else {
            minWalk
        }
    }

    // 최근 채택 위치들을 보고 차량 속도인지 판단
    private fun isVehicleNow(): Boolean {
        if (window.size < 3) return false

        // 3개 이상이 연속 빠름 + 정확도 양호 + 방향 안정
        val fastCount = window.count { it.hasSpeed() && it.speed > VEHICLE_SPEED }
        val accOk = window.all { it.accuracy < GOOD_ACC }
        val dirOk = window.zipWithNext().all { (a, b) ->
            (!a.hasBearing() || !b.hasBearing()) || bearingDelta(a.bearing, b.bearing) < BEARING_STABLE
        }
        return fastCount >= 3 && accOk && dirOk
    }

    // 각도 차이(0~180)
    private fun bearingDelta(a: Float, b: Float): Float {
        val d = kotlin.math.abs(a - b) % 360f
        return if (d > 180f) 360f - d else d
    }

    // 채택 공통 처리: 마지막 채택점 갱신 + 윈도우 유지 + 스냅샷 생성 후 브로드캐스트
    private fun accept(loc: Location) {
        lastKept = loc
        window += loc
        while (window.size > 5) window.removeFirst()

        val snapshot = TrackingSnapshot(
            timestamp = loc.time,
            latitude = loc.latitude,
            longitude = loc.longitude
        )

        Log.d("TrackingService", "kept lat=${loc.latitude}, lon=${loc.longitude}, acc=${loc.accuracy}")

        // 실시간 UI 갱신 브로드캐스트
        broadcastLocation(snapshot)
    }

    private fun broadcastLocation(snapshot: TrackingSnapshot) {
        val intent = Intent(ACTION_LOCATION_UPDATE).apply {
            putExtra("timestamp", snapshot.timestamp)
            putExtra("latitude", snapshot.latitude)
            putExtra("longitude", snapshot.longitude)
        }
        // 다른 앱에서는 브로드캐스트 받지 못하도록 제한
        intent.setPackage(packageName)
        // 단일 앱에서 전달하므로 일반 sendBroadcast로 충분
        sendBroadcast(intent)
    }

    // 포그라운드 서비스 : 사용자에게 진행 중임을 알리는 알림 필요
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "위치 서비스",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "여행 경로 추적용 포그라운드 알림"
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val pi = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("여행 경로 기록 중")
            .setContentText("앱이 위치를 기록하고 있어요!")
            .setSmallIcon(R.drawable.logo_galaemalae)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pi)
            .build()
    }

    override fun onDestroy() {
        cancelRestartAlarm()
        super.onDestroy()
        fusedClient.removeLocationUpdates(locationCallback)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
