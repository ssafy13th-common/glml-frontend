package com.ssafy.a705.feature.group.photo

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.ssafy.a705.feature.group.common.component.GroupTopBar
import com.ssafy.a705.feature.group.common.component.GroupStatusChip
import com.ssafy.a705.feature.group.common.component.GroupStatusChipSize
import com.ssafy.a705.common.network.GroupImageDto

@Composable
fun GroupPhotoScreen(
    navController: NavController,
    groupId: Long,
    onBackClick: () -> Unit = {}
) {
    val viewModel: GroupPhotoViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    // 이미지 선택을 위한 launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (!uris.isNullOrEmpty()) {
            viewModel.addPhotos(groupId, uris)
        }
    }

    // 초기 로드
    LaunchedEffect(groupId) {
        viewModel.loadGroupInfo(groupId)
        viewModel.loadGroupImages(groupId)
    }

    // 화면이 포커스를 받을 때마다 새로고침 (편집 후 돌아올 때 등)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                println("🔄 화면 포커스 - 그룹 정보 새로고침")
                viewModel.loadGroupInfo(groupId)
                viewModel.loadGroupImages(groupId)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(0.dp, 0.dp, 0.dp, 70.dp)
            .background(Color.White)
    ) {
        // 상단 헤더
        GroupTopBar(
            title = "그룹",
            onBackClick = onBackClick,
            groupId = groupId,
            navController = navController
        )

        // 로딩 상태 표시
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF2196F3)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "로딩 중...",
                        color = Color.Gray
                    )
                }
            }
        } else {
            // 메인 콘텐츠 영역
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // 그룹 제목과 상태 태그
                GroupTitleSection(
                    groupName = state.groupName,
                    status = state.status
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 에러 메시지 표시
                state.error?.let { error ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.1f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = error,
                                color = Color.Red,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { viewModel.clearError() }) {
                                Text("닫기")
                            }
                        }
                    }
                }

                // 업로드 진행 상황 표시
                if (state.isUploading) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2196F3).copy(alpha = 0.1f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color(0xFF2196F3),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "이미지 업로드 중...",
                                    color = Color(0xFF2196F3)
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = "${state.uploadProgress}%",
                                    color = Color(0xFF2196F3),
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = state.uploadProgress / 100f,
                                modifier = Modifier.fillMaxWidth(),
                                color = Color(0xFF2196F3)
                            )
                        }
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // 그룹 이미지들
                    items(state.groupImages) { image ->
                        GroupImageItem(
                            image = image,
                            onDeleteClick = { viewModel.deleteGroupImage(groupId, image.imageId) }
                        )
                    }

                    // 선택한 사진들 (업로드 중인 이미지들)
                    items(state.selectedUris) { uri ->
                        Box(
                            modifier = Modifier.aspectRatio(1f)
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(uri),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            
                            // 업로드 중 표시
                            if (state.isUploading) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.3f))
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 마지막 칸: 사진 추가 버튼
                    item {
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clickable(
                                    enabled = !state.isUploading
                                ) {
                                    imagePickerLauncher.launch("image/*")
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            // 원형 배경 + 테두리
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (state.isUploading) Color.Gray.copy(alpha = 0.3f) else Color.White
                                    )
                                    .border(
                                        2.dp, 
                                        Color.Gray,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "사진 추가",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GroupTitleSection(
    groupName: String,
    status: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 그룹 제목
        Text(
            text = groupName.ifEmpty { "그룹명" },
            fontSize = 24.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = Color(0xFF2196F3)
        )

        // 상태 태그
        GroupStatusChip(status = status.ifEmpty { "미정" }, size = GroupStatusChipSize.MEDIUM)
    }
}

@Composable
private fun GroupImageItem(
    image: GroupImageDto,
    onDeleteClick: () -> Unit
) {
    Box(
        modifier = Modifier.aspectRatio(1f)
    ) {
        Image(
            painter = rememberAsyncImagePainter(image.imageUrl),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )

        // 삭제 버튼 (우상단에 배치)
        IconButton(
            onClick = onDeleteClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(30.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "삭제",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}