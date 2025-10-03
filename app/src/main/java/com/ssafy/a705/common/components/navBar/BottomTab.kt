package com.ssafy.a705.common.components.navBar

import com.ssafy.a705.R
import com.ssafy.a705.common.navigation.GroupNavRoutes
import com.ssafy.a705.common.navigation.Screen
import com.ssafy.a705.feature.record.RecordNavRoutes
import com.ssafy.a705.feature.tracking.TrackingNavRoutes

enum class BottomTab(
    val label: String,
    val iconRes: Int,
    val route: List<String>
) {
    HOME("홈", R.drawable.nav_home,  mutableListOf(RecordNavRoutes.Map)),
    GROUP("그룹", R.drawable.nav_group,  mutableListOf(GroupNavRoutes.List)),
    TRACKING("트래킹", R.drawable.nav_tracking,  mutableListOf(TrackingNavRoutes.Tracking, TrackingNavRoutes.History)),
    COMPANION("동행", R.drawable.nav_companions,  mutableListOf(Screen.With.route)),
    MYPAGE("마이페이지", R.drawable.nav_profile,  mutableListOf("mypage"))

}