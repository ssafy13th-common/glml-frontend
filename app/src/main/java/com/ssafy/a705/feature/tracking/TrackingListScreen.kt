package com.ssafy.a705.feature.tracking

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.material3.Card
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.ssafy.a705.common.components.HeaderRow
import com.ssafy.a705.common.util.ImageSaver

@Composable
fun TrackingListScreen(
    navController: NavController,
    trackingViewModel: TrackingViewModel
) {
    val localContext = LocalContext.current
    val images by trackingViewModel.allImages.collectAsState()
    var selectedItem by remember { mutableStateOf<TrackingImageItem?>(null) }  // 카드 클릭 상태
    val imageUrl = selectedItem?.imageUrl.orEmpty()
    val trackingId = selectedItem?.trackingId.orEmpty()
    var isDelete by remember { mutableStateOf(false) }

    // 첫 진입 시 API 호출
    LaunchedEffect(Unit) { trackingViewModel.fetchAllImages() }

    // 변경 발생 시 API 호출
    val entry = remember(navController) { navController.currentBackStackEntry }
    LaunchedEffect(entry) {
        entry?.savedStateHandle
            ?.getStateFlow("needsRefresh", false)
            ?.collect { need ->
                if (need) {
                    trackingViewModel.fetchAllImages()
                    entry.savedStateHandle["needsRefresh"] = false
                }
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // 탭바
                TrackingTabBar(
                    currentTab = "history",
                    onRealtimeClick = { navController.navigate(TrackingNavRoutes.Tracking) },
                    onHistoryClick = { /* 현재 화면이므로 기능 X */ }
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (images.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 100.dp), // 위쪽 여백
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Text(
                            text = "기록이 없습니다.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(
                            count = images.size,
                            key = { it -> images[it].trackingId }
                        ) { index ->
                            val item = images[index]

                            Card(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clickable { selectedItem = item },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(Modifier.fillMaxSize()) {
                                    AsyncImage(
                                        model = item.imageUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }
                }

                if (selectedItem != null) {
                    Dialog(onDismissRequest = { selectedItem = null }) {

                        // 화면 크기 측정을 위한 최상위 박스
                        BoxWithConstraints {
                            val maxHeight = this.maxHeight

                            // 이미지 영역의 위아래 패딩 적용 및 중앙 배치
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { selectedItem = null } // 사진 외부 터치 시 닫힘
                                    .padding(top = maxHeight * 0.1f, bottom = maxHeight * 0.2f),
                                contentAlignment = Alignment.Center
                            ) {
                                // 이미지와 메뉴 버튼, 메뉴 창
                                Box(
                                    modifier = Modifier.wrapContentSize(),
                                    contentAlignment = Alignment.TopEnd
                                ) {
                                    AsyncImage(
                                        model = imageUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .wrapContentHeight()
                                    )

                                    var expanded by remember { mutableStateOf(false) }

                                    Box(
                                        modifier = Modifier.wrapContentSize()
                                    ) {
                                        IconButton(
                                            onClick = { expanded = true },
                                            modifier = Modifier.padding(8.dp) // 이미지와 너무 붙지 않게 여백 추가
                                        ) {
                                            Icon(Icons.Default.MoreVert, contentDescription = "메뉴")
                                        }

                                        DropdownMenu(
                                            expanded = expanded,
                                            onDismissRequest = { expanded = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("수정") },
                                                onClick = {
                                                    expanded = false
                                                    navController.navigate("${TrackingNavRoutes.Update}/$trackingId?from=History")
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("다운로드") },
                                                onClick = {
                                                    expanded = false
                                                    ImageSaver.saveImageFromCache(
                                                        context = localContext,
                                                        imageUrl = imageUrl
                                                    )
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        "삭제",
                                                        color = MaterialTheme.colorScheme.error
                                                    )
                                                },
                                                onClick = {
                                                    expanded = false
                                                    isDelete = true;
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (isDelete) {
                AlertDialog(
                    onDismissRequest = { isDelete = false },
                    title = { Text("트래킹 삭제") },
                    text = { Text("이 트래킹을 삭제할까요? 복구할 수 없습니다.") },
                    confirmButton = {
                        TextButton(onClick = {
                            isDelete = false
                            trackingViewModel.deleteTracking(trackingId) { ok, err ->
                                if (ok) {
                                    Toast.makeText(localContext, "삭제 완료", Toast.LENGTH_SHORT).show()
                                    trackingViewModel.fetchAllImages()
                                    selectedItem = null
                                    isDelete = false
                                } else {
                                    Toast.makeText(localContext, "삭제 실패: $err", Toast.LENGTH_SHORT)
                                        .show()
                                }
                            }
                        }) { Text("삭제") }
                    },
                    dismissButton = {
                        TextButton(onClick = { isDelete = false }) { Text("취소") }
                    }
                )
            }
        }
    }
}