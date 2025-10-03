package com.ssafy.a705.common.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ssafy.a705.common.components.navBar.BottomTab
import com.ssafy.a705.common.components.navBar.CustomBottomNavigationBar
import com.ssafy.a705.common.components.navBar.GroupBottomNavigationBar
import com.ssafy.a705.common.components.navBar.GroupBottomTab
import com.ssafy.a705.feature.record.RecordNavRoutes
import com.ssafy.a705.feature.tracking.TrackingNavRoutes


@Composable
fun AppNavigator(
    tokenProvider: () -> String? = { null }
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    AuthEventHandler(
        navController = navController,
        guestAllowedRoutes = setOf(
            "onboarding",
            "intro",
            RecordNavRoutes.Map,
            "PhoneVerify",
            "with",
            "signup"


        ), // 🔧 프로젝트에 맞게
        guestFallbackRoute = "onboarding",
        navigateToOnboarding = {
            if (navController.currentDestination?.route != "onboarding") {
                navController.navigate("onboarding") { launchSingleTop = true }
            }
        },
        navigateToPhoneVerify = { next ->
            val route = Screen.PhoneVerify.route(next)
            if (navController.currentDestination?.route != route) {
                navController.navigate(route) { launchSingleTop = true }
            }
        }
    )
    GuestRouteGuard(
        navController = navController,
        protectedRoutes = setOf(
            Screen.MyPage.route,
            "withDetail/",
            "with/write",
//            RecordNavRoutes.Map,
//            "record/map",     // NavGraph에 선언된 라우트 문자열 그대로
//            "record/map/",    // 파라미터/서브경로가 있으면 접두사로
            // RecordNavRoutes.Map.route  // 라우트 상수가 있다면 이걸 쓰는 게 제일 안전
        ),
        tokenProvider = tokenProvider
    )


    // 바텀 내브바가 표시될 루트들
    val bottomBarRoutes = listOf(
        RecordNavRoutes.Map,
        RecordNavRoutes.List,
        GroupNavRoutes.List,
        TrackingNavRoutes.Tracking,
        TrackingNavRoutes.History,
        "companion",
        "with",
        "mypage"
    )

    val groupRoutePrefixes = listOf(
        "group_memo/",
        "group_photo/",
        "group_receipt/",
        "group_latecheck/",
        "group_chat/"
    )
    val showMainBottomBar = currentRoute in bottomBarRoutes
    val showGroupBottomBar = groupRoutePrefixes.any { prefix ->
        currentRoute?.startsWith(prefix) == true
    }


    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            when {
                showMainBottomBar -> {
                    val currentTab = BottomTab.values().find { currentRoute in it.route } ?: BottomTab.HOME
                    CustomBottomNavigationBar(
                        selectedTab = currentTab,
                        onTabSelected = { tab ->
                            if (tab.route.toList().get(0) !in currentTab.route.toList()) {
                                navController.navigate(tab.route.get(0)) {
                                    popUpTo(Screen.Intro.route) { inclusive = false }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }

                showGroupBottomBar -> {
                    // currentGroupTab 계산 (enum 기반)
                    val currentGroupTab = GroupBottomTab.values().firstOrNull { tab ->
                        currentRoute?.startsWith(tab.matchPrefix) == true
                    } ?: GroupBottomTab.MEMO

                    GroupBottomNavigationBar(
                        selectedTab = currentGroupTab,
                        onTabSelected = { tab ->
                            // 현재 백스택 엔트리의 arguments에서 바로 꺼내기
                            val groupId = navBackStackEntry?.arguments?.getLong("groupId")
                                ?: navController.currentBackStackEntry?.arguments?.getLong("groupId")
                                ?: return@GroupBottomNavigationBar

                            // (선택) 바로 navigate 하려면:
                            val target = tab.routeBuilder(groupId)
                            if (currentRoute != target) {
                                navController.navigate(target) {
                                    launchSingleTop = true
                                    restoreState = true
                                    popUpTo("group_memo/$groupId") { saveState = true }
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { _ ->
        Column(
            modifier = Modifier
                .padding(0.dp, 30.dp, 0.dp, 65.dp)
                .fillMaxSize()
        ) {
            NavGraph(navController = navController)
        }
    }
}
