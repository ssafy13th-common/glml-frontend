package com.ssafy.a705.feature.board.ui.screen

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ssafy.a705.common.navigation.Screen
import com.ssafy.a705.feature.board.data.model.response.CommentResponse
import com.ssafy.a705.feature.board.ui.component.EditDeleteBottomSheet
import com.ssafy.a705.feature.board.ui.component.PostDetailContent
import com.ssafy.a705.feature.board.ui.viewmodel.PostDetailViewModel
import com.ssafy.a705.feature.controller.viewmodel.AppChatBridgeViewModel
import com.ssafy.a705.feature.controller.viewmodel.MyPageViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PostDetailScreen(
    postId: Long,
    navController: NavController,
    withPostDetailViewModel: PostDetailViewModel = hiltViewModel(),
    myPageViewModel: MyPageViewModel = hiltViewModel(),
    chatViewModel: AppChatBridgeViewModel = hiltViewModel()
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
        PostDetailContent(
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