package com.ssafy.a705.feature.board.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.a705.feature.board.data.model.request.CommentRequest
import com.ssafy.a705.feature.board.data.model.CommentDto
import com.ssafy.a705.feature.board.data.model.response.CommentResponse
import com.ssafy.a705.feature.board.data.model.WithPost
import com.ssafy.a705.feature.board.domain.repository.BoardRepository
import com.ssafy.a705.feature.board.domain.usecase.DeleteCommentUseCase
import com.ssafy.a705.feature.board.domain.usecase.DeletePostUseCase
import com.ssafy.a705.feature.board.domain.usecase.GetPostDetailUseCase
import com.ssafy.a705.feature.board.domain.usecase.UpdateCommentUseCase
import com.ssafy.a705.feature.board.domain.usecase.WriteCommentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PostDetailViewModel @Inject constructor(
    private val getPostDetailUseCase: GetPostDetailUseCase,
    private val deletePostUseCase: DeletePostUseCase,
    private val writeCommentUseCase: WriteCommentUseCase,
    private val updateCommentUseCase: UpdateCommentUseCase,
    private val deleteCommentUseCase: DeleteCommentUseCase
) : ViewModel() {

    /** 현재 보고 있는 게시글 ID */
    private var currentPostId: Long = -1L

    /** 게시글 헤더 정보(UI용) */
    private val _post = MutableStateFlow<WithPost?>(null)
    val post: StateFlow<WithPost?> = _post

    /** 🔑 서버 원본: 평평한(flat) 댓글 리스트를 단일 소스로 보관 */
    private val _flat = MutableStateFlow<List<CommentDto>>(emptyList())

    /** UI용: 매번 트리로 변환해서 노출 (항상 "새 인스턴스") */
    val comments: StateFlow<List<CommentResponse>> =
        _flat.map { buildTree(it) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** 상세 + 댓글 불러오기 */
    fun loadPost(postId: Long) {
        currentPostId = postId
        viewModelScope.launch {
            runCatching { getPostDetailUseCase(postId) }
                .onSuccess { data ->
                    _post.value = WithPost(
                        id = postId,
                        title = data.title,
                        content = data.content,
                        author = data.author,
                        authorEmail = data.authorEmail,
                        authorProfileUrl = data.authorProfileUrl,
                        date = data.updatedAt,
                        commentCount = data.comments.size
                    )
                    _flat.value = data.comments // 🔁 원본 평면 리스트만 갱신
                }
                .onFailure { it.printStackTrace() }
        }
    }

    /** 게시글 삭제 */
    fun deletePost(onSuccess: () -> Unit, onFailure: (Throwable) -> Unit) {
        viewModelScope.launch {
            runCatching { deletePostUseCase(currentPostId) }
                .onSuccess { onSuccess() }
                .onFailure { e ->
                    e.printStackTrace()
                    onFailure(e)
                }
        }
    }

    /** 댓글 작성 (루트/대댓글 공통, 낙관적 업데이트) */
    fun submitComment(
        content: String,
        parentId: Long?,
        onDone: (() -> Unit)? = null,
        onError: ((Throwable) -> Unit)? = null
    ) {
        viewModelScope.launch {
            runCatching {
                writeCommentUseCase(
                    currentPostId,
                    CommentRequest(content = content, parentId = parentId)
                )
            }.onSuccess {
                // ✅ 서버가 201 반환 → 성공 처리
                refreshCommentsMerge() // 최신 댓글 다시 가져오기
                onDone?.invoke()
            }.onFailure { e ->
                e.printStackTrace()
                onError?.invoke(e)
            }
        }
    }

    /** 댓글 수정 (여기선 간단히 서버 최신으로 재동기화) */
    fun updateComment(commentId: Long, content: String) {
        viewModelScope.launch {
            runCatching {
                updateCommentUseCase(
                    postId = currentPostId,
                    commentId = commentId,
                    comment = CommentRequest(content = content)
                )
            }.onSuccess {
                refreshCommentsMerge() // 수정 후엔 서버 최신으로 동기화
            }.onFailure { it.printStackTrace() }
        }
    }

    /** 댓글 삭제 */
    fun deleteComment(commentId: Long, onFailure: ((Throwable) -> Unit)? = null) {
        viewModelScope.launch {
            runCatching { deleteCommentUseCase(currentPostId, commentId) }
                .onSuccess { refreshCommentsMerge() }
                .onFailure {
                    it.printStackTrace()
                    onFailure?.invoke(it)
                }
        }
    }

    /** 서버 최신 댓글로 평면 리스트 갱신 */
    private suspend fun refreshCommentsMerge() {
        val fresh = getPostDetailUseCase(currentPostId).comments
        _flat.value = fresh
    }

    /** flat(CommentDto) → nested(CommentResponse) */
    private fun buildTree(all: List<CommentDto>): List<CommentResponse> {
        val byParent = all.groupBy { it.parentComment } // null = 루트
        fun node(dto: CommentDto): CommentResponse {
            // ✅ timestamp가 null이면 updatedDate → createdDate → "" 순으로 대체
            val safeTs = (dto.timestamp?.takeIf { it.isNotBlank() }
                ?: dto.updatedAt
                ?: dto.createdAt
                ?: "")

            return CommentResponse(
                id = dto.id,
                content = dto.content,
                author = dto.author,
                authorEmail = dto.authorEmail,
                authorProfileUrl = dto.authorProfileUrl,
                timestamp = safeTs,              // ✅ null 금지
                parentId = dto.parentComment,
                replies = byParent[dto.id].orEmpty().map { child -> node(child) }
            )
        }
        return byParent[null].orEmpty().map { node(it) }
    }
}
