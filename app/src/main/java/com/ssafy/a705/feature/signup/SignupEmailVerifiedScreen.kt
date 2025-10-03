package com.ssafy.a705.feature.signup

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ssafy.a705.common.navigation.Screen

@Composable
fun SignupEmailVerifiedScreen(
    navController: NavController,
    viewModel: SignupEmailVerifyViewModel
) {
    // 시스템 뒤로가기 막기
    BackHandler(enabled = true) { /* no-op */ }

    val ctx = LocalContext.current
    val ok   by viewModel.verifyOk.collectAsState()
    val msg  by viewModel.verifyMsg.collectAsState()

    LaunchedEffect(ok) {
        if (ok == false) {
            Toast.makeText(ctx, msg ?: "인증 실패", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (ok == true) "이메일 인증이 완료되었습니다." else "인증 확인 중…",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "로그인하러 가기",
            color = Color(0xFF2D92FF),
            modifier = Modifier.clickable {
                // 로그인(온보딩)으로 이동하면서 스택 싹 정리
                navController.navigate(Screen.Onboarding.route) {
                    // 앱의 첫 화면(인트로)까지 포함해서 모두 제거
                    popUpTo(Screen.Intro.route) { inclusive = true }
                    launchSingleTop = true
                }
            }
        )
    }
}