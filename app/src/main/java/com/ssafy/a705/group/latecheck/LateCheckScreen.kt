package com.ssafy.a705.group.latecheck

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ssafy.a705.group.common.component.GroupTopBar
import android.util.Log

@Composable
fun LateCheckScreen(
    navController: NavController,
    groupId: Long,
    viewModel: LateCheckViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val uiState by viewModel.uiState.collectAsState()
    val connectionState by viewModel.webSocketConnectionState.collectAsState()

    // 위치 권한 요청 launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (locationGranted) {
            // 권한이 허용되면 위치공유 시작
            viewModel.onShareStart(groupId, context)
        }
    }

    LaunchedEffect(groupId) {
        Log.d("LateCheckScreen", "🔵 LaunchedEffect: loadGroupInfo 호출 - groupId=$groupId")
        viewModel.loadGroupInfo(groupId, context)
    }

    // 화면 재진입 시 새로고침 (ON_RESUME만)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                Log.d("LateCheckScreen", "🔵 ON_RESUME: loadGroupInfo 호출 - groupId=$groupId")
                viewModel.loadGroupInfo(groupId, context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        GroupTopBar(
            title = "그룹",
            onBackClick = onBackClick,
            groupId = groupId,
            navController = navController
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // 웹소켓 연결 상태 표시
            ConnectionStatusCard(connectionState = connectionState)
            Spacer(modifier = Modifier.height(8.dp))

            LocationSharingButton(
                isActive = uiState.isLocationSharingActive,
                onStart = {
                    // 권한 확인 후 요청
                    when {
                        context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                                context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
                            // 권한이 있으면 바로 시작
                            viewModel.onShareStart(groupId, context)
                        }
                        else -> {
                            // 권한이 없으면 요청
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    }
                },
                onStop  = { viewModel.onShareStop(groupId, context) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                LateCheckMapView(
                    modifier = Modifier.matchParentSize(),
                    meetingPlace = uiState.meetingPlace,
                    members = uiState.groupMembers
                )

                val sortedMembers = uiState.groupMembers.sortedByDescending { it.lateFee }
                val top3 = sortedMembers.take(3)
                val others = sortedMembers.drop(3)

                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .heightIn(min = 60.dp, max = 100.dp)
                        .verticalScroll(rememberScrollState())
                        .widthIn(max = 160.dp)
                        .background(Color(0xAAFFFFFF), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    top3.forEachIndexed { index, member ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${index + 1}위 ${member.nickname}", color = member.color)
                            Spacer(Modifier.width(8.dp))
                            Text("${member.lateFee} 원")
                        }
                    }
                    if (others.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Column(
                            modifier = Modifier
                                .heightIn(max = 100.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            others.forEachIndexed { idx, member ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${idx + 4}위 ${member.nickname}", color = member.color)
                                    Spacer(Modifier.width(8.dp))
                                    Text("${member.lateFee} 원")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectionStatusCard(connectionState: WebSocketConnectionState) {
    val (backgroundColor, textColor, message) = when (connectionState) {
        is WebSocketConnectionState.Idle -> Triple(Color(0xFFF5F5F5), Color(0xFF666666), "연결 대기 중")
        is WebSocketConnectionState.Connecting -> Triple(Color(0xFFFFF3E0), Color(0xFFE65100), "연결 중...")
        is WebSocketConnectionState.Connected -> Triple(Color(0xFFE8F5E8), Color(0xFF2E7D32), "실시간 연결됨")
        is WebSocketConnectionState.Closed -> Triple(Color(0xFFFFEBEE), Color(0xFFC62828), "연결 종료")
        is WebSocketConnectionState.Failed -> Triple(Color(0xFFFFEBEE), Color(0xFFC62828), "연결 실패")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = message,
            color = textColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun LocationSharingButton(
    isActive: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    Button(
        onClick = { if (isActive) onStop() else onStart() },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isActive) Color(0xFFE57373) else Color(0xFF4CAF50)
        )
    ) {
        Icon(
            imageVector = if (isActive) Icons.Filled.LocationOff else Icons.Filled.LocationOn,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (isActive) "위치공유 중단" else "위치공유 시작",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
