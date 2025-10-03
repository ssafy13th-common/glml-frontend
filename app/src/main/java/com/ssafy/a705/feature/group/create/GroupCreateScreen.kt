package com.ssafy.a705.feature.group.create

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.ssafy.a705.feature.group.common.component.GroupTopBar
import com.ssafy.a705.feature.group.common.component.TimePickerDialog

@Composable
fun GroupCreateScreen(
    viewModel: GroupCreateViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {},
    onCreateComplete: (Long) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showTimePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            GroupTopBar(onBackClick = onBackClick)
            Spacer(modifier = Modifier.height(24.dp))

            // 그룹명
            Text("그룹명 *", fontWeight = FontWeight.Bold, fontSize = 19.sp)
            OutlinedTextField(
                value = state.groupName,
                onValueChange = { viewModel.updateGroupName(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 16.dp),
                placeholder = { Text("그룹 명을 입력해주세요.", color = Color.Gray, fontSize = 16.sp) },
                singleLine = true
            )

            // 그룹 설명
            Text("그룹 설명", fontWeight = FontWeight.Bold, fontSize = 19.sp)
            OutlinedTextField(
                value = state.description,
                onValueChange = { viewModel.updateDescription(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 16.dp),
                placeholder = { Text("그룹을 간단하게 설명해주세요.", color = Color.Gray, fontSize = 16.sp) }
            )

            // 모임 시간
            Text("모임 시간", fontWeight = FontWeight.Bold, fontSize = 19.sp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 16.dp)
                    .height(56.dp)
                    .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                    .clickable { showTimePicker = true },
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = state.meetingTime.ifEmpty { "만날 시간을 입력해주세요." },
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = if (state.meetingTime.isEmpty()) Color.Gray else Color.Black
                )
            }

            // 모임 장소
            Text("모임 장소", fontWeight = FontWeight.Bold, fontSize = 19.sp)
            OutlinedTextField(
                value = state.meetingLocation,
                onValueChange = { viewModel.updateMeetingLocation(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 16.dp),
                placeholder = { Text("만날 장소를 입력해주세요.", color = Color.Gray, fontSize = 16.sp) },
                singleLine = true
            )

            // 에러 메시지 표시
            state.errorMessage?.let {
                Text(it, color = Color.Red, fontSize = 14.sp)
            }
        }

        // 완료 버튼
        Button(
            onClick = { viewModel.createGroup(context) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = state.isButtonEnabled && !state.isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (state.isButtonEnabled) Color(0xFF2D92FF) else Color.LightGray
            )
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
            } else {
                Text("완료", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    // 성공 시 콜백
    LaunchedEffect(state.successGroupId) {
        state.successGroupId?.let { onCreateComplete(it) }
    }

    // 시간 선택 다이얼로그
    if (showTimePicker) {
        TimePickerDialog(
            onConfirm = { time ->
                viewModel.updateMeetingTime(time)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }
}


/*
@Preview(showBackground = true)
@Composable
fun PreviewGroupCreateScreen() {
    val fakeViewModel = GroupCreateViewModel()
    GroupCreateScreen(viewModel = fakeViewModel)
}
*/