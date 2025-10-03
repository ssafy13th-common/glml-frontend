package com.ssafy.a705.feature.group.latecheck

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.a705.common.network.GroupApiService
import com.ssafy.a705.common.network.LiveLocationStatusApi
import com.ssafy.a705.common.network.TokenManager
import com.ssafy.a705.common.network.sign.SessionManager
import com.ssafy.a705.group.member.GroupMemberRepository
import com.ssafy.a705.group.common.util.GeoUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import javax.inject.Inject

@HiltViewModel
class LateCheckViewModel @Inject constructor(
    private val groupApiService: GroupApiService,
    private val liveLocationStatusApi: LiveLocationStatusApi,
    private val tokenManager: TokenManager,
    private val sessionManager: SessionManager,
    private val groupMemberRepository: GroupMemberRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LateCheckUiState())
    val uiState: StateFlow<LateCheckUiState> = _uiState.asStateFlow()

    // 서비스 실행 상태
    private var isServiceRunning = false

    // 서비스 인스턴스 (싱글톤 패턴)
    private var locationService: LiveLocationService? = null

    // WebSocket 연결 상태
    private val _webSocketConnectionState = MutableStateFlow<WebSocketConnectionState>(WebSocketConnectionState.Idle)
    val webSocketConnectionState: StateFlow<WebSocketConnectionState> = _webSocketConnectionState.asStateFlow()

    // 위치 데이터
    private val _locations = MutableStateFlow<Map<String, LiveLocationReceived>>(emptyMap())
    val locations: StateFlow<Map<String, LiveLocationReceived>> = _locations.asStateFlow()

    // 지각비 데이터
    private val _lateFees = MutableStateFlow<Map<String, Int>>(emptyMap())
    val lateFees: StateFlow<Map<String, Int>> = _lateFees.asStateFlow()

    // 현재 사용자의 이메일 정보
    private val currentUserEmail: String?
        get() = sessionManager.load()?.email

    init {
        // 위치 데이터와 지각비 데이터를 멤버 정보와 결합
        viewModelScope.launch {
            combine(
                _uiState.map { it.groupMembers },
                locations,
                lateFees
            ) { members, locationMap, lateFeeMap ->
                val myLocation = currentUserEmail?.let { locationMap[it] } ?:
                locationMap["me"] ?: locationMap["current_user"]
                Log.d("LateCheckViewModel", "=== 🔍 위치-멤버 매칭 시작 ===")
                Log.d("LateCheckViewModel", "👤 현재 사용자 이메일: $currentUserEmail")
                Log.d("LateCheckViewModel", "📍 내 위치 데이터: $myLocation")
                Log.d("LateCheckViewModel", "📊 위치 맵 크기: ${locationMap.size}")
                Log.d("LateCheckViewModel", "🔑 위치 맵 키들: ${locationMap.keys}")
                Log.d("LateCheckViewModel", "👥 멤버 수: ${members.size}")

                // 위치 맵의 모든 데이터 로그
                locationMap.forEach { (key, value) ->
                    Log.d("LateCheckViewModel", "📍 위치 맵 데이터: $key -> ${value.memberEmail} (${value.latitude}, ${value.longitude}) 지각비: ${value.lateFee}원")
                }

                members.map { member ->
                    Log.d("LateCheckViewModel", "=== 🔍 멤버 매칭 시도 ===")
                    Log.d("LateCheckViewModel", "👤 멤버: ${member.nickname}")
                    Log.d("LateCheckViewModel", "📧 멤버 이메일: ${member.email}")
                    Log.d("LateCheckViewModel", "🆔 멤버 ID: ${member.groupMemberId}")

                    // 이메일 기반 매칭을 우선적으로 시도
                    val location = if (member.email != null && member.email == currentUserEmail) {
                        // 현재 사용자인 경우 내 위치 사용
                        Log.d("LateCheckViewModel", "✅ 현재 사용자 매칭: ${member.nickname}")
                        myLocation ?: locationMap[member.email]
                    } else if (member.email != null) {
                        // 다른 멤버인 경우 이메일로 매칭
                        val matchedLocation = locationMap[member.email]
                        Log.d("LateCheckViewModel", "🔍 이메일 매칭: ${member.email} -> ${matchedLocation != null}")
                        if (matchedLocation != null) {
                            Log.d("LateCheckViewModel", "✅ 이메일 매칭 성공: ${member.email} -> ${matchedLocation.memberEmail}")
                        }
                        matchedLocation
                    } else {
                        // 이메일이 없는 경우 닉네임으로만 매칭 시도 (현재 사용자 이메일 사용 금지)
                        val matchedLocation = locationMap[member.nickname] ?: locationMap.values.firstOrNull { loc ->
                            val local = loc.memberEmail.substringBefore("@")
                            member.nickname.equals(local, ignoreCase = true) ||
                                    member.nickname.contains(local, ignoreCase = true)
                        }
                        Log.d("LateCheckViewModel", "🔍 닉네임 매칭 (이메일 null): ${member.nickname} -> ${matchedLocation != null}")
                        if (matchedLocation != null) {
                            Log.d("LateCheckViewModel", "✅ 닉네임 매칭 성공: ${member.nickname} -> ${matchedLocation.memberEmail}")
                        }
                        matchedLocation
                    }

                    // lateFee는 위치에 있으면 그걸, 없으면 원래 값 사용
                    val lateFee = location?.lateFee ?: member.lateFee

                    if (location != null) {
                        Log.d("LateCheckViewModel", "✅ 위치 매칭 성공: ${member.nickname} -> (${location.latitude}, ${location.longitude}) 지각비: ${location.lateFee}원")
                    } else {
                        Log.d("LateCheckViewModel", "⚠️ 위치 매칭 실패: ${member.nickname}")
                    }

                    val updatedMember = member.copy(
                        latitude  = location?.latitude  ?: member.latitude,
                        longitude = location?.longitude ?: member.longitude,
                        lateFee   = lateFee,
                        color     = getMemberColor(member.groupMemberId.toInt())
                    )

                    Log.d("LateCheckViewModel", "📊 멤버 업데이트 결과: ${updatedMember.nickname} -> (${updatedMember.latitude}, ${updatedMember.longitude}) 지각비: ${updatedMember.lateFee}원")
                    Log.d("LateCheckViewModel", "=== 🔍 멤버 매칭 완료 ===")

                    updatedMember
                }
            }.collect { updatedMembers ->
                Log.d("LateCheckViewModel", "=== 🔍 멤버 업데이트 완료 ===")
                Log.d("LateCheckViewModel", "👥 업데이트된 멤버 수: ${updatedMembers.size}명")

                // 각 멤버의 최종 상태 로그
                updatedMembers.forEach { member ->
                    Log.d("LateCheckViewModel", "📊 최종 멤버 상태: ${member.nickname}")
                    Log.d("LateCheckViewModel", "  - 이메일: ${member.email}")
                    Log.d("LateCheckViewModel", "  - 위치: (${member.latitude}, ${member.longitude})")
                    Log.d("LateCheckViewModel", "  - 지각비: ${member.lateFee}원")
                    Log.d("LateCheckViewModel", "  - 색상: ${member.color}")
                }

                _uiState.update { it.copy(groupMembers = updatedMembers) }
                Log.d("LateCheckViewModel", "=== 🔍 UI 상태 업데이트 완료 ===")
            }
        }

        // LiveLocationService의 StateFlow를 직접 구독
        viewModelScope.launch {
            while (true) {
                val service = LiveLocationService.getInstance()
                if (service != null) {
                    Log.d("LateCheckViewModel", "🔗 LiveLocationService 인스턴스 발견, StateFlow 구독 시작")

                    // 위치 데이터 구독
                    service.locations.collect { locations ->
                        Log.d("LateCheckViewModel", "📡 서비스에서 위치 데이터 수신: ${locations.size}개")
                        _locations.value = locations
                    }
                } else {
                    Log.d("LateCheckViewModel", "⏳ LiveLocationService 인스턴스 대기 중...")
                    delay(1000) // 1초 대기
                }
            }
        }

        // 지각비 데이터 구독
        viewModelScope.launch {
            while (true) {
                val service = LiveLocationService.getInstance()
                if (service != null) {
                    service.lateFees.collect { lateFees ->
                        Log.d("LateCheckViewModel", "📡 서비스에서 지각비 데이터 수신: ${lateFees.size}개")
                        _lateFees.value = lateFees
                    }
                } else {
                    delay(1000)
                }
            }
        }

        // WebSocket 연결 상태 구독
        viewModelScope.launch {
            while (true) {
                val service = LiveLocationService.getInstance()
                if (service != null) {
                    service.connectionState.collect { state ->
                        Log.d("LateCheckViewModel", "📡 서비스에서 WebSocket 상태 수신: $state")
                        _webSocketConnectionState.value = state
                    }
                } else {
                    delay(1000)
                }
            }
        }
    }

    private fun getMemberColor(seed: Int): androidx.compose.ui.graphics.Color {
        val colors = listOf(
            androidx.compose.ui.graphics.Color(0xFF2196F3), // Blue
            androidx.compose.ui.graphics.Color(0xFF4CAF50), // Green
            androidx.compose.ui.graphics.Color(0xFFFF9800), // Orange
            androidx.compose.ui.graphics.Color(0xFFE91E63), // Pink
            androidx.compose.ui.graphics.Color(0xFF9C27B0), // Purple
            androidx.compose.ui.graphics.Color(0xFF607D8B), // Blue Grey
            androidx.compose.ui.graphics.Color(0xFF795548), // Brown
            androidx.compose.ui.graphics.Color(0xFF00BCD4)  // Cyan
        )
        return colors[seed % colors.size]
    }

    fun loadGroupInfo(groupId: Long, context: Context) {
        Log.d("LateCheckViewModel", "🔵 === loadGroupInfo 시작: groupId=$groupId ===")
        Log.d("LateCheckViewModel", "🔵 현재 사용자 이메일: $currentUserEmail")
        viewModelScope.launch {
            Log.d("LateCheckViewModel", "🔵 loadGroupInfo 코루틴 시작")
            _uiState.update { it.copy(isLoading = true) }

            try {
                Log.d("LateCheckViewModel", "1. 그룹 상세 정보 로드 시작")
                // 그룹 상세 정보 로드 (좌표 정보)
                var groupLatitude: Double? = null
                var groupLongitude: Double? = null

                try {
                    val groupInfo = groupApiService.getGroupInfo(groupId)
                    Log.d("LateCheckViewModel", "그룹 상세 정보 API 응답: isSuccess=${groupInfo.isSuccess}, isSuccessful=${groupInfo.isSuccessful}")

                    if (groupInfo.isSuccessful) {
                        groupInfo.data?.let { g ->
                            groupLatitude = g.locationLatitude
                            groupLongitude = g.locationLongitude
                            Log.d("LateCheckViewModel", "그룹 정보에서 모임 장소 좌표 가져옴: (${g.locationLatitude}, ${g.locationLongitude})")
                        }
                    } else {
                        Log.e("LateCheckViewModel", "그룹 상세 정보 API 실패: ${groupInfo.message}")
                    }
                } catch (e: Exception) {
                    Log.e("LateCheckViewModel", "그룹 상세 정보 로드 중 예외 발생", e)
                }

                Log.d("LateCheckViewModel", "2. 그룹 멤버 정보 로드 시작")
                // 그룹 멤버 정보 로드 (이메일 정보 포함)
                try {
                    Log.d("LateCheckViewModel", "그룹 멤버 정보 로드 시작: groupId=$groupId")
                    val memberItems = groupMemberRepository.getMembers(groupId)
                    Log.d("LateCheckViewModel", "그룹 멤버 데이터: ${memberItems.size}명")
                    memberItems.forEach { member ->
                        Log.d("LateCheckViewModel", "  - ${member.name} (${member.id}), 이메일: ${member.email}")
                    }

                    _uiState.update {
                        it.copy(
                            groupMembers = memberItems.map { member ->
                                MemberStatus(
                                    groupMemberId = member.id.toLong(),
                                    role = member.role,
                                    profileImageUrl = member.profileImageUrl ?: "",
                                    nickname = member.name,
                                    email = member.email,
                                    finalAmount = member.settlementAmount,
                                    lateFee = member.lateFee,
                                    color = getMemberColor(member.id.toIntOrNull() ?: 0)
                                )
                            }
                        )
                    }
                    Log.d("LateCheckViewModel", "그룹 멤버 정보 업데이트 완료: ${memberItems.size}명")
                } catch (e: Exception) {
                    Log.e("LateCheckViewModel", "그룹 멤버 정보 로드 중 예외 발생", e)
                }

                Log.d("LateCheckViewModel", "3. 모임 상세 정보 로드 시작")
                // 모임 상세 정보 로드 (모임 장소 이름)
                var locationName: String? = null
                try {
                    val gathering = groupApiService.getGatheringInfo(groupId)
                    Log.d("LateCheckViewModel", "모임 상세 정보 API 응답: isSuccess=${gathering.isSuccess}, isSuccessful=${gathering.isSuccessful}")

                    if (gathering.isSuccessful) {
                        gathering.data?.let { gatheringData ->
                            locationName = gatheringData.gatheringLocation
                            Log.d("LateCheckViewModel", "모임 상세 정보에서 장소 이름 가져옴: $locationName")
                        }
                    } else {
                        Log.e("LateCheckViewModel", "모임 상세 정보 API 실패: ${gathering.message}")
                    }
                } catch (e: Exception) {
                    Log.e("LateCheckViewModel", "모임 상세 정보 로드 중 예외 발생", e)
                }

                // 모임 장소 정보 설정 (좌표와 이름을 모두 가져온 후)
                if (groupLatitude != null && groupLongitude != null) {
                    val finalLocationName = locationName ?: "모임 장소"
                    _uiState.update {
                        it.copy(
                            meetingPlace = MeetingPlace(
                                name = finalLocationName,
                                latitude = groupLatitude!!,
                                longitude = groupLongitude!!
                            )
                        )
                    }
                    Log.d("LateCheckViewModel", "모임 장소 최종 설정: $finalLocationName (${groupLatitude}, ${groupLongitude})")
                } else {
                    // 서버에서 좌표를 받아오지 못한 경우, 모임장소 이름으로 지오코딩 시도
                    locationName?.let { name ->
                        if (name.isNotBlank()) {
                            Log.d("LateCheckViewModel", "서버 좌표 없음, 지오코딩 시도: $name")
                            try {
                                val coordinates = GeoUtil.geocodeKoreaOrNull(context, name)
                                if (coordinates != null) {
                                    val (lat, lng) = coordinates
                                    _uiState.update {
                                        it.copy(
                                            meetingPlace = MeetingPlace(
                                                name = name,
                                                latitude = lat,
                                                longitude = lng
                                            )
                                        )
                                    }
                                    Log.d("LateCheckViewModel", "지오코딩 성공: $name → (${lat}, ${lng})")
                                } else {
                                    Log.e("LateCheckViewModel", "지오코딩 실패: $name")
                                }
                            } catch (e: Exception) {
                                Log.e("LateCheckViewModel", "지오코딩 중 예외 발생", e)
                            }
                        } else {
                            Log.d("LateCheckViewModel", "모임 장소 이름이 비어있음")
                        }
                    } ?: run {
                        Log.d("LateCheckViewModel", "모임 장소 이름이 없어서 설정하지 않음")
                    }
                }

                Log.d("LateCheckViewModel", "�� === loadGroupInfo 완료 ===")
            } catch (e: Exception) {
                Log.e("LateCheckViewModel", "그룹 정보 로드 실패", e)
                _uiState.update { it.copy(error = "그룹 정보를 불러오는데 실패했습니다.") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onShareStart(groupId: Long, context: Context) {
        viewModelScope.launch {
            try {
                Log.d("LateCheckViewModel", "=== 🚀 위치 공유 시작 ===")

                // 0. 위치 권한 확인 및 요청
                if (!checkAndRequestLocationPermission(context)) {
                    Log.e("LateCheckViewModel", "❌ 위치 권한이 거부되었습니다.")
                    _uiState.update { it.copy(isLocationSharingActive = false) }
                    return@launch
                }
                Log.d("LateCheckViewModel", "✅ 위치 권한 확인됨")

                val jwt = tokenManager.getServerAccessToken()
                if (jwt.isNullOrBlank()) {
                    Log.e("LateCheckViewModel", "❌ TokenManager에서 서버 JWT를 찾지 못해 WS 인증 불가")
                    _uiState.update { it.copy(isLocationSharingActive = false) }
                    return@launch
                }
                Log.d("LateCheckViewModel", "✅ 서버 JWT 획득 성공: ${jwt.take(20)}...")

                // 1. 권한 ON API 호출 (순서 보장)
                Log.d("LateCheckViewModel", "🔧 권한 ON API 호출 시작")
                val enableResponse = liveLocationStatusApi.enable(groupId)
                if (!enableResponse.isSuccessful) {
                    Log.e("LateCheckViewModel", "❌ 권한 ON 실패: ${enableResponse.code()}")
                    _uiState.update { it.copy(isLocationSharingActive = false) }
                    return@launch
                }
                Log.d("LateCheckViewModel", "✅ 권한 ON 성공: ${enableResponse.code()}")

                // 2. Foreground Service 시작
                Log.d("LateCheckViewModel", "🔧 Foreground Service 시작")
                LiveLocationService.start(context, groupId, jwt)
                isServiceRunning = true

                _uiState.update { it.copy(isLocationSharingActive = true) }
                Log.d("LateCheckViewModel", "✅ Foreground Service 시작 완료")
                Log.d("LateCheckViewModel", "=== 🚀 위치 공유 시작 완료 ===")

            } catch (e: Exception) {
                Log.e("LateCheckViewModel", "❌ 위치 공유 시작 중 오류", e)
                _uiState.update { it.copy(isLocationSharingActive = false) }
            }
        }
    }

    private fun checkAndRequestLocationPermission(context: Context): Boolean {
        return when {
            context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                    context.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED -> {
                true
            }
            else -> {
                // 권한이 없으면 Activity에서 권한 요청을 처리해야 합니다.
                // 여기서는 false를 반환하고, Activity에서 권한 요청 후 다시 호출하도록 합니다.
                false
            }
        }
    }

    fun onShareStop(groupId: Long, context: Context) {
        viewModelScope.launch {
            try {
                Log.d("LateCheckViewModel", "=== 🛑 위치 공유 종료 ===")

                // 1. 권한 OFF API 호출
                Log.d("LateCheckViewModel", "🔧 권한 OFF API 호출 시작")
                val disableResponse = liveLocationStatusApi.disable(groupId)
                if (disableResponse.isSuccessful) {
                    Log.d("LateCheckViewModel", "✅ 권한 OFF 성공: ${disableResponse.code()}")
                } else {
                    Log.e("LateCheckViewModel", "❌ 권한 OFF 실패: ${disableResponse.code()}")
                }

                // 2. Foreground Service 종료
                Log.d("LateCheckViewModel", "🔧 Foreground Service 종료")
                LiveLocationService.stop(context)

                isServiceRunning = false
                _uiState.update { it.copy(isLocationSharingActive = false) }
                Log.d("LateCheckViewModel", "✅ 위치 공유 종료 완료")
                Log.d("LateCheckViewModel", "=== 🛑 위치 공유 종료 완료 ===")

            } catch (e: Exception) {
                Log.e("LateCheckViewModel", "❌ 위치 공유 종료 중 오류", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("LateCheckViewModel", "=== 🔧 ViewModel 정리 ===")

        // 서비스가 실행 중이면 종료
        if (isServiceRunning) {
            // Context가 필요하므로 여기서는 로그만 출력
            Log.d("LateCheckViewModel", "⚠️ 서비스가 실행 중이지만 Context가 없어 종료할 수 없음")
        }

        // 브로드캐스트 리시버 정리 (Context가 없으므로 서비스에서 처리)
        Log.d("LateCheckViewModel", "📡 ViewModel 정리 - 브로드캐스트 리시버는 Context가 필요하여 서비스에서 처리")

        Log.d("LateCheckViewModel", "=== 🔧 ViewModel 정리 완료 ===")
    }
}
