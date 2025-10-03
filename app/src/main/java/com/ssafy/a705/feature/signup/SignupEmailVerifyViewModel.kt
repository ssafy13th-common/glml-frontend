package com.ssafy.a705.feature.signup

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException
import javax.inject.Inject

@HiltViewModel
class SignupEmailVerifyViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: SignupRepository
) : ViewModel() {
    private val _verifyOk = MutableStateFlow<Boolean?>(null)
    val verifyOk: StateFlow<Boolean?> = _verifyOk

    private val _verifyMsg = MutableStateFlow<String?>(null)
    val verifyMsg: StateFlow<String?> = _verifyMsg

    init {
        // 딥링크의 token 파라미터 자동 주입
        val token = savedStateHandle.get<String>("token")
        if (!token.isNullOrBlank()) {
            verify(token)
        } else {
            _verifyOk.value = false
            _verifyMsg.value = "잘못된 인증 링크입니다."
        }
    }

    private fun verify(token: String) {
        viewModelScope.launch {
            try {
                val resp = repo.verifyEmail(token)       // 2xx면 성공
                _verifyOk.value = true
                _verifyMsg.value = resp.message ?: "이메일 인증이 완료되었습니다."
            } catch (e: HttpException) {                  // 예: 400 만료/사용됨
                val msg = e.response()?.errorBody()?.string()?.let {
                    runCatching { JSONObject(it).optString("message", null) }.getOrNull()
                }
                _verifyOk.value = false
                _verifyMsg.value = msg ?: "이메일 인증에 실패했습니다."
            } catch (_: Exception) {
                _verifyOk.value = false
                _verifyMsg.value = "네트워크 오류가 발생했습니다."
            }
        }
    }
}