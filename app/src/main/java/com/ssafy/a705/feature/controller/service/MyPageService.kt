package com.ssafy.a705.feature.controller.service

import android.net.Uri
import com.ssafy.a705.common.network.base.ApiException
import com.ssafy.a705.common.network.base.BaseResponse
import com.ssafy.a705.feature.model.req.PatchNicknameRequest
import com.ssafy.a705.feature.model.req.PatchProfileRequest
import com.ssafy.a705.feature.model.resp.MyBoardsPageResponse
import com.ssafy.a705.feature.model.resp.MyCommentsPageResponse
import com.ssafy.a705.feature.model.resp.MyProfileResponse
import com.ssafy.a705.common.network.ApiClient
import javax.inject.Inject

class MyPageService @Inject constructor(
    apiClient: ApiClient
) {
    private val api = apiClient.mypageApi

    /**
     * 내 게시글 목록 (페이지네이션)
     */
    suspend fun getMyBoards(page: Int, size: Int): MyBoardsPageResponse {
        val res: BaseResponse<MyBoardsPageResponse> = api.getMyBoards(page = page, size = size)
        res.message?.let { throw ApiException(it) }
        return res.data ?: throw ApiException("내 게시글을 불러오지 못했습니다.")
    }

    /**
     * 내 댓글 목록 (페이지네이션)
     */
    suspend fun getMyComments(page: Int, size: Int): MyCommentsPageResponse {
        val res: BaseResponse<MyCommentsPageResponse> = api.getMyComments(page = page, size = size)
        res.message?.let { throw ApiException(it) }
        return res.data ?: throw ApiException("내 댓글을 불러오지 못했습니다.")
    }

    suspend fun getMyProfile(): MyProfileResponse {
        val res = api.getMyProfile()
        res.message?.let { throw ApiException(it) }
        return res.data ?: throw ApiException("마이페이지 정보를 불러오지 못했습니다.")
    }
    suspend fun updateNickname(email: String, nickname: String) {
        api.patchNickname(PatchNicknameRequest(email = email, nickname = nickname))
    }
    suspend fun updateProfileUrl(email: String, profileUrlOrKey: String) {
        // 풀 URL이 와도 S3 Key로 정규화 (접두어 보존)
        val parsedPath = try {
            Uri.parse(profileUrlOrKey).path?.trimStart('/')
        } catch (_: Throwable) {
            null
        }
        val raw = parsedPath ?: profileUrlOrKey.trimStart('/')

        // "members/" 접두어가 없으면 붙여준다
        val key = if (raw.startsWith("members/")) raw else "members/$raw"

        api.patchProfile(PatchProfileRequest(email, key))
    }
    suspend fun logout()   = api.logout()
    suspend fun withdraw() = api.withdraw()
}