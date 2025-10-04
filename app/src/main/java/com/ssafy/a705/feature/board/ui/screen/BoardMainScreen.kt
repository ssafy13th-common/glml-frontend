package com.ssafy.a705.feature.board.ui.screen

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ssafy.a705.common.components.HeaderRow
import com.ssafy.a705.feature.board.ui.component.PostCard
import com.ssafy.a705.feature.board.ui.component.SimpleSearchBar
import com.ssafy.a705.feature.board.ui.viewmodel.BoardViewModel


@Composable
fun BoardMainScreen(
    onNavigateToDetail: (Long) -> Unit,
    onBack: () -> Unit,
    onNavigateToWrite: () -> Unit,
    viewModel: BoardViewModel = hiltViewModel()
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    // 화면 재진입 시에도 전체조회 호출
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPosts(forceIndicator = false) // 데이터만 조용히 갱신
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshPosts(forceIndicator = true) // 최초 진입은 로딩 표시
    }

    var searchQuery by remember { mutableStateOf("") }
    val postList by viewModel.postList.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.loadMorePosts() // 최초 1회 불러오기
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                if (lastVisibleIndex == viewModel.postList.value.lastIndex) {
                    viewModel.loadMorePosts()
                }
            }
    }

    // 에러 메시지 Toast
    val errorMsg by viewModel.errorMessage.collectAsState()
    val context = LocalContext.current
    LaunchedEffect(errorMsg) {
        errorMsg?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    val filteredPosts = postList.filter {
        it.title.contains(searchQuery, ignoreCase = true) ||
                (it.summary?.contains(searchQuery, ignoreCase = true) == true)
    }

    Scaffold(
        topBar = {
            // ✅ 제목 중앙정렬: 그룹 리스트와 동일하게 HeaderRow 사용
            HeaderRow(
                text = "동행게시판",
                showText = true,
                showLeftButton = false,
                menuActions = emptyList()
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToWrite,
                containerColor = Color.White,
                contentColor = Color.Black
            ) {
                Icon(Icons.Default.Add, contentDescription = "글 작성")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ✅ 간단한 검색바: 이름/뒤로가기 없음, 화면 전환 없음
            SimpleSearchBar(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                onClear = { searchQuery = "" }
            )

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                val itemsToShow =
                    if (searchQuery.isBlank()) postList else filteredPosts

                if (itemsToShow.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 100.dp),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Text("검색 결과가 없습니다.", color = Color.Gray)
                        }
                    }
                } else {
                    items(itemsToShow) { post ->
                        PostCard(post = post) {
                            onNavigateToDetail(post.id)
                        }
                    }
                }
            }
        }
    }
}