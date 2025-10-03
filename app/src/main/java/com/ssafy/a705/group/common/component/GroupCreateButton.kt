package com.ssafy.a705.group.common.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GroupCreateButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(65.41431.dp)
            .clip(CircleShape)
            .background(Color(0xFF2D92FF))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text("+", fontSize = 32.sp, color = Color.White)
    }
}
