package com.ssafy.a705.feature.controller.viewmodel

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
