package com.ssafy.a705.components

import android.content.Context
import android.view.View
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.kakao.vectormap.*

@Composable
fun KakaoMapView(
    modifier: Modifier = Modifier,
    onMapReady: (KakaoMap) -> Unit = {},     // 상위 컴포저블에서 지도 객체를 사용할 수 있도록
    onViewReady: (View) -> Unit = {}
) {
    AndroidView(
        factory = { ctx: Context ->
            val mapView = MapView(ctx)

            mapView.start(
                // 콜백 - 종료, 에러
                object : MapLifeCycleCallback() {
                    override fun onMapDestroy() {}
                    override fun onMapError(e: Exception) {
                        e.printStackTrace()
                    }
                },
                // 콜백 - 지도 준비 완료
                object : KakaoMapReadyCallback() {
                    override fun onMapReady(kakaoMap: KakaoMap) {
                        kakaoMap.isPoiVisible = false       // POI 끄기
                        MapOverlay.entries.forEach { overlay ->
                            kakaoMap.hideOverlay(overlay.name)  // MapOverlay 끄기
                        }

                        onMapReady(kakaoMap) // 부모에게 지도 반환
                    }
                }
            )

            onViewReady(mapView)
            mapView     // UI에 실제로 붙일 View 리턴
        },
        modifier = modifier
    )
}