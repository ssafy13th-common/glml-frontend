package com.ssafy.a705.global.components.NavBar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CustomBottomNavigationBar(
    selectedTab: BottomTab,
    onTabSelected: (BottomTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .height(65.dp)
            .background(Color.White),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        BottomTab.values().forEach { tab ->
            val isSelected = tab == selectedTab

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    //.background(if (isSelected) Color(0xFF2D92FF) else Color.Transparent)
                    .clickable { onTabSelected(tab) },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {

                    Icon(
                        painter = painterResource(id = tab.iconRes), // PNG나 Vector 리소스
                        contentDescription = tab.label,
                        modifier = Modifier.size(25.dp),
                        tint = if (isSelected) Color(0xFF2D92FF) else Color.Gray // 선택 여부에 따라 색상 변경
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = tab.label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color(0xFF2D92FF) else Color.Gray
                    )
                }
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun CustomBottomNavigationBarPreview() {
    var selectedTab by remember { mutableStateOf(BottomTab.HOME) }

    Column {
        Spacer(modifier = Modifier.weight(1f)) // 상단 빈 영역
        CustomBottomNavigationBar(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it }
        )
    }
}