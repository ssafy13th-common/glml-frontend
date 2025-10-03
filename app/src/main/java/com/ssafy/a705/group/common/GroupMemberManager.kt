package com.ssafy.a705.group.common

object GroupMemberManager {
    private var _groupMemberId: Int? = null

    val groupMemberId: Int
        get() = _groupMemberId
            ?: throw IllegalStateException("GroupMemberId가 설정되지 않았습니다.")

    fun setGroupMemberId(id: Int) {
        _groupMemberId = id
    }

    fun clear() {
        _groupMemberId = null
    }

    fun isInitialized(): Boolean {
        return _groupMemberId != null
    }
}
