package com.ssafy.a705.controller.service

import android.util.Log
import com.ssafy.a705.model.base.ApiException
import com.ssafy.a705.model.base.BaseResponse
import com.ssafy.a705.model.req.CommentRequest
import com.ssafy.a705.model.req.UpdatePostRequest
import com.ssafy.a705.model.req.WithPostWriteRequest
import com.ssafy.a705.model.resp.CommentResponse
import com.ssafy.a705.model.resp.CursorData
import com.ssafy.a705.model.resp.WithPostDetailData
import com.ssafy.a705.model.resp.WithPostDto
import com.ssafy.a705.model.resp.WithPostWriteResponse
import com.ssafy.a705.network.ApiClient
import javax.inject.Inject

class WithService @Inject constructor(
    private val apiClient: ApiClient
) {
    private val api = apiClient.companionPostApi

    // 1) 게시판 전체 조회
    suspend fun getWithPosts(cursorId: Long?): CursorData {
        // 1) 공통 에러 검사
        val res: BaseResponse<CursorData> = api.getWithPosts(cursorId)
        res.message?.let { throw ApiException(it) }

        // 2) data 자체가 null 인 경우만 예외로 처리 (이건 시스템 오류)
        val data = res.data
            ?: throw ApiException("앗, 게시글을 불러오는 중 문제가 생겼어요. 잠시 후 다시 시도해 주세요.")
        return data
    }

    // 2) 상세 조회
    suspend fun getPostDetail(postId: Long): WithPostDetailData {
        val res: BaseResponse<WithPostDetailData> = api.getPostDetail(postId) // ✅ 제네릭 변경
        res.message?.let { throw ApiException(it) }
        return res.data ?: throw ApiException("게시물 정보를 가져오는 중 문제가 발생했어요. 다시 시도해 주세요.")
    }

    // 3) 글 작성
    suspend fun writePost(request: WithPostWriteRequest): WithPostWriteResponse {
        val res: BaseResponse<WithPostWriteResponse> = api.writePost(request)
        res.message?.let { throw ApiException(it) }
        return res.data
            ?: throw ApiException("새 글을 등록하는 중에 문제가 생겼습니다. 다시 한 번 눌러보실래요?")
    }

    // 4) 글 수정
    suspend fun updateBoard(boardId: Long, title: String, content: String) {
        val res = api.updatePost(boardId, UpdatePostRequest(title, content))
        // 서버가 message에 오류를 넣는 계약이면 이것만 검사
        if (!res.message.isNullOrBlank()) throw ApiException(res.message)
        // data는 null이어도 성공으로 간주 (201/200)
    }

    // 5) 글 삭제
    suspend fun deletePost(postId: Long) {
        val res = api.deletePost(postId)
        res.message?.let { throw ApiException(it) }
        // ✅ data 확인 불필요 (null 정상)
    }

    // 6) 댓글 작성
    suspend fun writeComment(postId: Long, comment: CommentRequest) {
        // 인터페이스가 BaseResponse<CommentResponse> 를 리턴한다면 그대로 받기
        val res: BaseResponse<CommentResponse?> = api.writeComment(postId, comment)

        // 서버가 message에 에러를 담아줄 수 있으면 그때만 실패 처리
        if (!res.message.isNullOrBlank()) {
            throw ApiException(res.message)
        }
        // ✅ data(null/값) 여부는 보지 않음 (201 + data=null 계약 허용)
        // 성공이면 그냥 반환 (Unit)
    }

    // 7) 댓글 수정
    suspend fun updateComment(boardId: Long, commentId: Long, request: CommentRequest) {
        val res = api.updateComment(boardId, commentId, request)
        res.message?.let { throw ApiException(it) }
        // ✅ data 확인 불필요
    }


    // 8) 댓글 삭제
    suspend fun deleteComment(boardId: Long, commentId: Long) {
        val res = api.deleteComment(boardId, commentId)
        res.message?.let { throw ApiException(it) }
        // ✅ data 확인 불필요
    }
}