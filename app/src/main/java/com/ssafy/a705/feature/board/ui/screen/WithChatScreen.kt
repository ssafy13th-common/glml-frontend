package com.ssafy.a705.feature.board.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ssafy.a705.feature.controller.viewmodel.AppChatBridgeViewModel
import com.ssafy.a705.feature.controller.viewmodel.MyPageViewModel
import com.ssafy.a705.feature.chatSet.dto.ChatMessage
import kotlinx.coroutines.flow.distinctUntilChanged
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun WithChatScreen(
    roomId: String,
    onBack: () -> Unit,
    viewModel: AppChatBridgeViewModel = hiltViewModel(),
    myPageViewModel: MyPageViewModel = hiltViewModel()
) {
    val myProfile by myPageViewModel.profile.collectAsState()
    LaunchedEffect(Unit) { myPageViewModel.loadMyProfile() }
    LaunchedEffect(myProfile?.email) {
        myProfile?.email?.let { viewModel.setMyEmail(it) }
    }
    // 입장/연결
    LaunchedEffect(roomId) { viewModel.enterRoom(roomId) }
    // 종료 시 연결만 해제
    DisposableEffect(Unit) { onDispose { viewModel.disconnect() } }

    // 화면에서 필요한 상태 수집 (리플렉션 X)
    val messages    by viewModel.messages.collectAsState(initial = emptyList())
    val myId        by viewModel.myId.collectAsState(initial = 0)
    val nickByHash  by viewModel.nickByHash.collectAsState(initial = emptyMap())
    val readByMsgId by viewModel.readByMsgId.collectAsState(initial = emptyMap())

    WithChatContent(
        messages = messages,
        myId = myId,
        nameOf = { id -> nickByHash[id] ?: id.toString() },
        unreadOf = { msgId ->
            val read = readByMsgId[msgId] ?: 0           // 읽은 수
            val people = nickByHash.size                 // 참여자 수
            (people - read).coerceAtLeast(0)             // 안읽음 수
        },
        onSendMessage = { viewModel.sendMessage(it) },
        onLoadOlder   = { viewModel.loadOlder() }
    )
}

@Composable
fun WithChatContent(
    messages: List<ChatMessage>,
    myId: Int,
    nameOf: (Int) -> String,
    unreadOf: (Int) -> Int,
    onSendMessage: (String) -> Unit,
    onLoadOlder: () -> Unit,
) {
    var input by remember { mutableStateOf("") }

    //새 메세지 오면 스크롤 아래
    val listState = rememberLazyListState()
    LaunchedEffect(messages.firstOrNull()?.id) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(0)
    }
    //오래된 쪽이면 과거 페이지 로드
    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            // reverseLayout=true: index가 커질수록 과거쪽
            lastVisible >= (info.totalItemsCount - 1 - 2)
        }.distinctUntilChanged().collect { shouldLoad ->
            if (shouldLoad) onLoadOlder()
        }
    }

    // 날짜별 그룹핑
    val grouped: Map<String, List<ChatMessage>> = remember(messages) {
        messages
            .groupBy { SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(it.timestamp)) }
            .toSortedMap(compareByDescending { it })
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            reverseLayout = true,                            // ✅ 아래가 최신
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            grouped.forEach { (_, groupRaw) ->
                // 같은 날 안에서도 최신 메시지가 아래로 가도록 내림차순
                val group = groupRaw.sortedByDescending { it.timestamp }

                // ✅ 메시지 먼저 그리고, 날짜칩은 맨 뒤에 넣어서
                // reverseLayout=true 기준 '위쪽(오래된 쪽)'에 보이게 함
                items(group, key = { it.id }) { message ->
                    val isMe = message.senderId == myId
                    val timeText = SimpleDateFormat("a h:mm", Locale.KOREAN)
                        .format(Date(message.timestamp))
                    val unread = unreadOf(message.id) // ← 안읽음 수(인원-읽은수)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp, horizontal = 8.dp),
                        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                    ) {
                        if (!isMe) {
                            Column(horizontalAlignment = Alignment.Start) {
                                // 닉네임
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // 프로필 사진은 서버 URL 이슈로 주석/플레이스홀더 처리 유지
//                                    Box(
//                                        modifier = Modifier
//                                            .size(36.dp)
//                                            .clip(CircleShape)
//                                            .background(Color.LightGray)
//                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = nameOf(message.senderId),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.DarkGray
                                    )
                                }
                                Spacer(Modifier.height(2.dp))

                                // 말풍선 + 시간/안읽음 (아래 배치)
                                Column(horizontalAlignment = Alignment.Start) {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                Color(0xFFEDEDED),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .padding(12.dp)
                                    ) { Text(text = message.content) }

                                    Spacer(Modifier.height(2.dp))
                                    Row {
                                        Text(
                                            text = timeText,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Gray
                                        )
                                        if (unread > 0) {
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                text = unread.toString(),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Column(horizontalAlignment = Alignment.End) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                Color(0xFFCCE5FF),
                                                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                                            )
                                            .padding(12.dp)
                                    ) { Text(text = message.content) }

                                    Spacer(Modifier.height(2.dp))
                                    Row {
                                        Text(
                                            text = timeText,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Gray
                                        )
                                        if (unread > 0) {
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                text = unread.toString(),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                }
                            }
                        }

                    }
                }
                val dateText = SimpleDateFormat("M월 d일 EEEE", Locale.KOREAN)
                    .format(Date(group.last().timestamp)) // 그룹의 가장 오래된 것 기준
                item {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = dateText,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // 프로젝트 공용 입력바
        CommentInputBar(
            value = input,
            onValueChange = { input = it },
            onSend = {
                if (input.isNotBlank()) {
                    onSendMessage(input)
                    input = ""
                }
            },
            placeholderText = "메시지 보내기"
        )
    }
}
