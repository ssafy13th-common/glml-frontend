package com.ssafy.a705.feature.board.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.a705.feature.board.data.model.WithPost

@Composable
fun PostHeader(
    post: WithPost,
    onProfileClick: (author: String, email: String) -> Unit,
) {
    var showFullTitle by remember { mutableStateOf(false) }

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable {
                onProfileClick(post.author, post.authorEmail)
            }
        ) {
            Avatar(
                imageUrl = post.authorProfileUrl,   // ← 모델에 있는 필드명 사용
                nameFallback = post.author,
                size = 40.dp
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = post.author, fontWeight = FontWeight.Bold)
                Text(text = post.date, fontSize = 12.sp, color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 제목 2줄 제한 + 더보기
        Text(
            text = post.title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            maxLines = if (showFullTitle) Int.MAX_VALUE else 2,
            overflow = TextOverflow.Ellipsis
        )
        if (!showFullTitle && post.title.length > 20) { // 길이 임계값은 적절히 조정
            Text(
                text = "더보기",
                color = Color.Blue,
                fontSize = 14.sp,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clickable { showFullTitle = true }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(text = post.content)
        Spacer(modifier = Modifier.height(20.dp))
    }
}