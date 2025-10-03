package com.ssafy.a705.feature.controller.viewmodel

import com.ssafy.a705.common.network.sign.KakaoAuthManager
import com.ssafy.a705.common.network.sign.KakaoSession

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class KakaoViewModel @Inject constructor(
    private val kakaoAuthManager: KakaoAuthManager // 세션 보관/조회 담당
) : ViewModel() {

    private val _session = MutableStateFlow<KakaoSession?>(null)
    val session: StateFlow<KakaoSession?> = _session

    fun loadSession() {
        _session.value = kakaoAuthManager.getCurrentSession()
    }

    fun refreshFromSdk() = viewModelScope.launch {
        // 필요하면 SDK에서 갱신 → kakaoAuthManager.updateFromSdk() 같은 메서드로
        val updated = kakaoAuthManager.refreshIfNeeded()
        _session.value = updated
    }

    fun logout() = viewModelScope.launch {
        kakaoAuthManager.logout()
        _session.value = null
    }
}
