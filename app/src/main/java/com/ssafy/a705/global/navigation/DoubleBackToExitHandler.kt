package com.ssafy.a705.global.navigation

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

@Composable
fun DoubleBackToExitHandler() {
    val context = LocalContext.current
    var backPressedTime by remember { mutableStateOf(0L) }

    BackHandler {
        val currentTime = System.currentTimeMillis()
        if (currentTime - backPressedTime < 2000) {
            // 2초 이내 두 번 클릭 → 종료
            (context as? Activity)?.finish()
        } else {
            Toast.makeText(context, "한 번 더 누르면 종료됩니다", Toast.LENGTH_SHORT).show()
            backPressedTime = currentTime
        }
    }
}
