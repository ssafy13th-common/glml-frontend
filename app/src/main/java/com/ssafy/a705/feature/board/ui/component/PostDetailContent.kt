package com.ssafy.a705.feature.board.ui.component

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.a705.common.components.InputBar
import com.ssafy.a705.feature.board.data.model.WithPost
import com.ssafy.a705.feature.board.data.model.response.CommentResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PostDetailContent(
    post: WithPost,
    comments: List<CommentResponse>,
    currentUserEmail: String,                                      // 변경
    onCommentSubmit: (String, Long?, onDone: () -> Unit) -> Unit,
    onBackClick: () -> Unit,
    onProfileClick: (author: String, email: String) -> Unit,       // 변경
    onPostMenuClick: () -> Unit,
    onCommentMenuClick: (CommentResponse) -> Unit
) {
    var input by remember { mutableStateOf("") }
    var replyTo by remember { mutableStateOf<CommentResponse?>(null) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    LaunchedEffect(comments) { replyTo = null }

    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(
            onBackClick = onBackClick,
            showMenu = post.authorEmail == currentUserEmail,        // 내 글만 메뉴 보이기(이메일 비교)
            onMenuClick = onPostMenuClick
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            item {
                PostHeader(
                    post = post,
                    onProfileClick = onProfileClick               // 전달
                )
                Divider(
                    color = Color(0xFFE0E0E0),
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(comments, key = { it.id }) { comment ->
                CommentItem(
                    comment = comment,
                    onReplyClick = { replyTo = comment },
                    onProfileClick = onProfileClick,              // 전달
                    currentUserEmail = currentUserEmail,          // 변경
                    onMenuClick = onCommentMenuClick
                )
            }
        }

        if (replyTo != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF2F2F2))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${replyTo!!.author}님의 댓글에 답글 중",
                    fontSize = 13.sp,
                    color = Color.DarkGray,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "취소",
                    color = Color.Red,
                    modifier = Modifier.clickable {
                        replyTo = null
                        input = ""
                        focusManager.clearFocus()
                    }
                )
            }
        }
        InputBar(
            value = input,
            onValueChange = { input = it },
            onSend = {
                val wasRoot = (replyTo == null)
                onCommentSubmit(input, replyTo?.id) {
                    replyTo = null
                    input = ""
                    focusManager.clearFocus()

                    // ✅ 토스트 + 0.5초 대기
                    Toast.makeText(
                        context,
                        if (wasRoot) "댓글이 입력되었습니다." else "답글이 입력되었습니다.",
                        Toast.LENGTH_SHORT
                    ).show()

                    coroutineScope.launch {
                        delay(500)
                        // (선택) 댓글일 때 스크롤 이동 유지
                        if (wasRoot) {
                            listState.animateScrollToItem(0.coerceAtLeast(comments.size - 1))
                        }
                    }
                }
            },
            placeholderText = if (replyTo != null) "답글을 입력해주세요" else "댓글을 입력해주세요"
        )
    }
}