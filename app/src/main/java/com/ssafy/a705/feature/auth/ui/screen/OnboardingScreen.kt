package com.ssafy.a705.feature.auth.ui.screen

import android.app.Activity
import android.content.ContextWrapper
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kakao.sdk.auth.model.OAuthToken
import com.ssafy.a705.R
import com.ssafy.a705.feature.auth.ui.viewmodel.OnboardingViewModel
import com.ssafy.a705.common.network.sign.SignApi
import com.ssafy.a705.feature.signup.SignupViewModel
import kotlinx.coroutines.launch

@Composable
private fun currentActivity(): Activity? {
    var ctx = LocalContext.current
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onNavigateToMain: () -> Unit,
    onSignUpClick: () -> Unit,
    onNavigateToEmailResend: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel(),
    signupViewModel: SignupViewModel = hiltViewModel(),
    fromLogout: Boolean = false
) {
    val configuration = LocalConfiguration.current
    val yOffset = (configuration.screenHeightDp.dp / 4) - 80.dp

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val loginOk by signupViewModel.loginOk.collectAsState()
    val loginMsg by signupViewModel.loginMsg.collectAsState()
    val needsVerifyEmail by signupViewModel.loginNeedsVerifyEmail.collectAsState()

    val ctx = LocalContext.current
    val activity = currentActivity()

    // ▼ 2-스텝 카카오 로그인 상태
    var kakaoToken by rememberSaveable { mutableStateOf<OAuthToken?>(null) }
    var showSheet by rememberSaveable { mutableStateOf(false) }
    var inputName by rememberSaveable { mutableStateOf("") }
    var gender by rememberSaveable { mutableStateOf(SignApi.Gender.MALE) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var exchanging by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun validateName(s: String): String? = when {
        s.isBlank() -> "이름을 입력하세요"
        s.length > 50 -> "이름은 50자 이내"
        else -> null
    }

    LaunchedEffect(loginOk) {
        when (loginOk) {
            true -> {
                onNavigateToMain()     // 메인으로 이동
                signupViewModel.clearLoginState()
            }
            false -> {
                Toast.makeText(ctx, loginMsg ?: "로그인 실패", Toast.LENGTH_SHORT).show()
                signupViewModel.clearLoginState()
            }
            else -> Unit
        }
    }

    LaunchedEffect(needsVerifyEmail) {
        needsVerifyEmail?.let {
            signupViewModel.resendVerify(email.trim())
            onNavigateToEmailResend(email.trim())
            signupViewModel.clearLoginState()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(yOffset))

        Image(painter = painterResource(R.drawable.uplogo), contentDescription = null, modifier = Modifier.size(160.dp))

        Spacer(Modifier.height(24.dp))

        Column {
            CustomTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = "이메일"
            )
            Spacer(Modifier.height(12.dp))
            CustomTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = "비밀번호",
                isPassword = true
            )
        }

        Spacer(Modifier.height(25.dp))

        Button(
            onClick = {
                if (email.isBlank() || password.isBlank()) {
                    Toast.makeText(ctx, "이메일/비밀번호를 입력하세요.", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                signupViewModel.login(email.trim(), password)   // ←← 여기서 호출!
            },
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(44.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4376FF))
        ) {
            Text(
                text = "로그인",
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight(400),
                    color = Color.White
                )
            )
        }



        //Spacer(Modifier.height(20.dp))

        /*Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                thickness = 1.dp,
                color = Color.LightGray
            )
            Text(
                "Sign In with",
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                thickness = 1.dp,
                color = Color.LightGray
            )
        }
*/
        Spacer(Modifier.height(12.dp))

        // ✅ 기존 UI 유지, 동작만 2-스텝으로 변경
        CustomButton(
            text = "카카오톡으로 로그인",
            onClick = {
                val act = activity ?: run {
                    Toast.makeText(ctx, "Activity 컨텍스트 없음", Toast.LENGTH_SHORT).show()
                    return@CustomButton
                }
                viewModel.requestKakaoToken(
                    activity = act,
                    onSuccess = { token, nickname ->
                        kakaoToken = token
                        inputName = nickname?.take(50).orEmpty()
                        nameError = validateName(inputName)
                        showSheet = true
                    },
                    onFailure = { /* 실패 토스트는 AuthManager에서 이미 표시됨 */ }
                )
            },
            iskakao = true
        )

        //Spacer(Modifier.height(12.dp))

        Row {
            TextButton(onClick = onSignUpClick) {
                Text(
                    "회원가입",
                    color = Color(0xFF9E9E9E),
                    style = TextStyle(textDecoration = TextDecoration.Underline)
                )
            }
            TextButton(onClick = onNavigateToMain) {
                Text(
                    "둘러보기",
                    color = Color(0xFF9E9E9E),
                    style = TextStyle(textDecoration = TextDecoration.Underline)
                )
            }
        }


    }

    // ✅ 추가정보 입력 바텀시트
    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { if (!exchanging) showSheet = false }
        ) {
            Column(
                Modifier
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("추가 정보", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)

                OutlinedTextField(
                    value = inputName,
                    onValueChange = {
                        inputName = it.take(50)
                        nameError = validateName(inputName)
                    },
                    label = { Text("이름 (50자 이내)") },
                    singleLine = true,
                    isError = nameError != null,
                    supportingText = { if (nameError != null) Text(nameError!!) },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("성별", modifier = Modifier.padding(end = 12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = gender == SignApi.Gender.MALE,
                            onClick = { gender = SignApi.Gender.MALE }
                        )
                        Text("남성")
                        Spacer(Modifier.width(16.dp))
                        RadioButton(
                            selected = gender == SignApi.Gender.FEMALE,
                            onClick = { gender = SignApi.Gender.FEMALE }
                        )
                        Text("여성")
                    }
                }

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = {
                        val token = kakaoToken ?: run {
                            Toast.makeText(ctx, "카카오 토큰이 없습니다. 다시 시도하세요.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val err = validateName(inputName)
                        if (err != null) { nameError = err; return@Button }

                        exchanging = true
                        scope.launch {
                            try {
                                viewModel.exchangeWithServer(
                                    gender = gender,
                                    name = inputName.trim()
                                )
                                exchanging = false
                                showSheet = false
                                onNavigateToMain()
                            } catch (t: Throwable) {
                                exchanging = false
                                Toast.makeText(ctx, "서버 교환 실패: ${t.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = !exchanging && nameError == null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (exchanging) "전송 중..." else "확인")
                }

                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
fun CustomButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight(400)
    ),
    iskakao: Boolean = false
) {
    val containerColor = if (iskakao) Color(0xFFFFE812) else Color(0xFFDFDFDF)
    val contentColor = if (iskakao) Color(0xFF191600) else Color(0xFF371C1D)

    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth(0.85f)
            .height(44.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        if (iskakao) {
            Image(
                painter = painterResource(id = R.drawable.kakao),
                contentDescription = "Kakao Logo",
                modifier = Modifier
                    .size(40.dp)
                    .padding(end = 8.dp)
            )
        }
        Text(text = text, style = textStyle.copy(color = contentColor))
    }
}

@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, fontSize = 18.sp) },
        singleLine = true,
        modifier = modifier
            .fillMaxWidth(0.85f)
            .height(56.dp),
        textStyle = TextStyle(
            fontSize = 18.sp,
            fontWeight = FontWeight.Normal,
            color = Color.Black
        ),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            errorContainerColor = Color.Transparent,
            unfocusedIndicatorColor = Color(0xFFA8A6A7),
            focusedIndicatorColor = Color(0xFF4376FF)
        ),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None
    )
}

@Preview(showBackground = true)
@Composable
fun OnboardingScreenPreview() {
    OnboardingScreen(onNavigateToMain = {}, onSignUpClick = {}, onNavigateToEmailResend = {})
}
