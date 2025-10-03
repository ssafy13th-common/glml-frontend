package com.ssafy.a705.group.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.ssafy.a705.global.components.HeaderRow
import com.ssafy.a705.group.common.component.CustomFlagTab
import com.ssafy.a705.group.common.component.EmptyGroupCard
import com.ssafy.a705.group.common.component.GroupCard
import com.ssafy.a705.group.common.model.Group
import com.ssafy.a705.global.navigation.GroupNavRoutes

@Composable
fun GroupListScreen(
    navController: NavController,
    viewModel: GroupListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    // 초기 로드
    LaunchedEffect(Unit) {
        viewModel.loadGroups()
    }

    // 화면이 포커스를 받을 때마다 새로고침 (편집 후 돌아올 때 등)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                println("🔄 화면 포커스 - 그룹 목록 새로고침")
                viewModel.loadGroups()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            HeaderRow(
                text = "그룹",
                showText = true,
                showLeftButton = false,
                menuActions = emptyList()
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(GroupNavRoutes.Create) },
                containerColor = Color.White,
                contentColor = Color.Black
            ) {
                Icon(Icons.Default.Add, contentDescription = "그룹 생성")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 여행중인 그룹이 없을 때만 첫 번째 구분선 표시
            if (state.ongoingGroup == null) {
                HorizontalDivider()
            }

            // 현재 진행 중인 그룹
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(123.dp),
                contentAlignment = Alignment.Center
            ) {
                state.ongoingGroup?.let { g ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (g.id > 0) {
                                    navController.navigate(GroupNavRoutes.MemoWithId(g.id))
                                }
                            }
                    ) {
                        GroupCard(g)
                    }
                } ?: Text(
                    "여행중인 그룹이 없어요",
                    color = Color.Gray
                )
            }

            // 여행중인 그룹이 없을 때만 두 번째 구분선 표시
            if (state.ongoingGroup == null) {
                HorizontalDivider()
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 탭
            GroupTabRow(
                selectedTab = state.isPlanningSelected,
                onTabSelected = viewModel::toggleTab
            )

            // 탭별 그룹 리스트
            if (state.isPlanningSelected) {
                PlanningGroupList(state.planningGroups) { groupId ->
                    println("▶️ click planning id=$groupId")
                    if (groupId > 0) navController.navigate(GroupNavRoutes.MemoWithId(groupId))
                }
            } else {
                FinishedGroupList(state.finishedGroups) { groupId ->
                    if (groupId > 0) {
                        navController.navigate(GroupNavRoutes.MemoWithId(groupId))
                    }
                }
            }
        }
    }
}

// 계획중 리스트
@Composable
fun PlanningGroupList(
    groups: List<Group>,
    onGroupClick: (Long) -> Unit
) {
    if (groups.isEmpty()) {
        EmptyGroupCard()
    } else {
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(groups) { group ->
                println("🔍 PlanningGroupList rendering: id=${group.id}, name=${group.name}")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            println("🔍 PlanningGroupList click: id=${group.id}")
                            onGroupClick(group.id)
                        }
                ) {
                    GroupCard(group)
                }
            }
        }
    }
}

// 여행완료 리스트
@Composable
fun FinishedGroupList(
    groups: List<Group>,
    onGroupClick: (Long) -> Unit
) {
    if (groups.isEmpty()) {
        EmptyGroupCard()
    } else {
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(groups) { group ->
                println("🔍 FinishedGroupList rendering: id=${group.id}, name=${group.name}")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            println("🔍 FinishedGroupList click: id=${group.id}")
                            onGroupClick(group.id)
                        }
                ) {
                    GroupCard(group)
                }
            }
        }
    }
}

// 서류철 모양
@Composable
fun GroupTabRow(
    modifier: Modifier = Modifier,
    selectedTab: Boolean,
    onTabSelected: (Boolean) -> Unit
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CustomFlagTab(
            text = "여행 전",
            selected = selectedTab,
            onClick = { onTabSelected(true) },
            backgroundColor = Color(0xFF8bd666)
        )

        CustomFlagTab(
            text = "여행 완료",
            selected = !selectedTab,
            onClick = { onTabSelected(false) },
            backgroundColor = Color(0xFF81b5f8)
        )
    }
}
