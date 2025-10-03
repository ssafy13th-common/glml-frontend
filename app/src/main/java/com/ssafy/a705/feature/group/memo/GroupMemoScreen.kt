package com.ssafy.a705.feature.group.memo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.ssafy.a705.feature.group.common.component.GroupStatusChip
import com.ssafy.a705.feature.group.common.component.GroupStatusChipSize
import com.ssafy.a705.feature.group.common.component.GroupTopBar
import com.ssafy.a705.feature.group.common.model.Memo

@Composable
fun GroupMemoScreen(
    navController: NavController,
    groupId: Long,
    viewModel: GroupMemoViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {}
) {
    println("🔍 GroupMemoScreen 렌더링 - groupId: $groupId")
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(groupId) {
        println("🔍 LaunchedEffect 실행 - groupId: $groupId")
        viewModel.loadGroupInfoAndMemos(groupId)
    }

    // 화면이 포커스를 받을 때마다 새로고침 (편집 후 돌아올 때 등)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                println("🔄 화면 포커스 - 그룹 정보 새로고침")
                viewModel.loadGroupInfoAndMemos(groupId)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(0.dp, 0.dp, 0.dp, 50.dp)
            .background(Color.White)
    ) {
        // 상단 헤더
        GroupTopBar(
            title = "그룹",
            onBackClick = onBackClick,
            groupId = groupId,
            navController = navController
        )

        // 로딩 상태 표시
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.CircularProgressIndicator(
                    color = Color(0xFF2196F3)
                )
            }
        } else {
            // 메인 콘텐츠 영역
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // 그룹 제목과 상태 태그
                GroupTitleSection(
                    groupName = state.groupName,
                    status = state.status
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 모임 정보 섹션
                MeetingInfoSection(
                    gatheringTime = state.gatheringTime,
                    gatheringLocation = state.gatheringLocation
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 메모 목록 헤더와 플러스 버튼
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "메모",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    
                    IconButton(
                        onClick = { viewModel.createNewMemo() },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "새 메모 추가",
                            tint = Color(0xFF2196F3),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 메모 목록
                val listState = rememberLazyListState()
                
                // 새 메모가 생성되면 맨 위로 스크롤
                val hasNewMemo = remember { derivedStateOf { state.memos.any { it.id < 0 } } }
                LaunchedEffect(hasNewMemo.value) {
                    if (hasNewMemo.value) {
                        listState.animateScrollToItem(0)
                    }
                }
                
                // 새 메모 저장 후 스크롤 플래그가 설정되면 맨 위로 스크롤
                LaunchedEffect(state.shouldScrollToTop) {
                    if (state.shouldScrollToTop && state.memos.isNotEmpty()) {
                        listState.animateScrollToItem(0)
                        // 스크롤 후 플래그 리셋
                        viewModel.resetScrollFlag()
                    }
                }
                
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (state.memos.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "아직 메모가 없습니다.\n새로운 메모를 작성해보세요!",
                                    color = Color.Gray,
                                    fontSize = 16.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        items(state.memos, key = { it.id }) { memo ->
                            MemoCard(
                                memo = memo,
                                onContentChange = { viewModel.updateMemo(memo.id, it) },
                                onSave = {
                                    viewModel.saveMemo(groupId, memo.id, memo.content)
                                },
                                onDelete = {
                                    viewModel.deleteMemo(groupId, memo.id)
                                },
                                onEdit = {
                                    viewModel.editMemo(memo.id)
                                },
                                onCardClick = {
                                    // 내가 작성한 메모만 클릭 시 편집 모드로 전환
                                    if (memo.isMine && !memo.isEditing) {
                                        viewModel.editMemo(memo.id)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GroupTitleSection(
    groupName: String?,
    status: String?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 그룹 제목
        Text(
            text = groupName ?: "그룹명",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2196F3) // 파란색
        )

        // 상태 태그 (3가지 상태만 허용)
        GroupStatusChip(status = status ?: "미정", size = GroupStatusChipSize.MEDIUM)
    }
}

@Composable
fun MeetingInfoSection(
    gatheringTime: String?,
    gatheringLocation: String?
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // 제목
        Text(
            text = "모임 정보",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        // 모임 시간
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "모임 시간 : ",
                fontSize = 14.sp,
                color = Color.Black
            )
            Text(
                text = gatheringTime ?: "미정",
                fontSize = 14.sp,
                color = if (gatheringTime != null) Color.Black else Color.Gray
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 모임 장소
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "모임 장소 : ",
                fontSize = 14.sp,
                color = Color.Black
            )
            Text(
                text = gatheringLocation ?: "미정",
                fontSize = 14.sp,
                color = if (gatheringLocation != null) Color.Black else Color.Gray
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 구분선
        androidx.compose.material3.HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            thickness = 1.dp,
            color = Color.LightGray
        )
    }
}



@Composable
fun MemoCard(
    memo: Memo,
    onContentChange: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onCardClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 180.dp)
            .clickable(
                enabled = memo.isMine && !memo.isEditing,
                onClick = onCardClick
            ),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF2AB)),
        shape = MaterialTheme.shapes.medium
    ) {
        Box(Modifier.fillMaxSize()) {
            // 우상단 버튼들 (조건부 표시)
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 새로운 메모 작성 중이거나 수정 상태인 경우: 체크(저장) 버튼만 표시
                if (memo.isEditing || memo.id < 0) {
                    IconButton(
                        onClick = onSave,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "저장",
                            tint =  Color(0xFF4CAF50),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else if (memo.isMine) {
                    // 내가 작성한 메모인 경우: 수정(연필) 버튼, 삭제(x) 버튼 순으로 표시
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "수정",
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "삭제",
                            tint = Color.Red,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                // 다른 멤버가 작성한 메모인 경우: 아무 버튼도 표시하지 않음
            }
            
            // 메모 내용
            if (memo.isEditing) {
                // 편집 모드
                TextField(
                    value = memo.content,
                    onValueChange = { newContent ->
                        // 500자 제한 적용
                        if (newContent.length <= 500) {
                            onContentChange(newContent)
                        }
                    },
                    placeholder = { Text("메모를 입력하세요...") },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                        .padding(top = 48.dp), // 상단 버튼들과 겹치지 않도록
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Color.Black
                    )
                )
            } else {
                // 일반 모드
                Text(
                    text = if (memo.content.isBlank()) "메모를 입력하세요..." else memo.content,
                    modifier = Modifier
                        .padding(12.dp)
                        .padding(top = 48.dp) // 상단 버튼들과 겹치지 않도록
                        .align(Alignment.TopStart),
                    fontSize = 14.sp,
                    color = if (memo.content.isBlank()) Color.Gray else Color.Black
                )
            }
        }
    }
}

