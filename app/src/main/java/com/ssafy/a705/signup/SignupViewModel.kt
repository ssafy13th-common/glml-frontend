package com.ssafy.a705.signup

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.a705.model.resp.BasicResponse
import com.ssafy.a705.network.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException
import javax.inject.Inject

@HiltViewModel
class SignupViewModel @Inject constructor(
    private val repo: SignupRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private var lastSignupRequest: SignupRequest? = null
    private var lastRequestedEmail: String? = null

    private val _signupState = MutableStateFlow<BasicResponse?>(null)
    val signupState: StateFlow<BasicResponse?> = _signupState

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _loginMsg = MutableStateFlow<String?>(null)
    val loginMsg: StateFlow<String?> = _loginMsg

    private val _loginOk = MutableStateFlow<Boolean?>(null)
    val loginOk: StateFlow<Boolean?> = _loginOk

    private val _loginNeedsVerifyEmail = MutableStateFlow<String?>(null)
    val loginNeedsVerifyEmail: StateFlow<String?> = _loginNeedsVerifyEmail

    private val _emailCheckOk = MutableStateFlow<Boolean?>(null)
    val emailCheckOk: StateFlow<Boolean?> = _emailCheckOk
    private val _emailCheckMsg = MutableStateFlow<String?>(null)
    val emailCheckMsg: StateFlow<String?> = _emailCheckMsg

    private val _resendOk = MutableStateFlow<Boolean?>(null)
    val resendOk: StateFlow<Boolean?> = _resendOk
    private val _resendMsg = MutableStateFlow<String?>(null)
    val resendMsg: StateFlow<String?> = _resendMsg

    fun signup(req: SignupRequest) {
        lastSignupRequest = req
        viewModelScope.launch {
            runCatching { repo.signup(req) }
                .onSuccess { _signupState.value = it }
                .onFailure { _error.value = it.message ?: "회원가입 실패" }
        }
    }

    fun resendVerify(email: String) {
        viewModelScope.launch {
            try {
                val resp = repo.resendVerify(email)
                _resendOk.value = true
                _resendMsg.value = resp.message // 성공은 보통 null일 수 있음
            } catch (e: HttpException) {
                val msg = e.response()?.errorBody()?.string()?.let {
                    runCatching { JSONObject(it).optString("message", null) }.getOrNull()
                }
                _resendOk.value = false
                _resendMsg.value = msg ?: "재전송에 실패했습니다."
            } catch (_: Exception) {
                _resendOk.value = false
                _resendMsg.value = "네트워크 오류가 발생했습니다."
            }
        }
    }

    fun checkEmail(email: String) {
        lastRequestedEmail = email
        viewModelScope.launch {
            try {
                val resp = repo.checkEmail(email) // 2xx 응답이면 여기서 끝
                if (lastRequestedEmail == email) {
                    _emailCheckOk.value = true
                    _emailCheckMsg.value = resp.message
                }
            } catch (e: HttpException) {
                if (lastRequestedEmail == email) {
                    _emailCheckOk.value = false
                    _emailCheckMsg.value = e.message ?: "이미 존재하는 이메일입니다."
                }
                Log.e("SignupVM", "error code: ${e.code()}")
            } catch (e: Exception) {
                if (lastRequestedEmail == email) {
                    _emailCheckOk.value = false
                    _emailCheckMsg.value = "잠시 후 다시 시도해주세요."
                }
                Log.e("SignupVM", "error code: ${e.message}")
            }
        }
    }

    fun resetEmailCheck() {
        _emailCheckOk.value = null
        _emailCheckMsg.value = null
    }

    fun clear() {
        _signupState.value = null
        _error.value = null
    }

    fun getSignupEmail(): String {
        return lastSignupRequest?.email ?: "등록된 이메일이 없습니다."
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            try {
                val resp = repo.login(email, password)

                val at = resp.headers()["Authorization"]
                val rt = resp.headers()["Authorization-Refresh"]

                if (resp.isSuccessful && !at.isNullOrBlank()) {
                    val atClean = at.removePrefix("Bearer ").trim()
                    val rtClean = rt?.removePrefix("Bearer ")?.trim()

                    tokenManager.saveServerAccessToken(atClean)
                    rtClean?.let { tokenManager.saveServerRefreshToken(it) }

                    _loginOk.value = true
                    _loginMsg.value = resp.body()?.message ?: "로그인 성공"
                } else {
                    val code = resp.code()
                    val msg = resp.errorBody()?.string()?.let { s ->
                        runCatching { JSONObject(s).optString("message", null) }.getOrNull()
                    }

                    if (msg.equals("인증되지 않은 이메일입니다.")) {
                        // 미인증 사용자는 이메일 재전송 화면으로
                        _loginNeedsVerifyEmail.value = email
                    } else if(msg.equals("로그인 실패")) {
                        _loginOk.value = false
                        _loginMsg.value = "아이디 또는 비밀번호가 틀렸습니다."
                    }
                }
            } catch (e: HttpException) {
                val code = e.code()
                val msg = e.response()?.errorBody()?.string()?.let { s ->
                    runCatching { JSONObject(s).optString("message", null) }.getOrNull()
                }

                if (code == 401) {
                    _loginNeedsVerifyEmail.value = email
                    _loginOk.value = false
                    _loginMsg.value = msg ?: "인증되지 않은 이메일입니다."
                } else {
                    _loginOk.value = false
                    _loginMsg.value = msg ?: "로그인 실패"
                }
            } catch (_: Exception) {
                _loginOk.value = false
                _loginMsg.value = "네트워크 오류가 발생했습니다."
            }
        }
    }

    fun clearLoginState() {
        _loginOk.value = null
        _loginMsg.value = null
        _loginNeedsVerifyEmail.value = null
    }
}