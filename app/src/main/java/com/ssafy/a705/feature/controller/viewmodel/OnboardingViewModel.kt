package com.ssafy.a705.feature.controller.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import com.kakao.sdk.auth.model.OAuthToken
import com.ssafy.a705.common.network.sign.KakaoAuthManager
import com.ssafy.a705.common.network.sign.SignApi
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val auth: KakaoAuthManager
) : ViewModel() {

    fun requestKakaoToken(
        activity: Activity,
        onSuccess: (token: OAuthToken, nickname: String?) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        auth.requestKakaoToken(
            activity = activity,
            onSuccess = { token, user ->
                onSuccess(token, user?.kakaoAccount?.profile?.nickname)
            },
            onFailure = onFailure
        )
    }

    suspend fun exchangeWithServer(
        gender: SignApi.Gender,
        name: String
    ) {
        auth.exchangeWithServer(gender, name)
    }
}

/**
 * (옵션) UI가 이미 gender/name을 갖고 있을 때 원샷으로 처리하고 싶다면 이걸 사용
 */
//fun loginWithKakao(
//    activity: Activity,
//    gender: SignApi.Gender,
//    name: String,
//    onFailure: (Throwable) -> Unit = {}
//) {
//    auth.login(
//        activity = activity,
//        gender = gender,
//        name = name,
//        onSuccess = {
//            _isLoggedIn.value = true
//            _loginCompleted.tryEmit(Unit)
//        },
//        onFailure = onFailure
//    )
//}
//private val _isLoggedIn = MutableStateFlow(false)
//val isLoggedIn: StateFlow<Boolean> = _isLoggedIn
//
//private val _loginCompleted = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
//val loginCompleted: SharedFlow<Unit> = _loginCompleted
