package com.ssafy.a705.with

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.ssafy.a705.global.navigation.Screen
import com.ssafy.a705.model.resp.CommentResponse
import com.ssafy.a705.with.model.WithPost
import kotlinx.coroutines.launch
import com.ssafy.a705.controller.viewmodel.WithPostDetailViewModel
import com.ssafy.a705.controller.viewmodel.MyPageViewModel
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.delay
import com.ssafy.a705.R
import com.ssafy.a705.controller.viewmodel.AppChatBridgeViewModel

private const val S3_BASE = "https://glmlbucket.s3.ap-northeast-2.amazonaws.com/"
@Composable
fun WithPostDetailScreen(
    postId: Long,
    navController: NavController,
    withPostDetailViewModel: WithPostDetailViewModel = hiltViewModel(),
    myPageViewModel: MyPageViewModel = hiltViewModel(),
    chatViewModel: AppChatBridgeViewModel  = hiltViewModel()
) {

    val context = LocalContext.current
    val post by withPostDetailViewModel.post.collectAsState()
    val comments by withPostDetailViewModel.comments.collectAsState()

    // 내 정보 로드 (email 사용)
    val myProfile by myPageViewModel.profile.collectAsState()
    LaunchedEffect(Unit) { myPageViewModel.loadMyProfile() }
    val currentUserEmail = myProfile?.email ?: ""
    val currentUserNickname = myProfile?.nickname ?: ""

    var isPostMenu by remember { mutableStateOf(false) }
    var selectedComment by remember { mutableStateOf<CommentResponse?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }

    // ★ 클릭한 프로필 (author, email) 저장
    var showProfilePopup by remember { mutableStateOf(false) }
    var clickedProfile by remember { mutableStateOf<Pair<String, String>?>(null) }

    var showEditDialog by remember { mutableStateOf(false) }
    var editContent by remember { mutableStateOf("") }

    var showRoomNameDialog by remember { mutableStateOf(false) }
    var roomNameInput by remember { mutableStateOf("") }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(postId) { withPostDetailViewModel.loadPost(postId) }

    post?.let {
        WithPostDetailContent(
            post = it,
            comments = comments,
            currentUserEmail = currentUserEmail,                  // ★ 변경
            onCommentSubmit = { content, parentId, onDone ->
                withPostDetailViewModel.submitComment(
                    content = content,
                    parentId = parentId,
                    onDone = onDone
                )
            },
            onBackClick = { navController.popBackStack() },
            onProfileClick = { author, email ->                   // ★ (author,email) 전달
                clickedProfile = author to email
                showProfilePopup = true
            },
            onPostMenuClick = {
                isPostMenu = true
                showBottomSheet = true
            },
            onCommentMenuClick = {
                selectedComment = it
                isPostMenu = false
                showBottomSheet = true
            }
        )
    }

    // ★ 프로필 팝업: 이메일로 본인 여부 판단
    // ★ 프로필 팝업 (내/남 분기)
    if (showProfilePopup && clickedProfile != null) {
        val (author, email) = clickedProfile!!
        val isMe = email.isNotBlank() && email == currentUserEmail
        AlertDialog(
            onDismissRequest = { showProfilePopup = false },
            title = { Text("프로필 메뉴") },
            text = { Text(if (isMe) "내 정보 페이지로 이동할까요?" else "채팅을 시작할까요?") },
            confirmButton = {
                TextButton(onClick = {
                    showProfilePopup = false
                    if (isMe) {
                        navController.navigate(Screen.MyPage.route)
                    } else {
                        //  연락하기: 방 이름 입력창 띄우기
                        roomNameInput = "${author}님과의 채팅"   // 기본값 예시
                        showRoomNameDialog = true
                    }
                }) { Text(if (isMe) "내 정보 보기" else "연락하기") }
            },
            dismissButton = {
                TextButton(onClick = { showProfilePopup = false }) { Text("취소") }
            }
        )
    }
    if (showRoomNameDialog && clickedProfile != null) {
        val (targetNickname, targetEmail) = clickedProfile!!
        AlertDialog(
            onDismissRequest = { showRoomNameDialog = false },
            title = { Text("채팅방 이름") },
            text = {
                OutlinedTextField(
                    value = roomNameInput,
                    onValueChange = { roomNameInput = it },
                    placeholder = { Text("채팅방 이름을 입력하세요") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showRoomNameDialog = false
                    // ★ API 호출 → 방 보장 → 채팅 화면 이동
                    chatViewModel.setMyEmail(currentUserEmail)
                    chatViewModel.ensureRoomAnd(
                        requesterEmail = currentUserEmail,
                        requesterNickname = currentUserNickname.ifBlank { "나" },
                        targetEmail = targetEmail,
                        targetNickname = targetNickname,
                        roomName = roomNameInput.ifBlank { "${targetNickname}님과의 채팅" },
                        onSuccess = { roomId, title ->
                            navController.navigate(
                                Screen.WithChat(roomId, title).route
                            )
                        },
                        onError = {
                            // TODO: 에러 처리 (토스트 등)
                        }
                    )
                }) { Text("확인") }
            },
            dismissButton = {
                TextButton(onClick = { showRoomNameDialog = false }) { Text("취소") }
            }
        )
    }

    if (showBottomSheet) {
        EditDeleteBottomSheet(
            isPostMenu = isPostMenu,
            onDismiss = { showBottomSheet = false },
            onEditClick = {
                showBottomSheet = false
                if (!isPostMenu && selectedComment != null) {
                    showEditDialog = true
                    editContent = selectedComment!!.content
                } else {
                    navController.navigate(Screen.WithPostWrite(postId).route)
                }
            },

            onDeleteClick = {
                showBottomSheet = false
                if (isPostMenu) {
                    // 게시글: 바로 삭제하지 않고 확인 다이얼로그
                    showDeleteConfirm = true
                } else {
                    // 댓글: 기존처럼 즉시 삭제 + 토스트
                    selectedComment?.let {
                        withPostDetailViewModel.deleteComment(it.id)
                        Toast.makeText(context, "댓글이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    // ✅ 게시글 삭제 확인 다이얼로그 (네/아니요)
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("삭제 확인") },
            text = { Text("정말로 삭제할까요?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    withPostDetailViewModel.deletePost(
                        onSuccess = {
                            Toast.makeText(context, "게시물이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                            scope.launch {
                                delay(500)
                                navController.navigate(Screen.With.route) {
                                    popUpTo(Screen.WithDetail.routeWithArg) { inclusive = true }
                                }
                            }
                        },
                        onFailure = {
                            Toast.makeText(context, "삭제에 실패했습니다.", Toast.LENGTH_SHORT).show()
                        }
                    )
                }) { Text("네") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    Toast.makeText(context, "삭제가 취소되었습니다.", Toast.LENGTH_SHORT).show()
                }) { Text("아니오") }
            }
        )
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("댓글 수정") },
            text = {
                TextField(
                    value = editContent,
                    onValueChange = { editContent = it },
                    placeholder = { Text("수정할 내용을 입력하세요") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    selectedComment?.let {
                        withPostDetailViewModel.updateComment(
                            commentId = it.id,
                            content = editContent,
                        )
                    }
                    showEditDialog = false
                }) { Text("수정") }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("취소") }
            }
        )
    }
}

@Composable
fun WithPostDetailContent(
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
        CommentInputBar(
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

@Composable
fun TopBar(
    onBackClick: () -> Unit,
    showMenu: Boolean,
    onMenuClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
        }
        Text(
            text = "게시글 상세",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.weight(1f),
            color = Color.Black
        )
        if (showMenu) {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.MoreVert, contentDescription = "메뉴")
            }
        }
    }
}

@Composable
fun PostHeader(
    post: WithPost,
    onProfileClick: (author: String, email: String) -> Unit,
) {
    var showFullTitle by remember { mutableStateOf(false) }

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable {
                onProfileClick(post.author, post.authorEmail)
            }
        ) {
            Avatar(
                imageUrl = post.authorProfileUrl,   // ← 모델에 있는 필드명 사용
                nameFallback = post.author,
                size = 40.dp
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = post.author, fontWeight = FontWeight.Bold)
                Text(text = post.date, fontSize = 12.sp, color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 제목 2줄 제한 + 더보기
        Text(
            text = post.title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            maxLines = if (showFullTitle) Int.MAX_VALUE else 2,
            overflow = TextOverflow.Ellipsis
        )
        if (!showFullTitle && post.title.length > 20) { // 길이 임계값은 적절히 조정
            Text(
                text = "더보기",
                color = Color.Blue,
                fontSize = 14.sp,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clickable { showFullTitle = true }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(text = post.content)
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun CommentItem(
    comment: CommentResponse,
    onReplyClick: (CommentResponse) -> Unit,
    onProfileClick: (author: String, email: String) -> Unit,
    currentUserEmail: String,
    onMenuClick: (CommentResponse) -> Unit
) {
    Column(modifier = Modifier.padding(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onProfileClick(comment.author, comment.authorEmail) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(
                imageUrl = comment.authorProfileUrl,  // 또는 comment.profileImageUrl 등 실제 필드명
                nameFallback = comment.author,
                size = 36.dp
            )
            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(comment.author, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(comment.timestamp, fontSize = 12.sp, color = Color.Gray)
            }

            if (comment.authorEmail == currentUserEmail) {
                IconButton(onClick = { onMenuClick(comment) }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "댓글 메뉴")
                }
            }
        }

        Text(comment.content, modifier = Modifier.padding(start = 44.dp, top = 4.dp), fontSize = 13.sp)

        // 답글 버튼 개선
        TextButton(
            onClick = { onReplyClick(comment) },
            modifier = Modifier
                .padding(start = 44.dp)
        ) {
            Icon(Icons.Default.Reply, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("답글 달기", fontSize = 12.sp, color = Color.Gray)
        }

        // 대댓글 표시 로직 그대로...
        comment.replies.forEach { reply ->
            Column(
                modifier = Modifier
                    .padding(start = 64.dp, top = 4.dp)
                    .clickable { onProfileClick(reply.author, reply.authorEmail) }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(
                        imageUrl = reply.authorProfileUrl,    // 실제 필드명으로
                        nameFallback = reply.author,
                        size = 30.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(reply.author, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text(reply.timestamp, fontSize = 11.sp, color = Color.Gray)
                    }
                    if (reply.authorEmail == currentUserEmail) {
                        IconButton(onClick = { onMenuClick(reply) }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "대댓글 메뉴")
                        }
                    }
                }
                Text(reply.content, modifier = Modifier.padding(start = 38.dp, top = 2.dp), fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun CommentInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    placeholderText: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F5))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White),
            placeholder = { Text(placeholderText) },
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                disabledContainerColor = Color.White
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
            onClick = onSend,
            enabled = value.isNotBlank(),
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (value.isNotBlank()) Color(0xFF1976D2) else Color.Gray)
        ) {
            Icon(Icons.Default.Send, contentDescription = "전송", tint = Color.White)
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditDeleteBottomSheet(
    isPostMenu: Boolean,
    onDismiss: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            TextButton(
                onClick = onEditClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("수정", fontSize = 16.sp, color = Color.Black)
            }
            TextButton(
                onClick = onDeleteClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("삭제", fontSize = 16.sp, color = Color.Red)
            }
        }
    }
}
@Composable
fun Avatar(
    imageUrl: String?,
    nameFallback: String,
    size: Dp
) {
    val baseModifier = Modifier.size(size).clip(CircleShape)

    if (imageUrl.isNullOrBlank()) {
        DefaultAvatar(size)      // ← 이니셜 없이 빈 원
        return
    }

    // members 키 → 풀 URL, http → https
    val safeUrl = imageUrl.trim().let { raw ->
        when {
            raw.startsWith("https://") -> raw
            raw.startsWith("http://")  -> raw.replaceFirst("http://", "https://")
            raw.startsWith("/members/")-> S3_BASE + raw.trimStart('/')
            raw.startsWith("members/") -> S3_BASE + raw
            else -> raw
        }
    }

    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(safeUrl)
            .crossfade(true)
            .build(),
        contentDescription = "$nameFallback 프로필",
        modifier = baseModifier,
        contentScale = ContentScale.Crop,
        loading = { DefaultAvatar(size) },
        error   = { DefaultAvatar(size) }
    )
}

@Composable
private fun DefaultAvatar(size: Dp) {
    Image(
        painter = painterResource(id = R.drawable.profile),
        contentDescription = "기본 프로필",
        modifier = Modifier
            .size(size)
            .clip(CircleShape),
        contentScale = ContentScale.Crop
    )
}
