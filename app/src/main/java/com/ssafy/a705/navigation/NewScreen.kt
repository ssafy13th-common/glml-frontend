package com.ssafy.a705.navigation

sealed class Screen(val route: String) {
    object Intro : Screen("intro")
    object Onboarding : Screen("onboarding")
    object Signup : Screen("signup")
    object Main : Screen("main")       // 홈 탭
    object MyPage : Screen("mypage")
    object Messages : Screen("messages")//마이페이지 메시지
    object MyPosts  : Screen("myposts")//마이페이지 포스팅
    object MyComments : Screen("mycomments")// 마이페이지 댓글
    object EditProfile : Screen("edit_profile")
    object ChangePassword : Screen("change_password")
    // 동행
    object With : Screen("with")
    data class WithDetail(val postId: Long) : Screen("withDetail/$postId") {
        companion object {
            const val routeWithArg = "withDetail/{postId}"
        }
    }
    data class WithPostWrite(val postId: Long? = null) : Screen(
        route = if (postId != null) "with/write/$postId" else "with/write"
    ) {
        companion object {
            const val route = "with/write"
            const val routeWithArg = "with/write/{postId}"
        }
    }
    data class WithPostEdit(val postId: Long) {
        val route = "with/edit/$postId"
    }
    data class WithChat(val roomId: String, val title: String? = null): Screen("chat/$roomId?title=${title ?: ""}") {
        companion object {
            const val routeWithArg = "chat/{roomId}"
        }
    }

    object PhoneVerify : Screen("PhoneVerify?next={next}") {
        fun route(next: String?) = "PhoneVerify?next=${java.net.URLEncoder.encode(next ?: "", "UTF-8")}"
    }
}

// 그룹 내부 화면
object GroupNavRoutes {
    const val List = "group_list"
    const val Create = "group_create"
    const val Edit = "group_edit/{groupId}"
    const val Chat = "group_chat/{groupId}"
    const val Members = "group_members/{groupId}"
    const val Memo = "group_memo/{groupId}"
    const val Photo = "group_photo/{groupId}"
    const val Receipt = "group_receipt/{groupId}"
    const val LateCheck = "group_latecheck/{groupId}"
    
    fun MemoWithId(groupId: Long) = "group_memo/$groupId"
    fun PhotoWithId(groupId: Long) = "group_photo/$groupId"
    fun ReceiptWithId(groupId: Long) = "group_receipt/$groupId"
    fun LateCheckWithId(groupId: Long) = "group_latecheck/$groupId"
    fun ChatWithId(groupId: Long) = "group_chat/$groupId"
    fun EditWithId(groupId: Long) = "group_edit/$groupId"
    fun MembersWithId(groupId: Long) = "group_members/$groupId"
}