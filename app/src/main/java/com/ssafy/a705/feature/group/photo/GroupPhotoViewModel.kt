package com.ssafy.a705.feature.group.photo

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.a705.common.network.GroupImageDto
import com.ssafy.a705.group.list.GroupRepository
import com.ssafy.a705.group.common.util.GroupStatusUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GroupPhotoUiState(
    val selectedUris: List<Uri> = emptyList(),
    val groupImages: List<GroupImageDto> = emptyList(),
    val groupName: String = "",
    val status: String = "",
    val isLoading: Boolean = false,
    val isUploading: Boolean = false,
    val uploadProgress: Int = 0,
    val error: String? = null
)

@HiltViewModel
class GroupPhotoViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val photoRepository: GroupPhotoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupPhotoUiState())
    val uiState: StateFlow<GroupPhotoUiState> = _uiState.asStateFlow()

    fun loadGroupInfo(groupId: Long) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                val groupInfo = groupRepository.getGroupInfo(groupId)
                
                // 자동 상태 변경 적용
                val updatedStatus = GroupStatusUtil.getAutoUpdatedStatus(
                    groupInfo.status,
                    groupInfo.startAt,
                    groupInfo.endAt
                )
                
                val displayStatus = GroupStatusUtil.getDisplayStatus(updatedStatus)
                
                _uiState.value = _uiState.value.copy(
                    groupName = groupInfo.name,
                    status = displayStatus,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "그룹 정보를 불러오는데 실패했습니다: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    fun loadGroupImages(groupId: Long) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                val images = photoRepository.getGroupImages(groupId)
                _uiState.value = _uiState.value.copy(
                    groupImages = images,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "이미지를 불러오는데 실패했습니다: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    fun addPhotos(groupId: Long, newUris: List<Uri>) {
        _uiState.value = _uiState.value.copy(
            selectedUris = (_uiState.value.selectedUris + newUris).distinct(),
            error = null
        )
        uploadSelectedPhotos(groupId)
    }

    fun removePhoto(uri: Uri) {
        _uiState.value = _uiState.value.copy(
            selectedUris = _uiState.value.selectedUris - uri
        )
    }

    fun deleteGroupImage(groupId: Long, imageId: Long) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(error = null)
                val success = photoRepository.deleteImage(groupId, imageId)
                if (success) {
                    _uiState.value = _uiState.value.copy(
                        groupImages = _uiState.value.groupImages.filter { it.imageId != imageId }
                    )
                } else {
                    _uiState.value = _uiState.value.copy(error = "이미지 삭제에 실패했습니다.")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "이미지 삭제 중 오류가 발생했습니다: ${e.message}"
                )
            }
        }
    }

    private fun uploadSelectedPhotos(groupId: Long) {
        val uris = _uiState.value.selectedUris
        if (uris.isEmpty()) return

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isUploading = true, 
                    uploadProgress = 0,
                    error = null
                )
                
                val success = photoRepository.uploadImages(
                    groupId, 
                    uris,
                    onProgress = { progress ->
                        _uiState.value = _uiState.value.copy(uploadProgress = progress)
                    }
                )
                
                if (success) {
                    _uiState.value = _uiState.value.copy(
                        selectedUris = emptyList(),
                        isUploading = false,
                        uploadProgress = 100
                    )
                    // 업로드 완료 후 이미지 목록 새로고침
                    loadGroupImages(groupId)
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = "이미지 업로드에 실패했습니다.",
                        isUploading = false,
                        uploadProgress = 0
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "이미지 업로드 중 오류가 발생했습니다: ${e.message}",
                    isUploading = false,
                    uploadProgress = 0
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
