package com.ssafy.a705.common.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.a705.R

@Composable
fun InputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    placeholderText: String = "댓글을 입력해주세요",
    modifier: Modifier = Modifier
) {
    val isSendEnabled = value.isNotBlank()
    val sendButtonColor = if (isSendEnabled) Color(0xFF3366FF) else Color(0xFFB3C7FF)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.profile),
            contentDescription = "내 프로필 이미지",
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.width(15.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .border(1.dp, Color.LightGray, shape = RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = false,
                maxLines = 7,
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(placeholderText, color = Color.Gray, fontSize = 14.sp)
                    }
                    innerTextField()
                }
            )
        }
        Spacer(modifier = Modifier.width(18.dp))
        IconButton(
            onClick = { if (isSendEnabled) onSend() },
            modifier = Modifier
                .size(36.dp)
                .background(sendButtonColor, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = "전송",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
