package com.ssafy.a705.feature.board.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.a705.common.network.base.ApiException
import com.ssafy.a705.feature.board.data.model.response.PostData
import com.ssafy.a705.feature.board.domain.usecase.GetPostsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BoardViewModel @Inject constructor(
    private val getPostsUseCase: GetPostsUseCase
) : ViewModel() {

    // 1) 에러 메시지 StateFlow 추가
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _postList = MutableStateFlow<List<PostData>>(emptyList())
    val postList: StateFlow<List<PostData>> = _postList

    private var nextCursor: Long? = null
    private var isLoading = false
    private var endOfList = false

    fun loadMorePosts() {
        if (isLoading || endOfList) return
        isLoading = true

        viewModelScope.launch {
            try {
                val data = getPostsUseCase(nextCursor)
                _postList.update { it + data.boards }

                nextCursor = data.nextCursor
                if (nextCursor == null) endOfList = true

                // 오류가 없다면 에러 메시지 초기화
                _errorMessage.value = null

            } catch (e: ApiException) {
                // 서버가 보낸 구체적 메시지
                _errorMessage.value = e.message
            } catch (e: Exception) {
                // 기타 네트워크/인증 오류
                _errorMessage.value = "인증이 필요합니다"
            } finally {
                isLoading = false
            }
        }
    }

    fun refreshPosts(forceIndicator: Boolean = true) {
        if (isLoading) return
        isLoading = true

        viewModelScope.launch {
            if (forceIndicator) {
                _errorMessage.value = null
            }

            try {
                val data = getPostsUseCase(nextCursor)
                // 비우지 않고 덮어쓰기
                _postList.value = data.boards
                nextCursor = data.nextCursor
                endOfList = (nextCursor == null)
            } catch (e: ApiException) {
                _errorMessage.value = e.message
            } catch (e: Exception) {
                _errorMessage.value = "인증이 필요합니다"
            } finally {
                isLoading = false
            }
        }
    }

    /** Compose 쪽에서 한 번 띄운 뒤 에러 메시지를 지우기 위한 헬퍼 */
    fun clearError() {
        _errorMessage.value = null
    }
}