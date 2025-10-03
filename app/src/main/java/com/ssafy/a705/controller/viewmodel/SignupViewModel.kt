package com.ssafy.a705.controller.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.a705.controller.service.SignupService
import com.ssafy.a705.model.req.SignupRequest
import com.ssafy.a705.model.resp.SignupResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignupViewModel @Inject constructor(
    private val signupService: SignupService
) : ViewModel() {

    private val _signupState = MutableStateFlow<SignupResponse?>(null)
    val signupState: StateFlow<SignupResponse?> = _signupState

    fun signup(request: SignupRequest) {
        viewModelScope.launch {
            try {
                val response = signupService.signup(request)
                _signupState.value = response
            } catch (e: Exception) {
                // 로그 출력이나 에러 상태 관리 가능
                e.printStackTrace()
            }
        }
    }
}
