package com.ssafy.a705.feature.mypage.data.source

import com.ssafy.a705.common.network.base.BaseResponse
import com.ssafy.a705.feature.mypage.data.model.request.PatchNicknameRequest
import com.ssafy.a705.feature.mypage.data.model.request.PatchProfileRequest
import com.ssafy.a705.feature.mypage.data.model.response.MyBoardsPageResponse
import com.ssafy.a705.feature.mypage.data.model.response.MyCommentsPageResponse
import com.ssafy.a705.feature.mypage.data.model.response.MyProfileResponse
import javax.inject.Inject

class MyPageRemoteDataSource @Inject constructor(private val api: MyPageApi) {
    suspend fun getMyBoards(page: Int?, size: Int?): BaseResponse<MyBoardsPageResponse> =
        api.getMyBoards(page, size)

    suspend fun getMyComments(page: Int?, size: Int?): BaseResponse<MyCommentsPageResponse> =
        api.getMyComments(page, size)

    suspend fun getMyProfile(): BaseResponse<MyProfileResponse> = api.getMyProfile()

    suspend fun patchNickname(body: PatchNicknameRequest): BaseResponse<Unit> =
        api.patchNickname(body)

    suspend fun patchProfile(body: PatchProfileRequest): BaseResponse<Unit> =
        api.patchProfile(body)
}