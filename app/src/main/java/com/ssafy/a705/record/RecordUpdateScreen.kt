package com.ssafy.a705.record

import android.app.DatePickerDialog
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.ssafy.a705.components.HeaderRow
import com.ssafy.a705.components.MenuAction
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordUpdateScreen(
    recordViewModel: RecordViewModel,
    navController: NavController,
    recordId: Long
) {
    val state by recordViewModel.updateState.collectAsState()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    LaunchedEffect(recordId) {
        recordViewModel.loadDiaryDetail(recordId)
    }

    // 갤러리 멀티 선택(새 사진 추가)
    val multiImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) recordViewModel.addNewPhotos(uris)
    }

    Scaffold(
        topBar = {
            HeaderRow(
                text = "수정하기",
                showText = true,
                showLeftButton = true,
                onLeftClick = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    }
                },
                menuActions = listOf(
                    MenuAction(
                        label = if (state.isSaving) "저장 중..." else "수정",
                        enabled = !state.isSaving,
                        onClick = {
                            recordViewModel.updateDiaryWithUploads(context) { ok, msg ->
                                if (ok) {
                                    Toast.makeText(context, "수정 완료", Toast.LENGTH_SHORT).show()
                                    navController.popBackStack()
                                } else {
                                    Toast.makeText(context, msg ?: "수정 실패", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .pointerInput(Unit) {
                    detectTapGestures { focusManager.clearFocus() }
                }
        ) {
            Text(
                text = "장소: ${state.locationName}",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(8.dp))

            val dateFormatter = remember { SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()) }
            val calendar = remember { Calendar.getInstance() }
            var showStartPicker by remember { mutableStateOf(false) }
            var showEndPicker by remember { mutableStateOf(false) }

            if (showStartPicker) {
                LaunchedEffect(Unit) {
                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            val c = Calendar.getInstance().apply { set(year, month, day) }
                            recordViewModel.setUpdateStartedAt(dateFormatter.format(c.time))
                            showStartPicker = false
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                    ).apply { setOnCancelListener { showStartPicker = false } }.show()
                }
            }
            if (showEndPicker) {
                LaunchedEffect(Unit) {
                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            val c = Calendar.getInstance().apply { set(year, month, day) }
                            recordViewModel.setUpdateEndedAt(dateFormatter.format(c.time))
                            showEndPicker = false
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                    ).apply { setOnCancelListener { showEndPicker = false } }.show()
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showStartPicker = true }
                    .padding(vertical = 8.dp)
            ) {
                Text("시작일: ${state.startedAtDisplay}", modifier = Modifier.weight(1f))
                Icon(Icons.Default.DateRange, contentDescription = "달력")
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showEndPicker = true }
                    .padding(vertical = 8.dp)
            ) {
                Text("도착일: ${state.endedAtDisplay}", modifier = Modifier.weight(1f))
                Icon(Icons.Default.DateRange, contentDescription = "달력")
            }

            Spacer(Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { multiImagePickerLauncher.launch("image/*") }
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = "사진 +",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            Spacer(Modifier.height(4.dp))

            val density = LocalDensity.current
            val cellPx = with(density) { 120.dp.roundToPx() }
            val imageLoader = context.imageLoader

            val allItems: List<Any> = remember(state.keepServerKeys, state.newPhotoUris) {
                buildList {
                    addAll(state.keepServerKeys)
                    addAll(state.newPhotoUris)
                }
            }

            LaunchedEffect(allItems, cellPx) {
                if (cellPx <= 0 || allItems.isEmpty()) return@LaunchedEffect
                allItems.forEach { item ->
                    imageLoader.enqueue(
                        ImageRequest.Builder(context)
                            .data(item)
                            .size(cellPx, cellPx)   // 셀 픽셀 크기만큼 다운샘플해서 메모리/대역폭 절약
                            .crossfade(false)
                            .build()
                    )
                }
            }

            LazyRow {
                items(state.keepServerKeys.toList(), key = { it }) { key ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .size(120.dp)
                            .padding(4.dp)
                    ) {
                        Box {
                            SubcomposeAsyncImage(
                                model = remember(key, cellPx) {
                                    ImageRequest.Builder(context)
                                        .data(key)
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
                                        painter = painterResource(com.ssafy.a705.R.drawable.default_img),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            )
                            FilledIconButton(
                                onClick = { recordViewModel.removeKeep(key) },
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

                items(state.newPhotoUris, key = { it }) { uri ->
                    Box {
                        val model = remember(uri, cellPx) {
                            ImageRequest.Builder(context)
                                .data(uri)
                                .size(cellPx, cellPx)
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
                            onClick = { recordViewModel.removeNewPhoto(uri) },
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

            Spacer(Modifier.height(8.dp))

            // --- 본문 ---
            OutlinedTextField(
                value = state.content,
                onValueChange = recordViewModel::setUpdateContent,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .heightIn(min = 160.dp),
                placeholder = { Text("일기를 수정해 보세요!") },
                maxLines = 8
            )
        }
    }

    if (state.isSaving) {
        LoadingDialog("로딩 중")
    }
}

@Composable
private fun LoadingDialog(message: String) {
    androidx.compose.ui.window.Dialog(onDismissRequest = { }) {
        Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)) {
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

private fun mergeImages(localUris: List<android.net.Uri>, remoteUrls: List<String>): List<Any> {
    // Coil은 model = Any로 Uri/String 둘 다 지원
    return buildList {
        addAll(localUris)   // Uri
        addAll(remoteUrls)  // String
    }
}