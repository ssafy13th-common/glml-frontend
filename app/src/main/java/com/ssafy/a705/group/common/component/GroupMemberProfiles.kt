package com.ssafy.a705.group.common.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GroupMemberProfiles(members: List<String>?) {
    val safeMembers = members ?: emptyList()
    val displayMembers = safeMembers.take(4)
    val remainingCount = safeMembers.size - 4

    Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
        when (displayMembers.size) {
            1 -> ProfileImage(displayMembers[0])
            2 -> {
                ProfileImage(displayMembers[0], modifier = Modifier.offset(x = (-12).dp, y = (-12).dp))
                ProfileImage(displayMembers[1], modifier = Modifier.offset(x = 12.dp, y = 12.dp))
            }
            3 -> {
                ProfileImage(displayMembers[0], modifier = Modifier.offset(y = (-16).dp))
                ProfileImage(displayMembers[1], modifier = Modifier.offset(x = (-16).dp, y = 16.dp))
                ProfileImage(displayMembers[2], modifier = Modifier.offset(x = 16.dp, y = 16.dp))
            }
            4 -> {
                ProfileImage(displayMembers[0], modifier = Modifier.offset(x = (-12).dp, y = (-12).dp))
                ProfileImage(displayMembers[1], modifier = Modifier.offset(x = 12.dp, y = (-12).dp))
                ProfileImage(displayMembers[2], modifier = Modifier.offset(x = (-12).dp, y = 12.dp))
                if (remainingCount > 0) {
                    Box(
                        modifier = Modifier
                            .offset(x = 12.dp, y = 12.dp)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.LightGray),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.Text(
                            text = "+$remainingCount",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    }
                } else {
                    ProfileImage(displayMembers[3], modifier = Modifier.offset(x = 12.dp, y = 12.dp))
                }
            }
        }
    }
}
