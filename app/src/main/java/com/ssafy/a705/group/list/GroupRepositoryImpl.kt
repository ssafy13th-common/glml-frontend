package com.ssafy.a705.group.list

import com.ssafy.a705.group.common.model.Group
import javax.inject.Inject
import javax.inject.Singleton
import com.ssafy.a705.network.GroupApiService
import com.ssafy.a705.network.GroupDetailResponse
import com.ssafy.a705.network.GatheringDetailResponse
import com.ssafy.a705.network.GatheringUpdateRequest

@Singleton
class GroupRepositoryImpl @Inject constructor(
    private val groupApiService: GroupApiService
) : GroupRepository {

    override suspend fun getGroups(): List<Group> {
        try {
            val response = groupApiService.getGroups()
            
            if (response.message != null) {
                throw Exception(response.message)
            }
            
            val groups = response.data?.groups

            val result = groups?.map {
                println("✅ mapped id=${it.groupId}, name=${it.name}")
                Group(
                    id = it.groupId,
                    name = it.name,
                    status = it.status,
                    summary = it.summary ?: "",
                    members = it.memberProfiles ?: emptyList()
                )
            } ?: emptyList()


            println("🔍 Repository result: ${result.map { "id=${it.id}, name=${it.name}" }}")
            
            return result
            
        } catch (e: Exception) {
            throw e
        }
    }
    
    override suspend fun deleteGroup(groupId: Long) {
        val response = groupApiService.deleteGroup(groupId)
        if (response.message != null) {
            throw Exception(response.message)
        }
    }
    
    override suspend fun getGroupInfo(groupId: Long): GroupDetailResponse {
        val resp = groupApiService.getGroupInfo(groupId)
        if (resp.message != null) throw Exception(resp.message)
        return resp.data ?: throw Exception("그룹 상세 응답이 비어있습니다.")
    }
    
    override suspend fun getGatheringInfo(groupId: Long): GatheringDetailResponse {
        val resp = groupApiService.getGatheringInfo(groupId)
        if (resp.message != null) throw Exception(resp.message)
        return resp.data ?: throw Exception("모임 정보 응답이 비어있습니다.")
    }
    
    override suspend fun updateGathering(groupId: Long, request: GatheringUpdateRequest): GatheringDetailResponse {
        val resp = groupApiService.updateGathering(groupId, request)
        if (resp.message != null) throw Exception(resp.message)
        return resp.data ?: throw Exception("모임 정보 수정 응답이 비어있습니다.")
    }
}
