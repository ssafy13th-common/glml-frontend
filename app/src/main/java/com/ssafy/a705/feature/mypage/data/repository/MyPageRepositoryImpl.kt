package com.ssafy.a705.feature.mypage.data.repository

import androidx.core.net.toUri
import com.ssafy.a705.common.network.base.ApiException
import com.ssafy.a705.feature.mypage.data.model.request.PatchNicknameRequest
import com.ssafy.a705.feature.mypage.data.model.request.PatchProfileRequest
import com.ssafy.a705.feature.mypage.data.model.response.MyBoardsPageResponse
import com.ssafy.a705.feature.mypage.data.model.response.MyCommentsPageResponse
import com.ssafy.a705.feature.mypage.data.model.response.MyProfileResponse
import com.ssafy.a705.feature.mypage.data.source.MyPageRemoteDataSource
import com.ssafy.a705.feature.mypage.domain.repository.MyPageRepository
import javax.inject.Inject

class MyPageRepositoryImpl @Inject constructor(
    private val remoteDataSource: MyPageRemoteDataSource
) : MyPageRepository {

    override suspend fun getMyPosts(page: Int?, size: Int?): MyBoardsPageResponse {
        val res = remoteDataSource.getMyBoards(page, size)
        res.message?.let { throw ApiException(it) }
        return res.data ?: throw ApiException("내 게시글을 불러오지 못했습니다.")
    }

    override suspend fun getMyComments(page: Int?, size: Int?): MyCommentsPageResponse {
        val res = remoteDataSource.getMyComments(page, size)
        res.message?.let { throw ApiException(it) }
        return res.data ?: throw ApiException("내 댓글을 불러오지 못했습니다.")
    }

    override suspend fun getMyProfile(): MyProfileResponse {
        val res = remoteDataSource.getMyProfile()
        res.message?.let { throw ApiException(it) }
        return res.data ?: throw ApiException("마이페이지 정보를 불러오지 못했습니다.")
    }

    override suspend fun patchNickname(body: PatchNicknameRequest) {
        val res = remoteDataSource.patchNickname(body)
        res.message?.let { throw ApiException(it) }
    }

    override suspend fun patchProfile(email: String, profileUrlOrKey: String) {
        val parsedPath = try {
            profileUrlOrKey.toUri().path?.trimStart('/')
        } catch (_: Throwable) {
            null
        }
        val raw = parsedPath ?: profileUrlOrKey.trimStart('/')
        val key = if (raw.startsWith("members/")) raw else "members/$raw"

        val res = remoteDataSource.patchProfile(PatchProfileRequest(email, key))
        res.message?.let { throw ApiException(it) }
    }
}
