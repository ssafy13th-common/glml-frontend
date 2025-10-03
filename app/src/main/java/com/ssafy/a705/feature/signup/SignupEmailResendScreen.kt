package com.ssafy.a705.feature.signup

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.material3.Button
import androidx.core.net.toUri

@Composable
fun SignupEmailResendScreen(
    navController: NavController,
    viewModel: SignupViewModel = hiltViewModel(),
    emailArgs: String?
) {
    val context = LocalContext.current
    val error by viewModel.error.collectAsState()
    val signupState by viewModel.signupState.collectAsState()
    val targetEmail = emailArgs ?: viewModel.getSignupEmail()
    val resendOk by viewModel.resendOk.collectAsState()
    val resendMsg by viewModel.resendMsg.collectAsState()

    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clear()
        }
    }

    LaunchedEffect(resendOk) {
        when (resendOk) {
            true  -> Toast.makeText(context, "인증 메일을 재전송했어요.", Toast.LENGTH_SHORT).show()
            false -> Toast.makeText(context, resendMsg ?: "재전송 실패", Toast.LENGTH_SHORT).show()
            else  -> Unit
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
            text = "이메일 인증을 진행해주세요.",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "메일이 오지 않았나요?")

        Spacer(modifier = Modifier.width(8.dp))

        Text(text = "이메일 : $targetEmail")

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "이메일 재전송",
            color = Color(0xFF2D92FF),
            modifier = Modifier.clickable {
                viewModel.resendVerify(targetEmail)
                Log.d("ResendEmail", "email: $targetEmail")
                Toast.makeText(context, "메일을 재전송했어요!", Toast.LENGTH_SHORT).show()
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text("인증을 위해 '지원되는 웹 주소' 권한이 필요해요.\n")

        Button(
            onClick = { openSupportedLinksSettings(context) }
        ) {
            Text("권한 허용")
        }
    }
}

private fun openSupportedLinksSettings(context: Context) {
    val pkg = context.packageName

    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // Android 12+ (API 31 이상)
        Intent(Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS).apply {
            Intent.setData = "package:$pkg".toUri()
        }
    } else {
        // 하위 버전은 앱 정보 화면으로 대체
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            Intent.setData = "package:$pkg".toUri()
        }
    }

    try {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        // 혹시 둘 다 실패하면 그냥 앱 정보 화면
        val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            Intent.setData = "package:$pkg".toUri()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(fallback)
    }
}