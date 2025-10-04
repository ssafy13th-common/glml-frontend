package com.ssafy.a705.feature.board.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.ssafy.a705.feature.board.ui.viewmodel.PostWriteViewModel
import com.ssafy.a705.common.navigation.Screen
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun WithPostWriteScreen(
    postId: Long? = null,
    navController: NavController = rememberNavController(),
    viewModel: PostWriteViewModel = hiltViewModel()
) {
    val title         by viewModel.title.collectAsState()
    val content       by viewModel.content.collectAsState()
    val errorMsg      by viewModel.errorMessage.collectAsState()
    val writeSuccess  by viewModel.writeSuccess.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val isFormValid = title.isNotBlank() && content.isNotBlank()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(postId) {
        if (postId != null) {
            viewModel.loadPost(postId)
        } else {
            viewModel.clear()
        }
    }

    LaunchedEffect(errorMsg) {
        errorMsg?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.Close, contentDescription = "닫기")
                }
                Text(
                    text = if (postId == null) "새 게시물" else "게시물 수정",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier
                        .weight(1f)
                        .wrapContentWidth(Alignment.CenterHorizontally)
                )

                OutlinedButton(
                    onClick = { showConfirmDialog = true },
                    enabled = isFormValid,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isFormValid) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isFormValid) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = if (postId == null) "등록" else "수정",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Medium,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        ),
                        maxLines = 1
                    )
                }
            }

            BasicTextField(
                value = title,
                onValueChange = viewModel::onTitleChanged,
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 20.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 30.dp)
            ) {
                if (title.isBlank()) {
                    Text("제목을 작성해주세요", color = Color.Gray)
                }
                it()
            }
            Divider(color = Color(0xFFDFDFDF), thickness = 1.dp)
            Spacer(Modifier.height(20.dp))
            BasicTextField(
                value = content,
                onValueChange = viewModel::onContentChanged,
                textStyle = TextStyle(fontSize = 16.sp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
            ) {
                if (content.isBlank()) {
                    Text("내용을 작성해주세요", color = Color.Gray)
                }
                it()
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }

    // ✅ 네/아니요 확인 다이얼로그
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("확인") },
            text = {
                Text(
                    if (postId == null) "게시물을 작성할까요?" else "게시물을 수정할까요?"
                )
            },
            confirmButton = {
                // 네 버튼을 왼쪽에
                TextButton(onClick = {
                    showConfirmDialog = false
                    if (postId == null) {
                        viewModel.createPost { newId ->
                            Toast.makeText(context, "게시글이 등록되었습니다.", Toast.LENGTH_SHORT).show()
                            scope.launch {
                                delay(500)
                                navController.navigate(Screen.WithDetail(newId).route) {
                                    popUpTo(Screen.WithPostWrite.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        }
                    } else {
                        viewModel.updatePost(postId) {
                            Toast.makeText(context, "게시글이 수정되었습니다.", Toast.LENGTH_SHORT).show()
                            scope.launch {
                                delay(500)
                                navController.popBackStack()
                            }
                        }
                    }
                }) { Text("네") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    Toast.makeText(
                        context,
                        if (postId == null) "작성이 취소되었습니다." else "수정이 취소되었습니다.",
                        Toast.LENGTH_SHORT
                    ).show()
                }) { Text("아니오") }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun WithPostWriteScreenPreview() {
    WithPostWriteScreen()
}
