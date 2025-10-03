package com.ssafy.a705.record

import android.graphics.Point
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.graphics.toColorInt
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapView
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.ssafy.a705.components.ColorPickerDialog
import com.ssafy.a705.components.HeaderRow
import com.ssafy.a705.components.KakaoMapView
import com.ssafy.a705.navigation.DoubleBackToExitHandler
import com.ssafy.a705.record.geojson.GeoJsonDrawer
import com.ssafy.a705.record.geojson.GeoJsonLoader
import com.ssafy.a705.record.geojson.GeoJsonParser

@Composable
fun MapScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var mapView: MapView? by remember { mutableStateOf(null) }
    var selectedRegion by remember { mutableStateOf<Triple<String, String, LatLng>?>(null) }    // 터치된 영역 이름
    var kMap by remember { mutableStateOf<KakaoMap?>(null) }
    var screenPoint by remember { mutableStateOf<Point?>(null) }
    val mapViewModel: MapViewModel = hiltViewModel()
    val colorMap by mapViewModel.colorMap
    var showColorPicker by remember { mutableStateOf(false) }
    val fcState = remember { mutableStateOf<GeoJsonParser.FeatureCollection?>(null) }
    var updatedCode: String? by remember { mutableStateOf(null) }
    var prevZoom: Int? by remember {mutableStateOf(null)}

    val currentDestination = navController.currentBackStackEntryAsState().value?.destination?.route

    if (currentDestination == RecordNavRoutes.Map) {
        DoubleBackToExitHandler()
    }

    // lifecycle에 맞춰 onResume/onPause 자동 처리
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView?.resume()
                Lifecycle.Event.ON_PAUSE -> mapView?.pause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            HeaderRow(
                text = "여행 기록",
                showText = true,
                showLeftButton = false,
                menuActions = emptyList()
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            KakaoMapView(
                modifier = Modifier.fillMaxSize(),
                onMapReady = { kakaoMap ->
                    kMap = kakaoMap

                    // 지도 초기 위치
                    kakaoMap.moveCamera(
                        // todo: 전체 지도로 시작
                        CameraUpdateFactory.newCenterPosition(LatLng.from(36.5, 128.0))
                    )
                    kakaoMap.moveCamera(CameraUpdateFactory.zoomTo(7))

                    // GeoJson 불러오기 및 그리기
                    val rawJson = GeoJsonLoader.loadFromAssets(context, "시군.geojson")
                    val fc = GeoJsonParser.parse(rawJson)
                    fcState.value = fc

                    val rawSidoJson = GeoJsonLoader.loadFromAssets(context, "법정구역_시도_simplified.geojson")
                    val sidoFC = GeoJsonParser.parse(rawSidoJson)

                    sidoFC.features.forEach { feature ->
                        val code = feature.properties["CTPRVN_CD"] as? String ?: return@forEach
                        val colorInt = "#FFFFFFFF".toColorInt()
                        GeoJsonDrawer.drawSidoFeature(kakaoMap, feature, colorInt, true)
                    }

                    fc.features.forEach { feature ->
                        val code = feature.properties["ADMIN_CODE"] as? String ?: return@forEach
                        val colorHex = colorMap[code] ?: "#00FFFFFF" //색 불러와서 있으면 그거 쓰고 아니면 흰색
                        val colorInt = colorHex.toColorInt()
                        GeoJsonDrawer.drawFeature(kakaoMap, feature, colorInt, null, false)
                    }

                    // 라벨 클릭 처리
                    kakaoMap.setOnLabelClickListener { _, _, label ->
                        val code = label.labelId
                        val name = label.texts.firstOrNull() ?: code
                        val latLng = label.position
                        selectedRegion = Triple(code, name, latLng)
                        screenPoint = kakaoMap.toScreenPoint(latLng)
                        true
                    }

                    // 메뉴 닫기
                    kakaoMap.setOnMapClickListener { _, _, _, poi ->
                        selectedRegion = null
                        screenPoint = null
                    }

                    // 지도 이동 시 화면 좌표 갱신
                    kakaoMap.setOnCameraMoveEndListener { kakaoMap, cameraPosition, gestureType ->
                        selectedRegion?.third?.let { latLng ->
                            screenPoint = kakaoMap.toScreenPoint(latLng)
                        }

                        val zoom = cameraPosition.zoomLevel
                        Log.d("ZOOM", "$zoom")
                        if(prevZoom != null){
                            if(zoom >= 9 && prevZoom!! < 9) { // 그려주기
                                fc.features.forEach { feature ->
                                    val code = feature.properties["ADMIN_CODE"] as? String ?: return@forEach
                                    val colorHex = colorMap[code] ?: "#00FFFFFF"
                                    val colorInt = colorHex.toColorInt()
                                    GeoJsonDrawer.drawFeature(kakaoMap, feature, colorInt, code, true)
                                }
                            }else if(zoom < 9 && prevZoom!! >= 9){ // 안그려주기
                                fc.features.forEach { feature ->
                                    val code = feature.properties["ADMIN_CODE"] as? String ?: return@forEach
                                    val colorHex = colorMap[code] ?: "#00FFFFFF"
                                    val colorInt = colorHex.toColorInt()
                                    GeoJsonDrawer.drawFeature(kakaoMap, feature, colorInt, code, false)
                                }
                            }

                            prevZoom = zoom
                        }
                        else{
                            if(zoom >= 9) { // 그려주기
                                fc.features.forEach { feature ->
                                    val code = feature.properties["ADMIN_CODE"] as? String ?: return@forEach
                                    val colorHex = colorMap[code] ?: "#00FFFFFF"
                                    val colorInt = colorHex.toColorInt()
                                    GeoJsonDrawer.drawFeature(kakaoMap, feature, colorInt, code, true)
                                }
                            }else{ // 안그려주기
                                fc.features.forEach { feature ->
                                    val code = feature.properties["ADMIN_CODE"] as? String ?: return@forEach
                                    val colorHex = colorMap[code] ?: "#00FFFFFF"
                                    val colorInt = colorHex.toColorInt()
                                    GeoJsonDrawer.drawFeature(kakaoMap, feature, colorInt, code, false)
                                }
                            }

                            prevZoom = zoom
                        }
                    }
                }
            )

            val density = LocalDensity.current

            RecordTabBar(
                currentTab = "map",
                onMapClick = { /* 현재 화면이므로 기능 X */ },
                onRecordClick = { navController.navigate(RecordNavRoutes.List) }
            )

            // screenPoint 상태를 직접 쓰기
            screenPoint?.let { pt ->
                // selectedRegion 의 이름도 가져오기
                val code = selectedRegion?.first ?: return@let
                val name = selectedRegion?.second ?: return@let

                val xDp = with(density) { pt.x.toDp() }
                val yDp = with(density) { pt.y.toDp() }

                val colorHex = colorMap[code] ?: "#FFFFFFFF"
                val isColored = colorHex != "#FFFFFFFF"

                var menuWidthDp by remember { mutableStateOf(198f) }

                MapMenu(
                    modifier = Modifier
                        .absoluteOffset(
                            x = xDp - menuWidthDp.dp / 2 - 12.dp,
                            y = yDp - 74.35.dp
                        )
                        .zIndex(1f),
                    onWriteClick = {
                        // 네비게이션에 지역명 저장
                        navController.currentBackStackEntry?.savedStateHandle?.set("code", code)
                        navController.currentBackStackEntry?.savedStateHandle?.set("location", name)
                        navController.navigate(RecordNavRoutes.Create)
                    },
                    onListClick = {
                        navController.navigate("${RecordNavRoutes.List}/$code")
                    },
                    onColorClick = {
                        showColorPicker = true
                    },
                    isColored = isColored,
                    onMeasuredWidthDp = { w -> menuWidthDp = w }
                )
            }

            if (showColorPicker) {
                ColorPickerDialog(
                    onDismiss = { showColorPicker = false },
                    onColorSelected = { hexColor ->
                        showColorPicker = false
                        val code = selectedRegion?.first ?: return@ColorPickerDialog
                        mapViewModel.updateColor(code, hexColor)
                        updatedCode = code
                    }
                )
            }
        }
    }

    // 색 변경 시 재렌더링
    LaunchedEffect(colorMap) {
        val kakaoMap = kMap ?: return@LaunchedEffect
        val fc = fcState.value ?: return@LaunchedEffect

        val targetFeature = fc.features.find { feature ->
            val code = feature.properties["ADMIN_CODE"] as? String
            code == updatedCode
        }

        targetFeature?.let { feature ->
            val colorHex = colorMap[updatedCode] ?: "#00FFFFFF"
            val colorInt = colorHex.toColorInt()
            GeoJsonDrawer.drawFeature(kakaoMap, feature, colorInt, updatedCode, true)
        }
    }
}
