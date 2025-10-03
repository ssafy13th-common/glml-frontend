package com.ssafy.a705.feature.group.common.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.a705.feature.group.common.util.GroupStatusUtil

@Composable
fun GroupStatusChip(
    status: String,
    size: GroupStatusChipSize = GroupStatusChipSize.MEDIUM
) {
    val displayStatus = GroupStatusUtil.getDisplayStatus(status)
    val (label, color) = when (displayStatus) {
        GroupStatusUtil.DISPLAY_TRAVEL_BEFORE -> "여행 전" to Color(0xFF8bd666)
        GroupStatusUtil.DISPLAY_TRAVEL_IN_PROGRESS -> "여행 중" to Color(0xFFffc7ce)
        GroupStatusUtil.DISPLAY_TRAVEL_COMPLETED -> "여행 완료" to Color(0xFF81b5f8)
        else -> "미정" to Color.Gray
    }

    val (padding, fontSize, fontWeight) = when (size) {
        GroupStatusChipSize.SMALL -> Triple(8.dp, 10.sp, androidx.compose.ui.text.font.FontWeight.Normal)
        GroupStatusChipSize.MEDIUM -> Triple(12.dp, 12.sp, androidx.compose.ui.text.font.FontWeight.Medium)
        GroupStatusChipSize.LARGE -> Triple(16.dp, 14.sp, androidx.compose.ui.text.font.FontWeight.Bold)
    }

    Box(
        modifier = Modifier
            .background(color, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = padding / 2)
    ) {
        Text(
            text = label,
            color = Color.Black,
            fontSize = fontSize,
            fontWeight = fontWeight
        )
    }
}

enum class GroupStatusChipSize {
    SMALL, MEDIUM, LARGE
}

