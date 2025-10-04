package com.ssafy.a705.feature.mypage.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun PasswordChangeScreen(
    onBack: () -> Unit = {},
    onPasswordChanged: () -> Unit = {}
) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    val passwordRegex = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#\\\$%^&*()_+\\-=]).{8,16}$")
    val passwordMeetsCriteria = password.matches(passwordRegex)
    val isConfirmMatched = confirmPassword == password

    val showPasswordError = password.isNotEmpty() && !passwordMeetsCriteria
    val showConfirmError = confirmPassword.isNotEmpty() && !isConfirmMatched

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        // AppBar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(
                "비밀번호 변경",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0x80DFDFDF), shape = RoundedCornerShape(8.dp))
                .border(0.dp, Color(0x80DFDFDF), shape = RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Text(
                text = "8자 ~ 16자 길이로 만들어주세요.\n" +
                        "영문 대/소문자, 숫자, 특수문자를 조합해주세요.",
                fontSize = 12.sp,
                color = Color(0xB2000000)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text("새 비밀번호", fontSize = 14.sp, fontWeight = FontWeight.Medium)

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xCC2D92FF), shape = RoundedCornerShape(10.dp)),
            placeholder = { Text("8~16자의 영문 대/소문자, 숫자, 특수문자", color = Color(0xFFA8A6A7),fontSize = 15.sp) },
            isError = showPasswordError,
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    Icon(
                        imageVector = if (isPasswordVisible)
                            Icons.Outlined.Visibility
                        else
                            Icons.Outlined.VisibilityOff,
                        contentDescription = null,
                        tint = Color(0xFFD87234),
                        modifier = Modifier.graphicsLayer { scaleX = -1f }
                    )
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = Color.Transparent,
                errorBorderColor = Color.Transparent
            )
        )

        if (showPasswordError) {
            Text(
                text = "8~16자의 영문 대/소문자, 숫자, 특수문자를 사용해 주세요.",
                fontSize = 12.sp,
                color = Color.Red,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("비밀번호 확인", fontSize = 14.sp, fontWeight = FontWeight.Medium)

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xCC2D92FF), shape = RoundedCornerShape(10.dp)),
            placeholder = { Text("비밀번호를 한번 더 입력하세요.", color = Color(0xFFA8A6A7), fontSize = 15.sp) },
            isError = showConfirmError,
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    Icon(
                        imageVector = if (isPasswordVisible)
                            Icons.Outlined.Visibility
                        else
                            Icons.Outlined.VisibilityOff,
                        contentDescription = null,
                        tint = Color(0xFFD87234),
                        modifier = Modifier.graphicsLayer { scaleX = -1f }
                    )
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = Color.Transparent,
                errorBorderColor = Color.Transparent
            )
        )

        if (showConfirmError) {
            Text(
                text = "비밀번호가 일치하지 않습니다.",
                fontSize = 12.sp,
                color = Color.Red,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { onPasswordChanged() },
            enabled = passwordMeetsCriteria && isConfirmMatched,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (passwordMeetsCriteria && isConfirmMatched) Color(0xFF2D92FF) else Color(0x802D92FF),
                disabledContainerColor = Color(0x802D92FF),
                disabledContentColor = Color.White
            )
        ) {
            Text("변경하기", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PasswordChangeScreenPreview() {
    PasswordChangeScreen()
}
