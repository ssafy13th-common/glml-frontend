package com.ssafy.a705.feature.group.edit

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssafy.a705.group.common.component.DatePickerDialogComponent
import com.ssafy.a705.group.common.component.GroupTopBar
import com.ssafy.a705.group.common.component.TimePickerDialog

@Composable
fun GroupEditScreen(
    groupId: Long,
    viewModel: GroupEditViewModel,
    onBackClick: () -> Unit = {},
    onEditComplete: (Long) -> Unit = {}
) {
    // 초기 데이터 로드
    LaunchedEffect(groupId) {
        viewModel.initGroupInfo(groupId)
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val scrollState = rememberScrollState()
    // 다이얼로그 표시 상태
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val borderColor = Color(0xFFBDBDBD)
    val placeholderColor = Color.Gray
    val placeholderFontSize = 16.sp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            GroupTopBar(title = "그룹 수정하기", onBackClick = onBackClick)
            Spacer(modifier = Modifier.height(24.dp))

            // 그룹명
            Text("그룹명 *", fontWeight = FontWeight.Bold, fontSize = 19.sp)
            OutlinedTextField(
                value = state.groupName,
                onValueChange = { viewModel.updateGroupName(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 16.dp),
                placeholder = {
                    Text("그룹 명을 입력해주세요.", color = placeholderColor, fontSize = placeholderFontSize)
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = borderColor,
                    unfocusedIndicatorColor = borderColor,
                    cursorColor = Color.Black
                )
            )

            // 그룹 설명
            Text("그룹 설명", fontWeight = FontWeight.Bold, fontSize = 19.sp)
            OutlinedTextField(
                value = state.description,
                onValueChange = { viewModel.updateDescription(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 16.dp),
                placeholder = {
                    Text("그룹을 간단하게 설명해주세요.", color = placeholderColor, fontSize = placeholderFontSize)
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = borderColor,
                    unfocusedIndicatorColor = borderColor,
                    cursorColor = Color.Black
                )
            )


            // 여행 시작일
            Text("여행 시작일", fontWeight = FontWeight.Bold, fontSize = 19.sp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 16.dp)
                    .height(56.dp)
                    .border(1.dp, borderColor, RoundedCornerShape(4.dp))
                    .clickable { showStartDatePicker = true },
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = state.startAt.ifEmpty { "여행 시작일을 선택해주세요." },
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = if (state.startAt.isEmpty()) placeholderColor else Color.Black,
                    fontSize = placeholderFontSize
                )
            }

            // 여행 종료일
            Text("여행 종료일", fontWeight = FontWeight.Bold, fontSize = 19.sp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 16.dp)
                    .height(56.dp)
                    .border(1.dp, borderColor, RoundedCornerShape(4.dp))
                    .clickable { showEndDatePicker = true },
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = state.endAt.ifEmpty { "여행 종료일을 선택해주세요." },
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = if (state.endAt.isEmpty()) placeholderColor else Color.Black,
                    fontSize = placeholderFontSize
                )
            }

            // 모임 시간
            Text("모임 시간", fontWeight = FontWeight.Bold, fontSize = 19.sp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 16.dp)
                    .height(56.dp)
                    .border(1.dp, borderColor, RoundedCornerShape(4.dp))
                    .clickable { showTimePicker = true },
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = state.gatheringTime.ifEmpty { "모임 시간을 선택해주세요." },
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = if (state.gatheringTime.isEmpty()) placeholderColor else Color.Black,
                    fontSize = placeholderFontSize
                )
            }

            // 모임 장소
            Text("모임 장소", fontWeight = FontWeight.Bold, fontSize = 19.sp)
            OutlinedTextField(
                value = state.gatheringLocation,
                onValueChange = { viewModel.updateGatheringLocation(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 16.dp),
                placeholder = {
                    Text("모임 장소를 입력해주세요.", color = placeholderColor, fontSize = placeholderFontSize)
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = borderColor,
                    unfocusedIndicatorColor = borderColor,
                    cursorColor = Color.Black
                )
            )

            // 지각비 입력
            Text("분당 지각비(원)", fontWeight = FontWeight.Bold, fontSize = 19.sp)
            OutlinedTextField(
                value = state.feePerMinute,
                onValueChange = { viewModel.updateFeePerMinute(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 16.dp),
                placeholder = {
                    Text("지각 시 분당 벌금을 입력해주세요.", color = placeholderColor, fontSize = placeholderFontSize)
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = borderColor,
                    unfocusedIndicatorColor = borderColor,
                    cursorColor = Color.Black
                )
            )

            // 에러 메시지
            state.errorMessage?.let {
                Text(it, color = Color.Red, fontSize = 14.sp)
            }
        }

        // 완료 버튼
        Button(
            onClick = { viewModel.updateGroup(context) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = state.isButtonEnabled && !state.isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (state.isButtonEnabled) Color(0xFF2D92FF) else Color.LightGray
            )
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
            } else {
                Text("수정 완료", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    // 성공 시 콜백
    LaunchedEffect(state.success) {
        if (state.success) onEditComplete(groupId)
    }

    // 다이얼로그
    if (showStartDatePicker) {
        DatePickerDialogComponent(
            onDateSelected = { viewModel.updateStartDate(it) },
            onDismiss = { showStartDatePicker = false }
        )
    }
    if (showEndDatePicker) {
        DatePickerDialogComponent(
            onDateSelected = { viewModel.updateEndDate(it) },
            onDismiss = { showEndDatePicker = false }
        )
    }
    if (showTimePicker) {
        TimePickerDialog(
            onConfirm = { time ->
                viewModel.updateGatheringTime(time)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }
}
