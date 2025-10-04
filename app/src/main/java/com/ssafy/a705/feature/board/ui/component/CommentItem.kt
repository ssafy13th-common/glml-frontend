package com.ssafy.a705.feature.board.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.a705.feature.board.data.model.response.CommentResponse

@Composable
fun CommentItem(
    comment: CommentResponse,
    onReplyClick: (CommentResponse) -> Unit,
    onProfileClick: (author: String, email: String) -> Unit,
    currentUserEmail: String,
    onMenuClick: (CommentResponse) -> Unit
) {
    Column(modifier = Modifier.padding(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onProfileClick(comment.author, comment.authorEmail) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(
                imageUrl = comment.authorProfileUrl,  // 또는 comment.profileImageUrl 등 실제 필드명
                nameFallback = comment.author,
                size = 36.dp
            )
            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(comment.author, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(comment.timestamp, fontSize = 12.sp, color = Color.Gray)
            }

            if (comment.authorEmail == currentUserEmail) {
                IconButton(onClick = { onMenuClick(comment) }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "댓글 메뉴")
                }
            }
        }

        Text(
            comment.content,
            modifier = Modifier.padding(start = 44.dp, top = 4.dp),
            fontSize = 13.sp
        )

        // 답글 버튼 개선
        TextButton(
            onClick = { onReplyClick(comment) },
            modifier = Modifier
                .padding(start = 44.dp)
        ) {
            Icon(
                Icons.Default.Reply,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("답글 달기", fontSize = 12.sp, color = Color.Gray)
        }

        // 대댓글 표시 로직 그대로...
        comment.replies.forEach { reply ->
            Column(
                modifier = Modifier
                    .padding(start = 64.dp, top = 4.dp)
                    .clickable { onProfileClick(reply.author, reply.authorEmail) }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(
                        imageUrl = reply.authorProfileUrl,    // 실제 필드명으로
                        nameFallback = reply.author,
                        size = 30.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(reply.author, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text(reply.timestamp, fontSize = 11.sp, color = Color.Gray)
                    }
                    if (reply.authorEmail == currentUserEmail) {
                        IconButton(onClick = { onMenuClick(reply) }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "대댓글 메뉴")
                        }
                    }
                }
                Text(
                    reply.content,
                    modifier = Modifier.padding(start = 38.dp, top = 2.dp),
                    fontSize = 13.sp
                )
            }
        }
    }
}