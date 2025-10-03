package com.ssafy.a705.global.components.NavBar

import com.ssafy.a705.R
import com.ssafy.a705.global.navigation.GroupNavRoutes

enum class GroupBottomTab(
    val label: String,
    val iconRes: Int,
    val routeBuilder: (Long) -> String,
    val matchPrefix: String
) {
    MEMO("메모", R.drawable.nav_memo,{ id -> GroupNavRoutes.MemoWithId(id) }, "group_memo/"),
    PHOTO("사진", R.drawable.nav_picture, { id -> GroupNavRoutes.PhotoWithId(id) }, "group_photo/"),
    RECEIPT("정산", R.drawable.nav_won,  { id -> GroupNavRoutes.ReceiptWithId(id) }, "group_receipt/"),
    LATE_CHECK("위치", R.drawable.nav_location,  { id -> GroupNavRoutes.LateCheckWithId(id) }, "group_latecheck/"),
    CHAT("채팅", R.drawable.nav_chat,  { id -> GroupNavRoutes.ChatWithId(id) }, "group_chat/")
}
