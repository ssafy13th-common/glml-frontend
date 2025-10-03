package com.ssafy.a705.feature.record.diary

import android.app.DatePickerDialog
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.ssafy.a705.common.components.HeaderRow
import com.ssafy.a705.common.components.MenuAction
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordCreateScreen(
    recordViewModel: RecordViewModel,
    navController: NavController
) {
    val state by recordViewModel.createState.collectAsState()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    val locationFromMap = navController.previousBackStackEntry
        ?.savedStateHandle
        ?.get<String>("location") ?: ""
    val codeFromMap = navController.previousBackStackEntry
        ?.savedStateHandle
        ?.get<String>("code") ?: ""

    val multiImagePickerLauncher = rememberLauncherForActivityResult(
        // 사용자가 선택한 이미지만 접근 -> 따로 권한 설정 필요 X
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uri: List<Uri> ->
        if (uri.isNotEmpty()) {
            recordViewModel.addPhotos(uri)
        }
    }

    LaunchedEffect(locationFromMap) {
        if (locationFromMap.isNotEmpty()) {
            recordViewModel.setLocation(locationFromMap)
        }

        if (codeFromMap.isNotEmpty()) {
            recordViewModel.setCode(codeFromMap)
        }
    }

    Scaffold(
        topBar = {
            HeaderRow(
                text = "기록하기",
                showText = true,
                showLeftButton = true,
                onLeftClick = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    }
                },
                menuActions = listOf(
                    MenuAction(
                        label = "등록",
                        enabled = !state.isSaving,
                        onClick = {
                            recordViewModel.createRecordsWithUploads(context) { ok, id, msg ->
                                if (ok && id != null) {
                                    Toast.makeText(context, "등록 완료", Toast.LENGTH_SHORT).show()

                                    navController.navigate(RecordNavRoutes.List) {
                                        // 뒤로가기로 접근 시 현재 작성한 지역이 색칠되지 않으므로 버튼으로만 접근하도록 삭제
                                        popUpTo(RecordNavRoutes.Map) { inclusive = true }
                                    }

                                    navController.navigate("${RecordNavRoutes.Detail}/$id")

                                } else {
                                    Toast.makeText(context, msg ?: "등록 실패", Toast.LENGTH_SHORT).show()
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
                .verticalScroll(rememberScrollState())  // 스크롤 가능하도록 구현
                .pointerInput(Unit) {
                    detectTapGestures { focusManager.clearFocus() }
                }
        ) {
            val context = LocalContext.current
            val dateFormatter = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
            val calendar = Calendar.getInstance()
            var showStartPicker by remember { mutableStateOf(false) }
            var showEndPicker by remember { mutableStateOf(false) }

            val density = LocalDensity.current
            val cellPx = with(density) { 120.dp.roundToPx() }
            val imageLoader = context.imageLoader

            LaunchedEffect(state.photoUris, cellPx) {
                if (cellPx <= 0 || state.photoUris.isEmpty()) return@LaunchedEffect
                state.photoUris.forEach { uri ->
                    imageLoader.enqueue(
                        ImageRequest.Builder(context)
                            .data(uri)
                            .size(cellPx, cellPx)   // 다운샘플링
                            .crossfade(false)
                            .build()
                    )
                }
            }

            if (showStartPicker) {
                LaunchedEffect(Unit) {
                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            val date = Calendar.getInstance().apply {
                                set(year, month, day)
                            }
                            recordViewModel.setStartDate(dateFormatter.format(date.time))
                            showStartPicker = false
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                    ).apply {
                        setOnCancelListener { showStartPicker = false }
                    }.show()
                }
            }

            if (showEndPicker) {
                LaunchedEffect(Unit) {
                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            val date = Calendar.getInstance().apply {
                                set(year, month, day)
                            }
                            recordViewModel.setEndDate(dateFormatter.format(date.time))
                            showEndPicker = false
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                    ).apply {
                        setOnCancelListener { showEndPicker = false }
                    }.show()
                }
            }

            // 장소
            Text(text = "장소: ${state.location}", style = MaterialTheme.typography.bodyLarge)

            Spacer(modifier = Modifier.height(8.dp))

            // 시작일
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showStartPicker = true }
                    .padding(vertical = 8.dp)
            ) {
                Text(text = "시작일: ${state.startDate ?: ""}", modifier = Modifier.weight(1f))
                Icon(Icons.Default.DateRange, contentDescription = "달력")
            }

            // 도착일
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showEndPicker = true }
                    .padding(vertical = 8.dp)
            ) {
                Text(text = "도착일: ${state.endDate ?: ""}", modifier = Modifier.weight(1f))
                Icon(Icons.Default.DateRange, contentDescription = "달력")
            }

            Spacer(modifier = Modifier.height(8.dp))

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

            LazyRow {
                items(state.photoUris) { uri ->
                    Box {
                        val model = remember(uri, cellPx) {
                            ImageRequest.Builder(context)
                                .data(uri)
                                .size(cellPx, cellPx)    // ★ 생성 화면도 셀 픽셀로 리사이즈
                                .crossfade(false)
                                .build()
                        }

                        AsyncImage(
                            model = model,
                            contentDescription = "선택된 사진",
                            modifier = Modifier
                                .size(120.dp)
                                .padding(4.dp)
                        )
                        FilledIconButton(
                            onClick = { recordViewModel.removePhoto(uri) },
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

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = state.diary,
                onValueChange = recordViewModel::setDiary,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .heightIn(min = 160.dp),
                placeholder = { Text("일기를 작성해 보세요!") },
                maxLines = 8    // 텍스트박스 길이 제한 -> 해당 줄 이상 작성 시 내부 스크롤 적용
            )
        }
    }

    if (state.isSaving) {
        LoadingDialog("로딩 중")
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