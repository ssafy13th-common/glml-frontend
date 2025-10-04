package com.ssafy.a705.feature.board.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.a705.R
import com.ssafy.a705.feature.board.data.model.response.PostData

@Composable
fun PostCard(post: PostData, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(0.5.dp, Color.LightGray)
            .padding(horizontal = 30.dp)
            .padding(vertical = 10.dp)
    ) {
        Text(post.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(8.dp))
        Text(post.summary, fontSize = 14.sp, color = Color.Gray)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.drawable.comments),
                contentDescription = "댓글 말풍선",
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text("${post.comments}", fontSize = 12.sp, color = Color.Black)
            Spacer(Modifier.weight(1f))
            Spacer(modifier = Modifier.width(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(post.author, modifier = Modifier.padding(start = 4.dp), fontSize = 12.sp)
                Spacer(modifier = Modifier.width(20.dp))
                Text(post.updatedAt, fontSize = 11.sp)
            }
        }
    }
}
