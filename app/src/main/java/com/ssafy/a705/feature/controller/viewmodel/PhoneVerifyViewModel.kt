package com.ssafy.a705.feature.controller.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.a705.feature.controller.service.VerificationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PhoneVerifyViewModel @Inject constructor(
    private val service: VerificationService
) : ViewModel() {

    data class UiState(
        val phone: String = "",
        val code: String = "",
        val sending: Boolean = false,
        val verifying: Boolean = false,
        val sent: Boolean = false,
        val secondsLeft: Int = 0, // 카운트다운(예: 180초)
        val error: String? = null,
        val message: String? = null
    )

    private val _ui = MutableStateFlow(UiState())
    val ui = _ui.asStateFlow()

    private var timerJob: Job? = null

    fun setPhone(value: String) { _ui.value = _ui.value.copy(phone = value.filter { it.isDigit() }) }
    fun setCode(value: String) { _ui.value = _ui.value.copy(code = value.filter { it.isDigit() }) }

    fun sendSms() {
        val phone = _ui.value.phone
        if (phone.isBlank()) {
            _ui.value = _ui.value.copy(error = "전화번호를 입력하세요.")
            return
        }
        viewModelScope.launch {
            _ui.value = _ui.value.copy(sending = true, error = null, message = null)
            runCatching { service.sendSms(phone) }
                .onSuccess {
                    _ui.value = _ui.value.copy(sending = false, sent = true, message = "인증번호를 보냈습니다.", secondsLeft = 180)
                    startTimer()
                }
                .onFailure { e ->
                    _ui.value = _ui.value.copy(sending = false, error = e.message ?: "전송 실패")
                }
        }
    }

    fun resend() {
        timerJob?.cancel()
        sendSms()
    }

    fun verify(onSuccess: () -> Unit) {
        val s = _ui.value
        if (s.code.length < 6) {
            _ui.value = s.copy(error = "6자리 인증코드를 입력하세요.")
            return
        }
        viewModelScope.launch {
            _ui.value = _ui.value.copy(verifying = true, error = null, message = null)
            runCatching { service.verifySms(s.phone, s.code) }
                .onSuccess {
                    _ui.value = _ui.value.copy(verifying = false, message = "번호 인증 완료")
                    onSuccess()
                }
                .onFailure { e ->
                    _ui.value = _ui.value.copy(verifying = false, error = e.message ?: "검증 실패")
                }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_ui.value.secondsLeft > 0) {
                delay(1000)
                _ui.value = _ui.value.copy(secondsLeft = _ui.value.secondsLeft - 1)
            }
        }
    }

    override fun onCleared() {
        timerJob?.cancel()
        super.onCleared()
    }
}