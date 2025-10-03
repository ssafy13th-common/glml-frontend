package com.ssafy.a705.tracking

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.exifinterface.media.ExifInterface
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.ImageLoader
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.Label
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import com.kakao.vectormap.route.RouteLine
import com.kakao.vectormap.route.RouteLineOptions
import com.kakao.vectormap.route.RouteLineSegment
import com.kakao.vectormap.route.RouteLineStyle
import com.kakao.vectormap.route.RouteLineStyles
import com.ssafy.a705.global.components.KakaoMapView
import coil.request.ImageRequest
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.imageLoader
import com.ssafy.a705.global.components.HeaderRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import com.ssafy.a705.R

@Composable
fun TrackingUpdateScreen(
    navController: NavController,
    trackingId: String,
    trackingViewModel: TrackingViewModel = hiltViewModel()
) {
    val localContext = LocalContext.current

    // 상태 구독
    val trackPoints by trackingViewModel.trackPoints.collectAsState()
    val imageUrlsFromServer by trackingViewModel.imageUrls.collectAsState()
    val imageUrisFromDevice by trackingViewModel.photoUris.collectAsState()
    val isLoading by trackingViewModel.isLoading.collectAsState()

    Log.d("TrackingUI", "UI에서 받은 서버 이미지 목록: $imageUrlsFromServer")

    // KakaoMap refs
    var kakaoMapRef by remember { mutableStateOf<KakaoMap?>(null) }
    var currentRouteLine by remember { mutableStateOf<RouteLine?>(null) }
    var imageLabels by remember { mutableStateOf<List<Label>>(emptyList()) }
    var mapViewRef by remember { mutableStateOf<View?>(null) }

    val imageLoader = remember { ImageLoader(localContext) }
    var deviceUriToRemove by remember { mutableStateOf<Uri?>(null) }
    var serverUrlToRemove by remember { mutableStateOf<String?>(null) }
    var polylineDrawnOnce by rememberSaveable { mutableStateOf(false) }
    val navBackStackEntry = navController.currentBackStackEntryAsState().value
    val from = navBackStackEntry?.arguments?.getString("from") ?: "Tracking"

    val conf = LocalConfiguration.current
    val mapHeight = with(LocalDensity.current) { (conf.screenHeightDp * 0.55f).dp }

    // 사진 런처
    val multiImagePickerLauncher = rememberLauncherForActivityResult(
        // 사용자가 선택한 이미지만 접근 -> 따로 권한 설정 필요 X
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { selected: List<Uri> ->
        val currentCount = trackingViewModel.imageUrls.value.size + trackingViewModel.photoUris.value.size
        val available = 5 - currentCount

        when {
           available <= 0 -> {
                   Toast.makeText(localContext, "이미 5장의 사진을 선택하셨습니다.", Toast.LENGTH_SHORT).show()
              }
           selected.size > available -> {
                   trackingViewModel.addPhotoUris(selected.take(available))
               }
           else -> {
                   trackingViewModel.addPhotoUris(selected)
               }
           }
    }

    // 사진 오버레이
    val deviceImageLocations = remember(imageUrisFromDevice) {
        imageUrisFromDevice.mapNotNull { uri ->
            getLatLngFromImageUri(localContext, uri)?.let { latLng -> uri to latLng }
        }
    }

    var serverImageLocations by remember { mutableStateOf<List<Pair<String, LatLng>>>(emptyList()) }
    LaunchedEffect(imageUrlsFromServer) {
        // 키 → URL → EXIF 좌표
        val list = mutableListOf<Pair<String, LatLng>>()
        imageUrlsFromServer.forEach { key ->
            val url = toImageUrlFromKey(key)
            val pos = getLatLngFromRemoteImageUrl(url)
            if (pos != null) list += key to pos
        }
        serverImageLocations = list
    }

    LaunchedEffect(trackingId) {
        trackingViewModel.fetchTrackingDetail(trackingId)
        polylineDrawnOnce = false       // 새 ID 들어오면 다시 그릴 수 있게 reset
    }

    LaunchedEffect(kakaoMapRef, trackPoints) {
        val map = kakaoMapRef ?: return@LaunchedEffect
        if (!polylineDrawnOnce && trackPoints.isNotEmpty()) {
            drawRouteLineOnce(
                map = map,
                trackPoints = trackPoints,
                current = currentRouteLine
            ) { newly ->
                currentRouteLine = newly
                polylineDrawnOnce = true
            }
        }
    }

    LaunchedEffect(kakaoMapRef, deviceImageLocations, serverImageLocations) {
        val map = kakaoMapRef ?: return@LaunchedEffect
        val layer = map.labelManager?.layer

        // 이전 레이블 제거
        imageLabels.forEach { layer?.remove(it) }

        val newImageLabels = mutableListOf<Label>()

        suspend fun addLabelFromAnyImage(data: Any, pos: LatLng): Label? {
            val req = ImageRequest.Builder(localContext).data(data).size(100).build()
            val result = imageLoader.execute(req)
            val bmp = (result.drawable as? BitmapDrawable)?.bitmap ?: return null
            val styleSet = map.labelManager?.addLabelStyles(LabelStyles.from(LabelStyle.from(bmp)))
            val opts = styleSet?.let { LabelOptions.from(pos).setStyles(it) } ?: LabelOptions.from(pos)
            return layer?.addLabel(opts)
        }

        deviceImageLocations.forEach { (uri, pos) ->
            addLabelFromAnyImage(uri, pos)?.let { newImageLabels += it }
        }

        serverImageLocations.forEach { (key, pos) ->
            val url = toImageUrlFromKey(key)
            addLabelFromAnyImage(url, pos)?.let { newImageLabels += it }
        }

        imageLabels = newImageLabels
    }

    Scaffold(
        topBar = {
            HeaderRow(
                text = "트래킹",
                showText = true,
                showLeftButton = false,
                menuActions = emptyList()
            )
        },
        contentWindowInsets = WindowInsets(0.dp)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .height(mapHeight)
                        .fillMaxWidth()
                ) {
                    // 지도 띄우기
                    KakaoMapView(
                        modifier = Modifier.fillMaxSize(),
                        onMapReady = { map -> kakaoMapRef = map },
                        onViewReady = { view -> mapViewRef = view }
                    )
                }

                // 버튼
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    // 사진 추가 버튼
                    Button(
                        onClick = {
                            multiImagePickerLauncher.launch(arrayOf("image/*"))
                        },
                        modifier = Modifier
                            .padding(16.dp)
                    ) {
                        Text("사진 추가")
                    }

                    Button(
                        onClick = {
                            mapViewRef?.let { view ->
                                captureAndSaveThumbnailToTempFile(localContext, view) { thumbFile  ->
                                    if (thumbFile  != null) {
                                        val files = imageUrisFromDevice.map { uri -> uriToFile(localContext, uri) }

                                        saveImagesAndBack(
                                            context = localContext,
                                            navController = navController,
                                            trackingViewModel = trackingViewModel,
                                            trackingId = trackingId,
                                            files = files,
                                            thumbnailFile = thumbFile,
                                            from = from
                                        )
                                    } else {
                                        Toast
                                            .makeText(localContext, "썸네일 생성에 실패했습니다", Toast.LENGTH_SHORT)
                                            .show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .padding(16.dp)
                    ) {
                        Text("저장하기")
                    }
                }

                val density = LocalDensity.current
                val cellPx = with(density) { 120.dp.roundToPx() }
                val imageLoader = localContext.imageLoader

                val allItems: List<Any> = remember(imageUrlsFromServer, imageUrisFromDevice) {
                    buildList {
                        addAll(imageUrlsFromServer)   // 서버 키(문자열) 그대로
                        addAll(imageUrisFromDevice)   // 로컬 Uri
                    }
                }

                LaunchedEffect(allItems, cellPx) {
                    if (cellPx <= 0 || allItems.isEmpty()) return@LaunchedEffect
                    allItems.forEach { item ->
                        imageLoader.enqueue(
                            ImageRequest.Builder(localContext)
                                .data(item)
                                .size(cellPx, cellPx)     // ★ 핵심: 셀 픽셀 크기에 맞춰 디코딩
                                .crossfade(false)
                                .build()
                        )
                    }
                }

                if (imageUrlsFromServer.isNotEmpty() || imageUrisFromDevice.isNotEmpty()) {
                    Text(
                        text = "업로드할 사진 (${imageUrlsFromServer.size + imageUrisFromDevice.size}/5)",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        items(imageUrlsFromServer.toList(), key = { it }) { key ->
                            val url = toImageUrlFromKey(key)
                            Surface(
                                modifier = Modifier
                                    .size(120.dp)
                                    .padding(horizontal = 4.dp)
                            ) {
                                Box {
                                    SubcomposeAsyncImage(
                                        model = remember(key, cellPx) {
                                            ImageRequest.Builder(localContext)
                                                .data(url)
                                                .size(cellPx, cellPx)
                                                .crossfade(false)
                                                .build()
                                        },
                                        contentDescription = "서버 이미지",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit,
                                        loading = {
                                            Box(
                                                Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CircularProgressIndicator(strokeWidth = 2.dp)
                                            }
                                        },
                                        error = {
                                            Image(
                                                painter = painterResource(R.drawable.default_img),
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Fit
                                            )
                                        }
                                    )
                                    FilledIconButton(
                                        onClick = { serverUrlToRemove = key },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .size(24.dp),
                                        shape = CircleShape,
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = Color(0xFFD1D1D1),
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "삭제",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        items(imageUrisFromDevice) { uri ->
                            Box {
                                val model = remember(uri, cellPx) {
                                    ImageRequest.Builder(localContext)
                                        .data(uri)
                                        .size(cellPx, cellPx)   // ★ 셀 픽셀 리사이즈
                                        .crossfade(false)
                                        .build()
                                }
                                AsyncImage(
                                    model = model,
                                    contentDescription = "새로 추가된 사진",
                                    modifier = Modifier
                                        .size(120.dp)
                                        .padding(4.dp)
                                )
                                FilledIconButton(
                                    onClick = { deviceUriToRemove = uri },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                        .size(24.dp),
                                    shape = CircleShape,
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = Color(0xFFD1D1D1),
                                        contentColor   = Color.White
                                    )
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "삭제",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (serverUrlToRemove != null) {
                AlertDialog(
                    onDismissRequest = { serverUrlToRemove = null },
                    title = { Text("사진 삭제 확인") },
                    text = { Text("이 사진을 삭제하시겠습니까?") },
                    confirmButton = {
                        TextButton(onClick = {
                            trackingViewModel.removeServerImageUrl(serverUrlToRemove!!)
                            serverUrlToRemove = null
                        }) { Text("삭제") }
                    },
                    dismissButton = {
                        TextButton(onClick = { serverUrlToRemove = null }) { Text("취소") }
                    }
                )
            }

            if (deviceUriToRemove != null) {
                AlertDialog(
                    onDismissRequest = { deviceUriToRemove = null },
                    title = { Text("사진 삭제 확인") },
                    text = { Text("이 사진을 삭제하시겠습니까?") },
                    confirmButton = {
                        TextButton(onClick = {
                            trackingViewModel.removePhotoUri(deviceUriToRemove!!)
                            deviceUriToRemove = null
                        }) {
                            Text("삭제")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            // 취소 시 아무것도 안 하고 닫기
                            deviceUriToRemove = null
                        }) {
                            Text("취소")
                        }
                    }
                )
            }

            if (isLoading) {
                LoadingDialog("로딩 중")
            }
        }
    }

}

fun uriToFile(context: Context, uri: Uri): File {
    // 캐시 디렉터리에 임시파일 생성 (확장자는 URI에서 유추)
    val fileName = "${System.currentTimeMillis()}_${uri.lastPathSegment?.substringAfterLast('/')}"
    val tempFile = File(context.cacheDir, fileName)

    context.contentResolver.openInputStream(uri)?.use { input ->
        FileOutputStream(tempFile).use { output ->
            input.copyTo(output)
        }
    } ?: throw IllegalArgumentException("Cannot open URI: $uri")

    return tempFile
}

private fun toImageUrlFromKey(keyOrUrl: String): String {
    return if (keyOrUrl.startsWith("http")) keyOrUrl
    else "https://glmlbucket.s3.ap-northeast-2.amazonaws.com/$keyOrUrl"
}

private fun drawRouteLineOnce(
    map: KakaoMap,
    trackPoints: List<TrackingSnapshot>,
    current: RouteLine?,
    onDrawn: (RouteLine) -> Unit
) {
    val layer = map.routeLineManager?.layer

    // 기존 라인 제거
    current?.let { layer?.remove(it) }

    // 좌표 변환
    val path = trackPoints.map { LatLng.from(it.latitude, it.longitude) }
    if (path.size < 2) return

    // 스타일 (선 두께/색 + 외곽선)
    val routeStyle  = RouteLineStyle.from(6f, 0xFF2196F3.toInt())
    val routeStyles = RouteLineStyles.from(routeStyle)

    // 세그먼트 + 옵션 생성
    val segment = RouteLineSegment.from(path, routeStyles)
    val options = RouteLineOptions.from("footprint", segment)

    // 라인 추가
    val newRouteLine = layer?.addRouteLine(options)
    newRouteLine?.let(onDrawn)

    // 경계 맞춰 카메라 이동
    val bounds = calculateBoundsFromSnapshots(trackPoints)
    bounds?.let { map.moveCamera(CameraUpdateFactory.fitMapPoints(it, 50)) }
}

private fun saveImagesAndBack(
    context: Context,
    navController: NavController,
    trackingViewModel: TrackingViewModel,
    trackingId: String,
    files: List<File>,
    thumbnailFile: File,
    from: String
) {
    trackingViewModel.uploadTrackingImages(
        trackingId = trackingId,
        files = files,
        thumbnailFile = thumbnailFile
    ) { success, errorMsg ->
        if (success) {
            Toast.makeText(context, "등록 완료", Toast.LENGTH_SHORT).show()
            // 리스트 화면에 “갱신 필요” 신호 전달 후 복귀 (상태/스크롤 유지)
            navController.previousBackStackEntry
                ?.savedStateHandle
                ?.set("needsRefresh", true)

            if (from == "History") {
                // "기록"에서 진입 : 한 단계만 뒤로
                navController.popBackStack()
            } else {
                // "실시간"에서 진입 : History로 보내면서 실시간/선택/업데이트 스택 정리
                navController.navigate(TrackingNavRoutes.History) {
                    popUpTo(TrackingNavRoutes.Tracking) {
                        inclusive = true   // 실시간 화면까지 제거
                        saveState = false
                    }
                    launchSingleTop = true
                    restoreState = false
                }
            }
        } else {
            Toast.makeText(context, "실패: $errorMsg", Toast.LENGTH_SHORT).show()
        }

    }
}

// 사진 메타데이터 - 위치 정보
fun getLatLngFromImageUri(context: Context, uri: Uri): LatLng? {
    context.contentResolver.openInputStream(uri)?.use { stream ->
        val exif = ExifInterface(stream)

        exif.getLatLong()?.let { coords ->
            return LatLng.from(coords[0], coords[1])
        }
    }
    return null
}

suspend fun getLatLngFromRemoteImageUrl(url: String): LatLng? = withContext(Dispatchers.IO) {
    val client = OkHttpClient()
    val req = Request.Builder().url(url).build()
    client.newCall(req).execute().use { resp ->
        if (!resp.isSuccessful) return@use null
        resp.body?.byteStream()?.use { stream ->
            val exif = ExifInterface(stream)
            val lat = exif.latLong?.getOrNull(0)?.toDouble()
            val lng = exif.latLong?.getOrNull(1)?.toDouble()

            if (lat != null && lng != null) {
                LatLng.from(lat, lng)
            } else null
        }
    }
}

@Composable
private fun LoadingDialog(message: String) {
    Dialog(onDismissRequest = { /* 백버튼/밖터치로 닫히지 않게 */ }) {
        Surface(
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text(message)
            }
        }
    }
}