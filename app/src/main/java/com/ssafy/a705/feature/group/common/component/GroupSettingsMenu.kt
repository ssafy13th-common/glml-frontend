package com.ssafy.a705.feature.group.common.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ssafy.a705.feature.group.list.GroupDeleteViewModel
import com.ssafy.a705.feature.group.member.GroupMemberViewModel
import com.ssafy.a705.common.navigation.GroupNavRoutes

@Composable
fun GroupSettingsMenu(
    navController: NavController,
    groupId: Long
) {
    val viewModel: GroupMemberViewModel = hiltViewModel()
    val deleteviewmodle: GroupDeleteViewModel = hiltViewModel()

    var expanded by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = Color.Gray
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color.White)
        ) {
            DropdownMenuItem(
                text = { Text("친구 초대") },
                onClick = {
                    expanded = false
                    showAddDialog = true
                }
            )
            DropdownMenuItem(
                text = { Text("정보 수정") },
                onClick = {
                    expanded = false
                    navController.navigate(
                        GroupNavRoutes.EditWithId(groupId)
                    )
                }
            )
            DropdownMenuItem(
                text = { Text("인원 관리") },
                onClick = {
                    expanded = false
                    navController.navigate(
                        GroupNavRoutes.MembersWithId(groupId)
                    )
                }
            )
            DropdownMenuItem(
                text = { Text("그룹 삭제", color = Color.Red) },
                onClick = {
                    expanded = false
                    showDeleteDialog = true
                }
            )
        }
    }

    // 친구 초대 다이얼로그 (메뉴 내부에서 직접 관리)
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

    // 그룹 삭제 다이얼로그 (메뉴 내부에서 직접 관리)
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("그룹 삭제") },
            text = { Text("정말로 이 그룹을 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        deleteviewmodle.deleteGroup(
                            groupId = groupId,
                            onSuccess = {
                                // 그룹 리스트 화면으로 돌아가기
                                navController.popBackStack()
                            },
                            onError = { errorMessage ->
                                // 에러 메시지를 스낵바로 표시
                                expanded = false
                                // TODO: 스낵바 표시 로직 구현 필요
                                println("❌ 그룹 삭제 실패: $errorMessage")
                            }
                        )
                    }
                ) { Text("삭제", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("취소") }
            }
        )
    }
}
