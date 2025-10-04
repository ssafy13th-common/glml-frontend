package com.ssafy.a705.feature.mypage.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ssafy.a705.feature.controller.viewmodel.MyMessagesViewModel
import com.ssafy.a705.feature.mypage.ui.viewmodel.MyPageViewModel
import com.ssafy.a705.feature.mypage.ui.component.CollapsibleSearchBarStable
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private val KST: ZoneId = ZoneId.of("Asia/Seoul")
private val TIME_FMT_12H = DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN)
private val DATE_FMT = DateTimeFormatter.ofPattern("yyyy.MM.dd")
// === 기존 유틸/데이터 클래스 유지 ===
data class ChatItem(
    val roomId: String,
    val name: String,
    val lastMessage: String,
    val lastDate: LocalDateTime
)

fun formatRelativeDate(date: LocalDateTime): String {
    val msgDate = date.toLocalDate()
    val today = LocalDate.now(KST)
    val days = ChronoUnit.DAYS.between(msgDate, today)

    return when {
        days == 0L -> date.format(TIME_FMT_12H)        // 오늘: "오후 2:58"
        days == 1L -> "어제"
        days == 2L -> "그저께"
        days in 3..6 -> "${days}일 전"
        days in 7..29 -> "${days / 7}주 전"
        days in 30..364 -> "${days / 30}개월 전"
        days >= 365 -> "${days / 365}년 전"
        else -> msgDate.format(DATE_FMT)
    }
}

// === CollapsibleSearchBarStable 사용 버전 ===
@Composable
fun ChatListScreen(
    chatItems: List<ChatItem>,
    onBack: () -> Unit,
    onClickRoom: (roomId: String, title: String) -> Unit = { _, _ -> }
) {
    var searchExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // 물리 뒤로가기: 검색 펼침 상태면 닫고, 아니면 화면 이탈
    BackHandler(enabled = true) {
        if (searchExpanded) {
            searchExpanded = false
            searchQuery = ""
        } else {
            onBack()
        }
    }

    val filteredList = remember(chatItems, searchQuery) {
        chatItems.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 🔹 상단바 교체: CollapsibleSearchBarStable
        CollapsibleSearchBarStable(
            title = "내 채팅",
            searchExpanded = searchExpanded,
            onToggle = { searchExpanded = !searchExpanded },
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            onBack = onBack,                // ← 화면 이탈
            onClear = { searchQuery = "" }  // ← 입력만 비우고 닫기는 onToggle이 처리
        )

        Divider(color = Color(0xFFE0E0E0), thickness = 1.dp)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp)
        ) {
            items(filteredList) { chat ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onClickRoom(chat.roomId, chat.name) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
//                    Icon(
//                        Icons.Default.Person,
//                        contentDescription = null,
//                        modifier = Modifier
//                            .size(48.dp)
//                            .clip(CircleShape)
//                            .background(Color.LightGray)
//                            .padding(8.dp)
//                    )

                    Spacer(modifier = Modifier.width(15.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 10.dp)
                    ) {
                        Text(text = chat.name, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(7.dp))
                        Text(
                            text = chat.lastMessage,
                            fontSize = 14.sp,
                            color = Color.Gray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = formatRelativeDate(chat.lastDate),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                Divider(color = Color(0xFFE0E0E0), thickness = 1.dp)
            }
        }
    }
}

@Composable
fun MessageListEntry(
    vm: MyMessagesViewModel = hiltViewModel(),
    myPageVm: MyPageViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onClickRoom: (String, String) -> Unit = { _, _ -> }
) {
    // 내 프로필(email 포함) 구독
    val profile by myPageVm.profile.collectAsState()

    // 최초 진입 시 프로필 로드
    LaunchedEffect(Unit) {
        myPageVm.loadMyProfile()
    }

    // email이 준비되면 그때 메시지 목록 로드
    LaunchedEffect(profile?.email) {
        profile?.email?.let { myEmail ->
            vm.load(myEmail)     // ← 여기서 내 이메일 기준으로 상대 닉네임 계산해 로드
        }
    }

    val isLoading = vm.loading || profile?.email == null
    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        ChatListScreen(
            chatItems = vm.items,
            onBack = onBack,
            onClickRoom = onClickRoom
        )
    }
}