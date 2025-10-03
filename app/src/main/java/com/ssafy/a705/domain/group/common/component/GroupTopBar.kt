package com.ssafy.a705.domain.group.common.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavController

@Composable
fun GroupTopBar(
    title: String = "그룹",
    onBackClick: () -> Unit = {},
    groupId: Long? = null,
    navController: NavController? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        contentAlignment = Alignment.Center
    ) {
        // 뒤로가기 버튼 (좌측)
        Icon(
            imageVector = Icons.Default.ArrowBack,
            contentDescription = "뒤로가기",
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 8.dp)
                .clickable { onBackClick() }
        )
        
        // 제목 (중앙)
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        
        // 설정 메뉴 (우측) - groupId와 navController가 있을 때만 표시
        if (groupId != null && navController != null) {
            Box(
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                GroupSettingsMenu(navController = navController, groupId = groupId)
            }
        }
    }
}
