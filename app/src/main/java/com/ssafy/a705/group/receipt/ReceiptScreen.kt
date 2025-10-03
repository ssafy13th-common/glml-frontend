package com.ssafy.a705.group.receipt

import android.R
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ssafy.a705.network.ReceiptItem
import com.ssafy.a705.group.receipt.MemberInfo
import com.ssafy.a705.group.common.component.GroupTopBar
import coil.compose.rememberAsyncImagePainter
import androidx.compose.ui.layout.ContentScale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptScreen(
    navController: NavController,
    groupId: Long,
    receiptImageUrl: String = "",
    availableMembers: List<MemberInfo> = emptyList(),
    onNavigateBack: () -> Unit,
    viewModel: ReceiptViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val receiptItems by viewModel.receiptItems.collectAsState()
    val selectedItems by viewModel.selectedItems.collectAsState()
    val settledItems by viewModel.settledItems.collectAsState()
    val showSettlementDialog by viewModel.showSettlementDialog.collectAsState()
    val groupMembers by viewModel.availableMembers.collectAsState()
    val receiptImageUrl by viewModel.receiptImageUrl.collectAsState()
    val context = LocalContext.current

    // 이미지 선택 런처
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.uploadReceiptImage(groupId, it)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadGroupMembers(groupId)
        
        // receiptImageUrl이 비어있지 않을 때만 영수증 분석 실행
        if (receiptImageUrl.isNotEmpty()) {
            viewModel.analyzeReceipt(groupId, receiptImageUrl)
        }
    }

    // 실제 그룹 멤버 정보가 있으면 사용, 없으면 전달받은 멤버 정보 사용
    val membersToUse = if (groupMembers.isNotEmpty()) groupMembers else availableMembers

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(0.dp, 0.dp, 0.dp, 50.dp)
            .background(Color.White)
    ) {
        // 상단 헤더
        GroupTopBar(
            title = "그룹",
            onBackClick = onNavigateBack,
            groupId = groupId,
            navController = navController
        )
        
                            LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 에러 메시지
            uiState.errorMessage?.let { errorMessage ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                    ) {
                        Text(
                            text = errorMessage,
                            modifier = Modifier.padding(16.dp),
                            color = Color(0xFFD32F2F),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // 영수증 업로드/표시 영역 (분석 전에만 표시)
            if (!uiState.isReceiptAnalyzed) {
                item {
                    ReceiptDisplayArea(
                        receiptItems = receiptItems,
                        isLoading = uiState.isLoading,
                        onImageSelect = { imagePickerLauncher.launch("image/*") },
                        receiptImageUrl = receiptImageUrl
                    )
                }
            }

            // 영수증 분석이 완료되었을 때 품목 목록 표시
            if (uiState.isReceiptAnalyzed) {
                // 영수증 분석 실패 시 안내 메시지 (품목이 없을 때만)
                if (receiptItems.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "수동 입력",
                                    tint = Color(0xFFFF9800),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "영수증 분석에 실패했습니다. 수동으로 입력해주세요.",
                                    fontSize = 14.sp,
                                    color = Color(0xFFE65100),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // 전체선택 체크박스 (품목이 있을 때만)
                if (receiptItems.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedItems.size == receiptItems.filterIndexed { index, _ -> 
                                    !settledItems.contains(index) 
                                }.size && selectedItems.isNotEmpty(),
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        val availableIndices = receiptItems.indices.filter { !settledItems.contains(it) }
                                        viewModel.selectAllItems()
                                    } else {
                                        viewModel.clearItemSelection()
                                    }
                                }
                            )
                            Text("전체선택", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                // 품목 목록
                items(receiptItems) { item ->
                    val index = receiptItems.indexOf(item)
                    val isSettled = settledItems.contains(index)
                    
                    ReceiptItemCard(
                        item = item,
                        index = index,
                        isSelected = selectedItems.contains(index),
                        isSettled = isSettled,
                        onToggleSelection = { 
                            if (!isSettled) viewModel.toggleItemSelection(index) 
                        },
                        onUpdateItem = { name, price -> 
                            if (!isSettled) viewModel.updateItem(index, name, price) 
                        },
                        onDeleteItem = { 
                            if (!isSettled) viewModel.deleteItem(index) 
                        }
                    )
                }

                // 품목 추가 플러스 버튼 (항상 표시)
                item {
                    AddItemButton(
                        onAddItem = { name, price ->
                            viewModel.addCustomItem(name, price)
                        }
                    )
                }

                // 총 금액 (품목이 있을 때만)
                if (receiptItems.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "총 금액",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${receiptItems.filterIndexed { index, _ -> selectedItems.contains(index) }.sumOf { it.price }}원",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // 정산하기 버튼
                    item {
                        Button(
                            onClick = { viewModel.showSettlementDialog() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = selectedItems.isNotEmpty() && !uiState.isLoading
                        ) {
                            Text("정산하기")
                        }
                    }
                }
            }


        }

        // 정산 팝업
        if (showSettlementDialog) {
            SettlementDialog(
                members = membersToUse,
                selectedItems = receiptItems.filterIndexed { index, _ -> selectedItems.contains(index) },
                onDismiss = { viewModel.hideSettlementDialog() },
                onConfirm = { selectedMembers ->
                    viewModel.selectAllMembers(selectedMembers)
                    viewModel.settleGroup(groupId)
                }
            )
        }
    }
}

@Composable
fun AddItemButton(
    onAddItem: (String, Int) -> Unit
) {
    var showInput by remember { mutableStateOf(false) }
    var itemName by remember { mutableStateOf("") }
    var itemPrice by remember { mutableStateOf("") }

    if (showInput) {
        // 입력 폼 표시
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = itemName,
                    onValueChange = { itemName = it },
                    label = { Text("품목명") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = itemPrice,
                    onValueChange = { itemPrice = it },
                    label = { Text("가격") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                
                IconButton(
                    onClick = {
                        val price = itemPrice.toIntOrNull() ?: 0
                        if (itemName.isNotEmpty() && price > 0) {
                            onAddItem(itemName, price)
                            itemName = ""
                            itemPrice = ""
                            showInput = false
                        }
                    },
                    enabled = itemName.isNotEmpty() && itemPrice.toIntOrNull() ?: 0 > 0
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "추가"
                    )
                }
                
                IconButton(
                    onClick = {
                        showInput = false
                        itemName = ""
                        itemPrice = ""
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "취소"
                    )
                }
            }
        }
    } else {
        // 플러스 버튼 표시
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showInput = true }
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "품목 추가",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "품목 추가",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun ReceiptItemCard(
    item: ReceiptItem,
    index: Int,
    isSelected: Boolean,
    isSettled: Boolean,
    onToggleSelection: () -> Unit,
    onUpdateItem: (String, Int) -> Unit,
    onDeleteItem: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf(item.name) }
    var editedPrice by remember { mutableStateOf(item.price.toString()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
            // 체크박스
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelection() },
                enabled = !isSettled
            )

            // 품목 정보
            if (isEditing) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = editedName,
                        onValueChange = { editedName = it },
                        label = { Text("품목명") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editedPrice,
                        onValueChange = { editedPrice = it },
                        label = { Text("가격") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val price = editedPrice.toIntOrNull() ?: 0
                                onUpdateItem(editedName, price)
                                isEditing = false
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("저장")
                        }
                        Button(
                            onClick = { isEditing = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("취소")
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = item.name,
                        fontSize = 16.sp,
                        color = if (isSettled) Color.Gray else Color.Black
                    )
                    Text(
                        text = "${item.price}원",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSettled) Color.Gray else Color.Black
                    )
                }
            }

            // 수정/삭제 버튼
            if (!isEditing && !isSettled) {
                Row(horizontalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    IconButton(onClick = { isEditing = true }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "수정",
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onDeleteItem) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "삭제",
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

@Composable
fun MemberSelectionCard(
    member: MemberInfo,
    isSelected: Boolean,
    onToggleSelection: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleSelection() }
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = member.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                member.email?.let { email ->
                    Text(
                        text = email,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }
            
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "선택됨",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun ReceiptDisplayArea(
    receiptItems: List<ReceiptItem>,
    isLoading: Boolean,
    onImageSelect: () -> Unit,
    receiptImageUrl: String = ""
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
            .clickable(enabled = !isLoading) { onImageSelect() },
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(modifier = Modifier.size(48.dp), color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("사진을 분석중입니다...", style = MaterialTheme.typography.bodyMedium, color = Color.Black)
                }
            }
            else -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .border(1.dp, Color.LightGray, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "영수증 첨부", tint = Color.Gray, modifier = Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("영수증을 첨부해주세요", style = MaterialTheme.typography.bodyMedium, color = Color.Black)
                }
            }
        }
    }
}

@Composable
fun SettlementDialog(
    members: List<MemberInfo>,
    selectedItems: List<ReceiptItem>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit
) {
    var selectedMembers by remember { mutableStateOf(emptySet<String>()) }
    val totalAmount = selectedItems.sumOf { it.price }
    val pricePerPerson = if (selectedMembers.isNotEmpty()) totalAmount / selectedMembers.size else 0
    
    // 디버깅용 로그
    LaunchedEffect(members) {
        println("🔍 SettlementDialog - 멤버 수: ${members.size}")
        members.forEach { member ->
            println("🔍 SettlementDialog - 멤버: ${member.name}, 이메일: ${member.email}")
        }
    }
    
    // 멤버 식별자 생성 (이메일 사용)
    fun getMemberIdentifier(member: MemberInfo): String {
        return member.email ?: ""
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = "정산 금액",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${totalAmount}원",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 전체선택 체크박스
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = selectedMembers.size == members.size,
                        onCheckedChange = { checked ->
                            selectedMembers = if (checked) members.map { getMemberIdentifier(it) }.toSet() else emptySet()
                        }
                    )
                    Text("전체선택", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 멤버 목록 (2명씩 한 줄)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.height(200.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(members) { member ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val memberEmail = getMemberIdentifier(member)
                                    selectedMembers = if (selectedMembers.contains(memberEmail)) {
                                        selectedMembers - memberEmail
                                    } else {
                                        selectedMembers + memberEmail
                                    }
                                }
                                .border(
                                    width = if (selectedMembers.contains(getMemberIdentifier(member))) 2.dp else 0.dp,
                                    color = if (selectedMembers.contains(getMemberIdentifier(member))) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = selectedMembers.contains(getMemberIdentifier(member)),
                                    onCheckedChange = { checked ->
                                        val memberEmail = getMemberIdentifier(member)
                                        selectedMembers = if (checked) {
                                            selectedMembers + memberEmail
                                        } else {
                                            selectedMembers - memberEmail
                                        }
                                    }
                                )
                                Text(
                                    text = member.name,
                                    modifier = Modifier.weight(1f),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
                
                if (selectedMembers.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "1인당: ${pricePerPerson}원",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = { onConfirm(selectedMembers.toList()) },
                    enabled = selectedMembers.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("정산하기")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}
