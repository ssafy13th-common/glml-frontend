package com.ssafy.a705.feature.signup

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

@Composable
fun SignupScreen(
    navController: NavController,
    onNavigateToMain: () -> Unit,
    onOnboarding: () -> Unit,
    viewModel: SignupViewModel = hiltViewModel()
) {
    var name by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordCheck by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var isAgree by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val error by viewModel.error.collectAsState()
    val signupState by viewModel.signupState.collectAsState()
    val emailCheckOk by viewModel.emailCheckOk.collectAsState()
    val emailCheckMsg by viewModel.emailCheckMsg.collectAsState()

    // 닉네임: 형식만 검사 (중복확인 제거)
    val nickNameRegex = Regex("^[가-힣a-zA-Z]{2,20}$")
    val isNickNameFormatValid = nickname.matches(nickNameRegex)

    // 이메일: 형식 + 사용자가 '중복확인' 버튼을 눌렀는지 여부
    val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")
    val isEmailFormatValid = email.matches(emailRegex)

    // 비밀번호
    val isPasswordValid =
        password.matches(Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#\\\$%^&*()_+\\-=]).{8,16}$"))
    val isPasswordMatch = password == passwordCheck

    // 폼 유효성: 닉네임 형식 + 이메일 형식 & 중복확인 완료 + 비밀번호 조건 + 성별 선택 + 이름
    val isFormValid = name.isNotBlank() &&
            isNickNameFormatValid &&
            (isEmailFormatValid && emailCheckOk == true) &&
            isPasswordValid && isPasswordMatch &&
            gender.isNotEmpty() && isAgree

    val scrollState = rememberScrollState()

    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clear()
        }
    }

    LaunchedEffect(signupState) {
        signupState?.let {
            Toast.makeText(context, "이메일 전송 완료", Toast.LENGTH_SHORT).show()
            viewModel.clear()
            navController.navigate(SignupNavRoutes.EmailResend)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))
        Text(
            "회원가입",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(Modifier.height(18.dp))

        LabeledTextField("이름", name, onValueChange = { name = it }, isRequired = true)
        Spacer(Modifier.height(12.dp))

        // 닉네임: 중복확인 버튼 제거, 형식만 검증
        LabeledTextField(
            label = "닉네임",
            value = nickname,
            onValueChange = {
                nickname = it
            },
            isRequired = true,
            isValid = isNickNameFormatValid,
            errorMessage = if (nickname.isNotEmpty() && !isNickNameFormatValid)
                "닉네임은 2~20자의 한글 또는 영문만 가능합니다." else "",
            successMessage = if (nickname.isNotEmpty() && isNickNameFormatValid)
                "사용 가능한 닉네임입니다." else ""
        )
        Spacer(Modifier.height(12.dp))

        // 이메일: 중복확인 버튼 필요 (버튼 누를 때만 통과로 간주)
        LabeledTextField(
            label = "이메일",
            value = email,
            onValueChange = {
                email = it
                viewModel.resetEmailCheck()
            },
            isRequired = true,
            isValid = isEmailFormatValid && emailCheckOk == true,
            onCheckClick = {
                if (isEmailFormatValid) {
                    viewModel.checkEmail(email)
                }
            },
            errorMessage = when {
                email.isNotEmpty() && !isEmailFormatValid -> "이메일 형식이 올바르지 않습니다."
                isEmailFormatValid && emailCheckOk == false -> "이미 존재하는 이메일입니다."
                isEmailFormatValid && emailCheckOk == null -> "중복확인을 진행해 주세요."
                else -> ""
            },
            successMessage =
                if (isEmailFormatValid && emailCheckOk == true)
                    (emailCheckMsg ?: "사용 가능한 이메일입니다.")
                else ""
        )
        Spacer(Modifier.height(12.dp))

        LabeledTextField(
            label = "비밀번호",
            value = password,
            onValueChange = { password = it },
            isRequired = true,
            isPassword = true,
            isValid = isPasswordValid,
            errorMessage = if (!isPasswordValid && password.isNotEmpty())
                "8~16자의 영문 대/소문자, 숫자, 특수문자를 사용해 주세요." else ""
        )
        Spacer(Modifier.height(12.dp))

        LabeledTextField(
            label = "비밀번호 확인",
            value = passwordCheck,
            onValueChange = { passwordCheck = it },
            isRequired = true,
            isPassword = true,
            isValid = isPasswordMatch,
            errorMessage = if (!isPasswordMatch && passwordCheck.isNotEmpty())
                "비밀번호가 일치하지 않습니다." else ""
        )

        Spacer(Modifier.height(24.dp))

        // 성별 선택
        Text(
            "성별 *",
            fontSize = 19.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.fillMaxWidth(0.85f)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = gender == "남성",
                onCheckedChange = { gender = if (gender != "남성") "남성" else "" },
                colors = CheckboxDefaults.colors(uncheckedColor = Color(0xFFA8A6A7))
            )
            Text("남성",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable {
                    gender = if (gender != "남성") "남성" else ""
                })

            Checkbox(
                checked = gender == "여성",
                onCheckedChange = { gender = if (gender != "여성") "여성" else "" },
                colors = CheckboxDefaults.colors(uncheckedColor = Color(0xFFA8A6A7))
            )
            Text("여성",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable {
                    gender = if (gender != "여성") "여성" else ""
                })
        }
        Row(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isAgree == true,
                onCheckedChange = { isAgree = if(isAgree) false else true },
                colors = CheckboxDefaults.colors(uncheckedColor = Color(0xFFA8A6A7))
            )
            Text(
                "[필수] 개인정보 수집·이용 동의",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable {
                    showDialog = true
                }
            )

            if (showDialog) {
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = { Text("[필수] 개인정보 수집·이용 동의") },
                    text = {
                        Column(
                            modifier = Modifier
                                .heightIn(max = 300.dp) // 다이얼로그 높이 제한
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(text = buildAnnotatedString {
                                withStyle(style = SpanStyle(fontWeight = FontWeight.Black)) {
                                    append("1. 수집 항목")
                                }
                                append(": 이름, 닉네임, 이메일, 비밀번호, 성별")
                            },
                                modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
                            )
                            Text(
                                text = "2. 수집 목적",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            Text(text = buildAnnotatedString {
                                withStyle(style = SpanStyle(fontWeight = FontWeight.Black)) {
                                    append("• 이름")
                                }
                                append(": 실명기반 서비스 운영 정책 이행 및 사칭·부정 사용 방지")
                            },
                                modifier = Modifier.padding(start = 16.dp, bottom = 2.dp, top = 2.dp)
                            )
                            Text(text = buildAnnotatedString {
                                withStyle(style = SpanStyle(fontWeight = FontWeight.Black)) {
                                    append("• 닉네임")
                                }
                                append(": 커뮤니티 내 표시명 제공 및 이용자 식별")
                            },
                                modifier = Modifier.padding(start = 16.dp, bottom = 2.dp, top = 2.dp)
                            )
                            Text(text = buildAnnotatedString {
                                withStyle(style = SpanStyle(fontWeight = FontWeight.Black)) {
                                    append("• 이메일, 비밀번호")
                                }
                                append(": 회원가입, 로그인 및 계정 관리")
                            },
                                modifier = Modifier.padding(start = 16.dp, bottom = 2.dp, top = 2.dp)
                            )
                            Text(text = buildAnnotatedString {
                                withStyle(style = SpanStyle(fontWeight = FontWeight.Black)) {
                                    append("• 성별")
                                }
                                append(": 핵심 기능의 제공 및 운영 정책 이행")
                            },
                                modifier = Modifier.padding(start = 16.dp, bottom = 2.dp, top = 2.dp)
                            )
                            Text(text = buildAnnotatedString {
                                withStyle(style = SpanStyle(fontWeight = FontWeight.Black)) {
                                    append("3. 보유 기간")
                                }
                                append(": 회원 탈퇴 시 5일 이내 삭제. 서비스 종료 시 종료일로부터 5일 이내 일괄 파기.")
                            },
                                modifier = Modifier.padding(bottom = 8.dp, top = 10.dp)
                            )
                            Text(text = buildAnnotatedString {
                                withStyle(style = SpanStyle(fontWeight = FontWeight.Black)) {
                                    append("4. 동의 거부 권리 및 불이익")
                                }
                                append(": 동의를 거부할 권리가 있으나, 필수 항목 미동의 시 회원 가입이 제한됩니다.")
                            },
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showDialog = false }) {
                            Text("확인")
                        }
                    }
                )
            }
        }

        Spacer(Modifier.height(30.dp))

        Button(
            onClick = {
                val genderCode = when (gender) { "남성" -> "MALE"; "여성" -> "FEMALE"; else -> "" }
                val request = SignupRequest(
                    name = name,
                    nickname = nickname,
                    email = email,
                    password = password,
                    gender = genderCode,
                    profileImage = "members/standardProfile.png" // 선택 기능 생기면 교체
                )
                // 실제 회원가입 호출
                viewModel.signup(request)

            },
            enabled = isFormValid,
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4376FF))
        ) {
            Text("가입하기", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("이미 계정이 있습니까?", color = Color.Gray)
            Spacer(Modifier.width(4.dp))
            TextButton(onClick = onOnboarding) {
                Text(
                    "로그인하기",
                    color = Color(0xFFD87234),
                    style = TextStyle(textDecoration = TextDecoration.Underline)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        TextButton(onClick = onNavigateToMain) {
            Text(
                "둘러보기",
                color = Color(0xFF9E9E9E),
                style = TextStyle(textDecoration = TextDecoration.Underline)
            )
        }
    }
}

@Composable
fun LabeledTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = label,
    isPassword: Boolean = false,
    isRequired: Boolean = false,
    isValid: Boolean = true,
    onCheckClick: (() -> Unit)? = null,   // 이메일에서만 전달
    errorMessage: String = "",
    successMessage: String = "",
    modifier: Modifier = Modifier
) {
    val annotatedLabel = buildAnnotatedString {
        append(label)
        if (isRequired) append(" *")
    }
    var isPasswordVisible by remember { mutableStateOf(false) }

    Column(
        modifier.fillMaxWidth(0.85f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(annotatedLabel, fontSize = 19.sp, fontWeight = FontWeight.Medium)
            if (onCheckClick != null) { // 이메일에만 표출
                OutlinedButton(
                    onClick = onCheckClick,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(22.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
                ) {
                    Text("중복확인", fontSize = 12.sp)
                }
            }
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = Color.Gray) },
            singleLine = true,
            visualTransformation = if (isPassword && !isPasswordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            trailingIcon = {
                if (isPassword) {
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                            contentDescription = null,
                            tint = Color(0xFFD87234),
                            modifier = Modifier.graphicsLayer { scaleX = -1f }
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color(0xFFA8A6A7),
                focusedBorderColor = Color(0xFF4376FF),
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            )
        )
        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = Color.Red, fontSize = 12.sp)
        } else if (successMessage.isNotEmpty()) {
            Text(successMessage, color = Color(0xFF00A000), fontSize = 12.sp)
        }
    }
}
