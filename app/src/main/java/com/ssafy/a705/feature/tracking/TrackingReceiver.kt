package com.ssafy.a705.feature.tracking

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TrackingReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {

            // 위치 업데이트 브로드캐스트 처리
            TrackingService.ACTION_LOCATION_UPDATE -> {
                val lat = intent.getDoubleExtra("latitude", 0.0)
                val lng = intent.getDoubleExtra("longitude", 0.0)
                val ts  = intent.getLongExtra("timestamp", 0L)

                TrackingSnapshotStore.append(
                    TrackingSnapshot(timestamp = ts, latitude = lat, longitude = lng)
                )
            }

            // 부팅 완료 시 직전 상태 복원
            Intent.ACTION_BOOT_COMPLETED -> {
                val pr = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    // DataStore에서 사용자가 트래킹을 켜둔 상태였는지 isTracking 확인
                    val wasTracking = TrackingPreferenceManager(context).isTracking.first()

                    // 앱 컨텍스트로 포그라운드 서비스 재시작
                    if (wasTracking) TrackingService.start(context.applicationContext)

                    pr.finish()
                }
            }

            else -> Unit
        }
    }
}