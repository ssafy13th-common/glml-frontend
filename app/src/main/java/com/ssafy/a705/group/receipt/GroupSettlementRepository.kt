package com.ssafy.a705.group.receipt

import com.ssafy.a705.network.GroupApiService
import com.ssafy.a705.network.ReceiptData
import com.ssafy.a705.network.ReceiptRequest
import com.ssafy.a705.network.SettlementRequest
import com.ssafy.a705.network.SettlementResponse
import com.ssafy.a705.network.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GroupSettlementRepository @Inject constructor(
    private val groupApiService: GroupApiService,
    private val tokenManager: TokenManager
) {

    /**
     * 영수증 분석 (OCR)
     * @param groupId 그룹 ID
     * @param receiptImageUrl 영수증 이미지 URL 또는 오브젝트 키
     * @return 영수증 품목 리스트
     */
    suspend fun analyzeReceipt(groupId: Long, receiptImageUrl: String): Result<ReceiptData> {
        return withContext(Dispatchers.IO) {
            try {
                println("🔍 OCR 분석 시작: groupId=$groupId, imageUrl=$receiptImageUrl")
                
                val response = groupApiService.analyzeReceipt(
                    groupId = groupId,
                    body = ReceiptRequest(receiptImageUrl = receiptImageUrl)
                )
                
                println("🔍 OCR 분석 응답: $response")
                println("🔍 OCR 분석 결과 품목 수: ${response.data?.receiptItems?.size ?: 0}")
                
                if (response.isSuccessful && response.data != null) {
                    Result.success(response.data)
                } else {
                    Result.failure(Exception(response.message ?: "영수증 분석에 실패했습니다."))
                }
            } catch (e: Exception) {
                println("🔍 OCR 분석 실패: ${e.message}")
                Result.failure(e)
            }
        }
    }

    /**
     * 그룹 정산 확정
     * @param groupId 그룹 ID
     * @param pricePerPerson 인당 정산 금액
     * @param memberEmails 정산 대상 멤버 이메일 리스트
     * @return 정산 결과
     */
    suspend fun settleGroup(groupId: Long, pricePerPerson: Int, memberEmails: List<String>): Result<SettlementResponse> {
        return withContext(Dispatchers.IO) {
            try {
                if (memberEmails.isEmpty()) {
                    return@withContext Result.failure(Exception("정산 대상 멤버를 선택해주세요."))
                }
                
                val response = groupApiService.settleGroup(
                    groupId = groupId,
                    body = SettlementRequest(
                        pricePerPerson = pricePerPerson,
                        memberEmails = memberEmails
                    )
                )
                
                if (response.isSuccessful && response.data != null) {
                    Result.success(response.data)
                } else {
                    Result.failure(Exception(response.message ?: "정산 확정에 실패했습니다."))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
