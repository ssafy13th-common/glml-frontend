package com.ssafy.a705.tracking

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun TrackingTabBar(
    currentTab: String,           // "realtime" 또는 "history"
    onRealtimeClick: () -> Unit,
    onHistoryClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .padding(8.dp)
            .height(36.dp)
            .wrapContentWidth()
            .background(color = Color.White, shape = RoundedCornerShape(18.dp))
            .border(BorderStroke(1.dp, Color.LightGray), RoundedCornerShape(18.dp)),
        horizontalArrangement = Arrangement.Start
    ) {
        SegmentedTab(
            text = "실시간",
            selected = currentTab == "realtime",
            onClick = onRealtimeClick
        )
        SegmentedTab(
            text = "기록",
            selected = currentTab == "history",
            onClick = onHistoryClick
        )
    }
}

@Composable
fun SegmentedTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (selected) Color(0xFF2D92FF) else Color.White
    val contentColor = if (selected) Color.White else Color(0xFF2D92FF)
    val fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal

    Surface(
        modifier = Modifier
            .width(72.dp)
            .fillMaxHeight()
            .clickable(enabled = !selected, onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = backgroundColor,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = text,
                color = contentColor,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = fontWeight)
            )
        }
    }
}
