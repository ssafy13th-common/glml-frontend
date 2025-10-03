package com.ssafy.a705.feature.group.member

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.ssafy.a705.group.common.component.AddMemberDialog
import com.ssafy.a705.group.common.component.GroupTopBar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background

@Composable
fun GroupMemberScreen(
    navController: NavController,
    groupId: Long,
    viewModel: GroupMemberViewModel = hiltViewModel(),
    onBackClick: () -> Unit ={}
) {
    LaunchedEffect(groupId) {
        viewModel.loadGroup(groupId)
    }
    val members by viewModel.members.collectAsStateWithLifecycle()
    val groupName by viewModel.groupName.collectAsStateWithLifecycle()


    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        // 상단바
        GroupTopBar(title = "그룹", onBackClick = onBackClick)
        Spacer(Modifier.height(10.dp))
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(Modifier.fillMaxWidth(), 1.dp, Color.LightGray)
        Spacer(Modifier.height(12.dp))

        // 멤버 리스트
        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(
                items = members,
                key = { it.id }
            ) { member ->
                MemberRow(member = member)
                Spacer(Modifier.height(8.dp))
            }
        }

        // 하단 + 버튼
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                .clickable { showAddDialog = true },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Add, contentDescription = "멤버 추가")
        }
    }

    // 친구 초대
    if (showAddDialog) {
        AddMemberDialog(
            viewModel = viewModel,
            onDismiss = { showAddDialog = false },
            onMemberSelected = { member ->
                viewModel.inviteMember(groupId, member)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun MemberRow(member: MemberItem) {
    // 디버깅을 위한 로그 출력
    LaunchedEffect(member) {
        println("🔍 MemberRow - 멤버: ${member.name}, profileImageUrl: ${member.profileImageUrl}")
    }
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        // 프로필 이미지
        if (member.profileImageUrl != null && member.profileImageUrl.isNotBlank()) {
            println("🔍 MemberRow - 이미지 로딩 시도: ${member.profileImageUrl}")
            AsyncImage(
                model = member.profileImageUrl,
                contentDescription = "프로필 이미지",
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .border(1.dp, Color.LightGray, CircleShape),
                contentScale = ContentScale.Crop,
                placeholder = null,
                error = null
            )
        } else {
            println("🔍 MemberRow - profileImageUrl이 null이거나 빈 문자열: ${member.name}")
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "기본 프로필",
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .border(1.dp, Color.LightGray, CircleShape)
                    .padding(4.dp),
                tint = Color.Gray
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(member.name, style = MaterialTheme.typography.titleMedium)
                // 관리자 권한 표시
                if (member.role == "ADMIN") {
                    Text(
                        text = "관리자",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF2196F3),
                        modifier = Modifier
                            .background(
                                color = Color(0xFFE3F2FD),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Text("정산비 : ${member.settlementAmount}원")
            if (member.lateFee > 0) {
                Text("지각비 : ${member.lateFee}원", color = Color.Red)
            }
        }
    }
}


/*
@Preview(showBackground = true)
@Composable
fun PreviewGroupMemberScreen() {
    val previewViewModel = GroupMemberViewModel().apply {
        addMember(MemberItem("1", "나", settlementAmount = 57000, lateFee = 1000))
        addMember(MemberItem("2", "미니멀리스트", settlementAmount = 38000))
        addMember(MemberItem("3", "규규진진", settlementAmount = 57000))
        addMember(MemberItem("4", "큐티은수", settlementAmount = 38000, lateFee = 1200))
    }
    GroupMemberScreen(viewModel = previewViewModel, groupName = "강릉 여행팟", onBackClick = {})
}
*/