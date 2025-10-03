package com.ssafy.a705.feature.mypage

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ssafy.a705.feature.board.data.model.response.PostData
import com.ssafy.a705.feature.board.data.model.response.PostListResponse
import com.ssafy.a705.feature.controller.viewmodel.MyPageViewModel
import kotlinx.coroutines.launch
import kotlin.collections.distinctBy
import kotlin.collections.filter
import kotlin.collections.forEachIndexed
import kotlin.collections.map
import kotlin.let
import kotlin.text.contains
import kotlin.text.isBlank

@Composable
fun MyPostAndCommentScreen(
    viewModel: MyPageViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onPostClick: (boardId: Long) -> Unit = {},
    initialTab: Int = 0
) {
    var expanded by remember { mutableStateOf(false) }
    var q by remember { mutableStateOf("") }
    var selectedTabIndex by remember(initialTab) { mutableStateOf(initialTab) } // 0: 내 포스팅, 1: 내 댓글

    BackHandler(enabled = expanded) {
        q = ""; expanded = false
    }

    // state collect
    val myBoards by viewModel.myBoards.collectAsState()
    val myComments by viewModel.myComments.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.errorMessage.collectAsState()

    val postPage by viewModel.postPage.collectAsState()
    val postTotalPages by viewModel.postTotalPages.collectAsState()
    val commentPage by viewModel.commentPage.collectAsState()
    val commentTotalPages by viewModel.commentTotalPages.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()


    // 스크롤 상태 복원
    val postListState = rememberLazyListState(
        initialFirstVisibleItemIndex = viewModel.postScrollIndex
    )
    DisposableEffect(postListState) {
        onDispose { viewModel.postScrollIndex = postListState.firstVisibleItemIndex }
    }

    // 최초 로드
    LaunchedEffect(Unit) {
        viewModel.loadMyBoards()
        viewModel.loadMyComments(page = 0)
    }

    // 검색 + 중복 제거
    val filteredBoards: List<PostData> = remember(q, myBoards) {
        val base: List<PostData> = if (q.isBlank()) myBoards
        else myBoards.filter { it.title.contains(q, true)
                || it.summary.contains(q, true) }
        base.distinctBy { it.id }
    }




    // 댓글 DTO -> 화면용 매핑 (boardtitle은 추후 보강)
    val commentItems: List<CommentItem> = remember(myComments) {
        myComments.map { dto ->
            CommentItem(
                boardId = dto.boardId,
                commentId = dto.commentId,
                boardtitle = dto.boardTitle,
                comment = dto.content
            )
        }
    }

    val filteredComments = remember(q, commentItems) {
        if (q.isBlank()) commentItems
        else commentItems.filter { it.comment.contains(q, true) || it.boardtitle.contains(q, true) }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            CollapsibleSearchBarStable(
                title = "내 활동",
                searchExpanded = expanded,
                onToggle = { expanded = !expanded },
                query = q,
                onQueryChange = { q = it },
                onBack = onBack
            )

            Divider(color = Color(0xFFE0E0E0), thickness = 1.dp)

            // 탭
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("내 포스팅", "내 댓글").forEachIndexed { index, title ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight() // 세로 영역 채우기
                            .clickable { selectedTabIndex = index },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center // 세로 가운데 정렬
                    ) {
                        Text(
                            text = title,
                            fontSize = 14.sp,
                            color = if (selectedTabIndex == index) Color.Black else Color.Gray
                        )
                        Spacer(Modifier.height(4.dp))
                        if (selectedTabIndex == index) {
                            Box(
                                modifier = Modifier
                                    .width(24.dp)
                                    .height(2.dp)
                                    .background(Color.Black)
                            )
                        } else Spacer(Modifier.height(2.dp))
                    }
                }
            }

            Divider(color = Color(0xFFE0E0E0), thickness = 1.dp)

            // 리스트 + 페이지네이션 (탭별 한 번만 렌더)
            if (selectedTabIndex == 0) {
                if (!loading && filteredBoards.isEmpty()) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("아직 작성한 게시글이 없어요")
                    }
                } else {
                    PostList(
                        boards = filteredBoards,
                        onPostClick = onPostClick,
                        listState = postListState,
                        modifier = Modifier.weight(1f) // ← 리스트가 남은 높이만 차지
                    )
                }
                PaginationBar(
                    currentPage = postPage,
                    totalPages = postTotalPages,
                    onSelect = { page -> viewModel.loadMyBoards(page) },
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            } else {
                CommentList(
                    comments = commentItems,
                    onItemClick = { boardId, _ ->
                        scope.launch {
                            val exists = viewModel.checkBoardExists(boardId)
                            if (exists) {
                                onPostClick(boardId)
                            } else {
                                Toast.makeText(context, "삭제된 게시글입니다", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                PaginationBar(
                    currentPage = commentPage,
                    totalPages = commentTotalPages,
                    onSelect = { page -> viewModel.loadMyComments(page) },
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        error?.let {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color(0xAA000000))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) { Text(it, color = Color.White) }
        }
    }
}

data class CommentItem(
    val boardId: Long,
    val commentId: Long,
    val boardtitle: String,   // 제목은 나중에 채워도 되니 기본값 둠
    val comment: String
)
/* ===== 컴포저블 ===== */

@Composable
private fun PostList(
    boards: List<PostData>,
    onPostClick: (boardId: Long) -> Unit,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),   // ⬅︎ fillMaxSize() 대신
        state = listState
    ) {
        items(
            items = boards,
            key = { it.id }
        ) { dto ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPostClick(dto.id) }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = dto.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = dto.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "댓글 ${dto.comments}개",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Divider(color = Color(0xFFE0E0E0), thickness = 1.dp)
        }
    }
}



@Composable
private fun CommentList(
    comments: List<CommentItem>,
    onItemClick: (boardId: Long, commentId: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.fillMaxWidth()) {
        items(comments) { c ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onItemClick(c.boardId, c.commentId) }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text("[${c.boardtitle}] 에 작성한 댓글", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(6.dp))
                Text(c.comment, style = MaterialTheme.typography.titleMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            Divider()
        }
    }
}
