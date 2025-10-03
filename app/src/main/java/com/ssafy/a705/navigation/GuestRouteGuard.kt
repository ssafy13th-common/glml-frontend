package com.ssafy.a705.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import com.ssafy.a705.event.AuthEvent
import com.ssafy.a705.event.AuthEventBus

@Composable
fun GuestRouteGuard(
    navController: NavHostController,
    protectedRoutes: Set<String>,          // "record/map", "mypage", "group_memo/" 등
    tokenProvider: () -> String?           // { tokenManager.getServerAccessToken() }
) {
    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect { entry ->
            val route = entry.destination.route ?: return@collect
            val needsAuth = protectedRoutes.any { route.startsWith(it) }
            val hasJwt = !tokenProvider().isNullOrBlank()
            if (needsAuth && !hasJwt) {
                // 화면 그려지기 전에 즉시 취소하고 로그인 유도
                navController.popBackStack()
                AuthEventBus.emit(AuthEvent.RequireLogin(nextRoute = route))
            }
        }
    }
}
