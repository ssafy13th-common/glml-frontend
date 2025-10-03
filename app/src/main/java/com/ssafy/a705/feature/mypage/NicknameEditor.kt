package com.ssafy.a705.feature.mypage

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit

@Composable
fun NicknameEditor(
    nickname: String,
    onSave: (String) -> Unit,
    maxLen: Int = 20,
    isSaving: Boolean = false,
    error: String? = null,
) {
    var editing by remember { mutableStateOf(false) }
    var text by remember(nickname) { mutableStateOf(nickname) }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    fun commit() {
        val trimmed = text.trim()
        if (trimmed.isNotBlank() && trimmed != nickname) onSave(trimmed)
        editing = false
        keyboard?.hide()
    }

    Crossfade(targetState = editing, label = "nicknameEdit") { isEditing ->
        if (!isEditing) {
            Row(
                modifier = Modifier.fillMaxWidth().height(40.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (nickname.isBlank()) "닉네임 없음" else nickname,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = {
                    text = nickname
                    editing = true
                }) {
                    Icon(Icons.Filled.Edit, contentDescription = "닉네임 수정", tint = Color(0xFFA8A6A7))
                }
            }
        } else {
            // ✅ Column으로 감싼 뒤 가운데 정렬
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { if (it.length <= maxLen) text = it },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .heightIn(min = 40.dp)
                        .focusRequester(focusRequester),
                    placeholder = { Text("닉네임 입력") },
                    trailingIcon = {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Row {
                                IconButton(onClick = {
                                    text = nickname
                                    editing = false
                                    keyboard?.hide()
                                }) { Icon(Icons.Filled.Close, contentDescription = "취소") }
                                IconButton(onClick = { commit() }) {
                                    Icon(Icons.Filled.Check, contentDescription = "저장")
                                }
                            }
                        }
                    },
                    supportingText = {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${text.length} / $maxLen")
                            if (!error.isNullOrBlank()) {
                                Text(error, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { commit() })
                )
            }
            LaunchedEffect(Unit) { focusRequester.requestFocus() }
        }
    }
}
