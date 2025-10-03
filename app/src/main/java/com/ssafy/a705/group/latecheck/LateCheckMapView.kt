package com.ssafy.a705.group.latecheck

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.Label
import com.kakao.vectormap.label.LabelLayer
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import com.kakao.vectormap.label.LabelTextBuilder
import com.kakao.vectormap.label.LabelTextStyle
import com.ssafy.a705.R
import com.ssafy.a705.components.KakaoMapView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Canvas로 원형 마커 생성
fun createColoredMarker(color: Int): Bitmap {
    val size = 60
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint().apply {
        this.color = color
        isAntiAlias = true
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 2.2f, paint)
    return bitmap
}

@Composable
fun LateCheckMapView(
    modifier: Modifier = Modifier,
    meetingPlace: MeetingPlace?,
    members: List<MemberStatus>
) {
    var kakaoMap by remember { mutableStateOf<KakaoMap?>(null) }
    var hasMovedCamera by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    KakaoMapView(
        modifier = modifier,
        onMapReady = { map ->
            kakaoMap = map
            Log.d("LateCheckMapView", "지도 준비 완료")

            // 마커 클릭 시 닉네임 표시
            map.setOnLabelClickListener(object : KakaoMap.OnLabelClickListener {
                override fun onLabelClicked(
                    kakaoMap: KakaoMap,
                    layer: LabelLayer,
                    label: Label
                ): Boolean {
                    val nickname = label.tag as? String ?: return false
                    val latLng = label.position

                    // 텍스트 스타일 (LabelTextBuilder)
                    val textBuilder = LabelTextBuilder()
                    textBuilder.setTexts(nickname)

                    val textStyle = LabelStyles.from(
                        LabelStyle.from(LabelTextStyle.from(28, Color.WHITE))
                    )

                    // 텍스트 라벨 옵션
                    val textLabelOptions = LabelOptions.from(latLng)
                        .setStyles(textStyle)
                        .setTexts(textBuilder)
                        .setRank(999) // 항상 위에 표시

                    val textLabel = layer.addLabel(textLabelOptions)
                    // 텍스트 위치를 마커 안으로 이동
                    textLabel.changePixelOffset(0f, -30f)

                    // 2초 후 자동 제거
                    coroutineScope.launch {
                        delay(2000)
                        layer.remove(textLabel)
                    }

                    return true
                }
            })
        }
    )

    // 멤버 위치가 변경될 때마다 마커 업데이트
    LaunchedEffect(kakaoMap, meetingPlace, members) {
        val map = kakaoMap ?: return@LaunchedEffect
        val place = meetingPlace

        Log.d("LateCheckMapView", "=== 🗺️ 마커 업데이트 시작 ===")
        Log.d("LateCheckMapView", "🗺️ 지도 상태: ${if (map != null) "준비됨" else "준비 안됨"}")

        if (place != null) {
            Log.d("LateCheckMapView", "🏁 모임장소: ${place.name} (${place.latitude}, ${place.longitude})")
        } else {
            Log.d("LateCheckMapView", "⚠️ 모임장소: 설정되지 않음")
        }
        Log.d("LateCheckMapView", "👥 멤버 수: ${members.size}")

        // 각 멤버의 위치 정보 로그
        members.forEachIndexed { index, member ->
            Log.d("LateCheckMapView", "👤 멤버${index + 1}: ${member.nickname}")
            Log.d("LateCheckMapView", "  📧 이메일: ${member.email}")
            Log.d("LateCheckMapView", "  📍 위치: (${member.latitude}, ${member.longitude})")
            Log.d("LateCheckMapView", "  💰 지각비: ${member.lateFee}원")
            Log.d("LateCheckMapView", "  🎨 색상: ${member.color}")
            Log.d("LateCheckMapView", "  ✅ 마커 표시 여부: ${member.latitude != 0.0 && member.longitude != 0.0}")
        }

        val labelManager = map.labelManager ?: return@LaunchedEffect
        val layer = labelManager.layer ?: return@LaunchedEffect

        // 기존 마커 모두 제거
        Log.d("LateCheckMapView", "🗑️ 기존 마커 모두 제거")
        layer.removeAll()

        // 모임 장소가 유효한 경우에만 마커 추가
        place?.let { p ->
            if (p.latitude != 0.0 && p.longitude != 0.0) {
                Log.d("LateCheckMapView", "🏁 모임장소 마커 추가 시작")
                // 모임 장소 빨간 깃발
                val meetingStyles = labelManager.addLabelStyles(
                    LabelStyles.from(LabelStyle.from(R.drawable.flag_red))
                ) ?: return@LaunchedEffect
                val meetingLabel = LabelOptions.from(LatLng.from(p.latitude, p.longitude)).setStyles(meetingStyles)
                layer.addLabel(meetingLabel)
                Log.d("LateCheckMapView", "✅ 모임장소 라벨 추가 완료: ${p.name} (${p.latitude}, ${p.longitude})")

                // 초기 카메라 이동(모임 장소 포커싱)
                if (!hasMovedCamera) {
                    Log.d("LateCheckMapView", "📷 카메라 이동 시작")
                    map.moveCamera(CameraUpdateFactory.newCenterPosition(LatLng.from(p.latitude, p.longitude)))
                    map.moveCamera(CameraUpdateFactory.zoomTo(10))
                    hasMovedCamera = true
                    Log.d("LateCheckMapView", "✅ 카메라 이동 완료")
                }
            } else {
                Log.d("LateCheckMapView", "⚠️ 모임장소 좌표가 유효하지 않음: (${p.latitude}, ${p.longitude})")
            }
        } ?: run {
            Log.d("LateCheckMapView", "⚠️ 모임장소 정보 없음")
        }

        // 멤버 마커 (아이콘 라벨)
        var addedMarkerCount = 0
        members.forEachIndexed { index, member ->
            // 위치가 유효한 경우에만 마커 추가
            if (member.latitude != 0.0 && member.longitude != 0.0) {
                Log.d("LateCheckMapView", "=== 👤 멤버 마커 추가 시작 ===")
                Log.d("LateCheckMapView", "👤 멤버${index + 1}: ${member.nickname}")
                Log.d("LateCheckMapView", "📧 이메일: ${member.email}")
                Log.d("LateCheckMapView", "📍 위치: (${member.latitude}, ${member.longitude})")
                Log.d("LateCheckMapView", "💰 지각비: ${member.lateFee}원")
                Log.d("LateCheckMapView", "🎨 색상: ${member.color}")

                val bitmap = createColoredMarker(member.color.toArgb())
                val style = labelManager.addLabelStyles(
                    LabelStyles.from(LabelStyle.from(bitmap))
                ) ?: return@LaunchedEffect

                val iconLabel = LabelOptions.from(
                    LatLng.from(member.latitude, member.longitude)
                ).setStyles(style)
                    .setTag(member.nickname)
                    .setRank((index * 10).toLong()) // 기본 랭크

                layer.addLabel(iconLabel)
                addedMarkerCount++
                Log.d("LateCheckMapView", "✅ 멤버${index + 1} 마커 추가 완료: ${member.nickname}")
                Log.d("LateCheckMapView", "=== 👤 멤버 마커 추가 완료 ===")
            } else {
                Log.d("LateCheckMapView", "⚠️ 멤버 ${member.nickname} 위치 정보 없음 - 마커 추가 건너뜀")
            }
        }

        Log.d("LateCheckMapView", "=== 🗺️ 마커 업데이트 완료 ===")
        Log.d("LateCheckMapView", "📊 총 추가된 마커 수: ${addedMarkerCount}개")
        Log.d("LateCheckMapView", "📊 총 멤버 수: ${members.size}명")
        Log.d("LateCheckMapView", "📊 마커 추가 성공률: ${if (members.size > 0) (addedMarkerCount * 100 / members.size) else 0}%")
    }
}
