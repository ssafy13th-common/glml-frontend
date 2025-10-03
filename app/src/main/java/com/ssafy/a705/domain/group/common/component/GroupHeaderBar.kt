package com.ssafy.a705.domain.group.common.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ssafy.a705.group.common.component.GroupStatusChip
import com.ssafy.a705.group.common.component.GroupStatusChipSize
import com.ssafy.a705.group.common.component.GroupSettingsMenu

@Composable
fun GroupHeaderBar(
    groupName: String,
    status: String,
    groupId: Long,
    navController: NavController,
    onInviteClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = groupName,
                fontWeight = FontWeight.Bold,
                fontSize = 40.sp,
                color = Color(0xFF2D92FF)
            )
            Spacer(Modifier.width(10.dp))
            GroupStatusChip(status = status, size = GroupStatusChipSize.SMALL)
        }

        GroupSettingsMenu(
            navController = navController,
            groupId = groupId,
        )
    }
}
