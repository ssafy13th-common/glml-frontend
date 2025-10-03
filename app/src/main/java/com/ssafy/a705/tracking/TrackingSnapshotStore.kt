package com.ssafy.a705.tracking

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object TrackingSnapshotStore {
    // 위치 정보 전역 저장소
    private val _snapshots = MutableStateFlow<List<TrackingSnapshot>>(emptyList())
    val snapshots: StateFlow<List<TrackingSnapshot>> = _snapshots.asStateFlow()

    fun append(snapshot: TrackingSnapshot) {
        _snapshots.update { it + snapshot }
    }

    fun clear() {
        _snapshots.value = emptyList()
    }
}