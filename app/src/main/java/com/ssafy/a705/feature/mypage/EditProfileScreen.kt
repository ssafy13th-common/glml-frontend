package com.ssafy.a705.feature.mypage

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ssafy.a705.R
import com.ssafy.a705.common.components.HeaderRow
import com.ssafy.a705.feature.controller.viewmodel.MyPageViewModel
import com.ssafy.a705.feature.controller.viewmodel.ProfilePhotoViewModel
import kotlinx.coroutines.launch

@Composable
fun EditProfileScreen(
    onBack: () -> Unit = {},
    onDeleteAccount: () -> Unit = {},
    onChangePasswordClick: () -> Unit = {},
    onChangePhotoClick: () -> Unit = {},
    onLogoutSuccess: () -> Unit = {},
    myPageViewModel: MyPageViewModel = hiltViewModel(),
    photoVm: ProfilePhotoViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val profile by myPageViewModel.profile.collectAsState()
    LaunchedEffect(Unit) {
        if (profile == null) myPageViewModel.loadMyProfile()
    }
    val email = profile?.email ?: "이메일 없음"
    val emailRaw = profile?.email
    val profileUrl = profile?.profileUrl?.trim().orEmpty()

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showWithdrawDialog by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        val e = emailRaw
        if (uri == null || e.isNullOrBlank()) {
            Toast.makeText(context, "이미지/이메일 정보를 확인하세요.", Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }
        photoVm.pickAndUpload(e, uri) { ok ->
            if (ok) {
                myPageViewModel.loadMyProfile()
            } else {
                Toast.makeText(context, "사진을 조정해주세요", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 포그라운드 복귀 시 최신 프로필 재조회
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) {
                myPageViewModel.loadMyProfile()
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    Scaffold(
        topBar = {
            // ✅ 제목만 중앙 정렬 + 좌측 뒤로가기
            HeaderRow(
                text = "프로필 수정",
                showText = true,
                showLeftButton = true,
                onLeftClick = onBack,
                menuActions = emptyList()
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            // 프로필 이미지 + 카메라 버튼
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.BottomEnd
            ) {
                if (profileUrl.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(profileUrl)
                            .crossfade(true)
                            .listener(
                                onSuccess = { _, _ -> Log.d("ProfileImage", "성공: $profileUrl") },
                                onError = { _, result ->
                                    Log.e("ProfileImage", "실패: $profileUrl", result.throwable)
                                }
                            )
                            .build(),
                        contentDescription = "프로필 이미지",
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(R.drawable.profile),
                        error = painterResource(R.drawable.profile)
                    )
                } else {
                    Image(
                        painter = painterResource(R.drawable.profile),
                        contentDescription = "기본 프로필 이미지",
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                    )
                }

                Image(
                    painter = painterResource(id = R.drawable.camera),
                    contentDescription = "사진 변경",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                            onChangePhotoClick()
                        }
                        .padding(8.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            NicknameRow(
                nickname = profile?.nickname.orEmpty(),
                onSave = { newName ->
                    val e = emailRaw
                    if (e.isNullOrBlank()) {
                        Toast.makeText(context, "이메일 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                        return@NicknameRow
                    }
                    myPageViewModel.updateNickname(e, newName) { ok ->
                        if (ok) {
                            Toast.makeText(context, "닉네임이 변경되었습니다.", Toast.LENGTH_SHORT).show()
                            myPageViewModel.loadMyProfile()
                        } else {
                            Toast.makeText(context, "닉네임 변경에 실패했습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )

            Spacer(Modifier.height(12.dp))
            Divider(color = Color(0xFFDFDFDF), thickness = 1.dp)
            Spacer(Modifier.height(12.dp))

            ProfileItem(label = "이메일", value = email)

            Divider(color = Color(0xFFDFDFDF), thickness = 1.dp)

            Text(
                text = "로그아웃",
                fontSize = 16.sp,
                color = Color.Red,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .clickable { showLogoutDialog = true }
            )

            Spacer(Modifier.weight(1f))
            Divider(color = Color(0xFFDFDFDF), thickness = 1.dp)

            Text(
                text = "회원탈퇴",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 16.dp, top = 12.dp)
                    .clickable { showWithdrawDialog = true }
            )
        }
    }

    // 로그아웃 다이얼로그 (JWT 로그아웃 + 카카오 처리 ViewModel 내부)
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("갈래? 말래!") },
            text = { Text("정말 떠나실거에요?\n설레는 여정을 놓칠 수도 있어요") },
            dismissButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        scope.launch {
                            myPageViewModel.logoutAll { _ ->
                                onLogoutSuccess()
                            }
                        }
                    }
                ) { Text("그래 할래") }
            },
            confirmButton = {
                OutlinedButton(onClick = { showLogoutDialog = false }) {
                    Text("아냐 말래! ✈️")
                }
            }
        )
    }

    // 탈퇴 다이얼로그 (서버 탈퇴 + 카카오 연결끊기 ViewModel 내부)
    if (showWithdrawDialog) {
        AlertDialog(
            onDismissRequest = { showWithdrawDialog = false },
            title = { Text("정말 떠나실 건가요?") },
            text = { Text("탈퇴 시 데이터가 삭제되며 복구할 수 없어요.") },
            dismissButton = {
                TextButton(
                    onClick = {
                        showWithdrawDialog = false
                        scope.launch {
                            myPageViewModel.withdrawAll { _ ->
                                onLogoutSuccess()
                            }
                        }
                    }
                ) { Text("네, 탈퇴할래요") }
            },
            confirmButton = {
                OutlinedButton(onClick = { showWithdrawDialog = false }) {
                    Text("취소")
                }
            }
        )
    }
}

@Composable
fun ProfileItem(
    label: String,
    value: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .let { if (onClick != null) it.clickable { onClick() } else it },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color(0xFF000000), fontSize = 14.sp)
        Spacer(modifier = Modifier.weight(1f))
        Text(value, color = Color(0xFFA8A6A7), fontSize = 14.sp)
    }
}

/**
 * UX개선 닉네임 편집 행:
 * - 보기 상태: 중앙 텍스트 + 연필 아이콘
 * - 편집 상태: OutlinedTextField + 취소/저장 아이콘(✕/✓)
 */
@Composable
private fun NicknameRow(
    nickname: String,
    onSave: (String) -> Unit,
    maxLen: Int = 20,
) {
    var editing by remember { mutableStateOf(false) }
    var text by remember(nickname) { mutableStateOf(nickname) }
    val keyboard = LocalSoftwareKeyboardController.current

    if (!editing) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (nickname.isBlank()) "닉네임 없음" else nickname,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                painter = painterResource(id = R.drawable.pen),
                contentDescription = "닉네임 수정",
                tint = Color(0xFFA8A6A7),
                modifier = Modifier
                    .size(18.dp)
                    .clickable { editing = true }
            )
        }
    } else {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { if (it.length <= maxLen) text = it },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .heightIn(min = 40.dp),
                placeholder = { Text("닉네임 입력") },
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                trailingIcon = {
                    Row {
                        IconButton(onClick = {
                            text = nickname
                            editing = false
                            keyboard?.hide()
                        }) { Icon(Icons.Default.Close, contentDescription = "취소") }
                        IconButton(onClick = {
                            val trimmed = text.trim()
                            if (trimmed.isNotBlank() && trimmed != nickname) onSave(trimmed)
                            editing = false
                            keyboard?.hide()
                        }) { Icon(Icons.Default.Check, contentDescription = "저장") }
                    }
                }
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "${text.length} / $maxLen",
                fontSize = 12.sp,
                color = Color(0xFF9E9E9E)
            )
        }
    }
}
