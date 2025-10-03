package com.ssafy.a705.record

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import coil.request.ImageRequest
import coil.imageLoader
import com.ssafy.a705.components.HeaderRow
import kotlin.math.min

@Composable
fun RecordScreen(
    recordViewModel: RecordViewModel = hiltViewModel(),
    navController: NavController
) {
    val pagingItems = recordViewModel.diaries.collectAsLazyPagingItems()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val density = LocalDensity.current
    val thumbPx = with(density) { 80.dp.roundToPx() }
    val isEmpty = pagingItems.itemCount == 0 &&
            pagingItems.loadState.refresh is LoadState.NotLoading &&
            pagingItems.loadState.append  is LoadState.NotLoading

    LaunchedEffect(listState.firstVisibleItemIndex, pagingItems.itemCount, thumbPx) {
        val start = listState.firstVisibleItemIndex
        val end = min(start + 10, pagingItems.itemCount - 1)
        val loader = context.imageLoader
        for (i in start..end) {
            val url = pagingItems.peek(i)?.thumbnailUrl ?: continue
            loader.enqueue(
                ImageRequest.Builder(context)
                    .data(url)
                    .size(thumbPx, thumbPx)   // ★ 80dp 셀 크기만큼만 다운샘플
                    .build()
            )
        }
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
    ){ innerPadding ->
        // 세로 방향으로 구성
        Column(
            modifier = Modifier
                .fillMaxSize()      // 전체 화면
                .padding(innerPadding)
        ) {
            // 탭 버튼 (지도 / 기록)
            RecordTabBar(
                currentTab = "record",  // 현재 탭을 record로 변경
                onMapClick = { navController.navigate(RecordNavRoutes.Map) },
                onRecordClick = { /* 현재 화면이므로 기능 X */ }
            )

            // 리스트 출력
            if (isEmpty) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 48.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Text(
                        text = "기록이 없습니다",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(
                        count = pagingItems.itemCount,
                        key = { index -> pagingItems.peek(index)?.id?.let { "D:$it" }?: "L:${index}" },
                        contentType = { _ -> "record" }
                    ) { index ->
                        val item = pagingItems[index]
                        if (item != null) {
                            Box(
                                Modifier.fillMaxWidth()
                                    .clickable {
                                        Log.d("RecordListScreen", "item id: ${item.id}")
                                        navController.navigate("${RecordNavRoutes.Detail}/${item.id}")
                                    }
                            ) { RecordCard(item) }
                            HorizontalDivider(Modifier.padding(top = 8.dp))
                        }
                    }

                    // 초기 로딩
                    if (pagingItems.loadState.refresh is LoadState.Loading) {
                        item {
                            Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                    }

                    // 다음 페이지 로딩
                    if (pagingItems.loadState.append is LoadState.Loading) {
                        item { CircularProgressIndicator(Modifier.padding(16.dp)) }
                    }

                    // 에러 처리
                    val err = (pagingItems.loadState.refresh as? LoadState.Error)
                        ?: (pagingItems.loadState.append as? LoadState.Error)
                    if (err != null) {
                        item {
                            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("불러오기 실패: ${err.error.message ?: ""}", color = Color.Red)
                                Spacer(Modifier.height(8.dp))
                                TextButton(onClick = { pagingItems.retry() }) { Text("다시 시도") }
                            }
                        }
                    }
                }
            }
        }
    }
}