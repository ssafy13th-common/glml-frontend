package com.ssafy.a705.global.network

import com.google.gson.annotations.SerializedName
import com.ssafy.a705.group.member.GroupMembersData
import com.ssafy.a705.group.member.InviteMemberRequest
import com.ssafy.a705.group.member.MemberSearchData
import com.ssafy.a705.group.memo.MemoCreateRequest
import com.ssafy.a705.group.memo.MemoListEnvelope
import com.ssafy.a705.group.memo.MemoUpdateRequestDto
import retrofit2.http.*

interface GroupApiService {

    // 그룹 생성
    @POST("api/v1/groups")
    suspend fun createGroup(
        @Body request: GroupCreateRequest
    ): ApiResponse<GroupCreateResponse>

    // 그룹 목록 조회
    @GET("api/v1/groups")
    suspend fun getGroups(): ApiResponse<GroupListResponse>

    // 그룹 정보 조회
    @GET("api/v1/groups/{groupId}")
    suspend fun getGroupInfo(
        @Path("groupId") groupId: Long
    ): ApiResponse<GroupDetailResponse>

    // 모임 정보 조회
    @GET("api/v1/groups/{groupId}/gathering")
    suspend fun getGatheringInfo(
        @Path("groupId") groupId: Long
    ): ApiResponse<GatheringDetailResponse>

//     모임 정보 수정 (유일본)
    @PATCH("api/v1/groups/{groupId}/gathering")
    suspend fun updateGathering(
        @Path("groupId") groupId: Long,
        @Body body: GatheringUpdateRequest
    ): ApiResponse<GatheringDetailResponse>

    // 그룹 정보 수정
    @PUT("api/v1/groups/{groupId}")
    suspend fun updateGroupInfo(
        @Path("groupId") groupId: Long,
        @Body request: GroupUpdateRequest
    ): ApiResponse<GroupDetailResponse>

    // 그룹 삭제
    @DELETE("api/v1/groups/{groupId}")
    suspend fun deleteGroup(
        @Path("groupId") groupId: Long
    ): ApiResponse<Unit>

    // 메모 전체 조회
    @GET("api/v1/groups/{groupId}/memos")
    suspend fun getGroupMemos(
        @Path("groupId") groupId: Long
    ): ApiResponse<MemoListEnvelope>

    // 메모 생성
    @POST("api/v1/groups/{groupId}/memos")
    suspend fun createGroupMemo(
        @Path("groupId") groupId: Long,
        @Body request: MemoCreateRequest
    ): ApiResponse<Unit>

    // 메모 수정
    @PUT("api/v1/groups/{groupId}/memos/{memoId}")
    suspend fun updateGroupMemo(
        @Path("groupId") groupId: Long,
        @Path("memoId") memoId: Long,
        @Body request: MemoUpdateRequestDto
    ): ApiResponse<Unit>

    // 메모 삭제
    @DELETE("api/v1/groups/{groupId}/memos/{memoId}")
    suspend fun deleteGroupMemo(
        @Path("groupId") groupId: Long,
        @Path("memoId") memoId: Long
    ): ApiResponse<Unit>

    // 회원검색
    @GET("api/v1/members")
    suspend fun searchMembers(
        @Query("search") query: String
    ): ApiResponse<MemberSearchData>

    // 그룹 멤버 초대
    @POST("api/v1/groups/{group-id}/members")
    suspend fun inviteMemberByEmail(
        @Path("group-id") groupId: Long,
        @Body request: InviteMemberRequest
    ): ApiResponse<Unit>

    // 그룹 멤버 조회
    @GET("api/v1/groups/{group-id}/members")
    suspend fun getGroupMembers(
        @Path("group-id") groupId: Long
    ): ApiResponse<GroupMembersData>

    // 그룹 이미지 조회
    @GET("api/v1/groups/{groupId}/images")
    suspend fun getGroupImages(
        @Path("groupId") groupId: Long,
        @Query("cursorId") cursorId: Long? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): ApiResponse<GroupImagesResponse>

    // 그룹 이미지 업로드 (이미지 URL 포함)
    @POST("api/v1/groups/{groupId}/images")
    suspend fun addGroupImages(
        @Path("groupId") groupId: Long,
        @Body request: GroupImagesPostRequest
    ): ApiResponse<Unit>

    // 그룹 이미지 삭제
    @DELETE("api/v1/groups/{groupId}/images/{groupImageId}")
    suspend fun deleteGroupImage(
        @Path("groupId") groupId: Long,
        @Path("groupImageId") groupImageId: Long
    ): ApiResponse<Unit>

    // Presigned URL 요청
    @POST("api/v1/images/presigned-urls")
    suspend fun getPresignedUrls(
        @Body request: PresignedUrlRequest
    ): ApiResponse<PresignedUrlResponse>

    // 영수증 분석 (OCR)
    @POST("api/v1/groups/{groupId}/receipts")
    suspend fun analyzeReceipt(
        @Path("groupId") groupId: Long,
        @Body body: ReceiptRequest
    ): ApiResponse<ReceiptData>

    // 그룹 정산
    @PUT("api/v1/groups/{groupId}/receipts/settlements")
    suspend fun settleGroup(
        @Path("groupId") groupId: Long,
        @Body body: SettlementRequest
    ): ApiResponse<SettlementResponse>


}

/* ===== DTOs ===== */

data class GroupCreateRequest(
    val name: String,
    val summary: String? = null,
    val members: List<String>? = null
)

data class GroupCreateResponse(
    val groupId: Long,
    val name: String,
    val summary: String?,
    val chatRoomId: String?,
    val status: String,
    val startAt: String?,
    val endAt: String?,
    val feePerMinute: Int?
)

data class GroupUpdateRequest(
    val name: String,
    val summary: String? = null,
    val gatheringTime: String? = null,
    val gatheringLocation: String? = null,
    val locationLatitude: Double? = null,
    val locationLongitude: Double? = null,
    @SerializedName("startAt") val startAt: String? = null,
    @SerializedName("endAt") val endAt: String? = null,
    @SerializedName("feePerMinute") val feePerMinute: Int? = null,
)

data class GroupListResponse(
    @SerializedName("groupsCount") val groupsCount: Int,
    @SerializedName("groups") val groups: List<GroupSummary>
)

data class GroupSummary(
    @SerializedName("groupId") val groupId: Long,
    val name: String,
    val status: String,
    val summary: String?,
    val memberProfiles: List<String>
)

data class ApiResponse<T>(
    val message: String?,
    val data: T?,
    val isSuccess: Boolean = true
) {
    // 서버 응답에서 isSuccess 필드가 없으면 message가 null이고 data가 있으면 성공으로 간주
    val isSuccessful: Boolean
        get() = isSuccess || (message == null && data != null)
}

data class GroupDetailResponse(
    @SerializedName("groupId") val groupId: Long,
    @SerializedName("name") val name: String,
    @SerializedName("summary") val summary: String?,
    @SerializedName("chatRoomId") val chatRoomId: String?,
    @SerializedName("status") val status: String,         // "TO_DO" 등 서버 Enum 그대로
    @SerializedName("startAt") val startAt: String?,      // "2025-08-10"
    @SerializedName("endAt") val endAt: String?,          // "2025-08-10"
    @SerializedName("feePerMinute") val feePerMinute: Int?,
    @SerializedName("locationLatitude") val locationLatitude: Double?,
    @SerializedName("locationLongitude") val locationLongitude: Double?
)

data class GatheringDetailResponse(
    @SerializedName("gatheringTime") val gatheringTime: String?,       // ISO 8601 UTC
    @SerializedName("gatheringLocation") val gatheringLocation: String?
)


data class GatheringUpdateRequest(
    val gatheringTime: String,       // "2025-08-10T06:28:18.861Z" 또는 서버 허용 포맷
    val gatheringLocation: String
)


/* 그룹 이미지 */
data class GroupImagesResponse(
    val images: List<GroupImageDto>
)

data class GroupImageDto(
    val imageId: Long,
    val imageUrl: String,
    val memberEmail: String
)

data class GroupImagesPostRequest(
    val images: List<String>
)

/* Presigned URL */
data class PresignedUrlRequest(
    val fileNames: List<String>,
    val domain: String
)

data class PresignedUrlResponse(
    val presignedUrls: List<PresignedUrlDto>
)

data class PresignedUrlDto(
    val fileName: String,
    val presignedUrl: String
)

/* 영수증 분석 & 정산 */
data class ReceiptRequest(
    val receiptImageUrl: String
)

data class ReceiptItem(
    val name: String,
    val price: Int
)

data class ReceiptData(
    val receiptItems: List<ReceiptItem>
)

data class SettlementRequest(
    val pricePerPerson: Int,
    val memberEmails: List<String>
)

data class SettlementResponse(
    val settlements: List<SettlementDto>
)

data class SettlementDto(
    val memberEmail: String,
    val finalAmount: Int
)
