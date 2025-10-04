package com.ssafy.a705.feature.mypage.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.a705.common.network.base.ApiException
import com.ssafy.a705.common.network.sign.KakaoAuthManager
import com.ssafy.a705.feature.auth.domain.usecase.LogOutUseCase
import com.ssafy.a705.feature.board.data.model.MyCommentDto
import com.ssafy.a705.feature.board.data.model.response.PostData
import com.ssafy.a705.feature.board.domain.usecase.GetPostDetailUseCase
import com.ssafy.a705.feature.mypage.data.model.request.PatchNicknameRequest
import com.ssafy.a705.feature.mypage.data.model.response.MyProfileResponse
import com.ssafy.a705.feature.mypage.domain.usecase.GetMyCommentsUseCase
import com.ssafy.a705.feature.mypage.domain.usecase.GetMyPostsUseCase
import com.ssafy.a705.feature.mypage.domain.usecase.GetProfileUseCase
import com.ssafy.a705.feature.mypage.domain.usecase.UpdateNicknameUseCase
import com.ssafy.a705.feature.signup.SignupApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyPageViewModel @Inject constructor(
    private val kakaoAuth: KakaoAuthManager,
    private val getPostDetailUseCase: GetPostDetailUseCase,
    private val getMyPostsUseCase: GetMyPostsUseCase,
    private val getMyCommentsUseCase: GetMyCommentsUseCase,
    private val updateNicknameUseCase: UpdateNicknameUseCase,
    private val getProfileUseCase: GetProfileUseCase,
    private val logoutUseCase: LogOutUseCase,
    private val signupApi: SignupApi
) : ViewModel() {

    /** 공통 페이지 사이즈 */
    private val PAGE_SIZE = 10

    /** 로딩/에러 */
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    /** 스크롤 복원 (내 포스팅 탭) */
    var postScrollIndex: Int = 0

    /* -------------------- 내 포스팅 -------------------- */

    private val _myBoards = MutableStateFlow<List<PostData>>(emptyList())
    val myBoards: StateFlow<List<PostData>> = _myBoards

    private val _postPage = MutableStateFlow(0)
    val postPage: StateFlow<Int> = _postPage

    private val _postTotalPages = MutableStateFlow(1)
    val postTotalPages: StateFlow<Int> = _postTotalPages

    /** 서버 페이지네이션 기반 로드 */
    fun loadMyBoards(page: Int = _postPage.value, size: Int = PAGE_SIZE) {
        viewModelScope.launch {
            _loading.value = true
            _errorMessage.value = null
            try {
                val r = getMyPostsUseCase(page, size)
                // 혹시 중복이 내려올 수도 있어 안전하게 distinct
                _myBoards.value = r.boards.boards.distinctBy { it.id }
                _postPage.value = r.pageNumber
                _postTotalPages.value = r.totalPages
            } catch (e: ApiException) {
                _errorMessage.value = e.message
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "게시글을 불러오지 못했습니다."
            } finally {
                _loading.value = false
            }
        }
    }

    fun nextBoardsPage() {
        val next = _postPage.value + 1
        if (next < _postTotalPages.value) loadMyBoards(next)
    }

    fun prevBoardsPage() {
        val prev = _postPage.value - 1
        if (prev >= 0) loadMyBoards(prev)
    }

    /* -------------------- 내 댓글 -------------------- */

    private val _myComments = MutableStateFlow<List<MyCommentDto>>(emptyList())
    val myComments: StateFlow<List<MyCommentDto>> = _myComments

    private val _commentPage = MutableStateFlow(0)
    val commentPage: StateFlow<Int> = _commentPage

    private val _commentTotalPages = MutableStateFlow(1)
    val commentTotalPages: StateFlow<Int> = _commentTotalPages

    /** 서버 페이지네이션 기반 로드 */
    fun loadMyComments(page: Int = _commentPage.value, size: Int = PAGE_SIZE) {
        viewModelScope.launch {
            _loading.value = true
            _errorMessage.value = null
            try {
                val r = getMyCommentsUseCase(page = page, size = size)
                _myComments.value = r.comments
                _commentPage.value = r.pageNumber
                _commentTotalPages.value = r.totalPages
            } catch (e: ApiException) {
                _errorMessage.value = e.message
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "댓글을 불러오지 못했습니다."
            } finally {
                _loading.value = false
            }
        }
    }

    fun nextCommentsPage() {
        val next = _commentPage.value + 1
        if (next < _commentTotalPages.value) loadMyComments(next)
    }

    fun prevCommentsPage() {
        val prev = _commentPage.value - 1
        if (prev >= 0) loadMyComments(prev)
    }

    // 내 프로필 상태
    private val _profile = MutableStateFlow<MyProfileResponse?>(null)
    val profile: StateFlow<MyProfileResponse?> = _profile

    fun loadMyProfile() {
        viewModelScope.launch {
            try {
                _profile.value = getProfileUseCase()
            } catch (e: ApiException) {
                // 선택: 공용 에러 상태에 같이 넣거나, 프로필 전용 에러를 추가
            }
        }
    }

    // NEW
    fun updateNickname(email: String, newNickname: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            runCatching { updateNicknameUseCase(PatchNicknameRequest(email, newNickname)) }
                .onSuccess {
                    // 낙관적 갱신 or 재로딩 택1
                    loadMyProfile()      // CHANGED: 서버 값 동기화
                    onResult(true)
                }
                .onFailure { onResult(false) }
        }
    }

    fun logoutAll(onDone: (Boolean) -> Unit = {}) = viewModelScope.launch {
        var ok = true
        try {
            // 1) 서버 세션 무효화 (실패할 수도 있음)
            logoutUseCase()
        } catch (_: Throwable) {
            ok = false
        } finally {
            // 2) 성공/실패와 관계없이 카카오/로컬 세션 정리는 반드시 수행
            //    KakaoAuthManager.logout() 내부에서 clearSession()까지 호출하도록 유지
            runCatching { kakaoAuth.logout() }
            onDone(ok)
        }
    }

    /** 서버 회원탈퇴 + (가능하면) 카카오 연결끊기 */
    fun withdrawAll(onDone: (Boolean) -> Unit = {}) = viewModelScope.launch {
        var ok = true
        try {
            // 1) 서버 회원탈퇴
            signupApi.withdrawal()
        } catch (_: Throwable) {
            ok = false
        } finally {
            // 2) 연결끊기 + 로컬 세션 정리 (실패해도 앱 로컬은 비운다)
            runCatching { kakaoAuth.unlinkKakao() }
            onDone(ok)
        }
    }

    suspend fun checkBoardExists(postId: Long): Boolean {
        return runCatching<Unit> {
            getPostDetailUseCase(postId)
        }.isSuccess
    }
}