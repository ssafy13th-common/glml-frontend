package com.ssafy.a705.tracking

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.ActivityNotFoundException
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.ImageLoader
import coil.request.ImageRequest
import coil.size.Precision
import coil.transform.CircleCropTransformation
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.label.Label
import com.kakao.vectormap.label.TrackingManager
import com.kakao.vectormap.route.RouteLine
import com.kakao.vectormap.route.RouteLineOptions
import com.kakao.vectormap.route.RouteLineSegment
import com.kakao.vectormap.route.RouteLineStyle
import com.kakao.vectormap.route.RouteLineStyles
import com.ssafy.a705.R
import com.ssafy.a705.components.HeaderRow
import com.ssafy.a705.components.KakaoMapView
import com.ssafy.a705.controller.viewmodel.MyPageViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun TrackingScreen(
    navController: NavController,
    trackingViewModel: TrackingViewModel = hiltViewModel(),
    myPageViewModel: MyPageViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    // UI 상태
    var showInfo by remember { mutableStateOf(false) }              // info 버튼을 눌렀는지 확인
    val locations by trackingViewModel.locations.collectAsState()       // 사용자 위치 트래킹

    val isTracking by trackingViewModel.isTracking.collectAsState()     // 트래킹 여부 확인
    LaunchedEffect(isTracking) {
        if (isTracking) {
            TrackingService.start(context) // idempotent
        }
    }

    // 백그라운드 권한 보유 여부 (API 29 미만은 true로 간주)
    var bgGranted by remember { mutableStateOf(isBackgroundLocationGranted(context)) }

    // 설정 다녀왔을 때 재확인 : Resume 시점
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) {
                bgGranted = isBackgroundLocationGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    if (bgGranted) {
        TrackingAfterPermissionGrantedContent(
            navController = navController,
            trackingViewModel = trackingViewModel,
            myPageViewModel = myPageViewModel,
            isTracking = isTracking,
            locations = locations,
            showInfo = showInfo,
            setShowInfo = { showInfo = it },
            context = context
        )
    } else {
        // 권한 전용 화면 (지도/버튼 없이 안내 + 설정 이동)
        BackgroundLocationPermissionScreen(
            onOpenSettings = { openAppDetailsSettings(context) },
            onRefreshAfterReturn = { bgGranted = isBackgroundLocationGranted(context) }
        )
    }
}

@Composable
private fun TrackingAfterPermissionGrantedContent(
    navController: NavController,
    trackingViewModel: TrackingViewModel,
    myPageViewModel: MyPageViewModel,
    isTracking: Boolean,
    locations: List<TrackingSnapshot>,
    showInfo: Boolean,
    setShowInfo: (Boolean) -> Unit,
    context: Context
) {

    var kakaoMapRef by remember { mutableStateOf<KakaoMap?>(null) }         // KakaoMap 참조(실시간 변경 반영 위해 기억)
    var currentRouteLine by remember { mutableStateOf<RouteLine?>(null) }   // 현재 그려진 폴리 라인
    var trackingManagerRef by remember { mutableStateOf<TrackingManager?>(null) }   // 카메라 최근 위치
    var currentLabel by remember { mutableStateOf<Label?>(null) }           // 라벨(아이콘) 위치
    var mapViewRef by remember { mutableStateOf<View?>(null) }
    val scope = rememberCoroutineScope()
    val imageLoader = remember(context) { ImageLoader(context) }

    val profile by myPageViewModel.profile.collectAsState()             // 사용자 프로필 사진
    LaunchedEffect(Unit) {
        if (profile == null) myPageViewModel.loadMyProfile()
    }

    // 알림 권한 요청 런처 (API 33+)
    val requestNotifPerm = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            // 권한 허용 → 실제 시작
            TrackingService.start(context)
            trackingViewModel.startTracking()
        } else {
            // 거부 → 알림 설정 화면으로 유도(선택)
            openAppNotificationSettings(context)
        }
    }

    // 유저 아이콘 스타일 - 프로필 사진 없으면 기본 사진으로
    val iconSizePx = remember(context) { 20f.dpToPx(context).toInt() }   // 프로필 아이콘 크기(px)
    var defaultLabelStyles by remember { mutableStateOf<LabelStyles?>(null) }
    var userLabelStyles by remember { mutableStateOf<LabelStyles?>(null) }
    val labelStyles = userLabelStyles ?: defaultLabelStyles

    LaunchedEffect(kakaoMapRef, iconSizePx) {
        val map = kakaoMapRef ?: return@LaunchedEffect
        val req = ImageRequest.Builder(context)
            .data(R.drawable.profile)          // 기본 아이콘(리소스)
            .size(iconSizePx, iconSizePx)      // 원하는 px 크기
            .precision(Precision.EXACT)
            .transformations(CircleCropTransformation())
            .allowHardware(false)
            .build()
        val bmp = (imageLoader.execute(req).drawable as? BitmapDrawable)?.bitmap
        defaultLabelStyles = bmp?.let {
            map.labelManager?.addLabelStyles(LabelStyles.from(LabelStyle.from(it)))
        }
    }

    LaunchedEffect(profile?.profileUrl, kakaoMapRef) {
        val map = kakaoMapRef ?: return@LaunchedEffect
        val url = profile?.profileUrl
        if (url.isNullOrBlank()) {
            userLabelStyles = null
            defaultLabelStyles?.let { styles -> currentLabel?.setStyles(styles) }
            return@LaunchedEffect
        }

        // Coil 로 비트맵 로딩
        val req = ImageRequest.Builder(context)
            .data(url)
            .size(iconSizePx, iconSizePx)           // 아이콘 픽셀 크기로 다운샘플링
            .precision(Precision.EXACT)
            .transformations(CircleCropTransformation())
            .allowHardware(false)                    // Bitmap 추출
            .build()

        val bmp = (imageLoader.execute(req).drawable as? BitmapDrawable)?.bitmap

        userLabelStyles = bmp?.let {
            map.labelManager?.addLabelStyles(LabelStyles.from(LabelStyle.from(it)))
        }

        // 라벨이 이미 떠 있으면 깜빡임 없이 스타일만 교체
        (userLabelStyles ?: defaultLabelStyles)?.let { styles ->
            currentLabel?.setStyles(styles)
        }
    }

    // 위치 리스트가 바뀔 때 지도 위에 전체 경로(polyline) 갱신 및 카메라 이동
    LaunchedEffect(locations, kakaoMapRef) {
        val map = kakaoMapRef ?: return@LaunchedEffect
        if (locations.isEmpty()) return@LaunchedEffect

        val path = locations.map { LatLng.from(it.latitude, it.longitude) }
        if (path.size >= 2) {
            val routeLineManager = map.routeLineManager
            val routeLayer = routeLineManager?.layer

            // 기존 라인 제거(전체 재생성 방식)
            currentRouteLine?.let { routeLayer?.remove(it) }

            // 스타일 정의
            val routeStyle  = RouteLineStyle.from(6f, 0xFF2196F3.toInt())
            val routeStyles = RouteLineStyles.from(routeStyle)

            // 세그먼트 생성 (최소 2개 좌표 필요)
            val segment = RouteLineSegment.from(path, routeStyles)

            // 옵션 생성 + 레이어에 추가
            val options = RouteLineOptions.from("trackingLine", segment)
            currentRouteLine = routeLayer?.addRouteLine(options)
            currentRouteLine?.show()
        }

        // 최신 위치로 아이콘 이동
        val lastLocation = locations.last()
        val latLng = LatLng.from(lastLocation.latitude, lastLocation.longitude)

        if (currentLabel == null) {
            labelStyles?.let { styles ->
                val opt = LabelOptions.from(latLng).setStyles(styles)
                currentLabel = map.labelManager?.layer?.addLabel(opt)
                currentLabel?.let { trackingManagerRef?.startTracking(it) }
            }
        } else {
            currentLabel?.moveTo(latLng)

            labelStyles?.let { styles -> currentLabel?.setStyles(styles) }
        }
    }

    Scaffold(
        topBar = {
            HeaderRow(
                text = "트래킹",
                showText = true,
                showLeftButton = false,
                menuActions = emptyList()
            )
        }
    ) { innerPadding ->
        // 실제 화면 UI
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                TrackingTabBar(
                    currentTab = "realtime",
                    onRealtimeClick = { /* 현재 화면이므로 기능 X */ },
                    onHistoryClick = { navController.navigate(TrackingNavRoutes.History) }
                )
                // 지도 + Info 버튼
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    KakaoMapView(
                        modifier = Modifier.fillMaxSize(),
                        onMapReady = @androidx.annotation.RequiresPermission(allOf = [
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                        ]) { kakaoMap ->
                            kakaoMapRef = kakaoMap

                            trackingManagerRef = kakaoMap.getTrackingManager()
                            trackingManagerRef?.setTrackingRotation(false)

                            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                            val fallback = {
                                val defaultLatLng = LatLng.from(36.5, 127.5)
                                kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(defaultLatLng))
                                kakaoMap.moveCamera(CameraUpdateFactory.zoomTo(10))
                                Log.w("TrackingScreen", "fallback 위치로 이동")
                            }

                            try {
                                val cancellationTokenSource = CancellationTokenSource()
                                fusedClient.getCurrentLocation(
                                    Priority.PRIORITY_HIGH_ACCURACY,
                                    cancellationTokenSource.token
                                )
                                    .addOnSuccessListener { location ->
                                        if (location != null) {
                                            val userLatLng = LatLng.from(location.latitude, location.longitude)
                                            kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(userLatLng))
                                            kakaoMap.moveCamera(CameraUpdateFactory.zoomTo(15))

                                            labelStyles?.let { styles ->
                                                if (currentLabel == null) {
                                                    val labelOptions = LabelOptions.from(userLatLng).setStyles(styles)
                                                    currentLabel = kakaoMap.labelManager?.layer?.addLabel(labelOptions)

                                                    currentLabel?.let { trackingManagerRef?.startTracking(it) }
                                                }
                                            }
                                        } else {
                                            fallback()
                                        }
                                    }
                                    .addOnFailureListener {
                                        Log.e("TrackingScreen", "currentLocation 실패: ${it.message}")
                                        fallback()
                                    }
                            } catch (e: SecurityException) {
                                Log.e("TrackingScreen", "위치 권한 없음: ${e.message}")
                                fallback()
                            }
                        },
                        onViewReady = { view -> mapViewRef = view }
                    )
                }

                // Info 버튼 우측 아래에 배치
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    InfoButton(onClick = { setShowInfo(true) })
                }

                // 버튼
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = {
                            if (isTracking) {
                                // 카메라 추적 중단
                                trackingManagerRef?.stopTracking()

                                // 사용자 아이콘 제거
                                currentLabel?.let {
                                    kakaoMapRef?.labelManager?.layer?.remove(it)
                                    currentLabel = null
                                }

                                // 줌 레벨 및 카메라 위치 조정
                                val bounds = calculateBoundsFromSnapshots(locations)
                                bounds?.let {
                                    kakaoMapRef?.moveCamera(CameraUpdateFactory.fitMapPoints(it, 50))
                                }

                                scope.launch {
                                    // 사용자 아이콘 제거 후 반영되는 시간
                                    delay(500)

                                    mapViewRef?.let { view ->
                                        captureAndSaveThumbnailToTempFile(context, view) { thumbFile ->
                                            if (thumbFile == null) {
                                                Toast.makeText(context, "썸네일 생성 실패", Toast.LENGTH_SHORT).show()
                                                return@captureAndSaveThumbnailToTempFile
                                            }

                                            TrackingService.stop(context)

                                            scope.launch {
                                                // createTracking: 위치 JSON + 썸네일 S3 업로드 → ID 받기
                                                val id = trackingViewModel.createTracking(context, thumbFile)
                                                if (id == null) {
                                                    Toast.makeText(context, "경로 저장에 실패했습니다.", Toast.LENGTH_SHORT).show()
                                                    return@launch
                                                }

                                                //  uploadTrackingImages: (추가 사진 없으니 files=emptyList)
                                                trackingViewModel.uploadTrackingImages(
                                                    trackingId = id,
                                                    files = emptyList(),
                                                    thumbnailFile = thumbFile
                                                ) { success, error ->
                                                    if (success) {
                                                        // 완료 후 UpdateScreen 으로 이동
                                                        currentRouteLine?.remove()
                                                        navController.navigate("${TrackingNavRoutes.Update}/$id?from=Tracking")
                                                    } else {
                                                        Toast.makeText(context, "이미지 등록 실패: $error", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                if (!isBackgroundLocationGranted(context)) {
                                    Toast.makeText(context, "백그라운드 위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
                                    openAppDetailsSettings(context)
                                } else {
                                    // 알림 권한 확인 후 실행
                                    if (Build.VERSION.SDK_INT >= 33) {
                                        val granted = ContextCompat.checkSelfPermission(
                                            context, android.Manifest.permission.POST_NOTIFICATIONS
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (!granted) {
                                            requestNotifPerm.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                            return@Button
                                        }
                                    }

                                    trackingViewModel.startTracking()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isTracking) Color.Red else MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .width(150.dp)
                            .padding(16.dp)
                    ) {
                        Text(if (isTracking) "Stop" else "Start")
                    }
                }

                Spacer(Modifier.height(8.dp))
            }

            // todo: 설명 툴팁 위치를 버튼 바로 아래로
            if (showInfo) {
                InfoTooltip(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                )
                LaunchedEffect(Unit) {
                    delay(3000L)    // 3초 후 info창 사라짐
                    setShowInfo(false)
                }
            }
        }
    }
}

@Composable
fun InfoButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(
        onClick = onClick,
        modifier = modifier,
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = Color(0xFFD1D1D1)
        )
    ) {
        Icon(Icons.Default.Info, contentDescription = "설명")
    }
}

@Composable
fun InfoTooltip(modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 8.dp,
        color = Color.White,
        modifier = modifier
    ) {
        Text(
            text = "이동 경로를 추적하는 화면입니다.\n여행 경로를 그려드려요!",
            modifier = Modifier
                .padding(12.dp)
                .widthIn(max = 200.dp),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

private fun isBackgroundLocationGranted(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

private fun openAppDetailsSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = "package:${context.packageName}".toUri()
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

@Composable
private fun BackgroundLocationPermissionScreen(
    onOpenSettings: () -> Unit,
    onRefreshAfterReturn: () -> Unit
) {
    // Resume에서도 혹시 바뀌었는지 재확인(이중 안전장치)
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) onRefreshAfterReturn()
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    Scaffold(
        topBar = {
            HeaderRow(
                text = "트래킹",
                showText = true,
                showLeftButton = false,
                menuActions = emptyList()
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("권한이 필요합니다", style = MaterialTheme.typography.titleLarge)

            Spacer(Modifier.height(24.dp))

            Text(
                "(필수) 권한 > 위치 > ‘항상 허용’",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(16.dp))

            Text(
                "(선택) 권한 > 알림 > ‘알림 허용’",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    onOpenSettings()
                }
            ) {
                Text("설정으로 이동")
            }
        }
    }
}

private fun openAppNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}