package com.ssafy.a705.mypage

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.ssafy.a705.R
import com.ssafy.a705.controller.viewmodel.MyPageViewModel
import com.ssafy.a705.components.HeaderRow

@Composable
fun MyPageScreen(
    onEditProfileClick: () -> Unit = {},
    onNavigateToMessages: () -> Unit = {},
    onNavigateToMyPostings: () -> Unit = {},
    onNavigateToMyComments: () -> Unit = {},
    viewModel: MyPageViewModel = hiltViewModel()
) {
    val profile by viewModel.profile.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadMyProfile() }

    val profileUrl = profile?.profileUrl
    val nickname = profile?.nickname ?: "닉네임 없음"
    val email = profile?.email ?: "이메일 없음"

    Scaffold(
        topBar = {
            // ✅ 제목만 중앙 정렬
            HeaderRow(
                text = "마이페이지",
                showText = true,
                showLeftButton = false,
                menuActions = emptyList()
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            // 이하 내용은 기존 그대로
            if (!profileUrl.isNullOrBlank()) {
                AsyncImage(
                    model = profileUrl,
                    contentDescription = "프로필 이미지",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(R.drawable.profile),
                    error = painterResource(R.drawable.profile)
                )
            } else {
                Image(
                    painter = painterResource(R.drawable.profile),
                    contentDescription = "기본 프로필 이미지",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(text = nickname, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(text = email, fontSize = 14.sp, color = Color.Black)

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { onEditProfileClick() },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(0.85f),
                border = ButtonDefaults.outlinedButtonBorder
            ) {
                Text(text = "프로필 수정", fontSize = 14.sp, color = Color.Black)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToMessages() }
                        .padding(horizontal = 40.dp, vertical = 15.dp)
                ) {
                    Spacer(modifier = Modifier.width(2.dp))
                    Image(painterResource(R.drawable.my_send), contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("메시지")
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToMyPostings() }
                        .padding(horizontal = 40.dp, vertical = 15.dp)
                ) {
                    Image(painterResource(R.drawable.my_postings), contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("내 포스팅")
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToMyComments() }
                        .padding(horizontal = 40.dp, vertical = 15.dp)
                ) {
                    Spacer(modifier = Modifier.width(1.5.dp))
                    Image(painterResource(R.drawable.my_comments), contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("내 댓글")
                }
            }
        }
    }
}
