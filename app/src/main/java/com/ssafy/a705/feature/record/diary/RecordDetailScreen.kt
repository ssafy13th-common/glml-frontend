package com.ssafy.a705.feature.record.diary

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import coil.compose.SubcomposeAsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.ssafy.a705.R     // 자동으로 확인하지 못해 수동 추가함
import com.ssafy.a705.common.components.HeaderRow
import com.ssafy.a705.common.components.MenuAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordDetailScreen(
    recordViewModel: RecordViewModel,
    navController: NavController,
    recordId: Long
) {
    val ui = recordViewModel.detail.collectAsState().value
    val context = LocalContext.current
    var showDelete by remember { mutableStateOf(false) }

    // id로 해당 레코드 찾기
    LaunchedEffect(recordId) {
        Log.d("RecordDetailScreen", "recordId: $recordId")
        recordViewModel.loadDiaryDetail(recordId)
    }

    Scaffold(
        topBar = {
            HeaderRow(
                showText = false,
                showLeftButton = true,
                onLeftClick = { navController.popBackStack() },
                menuActions = listOf(
                    MenuAction(
                        label = "수정",
                        onClick = {
                             navController.navigate("${RecordNavRoutes.Update}/$recordId")
                        }
                    ),
                    MenuAction(
                        label = "삭제",
                        isDestructive = true,
                        onClick = { showDelete = true }
                    )
                )
            )
        }
    ) { innerPadding ->
        when {
            ui.loading -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            ui.error != null -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(ui.error, color = Color.Red)
            }
            ui.data != null -> {
                RecordDetailContent(
                    detail = ui.data,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }

        if (showDelete) {
            AlertDialog(
                onDismissRequest = { showDelete = false },
                title = { Text("삭제") },
                text = { Text("이 기록을 삭제할까요? 되돌릴 수 없습니다.") },
                confirmButton = {
                    TextButton(onClick = {
                        showDelete = false
                        recordViewModel.deleteDiary(recordId) { ok, msg ->
                            if (ok) {
                                Toast.makeText(context, "삭제 완료", Toast.LENGTH_SHORT).show()
                                navController.popBackStack() // 목록/이전 화면으로
                            } else {
                                Toast.makeText(context, msg ?: "삭제 실패", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }) { Text("삭제") }
                },
                dismissButton = {
                    TextButton(onClick = { showDelete = false }) { Text("취소") }
                }
            )
        }
    }
}

@Composable
fun RecordDetailContent(
    detail: RecordDetailItem,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // 제목 + 날짜
        Text(detail.location, style = MaterialTheme.typography.headlineMedium, color = Color(0xFF2D92FF))
        Text(detail.startedAt, style = MaterialTheme.typography.bodySmall, color = Color.Gray)

        Spacer(Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))

        // 사진
        val images = detail.imageUrls.orEmpty()
        if (images.isNotEmpty()) {
            val pagerState = rememberPagerState(initialPage = 0, pageCount = { images.size })
            val context = LocalContext.current
            val density = LocalDensity.current
            val imageLoader = context.imageLoader

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) {
                val widthPx = with(density) { maxWidth.roundToPx() }
                val renderingIdx = 2

                LaunchedEffect(pagerState.currentPage, images.size, widthPx) {
                    val start = (pagerState.currentPage - renderingIdx).coerceAtLeast(0)
                    val end   = (pagerState.currentPage + renderingIdx).coerceAtMost(images.lastIndex)
                    for (i in start..end) {
                        imageLoader.enqueue(
                            ImageRequest.Builder(context)
                                .data(images[i])
                                .size(widthPx, widthPx)   // 화면 셀 크기로 다운샘플
                                .build()
                        )
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    beyondViewportPageCount = renderingIdx,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val request = remember(images[page], widthPx) {
                        ImageRequest.Builder(context)
                            .data(images[page])
                            .size(widthPx, widthPx)
                            .crossfade(false)
                            .build()
                    }

                    SubcomposeAsyncImage(
                        model = request,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                        loading = {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
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
                }

                // 우상단 (현재/전체) 배지
                val current = remember { derivedStateOf { pagerState.currentPage + 1 } }
                Text(
                    text = "${current.value} / ${images.size}",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(
                            color = Color.Black.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            // 이미지 없을 때 기본 이미지
            Image(
                painter = painterResource(R.drawable.default_img),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                contentScale = ContentScale.Fit
            )
        }

//        Box(
//            modifier = Modifier
//                .align(Alignment.BottomCenter)
//                .fillMaxWidth()
//                .height(8.dp) // 두께 조절 (8~16dp 추천)
//                .background(
//                    brush = Brush.verticalGradient(
//                        colors = listOf(
//                            Color.Black.copy(alpha = 0.18f), // 위(가까운 쪽)
//                            Color.Transparent                 // 아래
//                        )
//                    )
//                )
//        )

        Spacer(Modifier.height(8.dp))



        if (!detail.content.isNullOrBlank()) {
            Text(
                text = detail.content,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
