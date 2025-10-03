package com.ssafy.a705.common.navigation

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.navigation.navArgument
import com.ssafy.a705.feature.group.create.GroupCreateScreen
import com.ssafy.a705.feature.group.edit.GroupEditScreen
import com.ssafy.a705.feature.group.list.GroupListScreen
import com.ssafy.a705.feature.group.memo.GroupMemoScreen
import com.ssafy.a705.feature.group.photo.GroupPhotoScreen
import com.ssafy.a705.feature.group.receipt.ReceiptScreen
import com.ssafy.a705.feature.group.latecheck.LateCheckScreen
import com.ssafy.a705.feature.group.chat.GroupChatScreen
import com.ssafy.a705.feature.group.member.GroupMemberScreen
import com.ssafy.a705.feature.record.MapScreen
import com.ssafy.a705.feature.record.RecordCreateScreen
import com.ssafy.a705.feature.record.RecordViewModel
import com.ssafy.a705.feature.record.RecordDetailScreen
import com.ssafy.a705.feature.record.RecordNavRoutes
import com.ssafy.a705.feature.record.RecordScreen
import com.ssafy.a705.feature.tracking.TrackingListScreen
import com.ssafy.a705.feature.tracking.TrackingNavRoutes
import com.ssafy.a705.feature.tracking.TrackingScreen
import com.ssafy.a705.feature.tracking.TrackingUpdateScreen
import com.ssafy.a705.feature.tracking.TrackingViewModel
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.navDeepLink
import com.ssafy.a705.feature.group.edit.GroupEditViewModel
import com.ssafy.a705.feature.mypage.EditProfileScreen
import com.ssafy.a705.feature.mypage.MessageListEntry
import com.ssafy.a705.feature.mypage.MyPageScreen
import com.ssafy.a705.feature.mypage.MyPostAndCommentScreen
import com.ssafy.a705.feature.record.RecordUpdateScreen
import com.ssafy.a705.feature.sign.OnboardingIntroScreen
import com.ssafy.a705.feature.sign.OnboardingScreen
import com.ssafy.a705.feature.sign.PhoneVerifyScreen
import com.ssafy.a705.feature.signup.SignupEmailResendScreen
import com.ssafy.a705.feature.signup.SignupEmailVerifiedScreen
import com.ssafy.a705.feature.signup.SignupEmailVerifyViewModel
import com.ssafy.a705.feature.signup.SignupNavRoutes
import com.ssafy.a705.feature.signup.SignupScreen
import com.ssafy.a705.feature.signup.SignupViewModel
import com.ssafy.a705.feature.board.ui.view.WithChatScreen
import com.ssafy.a705.feature.board.ui.view.WithMainScreen
import com.ssafy.a705.feature.board.ui.view.WithPostDetailScreen
import com.ssafy.a705.feature.board.ui.view.WithPostWriteScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    recordViewModel: RecordViewModel = viewModel(),
    signupViewModel: SignupViewModel = viewModel()
) {

    NavHost(
        navController = navController,
        startDestination = "intro"
    ) {
        // 인트로
        composable(Screen.Intro.route) {
            OnboardingIntroScreen(
                onIntroFinished = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Intro.route) { inclusive = true }
                    }
                }
            )
        }
        //  온보딩 등록부 수정: fromLogout 인자 받기
        composable(
            route = Screen.Onboarding.route, // "onboarding?fromLogout={fromLogout}"
            arguments = listOf(
                navArgument("fromLogout") {
                type = NavType.BoolType
                defaultValue = false
            }, navArgument("email") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val fromLogout = backStackEntry.arguments?.getBoolean("fromLogout") ?: false
            val email = backStackEntry.arguments?.getString("email")
            OnboardingScreen(
                onNavigateToMain = {
                    navController.navigate(RecordNavRoutes.Map) {
                        popUpTo(Screen.Onboarding.route) {
                            inclusive = true   // 시작 지점도 함께 제거
                        }
                        launchSingleTop = true   // 중복 푸시 방지
                        restoreState = false     // 이전 저장 상태 복원하지 않음
                    }
                },
                onSignUpClick = { navController.navigate(Screen.Signup.route) },
                onNavigateToEmailResend = { typedEmail ->
                    Log.d("NavGraph", "email: $typedEmail")
                    navController.navigate("${SignupNavRoutes.EmailResend}?email=${Uri.encode(typedEmail)}")
                },
                fromLogout = fromLogout
            )
        }
        //프로필 수정
        composable(Screen.EditProfile.route) {
            EditProfileScreen(
                onChangePasswordClick = { navController.navigate(Screen.ChangePassword.route) },
                onBack = { navController.popBackStack() },
                onLogoutSuccess = {
                    // 온보딩으로 이동 + 스택 정리
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Intro.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        //회원가입
//        composable(Screen.Signup.route) {
//            SignupScreen(
//                onNavigateToOnboardingNoAnim = {
//                    navController.navigate(Screen.Onboarding.route) {
//                        popUpTo(Screen.Signup.route) { inclusive = true }
//                    }
//                },
//                onNavigateToMain = { navController.navigate(RecordNavRoutes.Map) },
//                onBack = { navController.popBackStack() }
//            )
//        }
        // 회원가입
        composable(SignupNavRoutes.Signup) {
            SignupScreen(navController,
                onNavigateToMain = {
                    navController.navigate(RecordNavRoutes.Map) {
                        popUpTo(Screen.Onboarding.route) {
                            inclusive = true   // 시작 지점도 함께 제거
                        }
                        launchSingleTop = true   // 중복 푸시 방지
                        restoreState = false     // 이전 저장 상태 복원하지 않음
                    }
                },
                onOnboarding = {
                    navController.navigate(Screen.Onboarding.route) {
                    popUpTo(Screen.Intro.route) { inclusive = true }
                    launchSingleTop = true
                    } },
                signupViewModel)
        }
        // 이메일 재전송
        composable(
            route = SignupNavRoutes.EmailResendWithArgs,
            arguments = listOf(navArgument("email"){ nullable = true; defaultValue = null })
        ) { backStack ->
            val emailArg = backStack.arguments?.getString("email")
            Log.d("NavGraph", "emailArg: $emailArg")
            SignupEmailResendScreen(navController, signupViewModel, emailArg)
        }
        // 가입 완료
        composable(
            route = SignupNavRoutes.EmailVerified,
            arguments = listOf(navArgument("token") { nullable = true; defaultValue = null }),
            deepLinks = listOf(
                navDeepLink { uriPattern = "a705://email_verified?token={token}" },
                navDeepLink { uriPattern = "https://glml.store/auth/verify?token={token}" }
            )
        ) { backStack ->
            val signupEmailVerifyViewModel: SignupEmailVerifyViewModel = hiltViewModel() // init에서 저장 끝
            val token = backStack.arguments?.getString("token")
            Log.d("DeepLink", "Nav to EmailVerified token=$token")
            SignupEmailVerifiedScreen(navController, signupEmailVerifyViewModel)
        }
        // 마이페이지
        composable(Screen.MyPage.route) {
            MyPageScreen(
                onEditProfileClick = { navController.navigate(Screen.EditProfile.route) },
                onNavigateToMessages = { navController.navigate(Screen.Messages.route) },
                onNavigateToMyPostings = { navController.navigate(Screen.MyPosts.route) },
                onNavigateToMyComments = { navController.navigate(Screen.MyComments.route) }
            )
        }
        // 내 포스팅
        composable(route = Screen.MyPosts.route) {
            MyPostAndCommentScreen(
                onBack = { navController.popBackStack() },
                onPostClick = { boardId ->
                    navController.navigate(Screen.WithDetail(boardId).route)
                },
                initialTab = 0 // 포스팅 탭
            )
        }
        // 내 댓글
        composable(route = Screen.MyComments.route) {
            MyPostAndCommentScreen(
                onBack = { navController.popBackStack() },
                onPostClick = { boardId ->
                    navController.navigate(Screen.WithDetail(boardId).route)
                },
                initialTab = 1 // 댓글 탭
            )
        }


        // 동행 메인
        composable(Screen.With.route) {
            WithMainScreen(
                onNavigateToDetail = { postId ->
                    navController.navigate(Screen.WithDetail(postId).route)
                },
                onNavigateToWrite = {
                    navController.navigate(Screen.WithPostWrite().route)
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // 동행 상세
        composable(
            route = Screen.WithDetail.routeWithArg,
            arguments = listOf(navArgument("postId") { type = NavType.LongType })
        ) { backStackEntry ->
            val postId = backStackEntry.arguments?.getLong("postId") ?: return@composable
            WithPostDetailScreen(
                postId = postId,
                navController = navController
            )
        }

        // 동행 작성
        composable(Screen.WithPostWrite.route) {
            WithPostWriteScreen(
                postId = null,
                navController = navController
            )
        }
        //동행 수정
        composable(
            route = Screen.WithPostWrite.routeWithArg, // "with/write/{postId}"
            arguments = listOf(navArgument("postId") { type = NavType.LongType })
        ) { backStackEntry ->
            val postId = backStackEntry.arguments?.getLong("postId")
            WithPostWriteScreen(
                postId = postId,   // Long?
                navController = navController
            )
        }
        //번호 인증
        composable(
            route = Screen.PhoneVerify.route,
            arguments = listOf(navArgument("next") { nullable = true })
        ) { backStackEntry ->
            val next = backStackEntry.arguments?.getString("next")
            PhoneVerifyScreen(
                nextRoute = next,
                onVerified = { route ->
                    // 인증 성공 → 원래 가려던 곳으로 복귀
                    navController.popBackStack()
                    navController.navigate(route ?: Screen.With.route) { launchSingleTop = true }
                }
            )
        }
        //동행 채팅
        composable(
            route = Screen.WithChat.routeWithArg,
            arguments = listOf(navArgument("roomId") { type = NavType.StringType })
        ) { backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId") ?: return@composable
            WithChatScreen(
                roomId = roomId,
                onBack = { navController.popBackStack() }
            )
        }
        composable("messages") {
            MessageListEntry(
                onBack = { navController.popBackStack() }, // ← 뒤로가기 추가
                onClickRoom = { roomId, title ->
                    navController.navigate(Screen.WithChat(roomId, title).route)
                }
            )
        }

        // 기록 지도
        composable(RecordNavRoutes.Map) {
            MapScreen(navController)
        }

        // 기록 작성
        composable(RecordNavRoutes.Create) {
            RecordCreateScreen(recordViewModel, navController)
        }

        // 기록 목록
        composable(RecordNavRoutes.List) {
            LaunchedEffect(Unit) {
                recordViewModel.setLocationFilter(null)
            }

            RecordScreen(recordViewModel, navController)
        }

        // 기록 목록
        composable(
            route = RecordNavRoutes.ListWithArg,
            arguments = listOf(navArgument("locationId") { type = NavType.StringType })
        ) { backStackEntry ->
            val locationId = backStackEntry.arguments?.getString("locationId")

            LaunchedEffect(locationId) {
                recordViewModel.setLocationFilter(locationId?.toIntOrNull())
            }

            RecordScreen(recordViewModel, navController)
        }

        // 기록 상세
        composable(
            route = RecordNavRoutes.DetailWithArg,
            arguments = listOf(navArgument("recordId") { type = NavType.LongType })
        ) { backStackEntry ->
            val recordId = backStackEntry.arguments!!.getLong("recordId")
            RecordDetailScreen(recordViewModel, navController, recordId)
        }

        // 기록 수정
        composable(
            route = RecordNavRoutes.UpdateWithArg,
            arguments = listOf(navArgument("recordId") { type = NavType.LongType })
        ) { backStackEntry ->
            val recordId = backStackEntry.arguments!!.getLong("recordId")
            RecordUpdateScreen(recordViewModel, navController, recordId)
        }

        //아래는 구현 예정
//        composable(Screen.Messages.route) { MessageListScreen() }
//        composable(Screen.MyPostings.route) { MyPostingListScreen() }
//        composable(Screen.MyComments.route) { MyCommentListScreen() }
//
//        composable(Screen.RecordDetail.routeWithArg,
//            arguments = listOf(navArgument("recordId") { type = NavType.StringType })
//        ) { backStackEntry ->
//            val recordId = backStackEntry.arguments?.getString("recordId") ?: return@composable
//            RecordDetailScreen(navController, recordId)
//        }
//
        // ===== 그룹 네비게이션 섹션만 =====

// 그룹 리스트
        composable(GroupNavRoutes.List) {
            GroupListScreen(navController = navController)
        }

        // 그룹 생성
        composable(GroupNavRoutes.Create) {
            GroupCreateScreen(
                onBackClick = {
                    navController.navigate(GroupNavRoutes.List) {
                        popUpTo(GroupNavRoutes.List) { inclusive = true }
                    }
                },
                onCreateComplete = { groupId ->
                    navController.navigate(
                        GroupNavRoutes.MemoWithId(groupId)
                    ) {
                        popUpTo(GroupNavRoutes.List) { inclusive = false }
                    }
                }
            )
        }

        // 그룹 메모
        composable(
            route = GroupNavRoutes.Memo,
            arguments = listOf(navArgument("groupId") { type = NavType.LongType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getLong("groupId")
            if (groupId == null || groupId <= 0) {
                navController.popBackStack()
                return@composable
            }
            GroupMemoScreen(
                navController = navController,
                groupId = groupId,
                onBackClick = {
                    navController.navigate(GroupNavRoutes.List) {
                        popUpTo(GroupNavRoutes.List) { inclusive = true }
                    }
                }
            )
        }

        // 그룹 수정
        composable(
            route = GroupNavRoutes.Edit,
            arguments = listOf(navArgument("groupId") { type = NavType.LongType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getLong("groupId")
            if (groupId == null || groupId <= 0) {
                navController.popBackStack()
                return@composable
            }

            val vm: GroupEditViewModel = hiltViewModel(backStackEntry)

            GroupEditScreen(
                groupId = groupId,
                viewModel = vm,
                onBackClick = {
                    navController.popBackStack()
                },
                onEditComplete = { groupId ->
                    navController.navigate(GroupNavRoutes.MemoWithId(groupId)) {
                        popUpTo(GroupNavRoutes.List) { inclusive = false }
                    }
                }
            )
        }

        // 그룹 사진
        composable(
            route = GroupNavRoutes.Photo,
            arguments = listOf(navArgument("groupId") { type = NavType.LongType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getLong("groupId")
            if (groupId == null || groupId <= 0) {
                navController.popBackStack()
                return@composable
            }
            GroupPhotoScreen(
                navController = navController,
                groupId = groupId,
                onBackClick = {
                    navController.navigate(GroupNavRoutes.List) {
                        popUpTo(GroupNavRoutes.List) { inclusive = true }
                    }
                }
            )
        }

        // 그룹 정산 (기존 - 이미지 업로드 화면)
        composable(
            route = GroupNavRoutes.Receipt,
            arguments = listOf(navArgument("groupId") { type = NavType.LongType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getLong("groupId")
            if (groupId == null || groupId <= 0) {
                navController.popBackStack()
                return@composable
            }
            ReceiptScreen(
                navController = navController,
                groupId = groupId,
                receiptImageUrl = "",
                onNavigateBack = {
                    navController.navigate(GroupNavRoutes.List) {
                        popUpTo(GroupNavRoutes.List) { inclusive = true }
                    }
                }
            )
        }



// 그룹 위치(지각 체크)
        composable(
            route = GroupNavRoutes.LateCheck,
            arguments = listOf(navArgument("groupId") { type = NavType.LongType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getLong("groupId")
            if (groupId == null || groupId <= 0) {
                navController.popBackStack()
                return@composable
            }
            LateCheckScreen(
                navController = navController,
                groupId = groupId,
                onBackClick = {
                    navController.navigate(GroupNavRoutes.List) {
                        popUpTo(GroupNavRoutes.List) { inclusive = true }
                    }
                }
            )
        }

// 그룹 채팅
        composable(
            route = GroupNavRoutes.Chat,
            arguments = listOf(navArgument("groupId") { type = NavType.LongType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getLong("groupId")
            if (groupId == null || groupId <= 0) {
                navController.popBackStack()
                return@composable
            }
            GroupChatScreen(
                navController = navController,
                groupId = groupId,
                onBackClick = {
                    navController.navigate(GroupNavRoutes.List) {
                        popUpTo(GroupNavRoutes.List) { inclusive = true }
                    }
                }
            )
        }

// 그룹 인원 관리
        composable(
            route = GroupNavRoutes.Members,
            arguments = listOf(navArgument("groupId") { type = NavType.LongType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getLong("groupId")
            if (groupId == null || groupId <= 0) {
                navController.popBackStack()
                return@composable
            }
            GroupMemberScreen(
                navController = navController,
                groupId = groupId,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        navigation(
            route = TrackingNavRoutes.Graph,
            startDestination = TrackingNavRoutes.Tracking
        ) {
            // 실시간 트래킹
            composable(TrackingNavRoutes.Tracking) {
                val parentEntry = remember(navController.currentBackStackEntry) {
                    navController.getBackStackEntry(TrackingNavRoutes.Graph)
                }
                val sharedVm = hiltViewModel<TrackingViewModel>(parentEntry)
                TrackingScreen(navController, trackingViewModel = sharedVm)
            }

            // 사진 선택
            composable(
                route = TrackingNavRoutes.UpdateWithArg,
                arguments = listOf(navArgument("trackingId") { type = NavType.StringType })
            ) { backStackEntry ->
                val trackingId = backStackEntry.arguments!!.getString("trackingId")!!
                val parentEntry = remember(navController.currentBackStackEntry) {
                    navController.getBackStackEntry(TrackingNavRoutes.Graph)
                }
                val sharedVm = hiltViewModel<TrackingViewModel>(parentEntry)
                TrackingUpdateScreen(
                    navController,
                    trackingId = trackingId,
                    trackingViewModel = sharedVm)
            }

            // 기록 창
            composable(TrackingNavRoutes.History) {
                val parentEntry = remember(navController.currentBackStackEntry) {
                    navController.getBackStackEntry(TrackingNavRoutes.Graph)
                }
                val sharedVm = hiltViewModel<TrackingViewModel>(parentEntry)
                TrackingListScreen(navController, trackingViewModel = sharedVm)
            }
        }
    }
}