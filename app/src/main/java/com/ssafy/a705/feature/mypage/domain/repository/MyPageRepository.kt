package com.ssafy.a705.feature.mypage.domain.repository

import com.ssafy.a705.feature.mypage.data.model.request.PatchNicknameRequest
import com.ssafy.a705.feature.mypage.data.model.response.MyBoardsPageResponse
import com.ssafy.a705.feature.mypage.data.model.response.MyCommentsPageResponse
import com.ssafy.a705.feature.mypage.data.model.response.MyProfileResponse

interface MyPageRepository {
    suspend fun getMyPosts(page: Int?, size: Int?): MyBoardsPageResponse
    suspend fun getMyComments(page: Int?, size: Int?): MyCommentsPageResponse
    suspend fun getMyProfile(): MyProfileResponse
    suspend fun patchNickname(body: PatchNicknameRequest)
    suspend fun patchProfile(email: String, profileUrlOrKey: String)
}