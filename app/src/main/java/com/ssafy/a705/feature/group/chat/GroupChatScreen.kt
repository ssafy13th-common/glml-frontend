package com.ssafy.a705.feature.group.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.ssafy.a705.common.components.InputBar
import com.ssafy.a705.feature.group.common.component.GroupStatusChip
import com.ssafy.a705.feature.group.common.component.GroupStatusChipSize
import com.ssafy.a705.feature.group.common.component.GroupTopBar
import com.ssafy.a705.feature.group.common.model.ChatMessage
import com.ssafy.a705.feature.group.common.model.MessageStatus
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun GroupChatScreen(
    navController: NavController,
    groupId: Long,
    viewModel: GroupChatViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    // 새 메시지 자동 스크롤 (reverseLayout = true 사용 시 첫 번째 아이템이 최신)
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            val firstVisibleItemIndex =
                listState.layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: 0
            if (firstVisibleItemIndex <= 3 || state.messages.firstOrNull()?.isMine == true) {
                listState.animateScrollToItem(0)
            }
        }
    }

    // 초기 로드
    LaunchedEffect(groupId) {
        viewModel.loadGroupInfo(groupId)
    }

    // 화면 포커스 때마다 새로고침
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadGroupInfo(groupId)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // 에러 처리
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    // 무한 스크롤 (reverseLayout = true ⇒ 마지막 인덱스가 오래된 메시지)
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .collect { lastVisibleIndex ->
                val totalItems = state.messages.size
                if (lastVisibleIndex >= totalItems - 5 && !state.isLoading) {
                    viewModel.loadMoreHistory()
                }
            }
    }

    // ===== 날짜별 그룹핑 (KST LocalDate 기준, 최신 날짜 먼저) =====
    val grouped: Map<LocalDate, List<ChatMessage>> = remember(state.messages) {
        state.messages
            .groupBy { msg -> parseToLocalDateKST(msg.timestamp) ?: LocalDate.now(KST_ZONE) }
            .toSortedMap(compareByDescending { it })
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            GroupTopBar(
                title = "그룹",
                onBackClick = onBackClick,
                groupId = groupId,
                navController = navController
            )

            Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(Color.White)
                ) {
                    GroupTitleSection(
                        groupName = state.groupName,
                        status = state.status
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    reverseLayout = true,
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    grouped.forEach { (date, groupRaw) ->
                        // 같은 날 안에서도 최신 메시지가 아래로 가도록 내림차순
                        val group = groupRaw.sortedByDescending { parseEpochMillis(it.timestamp) }

                        items(
                            group,
                            key = { it.messageId.ifEmpty { it.tempId ?: "" } }) { message ->
                            val timeText = formatChatTimeKST(message.timestamp)
                            val unread = if (message.isMine) maxOf(
                                0,
                                message.totalMembers - message.readCount - 1
                            ) else 0

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp, horizontal = 8.dp),
                                horizontalArrangement = if (message.isMine) Arrangement.End else Arrangement.Start
                            ) {
                                if (!message.isMine) {
                                    // 다른 사람 메시지 (왼쪽)
                                    Column(horizontalAlignment = Alignment.Start) {
                                        // 닉네임
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = message.sender,
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
                                            ) {
                                                Text(text = message.message)
                                            }

                                            Spacer(Modifier.height(2.dp))

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = timeText,
                                                    fontSize = 10.sp,
                                                    color = Color.Gray
                                                )

                                                when (message.status) {
                                                    MessageStatus.SENDING -> {
                                                        CircularProgressIndicator(
                                                            modifier = Modifier.size(12.dp),
                                                            strokeWidth = 1.dp
                                                        )
                                                    }

                                                    MessageStatus.SENT -> Unit // 읽음 표시 없음
                                                    MessageStatus.FAILED -> {
                                                        Text(
                                                            text = "전송실패",
                                                            fontSize = 10.sp,
                                                            color = Color.Red
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // 내 메시지 (오른쪽)
                                    Column(horizontalAlignment = Alignment.End) {
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    Color(0xFF2196F3),
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .padding(12.dp)
                                        ) {
                                            Text(
                                                text = message.message,
                                                color = Color.White
                                            )
                                        }

                                        Spacer(Modifier.height(2.dp))

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = timeText,
                                                fontSize = 10.sp,
                                                color = Color.Gray
                                            )

                                            when (message.status) {
                                                MessageStatus.SENDING -> {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(12.dp),
                                                        strokeWidth = 1.dp
                                                    )
                                                }

                                                MessageStatus.SENT -> {
                                                    if (unread > 0) {
                                                        Text(
                                                            text = "읽지 않음 $unread",
                                                            fontSize = 10.sp,
                                                            color = Color.Gray
                                                        )
                                                    }
                                                }

                                                MessageStatus.FAILED -> {
                                                    Text(
                                                        text = "전송실패",
                                                        fontSize = 10.sp,
                                                        color = Color.Red
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 날짜 구분선: "8월 13일 수요일"
                        item {
                            val dateKey = formatDateHeaderKST(date)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = dateKey,
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    modifier = Modifier
                                        .padding(
                                            horizontal = 12.dp,
                                            vertical = 4.dp
                                        ) // 배경 없이 여백만 유지
                                )
                            }
                        }
                    }
                }

                // 동행 채팅과 동일한 입력바 사용
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 50.dp) // 네비게이션 바 위에 표시
                ) {
                    InputBar(
                        value = state.inputText,
                        onValueChange = viewModel::onInputChange,
                        onSend = {
                            if (state.inputText.isNotBlank()) {
                                viewModel.sendMessage()
                            }
                        },
                        placeholderText = "메시지 보내기"
                    )
                }
            }
        }

        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun GroupTitleSection(
    groupName: String,
    status: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = groupName.ifEmpty { "그룹명" },
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2196F3)
        )
        GroupStatusChip(status = status.ifEmpty { "미정" }, size = GroupStatusChipSize.MEDIUM)
    }
}

/* -------------------- 시간/날짜 포맷 유틸 (KST 표기) -------------------- */

// KST 고정 표기
private val KST_ZONE: ZoneId = ZoneId.of("Asia/Seoul")
private val AMPM_HH_MM: DateTimeFormatter =
    DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN)

// "오전/오후 1:23" (이미 한글로 들어온 경우 그대로 사용)
private val KO_AMPM_TIME = Regex("""(오전|오후)\s*(\d{1,2}):(\d{2})""")

// "AM/PM 1:23" (영문 AM/PM → 한글로 변환)
private val EN_AMPM_TIME = Regex("""\b(AM|PM)\s*(\d{1,2}):(\d{2})\b""", RegexOption.IGNORE_CASE)

// 오프셋 없는 문자열 파서들(ISO + 점(.) 구분)
private val LDT_FORMATS: List<DateTimeFormatter> = listOf(
    // ISO (T/공백, 소수점 0~9자리)
    "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS",
    "yyyy-MM-dd'T'HH:mm:ss.SSSSSS",
    "yyyy-MM-dd'T'HH:mm:ss.SSS",
    "yyyy-MM-dd'T'HH:mm:ss",
    "yyyy-MM-dd HH:mm:ss.SSSSSSSSS",
    "yyyy-MM-dd HH:mm:ss.SSSSSS",
    "yyyy-MM-dd HH:mm:ss.SSS",
    "yyyy-MM-dd HH:mm:ss",
    "yyyy-MM-dd HH:mm",
    // 점(.) 구분 (예: 2025.08.14 10:30:15)
    "yyyy.MM.dd HH:mm:ss.SSSSSSSSS",
    "yyyy.MM.dd HH:mm:ss.SSSSSS",
    "yyyy.MM.dd HH:mm:ss.SSS",
    "yyyy.MM.dd HH:mm:ss",
    "yyyy.MM.dd HH:mm"
).map(DateTimeFormatter::ofPattern)

/** 메시지 시간 표기: 여러 형태의 timestamp 문자열을 KST로 변환해 "오전/오후 h:mm"으로 보여준다. */
private fun formatChatTimeKST(raw: String?): String {
    if (raw.isNullOrBlank()) return ""

    // 이미 한글 AM/PM 표기면 날짜 제거 후 그대로 반환
    KO_AMPM_TIME.find(raw)?.let { m ->
        val ampm = m.groupValues[1]
        val h = m.groupValues[2].toInt()
        val mm = m.groupValues[3]
        return "$ampm $h:$mm"
    }
    // 영문 AM/PM → 한글로 변환
    EN_AMPM_TIME.find(raw)?.let { m ->
        val ampmKo = if (m.groupValues[1].equals("PM", true)) "오후" else "오전"
        val h = m.groupValues[2].toInt()
        val mm = m.groupValues[3]
        return "$ampmKo $h:$mm"
    }

    // 1) 오프셋 포함 ISO → 즉시 KST로
    runCatching { OffsetDateTime.parse(raw).atZoneSameInstant(KST_ZONE) }
        .getOrNull()
        ?.let { return it.format(AMPM_HH_MM) }

    // 2) epoch(초/밀리초) 숫자
    if (raw.all { it.isDigit() }) {
        val epoch = raw.toLong()
        val instant =
            if (raw.length > 10) Instant.ofEpochMilli(epoch) else Instant.ofEpochSecond(epoch)
        return instant.atZone(KST_ZONE).format(AMPM_HH_MM)
    }

    // 3) 오프셋 없는 문자열(T/공백/.) 소수점 0~9자리
    val ldt = LDT_FORMATS.asSequence()
        .mapNotNull { fmt -> runCatching { LocalDateTime.parse(raw, fmt) }.getOrNull() }
        .firstOrNull() ?: return raw

    return ldt.atZone(KST_ZONE).format(AMPM_HH_MM)
}

/** timestamp 문자열을 KST 기준 LocalDate로 변환 (날짜 그룹핑용) */
private fun parseToLocalDateKST(raw: String?): LocalDate? {
    if (raw.isNullOrBlank()) return null

    // 1) 오프셋 포함 ISO
    runCatching {
        OffsetDateTime.parse(raw).atZoneSameInstant(KST_ZONE).toLocalDate()
    }.getOrNull()?.let { return it }

    // 2) epoch(초/밀리초)
    if (raw.all { it.isDigit() }) {
        val epoch = raw.toLong()
        val instant = if (raw.length > 10)
            Instant.ofEpochMilli(epoch)
        else
            Instant.ofEpochSecond(epoch)
        return instant.atZone(KST_ZONE).toLocalDate()
    }

    // 3) 오프셋 없는 문자열 (T/공백/.) 다양한 포맷 시도
    LDT_FORMATS.asSequence()
        .mapNotNull { fmt -> runCatching { LocalDateTime.parse(raw, fmt) }.getOrNull() }
        .firstOrNull()
        ?.let { return it.atZone(KST_ZONE).toLocalDate() }

    return null
}

/** 정렬/비교용: timestamp → epoch millis (파싱 실패 시 0) */
private fun parseEpochMillis(raw: String?): Long {
    if (raw.isNullOrBlank()) return 0L

    // 1) 오프셋 포함 ISO
    runCatching {
        OffsetDateTime.parse(raw).toInstant().toEpochMilli()
    }.getOrNull()?.let { return it }

    // 2) epoch 숫자
    if (raw.all { it.isDigit() }) {
        return if (raw.length > 10) raw.toLong()
        else raw.toLong() * 1000
    }

    // 3) 오프셋 없는 문자열
    LDT_FORMATS.asSequence()
        .mapNotNull { fmt -> runCatching { LocalDateTime.parse(raw, fmt) }.getOrNull() }
        .firstOrNull()
        ?.let { return it.atZone(KST_ZONE).toInstant().toEpochMilli() }

    return 0L
}

/** 날짜 헤더 포맷: "8월 13일 수요일" */
private fun formatDateHeaderKST(date: LocalDate): String {
    val fmt = DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN)
    return date.format(fmt)
}
