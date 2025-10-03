package com.ssafy.a705.controller.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.a705.controller.service.WithService
import com.ssafy.a705.model.base.ApiException
import com.ssafy.a705.model.req.WithPostWriteRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WithPostWriteViewModel @Inject constructor(
    private val service: WithService
) : ViewModel() {

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title

    private val _content = MutableStateFlow("")
    val content: StateFlow<String> = _content

    private val _writeSuccess = MutableStateFlow<Long?>(null)
    val writeSuccess: StateFlow<Long?> = _writeSuccess

    fun onTitleChanged(text: String) {
        _title.value = text
    }

    fun onContentChanged(text: String) {
        _content.value = text
    }

    fun createPost(onSuccess: (Long) -> Unit) {
        viewModelScope.launch {
            // 에러 메시지 초기화
            _errorMessage.value = null

            try {
                val request = WithPostWriteRequest(
                    title = _title.value,
                    content = _content.value
                )
                val response = service.writePost(request)
                _writeSuccess.value = response.id
                onSuccess(response.id)
            } catch (e: ApiException) {
                // 서버가 보낸 message 그대로 화면으로 전달
                _errorMessage.value = e.message
                _writeSuccess.value = null
            } catch (e: Exception) {
                // 기타 예외
                _errorMessage.value = "잠시 후에 다시 시도해주세요"
                _writeSuccess.value = null
            }
        }
    }
    fun loadPost(postId: Long) {
        viewModelScope.launch {
            try {
                val data = service.getPostDetail(postId.toLong())   // ✅ 바로 데이터
                _title.value = data.title
                _content.value = data.content
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updatePost(postId: Long, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                service.updateBoard(
                    boardId = postId,
                    title = title.value,
                    content = content.value
                )
                _writeSuccess.value = postId
                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
                _writeSuccess.value = null
                _errorMessage.value = "수정에 실패했어요. 잠시 후 다시 시도해주세요."
            }
        }
    }



    fun clear() {
        _title.value = ""
        _content.value = ""
        _writeSuccess.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
