package com.ssafy.a705.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavHostController
import com.ssafy.a705.event.AuthEvent
import com.ssafy.a705.event.AuthEventBus

@Composable
fun AuthEventHandler(
    navController: NavHostController,
    // 👇 게스트가 머물러도 되는 라우트 목록(필요에 맞게 채우세요)
    guestAllowedRoutes: Set<String> = setOf("onboarding", "intro", "home_public"),
    // 👇 게스트로 떨어뜨릴 기본 라우트(없으면 onboarding 권장)
    guestFallbackRoute: String = "onboarding",
    navigateToOnboarding: () -> Unit,
    navigateToPhoneVerify: (nextRoute: String?) -> Unit
) {
    var blockUi by remember { mutableStateOf(false) }
    var showLogin by remember { mutableStateOf(false) }
    var pendingNext by remember { mutableStateOf<String?>(null) }
    var handling by remember { mutableStateOf(false) }

    fun currentRoute(): String? = navController.currentBackStackEntry?.destination?.route

    // 게스트 안전 경로로 빠져나가기
    fun exitToGuestSafeRoute() {
        // 1) 현재 라우트가 허용이면 그대로 머문다
        val cur = currentRoute()
        if (cur != null && cur in guestAllowedRoutes) return

        // 2) 허용 라우트가 나올 때까지 pop
        var found = false
        while (true) {
            val now = currentRoute() ?: break
            if (now in guestAllowedRoutes) { found = true; break }
            val popped = navController.popBackStack()
            if (!popped) break
        }
        if (found) return

        // 3) 그래도 없으면 안전 라우트로 이동(중복 이동 가드)
        if (currentRoute() != guestFallbackRoute) {
            navController.navigate(guestFallbackRoute) { launchSingleTop = true }
        }
    }

    // 이벤트 수신 (401/403)
    LaunchedEffect(navController) {
        AuthEventBus.events.collect { ev ->
            if (handling) return@collect
            handling = true
            when (ev) {
                is AuthEvent.RequireLogin -> {
                    pendingNext = ev.nextRoute
                    blockUi = true
                    showLogin = true
                }
                is AuthEvent.RequirePhoneVerification -> {
                    blockUi = true
                    // 전화인증 화면으로 이동 (중복 이동 가드)
                    navigateToPhoneVerify(ev.nextRoute)
                }
            }
            handling = false
        }
    }

    // 특정 화면에 도착하면 오버레이 해제
    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect { entry ->
            val route = entry.destination.route ?: return@collect
            if (
                route.startsWith("PhoneVerify") ||
                route == "onboarding" ||
                route == "intro"                // ← 게스트 허용 라우트들 포함
            ) {
                blockUi = false
            }
        }
    }

    // 전역 오버레이(401/403 순간에만)
    if (blockUi) {
        Dialog(
            onDismissRequest = {}, // 닫기 금지
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false   // 풀스크린
            )
        ) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                // 원하면 중앙에 Progress 넣기
            }
        }
    }

    // 401: 로그인 다이얼로그
    if (showLogin) {
        AlertDialog(
            onDismissRequest = {
                // 닫기 = 계속 둘러보기
                showLogin = false
                blockUi = false
                pendingNext = null

                // 2) 게스트 허용 라우트로 정리 (이미 허용 라우트면 아무것도 안 함)
                exitToGuestSafeRoute()
            },
            title = { Text("로그인이 필요합니다") },
            text  = { Text("해당 기능을 이용하려면 로그인하세요.") },
            confirmButton = {
                TextButton(onClick = {
                    showLogin = false
//                    blockUi = false
                    // 이미 온보딩이면 이동 안 함
                    if (currentRoute() != "onboarding") navigateToOnboarding()
                }) { Text("로그인하기") }
            },
            dismissButton = {
                TextButton(onClick = {
                    // 1) 오버레이/다이얼로그 즉시 해제
                    showLogin = false
                    blockUi = false
                    pendingNext = null

                    // 2) 게스트 허용 라우트로 정리 (이미 허용 라우트면 아무것도 안 함)
                    exitToGuestSafeRoute()
                }) {
                    Text("계속 둘러보기")
                }
            }
        )
    }
}
