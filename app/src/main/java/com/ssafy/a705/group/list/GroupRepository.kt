package com.ssafy.a705.group.list

import com.ssafy.a705.group.common.model.Group
import com.ssafy.a705.network.GroupDetailResponse
import com.ssafy.a705.network.GatheringDetailResponse
import com.ssafy.a705.network.GatheringUpdateRequest

interface GroupRepository {
    suspend fun getGroups(): List<Group>
    suspend fun deleteGroup(groupId: Long): Unit
    suspend fun getGroupInfo(groupId: Long): GroupDetailResponse
    suspend fun getGatheringInfo(groupId: Long): GatheringDetailResponse
    suspend fun updateGathering(groupId: Long, request: GatheringUpdateRequest): GatheringDetailResponse
}
