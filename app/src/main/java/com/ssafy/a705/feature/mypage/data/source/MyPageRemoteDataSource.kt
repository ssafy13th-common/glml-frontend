package com.ssafy.a705.feature.mypage.data.source

import android.util.Log
import com.ssafy.a705.common.network.base.ApiException
import com.ssafy.a705.common.network.base.BaseResponse
import com.ssafy.a705.feature.mypage.data.model.request.PatchNicknameRequest
import com.ssafy.a705.feature.mypage.data.model.request.PatchProfileRequest
import com.ssafy.a705.feature.mypage.data.model.response.MyBoardsPageResponse
import com.ssafy.a705.feature.mypage.data.model.response.MyCommentsPageResponse
import com.ssafy.a705.feature.mypage.data.model.response.MyProfileResponse
import javax.inject.Inject

class MyPageRemoteDataSource @Inject constructor(private val api: MyPageApi) {
    suspend fun getMyBoards(page: Int?, size: Int?): BaseResponse<MyBoardsPageResponse> {
        try {
            val response = api.getMyBoards(page, size)
            if (response.isSuccessful) {
                val body = response.body() ?: throw ApiException("응답 비어있음")
                return body
            } else {
                val code = response.code()
                val msg = response.errorBody()?.string() ?: "알 수 없는 오류"
                throw ApiException("서버 오류 $code: $msg")
            }
        } catch (e: Exception) {
            Log.e("MyPage", "내 게시물 조회 중 오류 발생: ${e.message}")
            throw e
        }
    }


    suspend fun getMyComments(page: Int?, size: Int?): BaseResponse<MyCommentsPageResponse> {
        try {
            val response = api.getMyComments(page, size)
            if (response.isSuccessful) {
                val body = response.body() ?: throw ApiException("댓글 응답이 비어있습니다.")
                return body
            } else {
                val code = response.code()
                val msg = response.errorBody()?.string() ?: "알 수 없는 오류"
                throw ApiException("댓글 불러오기 실패: $code - $msg")
            }
        } catch (e: Exception) {
            Log.e("MyPage", "내 댓글 조회 중 오류 발생: ${e.message}")
            throw e
        }
    }

    suspend fun getMyProfile(): BaseResponse<MyProfileResponse> {
        try {
            val response = api.getMyProfile()
            if (response.isSuccessful) {
                val body = response.body() ?: throw ApiException("프로필 응답이 비어있습니다.")
                return body
            } else {
                val code = response.code()
                val msg = response.errorBody()?.string() ?: "알 수 없는 오류"
                throw ApiException("프로필 불러오기 실패: $code - $msg")
            }
        } catch (e: Exception) {
            Log.e("MyPage", "프로필 불러오기 중 오류 발생: ${e.message}")
            throw e
        }
    }

    suspend fun patchNickname(body: PatchNicknameRequest): BaseResponse<Unit> {
        try {
            val response = api.patchNickname(body)
            if (response.isSuccessful) {
                val body = response.body() ?: throw ApiException("닉네임 변경 응답이 비어있습니다.")
                return body
            } else {
                val code = response.code()
                val msg = response.errorBody()?.string() ?: "알 수 없는 오류"
                throw ApiException("닉네임 변경 실패: $code - $msg")
            }
        } catch (e: Exception) {
            Log.e("MyPage", "닉네임 변경 중 오류 발생: ${e.message}")
            throw e
        }
    }

    suspend fun patchProfile(body: PatchProfileRequest): BaseResponse<Unit> {
        try {
            val response = api.patchProfile(body)
            if (response.isSuccessful) {
                val body = response.body() ?: throw ApiException("프로필 변경 응답이 비어있습니다.")
                return body
            } else {
                val code = response.code()
                val msg = response.errorBody()?.string() ?: "알 수 없는 오류"
                throw ApiException("프로필 변경 실패: $code - $msg")
            }
        } catch (e: Exception) {
            Log.e("MyPage", "프로필 변경 중 오류 발생: ${e.message}")
            throw e
        }
    }
}