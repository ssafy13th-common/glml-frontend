package com.ssafy.a705.domain.group.receipt

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.a705.group.member.GroupMemberRepository
import com.ssafy.a705.global.imageS3.ImageRepository
import com.ssafy.a705.global.network.ReceiptItem
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject

data class MemberInfo(
    val id: String,
    val name: String,
    val email: String? = null
)

@HiltViewModel
class ReceiptViewModel @Inject constructor(
    private val settlementRepository: GroupSettlementRepository,
    private val memberRepository: GroupMemberRepository,
    private val imageRepository: ImageRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // UI 상태
    private val _uiState = MutableStateFlow(ReceiptUiState())
    val uiState: StateFlow<ReceiptUiState> = _uiState.asStateFlow()

    // 영수증 분석 결과
    private val _receiptItems = MutableStateFlow<List<ReceiptItem>>(emptyList())
    val receiptItems: StateFlow<List<ReceiptItem>> = _receiptItems.asStateFlow()

    // 선택된 품목들
    private val _selectedItems = MutableStateFlow<Set<Int>>(emptySet())
    val selectedItems: StateFlow<Set<Int>> = _selectedItems.asStateFlow()

    // 선택된 멤버들 (이메일)
    private val _selectedMembers = MutableStateFlow<List<String>>(emptyList())
    val selectedMembers: StateFlow<List<String>> = _selectedMembers.asStateFlow()

    // 인당 정산 금액
    private val _pricePerPerson = MutableStateFlow(0)
    val pricePerPerson: StateFlow<Int> = _pricePerPerson.asStateFlow()

    // 그룹 멤버 정보
    private val _availableMembers = MutableStateFlow<List<MemberInfo>>(emptyList())
    val availableMembers: StateFlow<List<MemberInfo>> = _availableMembers.asStateFlow()

    // 정산된 품목들
    private val _settledItems = MutableStateFlow<Set<Int>>(emptySet())
    val settledItems: StateFlow<Set<Int>> = _settledItems.asStateFlow()

    // 정산 팝업 표시 여부
    private val _showSettlementDialog = MutableStateFlow(false)
    val showSettlementDialog: StateFlow<Boolean> = _showSettlementDialog.asStateFlow()

    // 영수증 이미지 URL
    private val _receiptImageUrl = MutableStateFlow<String>("")
    val receiptImageUrl: StateFlow<String> = _receiptImageUrl.asStateFlow()

    /**
     * 그룹 멤버 정보 로드
     */
    fun loadGroupMembers(groupId: Long) {
        viewModelScope.launch {
            val members = memberRepository.getMembers(groupId)
            println("🔍 ReceiptViewModel - 로드된 멤버 수: ${members.size}")
            
            val memberInfos = members.map { member ->
                println("🔍 ReceiptViewModel - 멤버: ${member.name}, 이메일: ${member.email}")
                MemberInfo(
                    id = member.id,
                    name = member.name,
                    email = member.email
                )
            }
            _availableMembers.value = memberInfos
            
            // 기본적으로 모든 멤버 선택
            if (memberInfos.isNotEmpty()) {
                _selectedMembers.value = memberInfos.map { it.email ?: "" }
                println("🔍 ReceiptViewModel - 선택된 멤버 이메일: ${_selectedMembers.value}")
            }
        }
    }

    /**
     * 영수증 이미지 업로드 및 분석
     */
    fun uploadReceiptImage(groupId: Long, uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            try {
                // 1. URI를 임시 파일로 복사
                val tempFile = copyUriToTempFile(uri)
                
                // 2. 파일명 생성
                val fileName = "receipt_${UUID.randomUUID()}.jpg"
                
                // 3. 프리사인드 URL 요청
                val presignedUrls = imageRepository.fetchPresignedUrls(listOf(fileName), "receipts")
                val presignedData = presignedUrls.firstOrNull() 
                    ?: throw Exception("프리사인드 URL을 받을 수 없습니다.")
                
                // 4. S3에 업로드
                val uploadSuccess = imageRepository.uploadFileToPresignedUrl(
                    presignedData.presignedUrl,
                    tempFile,
                    "image/jpeg"
                )
                
                if (!uploadSuccess) {
                    throw Exception("이미지 업로드에 실패했습니다.")
                }
                
                // 5. 업로드된 이미지 키 추출
                val imageKey = presignedData.fileName
                
                // 6. 영수증 이미지 URL 저장
                _receiptImageUrl.value = imageKey
                

                
                settlementRepository.analyzeReceipt(groupId, imageKey)
                                    .onSuccess { receiptData ->
                    _receiptItems.value = receiptData.receiptItems
                    // 기본적으로 모든 품목 선택 (빈 배열이어도 선택 상태는 빈 set)
                    _selectedItems.value = receiptData.receiptItems.indices.toSet()
                    _uiState.value = _uiState.value.copy(
                        isReceiptAnalyzed = true
                    )
                }
                    .onFailure { exception ->
                        _uiState.value = _uiState.value.copy(
                            isReceiptAnalyzed = true // 분석 실패해도 분석 완료로 처리
                        )
                    }
                // 7. 로딩 상태 해제
                _uiState.value = _uiState.value.copy(isLoading = false)
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "이미지 업로드에 실패했습니다."
                )
            }
        }
    }

    /**
     * 영수증 분석 실행 (이미지 URL이 있는 경우)
     */
    fun analyzeReceipt(groupId: Long, receiptImageUrl: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            // 영수증 이미지 URL 저장
            _receiptImageUrl.value = receiptImageUrl

            settlementRepository.analyzeReceipt(groupId, receiptImageUrl)
                .onSuccess { receiptData ->
                    _receiptItems.value = receiptData.receiptItems
                    // 기본적으로 모든 품목 선택 (빈 배열이어도 선택 상태는 빈 set)
                    _selectedItems.value = receiptData.receiptItems.indices.toSet()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isReceiptAnalyzed = true
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isReceiptAnalyzed = true // 분석 실패해도 분석 완료로 처리
                    )
                }
        }
    }



    /**
     * 품목 선택/해제
     */
    fun toggleItemSelection(index: Int) {
        val currentSelected = _selectedItems.value.toMutableSet()
        if (currentSelected.contains(index)) {
            currentSelected.remove(index)
        } else {
            currentSelected.add(index)
        }
        _selectedItems.value = currentSelected
        updatePricePerPerson()
    }

    /**
     * 모든 품목 선택
     */
    fun selectAllItems() {
        _selectedItems.value = _receiptItems.value.indices.toSet()
        updatePricePerPerson()
    }

    /**
     * 모든 품목 선택 해제
     */
    fun clearItemSelection() {
        _selectedItems.value = emptySet()
        updatePricePerPerson()
    }

    /**
     * 품목 수정
     */
    fun updateItem(index: Int, name: String, price: Int) {
        val currentItems = _receiptItems.value.toMutableList()
        if (index in currentItems.indices) {
            currentItems[index] = ReceiptItem(name, price)
            _receiptItems.value = currentItems
            updatePricePerPerson()
        }
    }

    /**
     * 품목 삭제
     */
    fun deleteItem(index: Int) {
        val currentItems = _receiptItems.value.toMutableList()
        if (index in currentItems.indices) {
            currentItems.removeAt(index)
            _receiptItems.value = currentItems
            
            // 선택된 품목 인덱스 조정
            val currentSelected = _selectedItems.value.toMutableSet()
            currentSelected.remove(index)
            currentSelected.map { if (it > index) it - 1 else it }.toSet().also {
                _selectedItems.value = it
            }
            updatePricePerPerson()
        }
    }

    /**
     * 커스텀 품목 추가
     */
    fun addCustomItem(name: String, price: Int) {
        val currentItems = _receiptItems.value.toMutableList()
        currentItems.add(ReceiptItem(name, price))
        _receiptItems.value = currentItems
        updatePricePerPerson()
    }

    /**
     * 정산 대상 멤버 선택/해제
     */
    fun toggleMemberSelection(memberId: String) {
        val currentMembers = _selectedMembers.value.toMutableList()
        if (currentMembers.contains(memberId)) {
            currentMembers.remove(memberId)
        } else {
            currentMembers.add(memberId)
        }
        _selectedMembers.value = currentMembers
        updatePricePerPerson()
    }

    /**
     * 모든 멤버 선택 (이메일 또는 ID 리스트)
     */
    fun selectAllMembers(memberIdentifiers: List<String>) {
        _selectedMembers.value = memberIdentifiers
        updatePricePerPerson()
    }

    /**
     * 모든 멤버 선택 해제
     */
    fun clearMemberSelection() {
        _selectedMembers.value = emptyList()
        updatePricePerPerson()
    }

    /**
     * 인당 정산 금액 계산
     */
    private fun updatePricePerPerson() {
        val selectedItemsList = _receiptItems.value.filterIndexed { index, _ -> 
            _selectedItems.value.contains(index) 
        }
        val totalPrice = selectedItemsList.sumOf { it.price }
        val memberCount = _selectedMembers.value.size
        
        _pricePerPerson.value = if (memberCount > 0) {
            totalPrice / memberCount
        } else {
            0
        }
    }

    /**
     * 정산 팝업 표시
     */
    fun showSettlementDialog() {
        val selectedItemsList = _receiptItems.value.filterIndexed { index, _ -> 
            _selectedItems.value.contains(index) 
        }
        
        if (selectedItemsList.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "정산할 품목을 선택해주세요."
            )
            return
        }
        
        _showSettlementDialog.value = true
    }

    /**
     * 정산 팝업 숨기기
     */
    fun hideSettlementDialog() {
        _showSettlementDialog.value = false
    }

    /**
     * 정산 확정 실행
     */
    fun settleGroup(groupId: Long) {
        val members = _selectedMembers.value
        val pricePerPerson = _pricePerPerson.value

        if (members.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "정산 대상 멤버를 선택해주세요."
            )
            return
        }

        if (pricePerPerson <= 0) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "정산 금액이 올바르지 않습니다."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            settlementRepository.settleGroup(groupId, pricePerPerson, members)
                .onSuccess {
                    // 정산된 품목들을 settledItems에 추가
                    _settledItems.value = _settledItems.value + _selectedItems.value
                    // 선택된 품목들 초기화
                    _selectedItems.value = emptySet()
                    // 선택된 멤버들 초기화
                    _selectedMembers.value = emptyList()
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSettlementCompleted = true
                    )
                    _showSettlementDialog.value = false
                    
                    // 모든 품목이 정산완료되었는지 확인
                    if (_settledItems.value.size == _receiptItems.value.size && _receiptItems.value.isNotEmpty()) {
                        // 모든 품목이 정산완료되면 UI 리셋
                        resetReceiptUI()
                    }
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: "정산 확정에 실패했습니다."
                    )
                }
        }
    }



    /**
     * 영수증 UI 리셋 (모든 품목 정산완료 시)
     */
    fun resetReceiptUI() {
        _uiState.value = _uiState.value.copy(
            isReceiptAnalyzed = false,
            isSettlementCompleted = false
        )
        _receiptItems.value = emptyList()
        _selectedItems.value = emptySet()
        _settledItems.value = emptySet()
        _receiptImageUrl.value = ""
        _pricePerPerson.value = 0
    }

    /**
     * 상태 초기화
     */
    fun resetState() {
        _uiState.value = ReceiptUiState()
        _receiptItems.value = emptyList()
        _selectedMembers.value = emptyList()
        _pricePerPerson.value = 0
    }

    /**
     * 에러 메시지 초기화
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    /**
     * URI를 임시 파일로 복사
     */
    private suspend fun copyUriToTempFile(uri: Uri): File {
        return withContext(kotlinx.coroutines.Dispatchers.IO) {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw Exception("이미지를 열 수 없습니다.")
            
            val tempFile = File.createTempFile("receipt_", ".jpg", context.cacheDir)
            FileOutputStream(tempFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            inputStream.close()
            
            tempFile
        }
    }
}

/**
 * 정산 UI 상태
 */
data class ReceiptUiState(
    val isLoading: Boolean = false,
    val isReceiptAnalyzed: Boolean = false,
    val isSettlementCompleted: Boolean = false,
    val errorMessage: String? = null
)
