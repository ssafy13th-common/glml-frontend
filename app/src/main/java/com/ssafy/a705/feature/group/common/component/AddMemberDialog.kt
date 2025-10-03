package com.ssafy.a705.feature.group.common.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ssafy.a705.group.member.GroupMemberViewModel
import com.ssafy.a705.group.member.MemberItem
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AddMemberDialog(
    viewModel: GroupMemberViewModel = hiltViewModel(), // 화면에서 넘겨도 되고, 여기서 받아도 OK
    onDismiss: () -> Unit,
    onMemberSelected: (MemberItem) -> Unit            // ✅ 콜백만 받는다 (여기서 groupId/상태 변경 X)
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("멤버 초대") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TextField(
                    value = searchQuery,
                    onValueChange = viewModel::updateSearchQuery,
                    placeholder = { Text("닉네임으로 검색") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                if (searchResults.isEmpty()) {
                    Text("검색 결과가 없습니다.", style = MaterialTheme.typography.bodySmall)
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 200.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(searchResults) { member ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onMemberSelected(member) } // ✅ 선택 시 부모 콜백 호출
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(member.name, style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.weight(1f))
                                member.email?.let { email ->
                                    Text(maskEmail(email), style = MaterialTheme.typography.bodyMedium)
                                }
                                Spacer(Modifier.weight(1f))
                                Text("초대", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("닫기") }
        }
    )

}

private fun maskEmail(email: String): String {
    val parts = email.split("@")
    if (parts.size != 2) return email

    val localPart = parts[0]
    val domainPart = parts[1]

    val visibleLocal = localPart.take(2)
    val maskedLocal = visibleLocal + "*".repeat((localPart.length - 2).coerceAtLeast(0))

    return "$maskedLocal@$domainPart"
}
