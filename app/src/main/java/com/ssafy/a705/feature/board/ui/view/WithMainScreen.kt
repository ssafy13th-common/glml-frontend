package com.ssafy.a705.feature.board.ui.view

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ssafy.a705.R
import com.ssafy.a705.common.components.HeaderRow
import com.ssafy.a705.feature.board.data.model.response.PostDetailResponse
import com.ssafy.a705.feature.board.data.model.response.PostListResponse
import com.ssafy.a705.feature.board.ui.viewmodel.BoardViewModel


@Composable
fun WithMainScreen(
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
                        WithPostCard(post = post) {
                            onNavigateToDetail(post.id)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SimpleSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    onClear: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .height(44.dp)
            .background(Color(0xFFE5E5E5), shape = RoundedCornerShape(22.dp))
            .padding(horizontal = 12.dp)
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray)
        Spacer(Modifier.width(6.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            modifier = Modifier.weight(1f),
            textStyle = TextStyle(fontSize = 16.sp),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text("검색", color = Color.Gray, fontSize = 16.sp)
                }
                inner()
            }
        )
        if (value.isNotEmpty()) {
            Spacer(Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "지우기",
                modifier = Modifier.clickable { onClear() },
                tint = Color.Gray
            )
        }
    }
}

@Composable
fun WithPostCard(post: PostListResponse, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(0.5.dp, Color.LightGray)
            .padding(horizontal = 30.dp)
            .padding(vertical = 10.dp)
    ) {
        Text(post.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(8.dp))
        Text(post.summary, fontSize = 14.sp, color = Color.Gray)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.drawable.comments),
                contentDescription = "댓글 말풍선",
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text("${post.comments}", fontSize = 12.sp, color = Color.Black)
            Spacer(Modifier.weight(1f))
            Spacer(modifier = Modifier.width(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(post.author, modifier = Modifier.padding(start = 4.dp), fontSize = 12.sp)
                Spacer(modifier = Modifier.width(20.dp))
                Text(post.updatedAt, fontSize = 11.sp)
            }
        }
    }
}
