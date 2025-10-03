package com.ssafy.a705.feature.tracking

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import javax.inject.Inject

class TrackingRepository @Inject constructor(
    private val api: TrackingApi
) {
    suspend fun createTracking(
        snapshots: List<TrackingSnapshot>,
        thumbnailPath: String
    ): TrackingCreateResponse {
        val req = TrackingCreateRequest(
            tracks         = snapshots,
            thumbnailImage = thumbnailPath
        )

        // Retrofit 호출
        val resp : Response<TrackingCreateResponse> = api.postTrackingWithThumbnail(req)

        if (resp.isSuccessful) {
            return resp.body()!!
        } else {
            throw Exception("레코드 생성 실패: ${resp.code()} ${resp.message()}")
        }
    }

    suspend fun fetchTrackingDetail(id: String): TrackingDetailResponse.DetailData? {
        val resp = api.getTrackingDetail(id)

        Log.d("TrackingRepo", "서버 응답: ${resp.body()?.data?.images}")

        if (resp.isSuccessful) {
            return resp.body()?.data
        } else {
            throw Exception("서버 에러: ${resp.code()} ${resp.message()}")
        }
    }

    suspend fun fetchAllTrackingList(): List<TrackingImageItem>  = withContext(Dispatchers.IO) {
        val resp = api.getTrackingList()
        if (!resp.isSuccessful) {
            throw Exception("전체 조회 실패: ${resp.code()} ${resp.message()}")
        }
        resp.body()?.data?.trackingImages.orEmpty()
    }

    suspend fun updateTrackingImages(
        trackingId: String,
        images: List<String>,
        thumbnailImage: String
    ): Boolean = withContext(Dispatchers.IO) {
        val resp = api.updateTrackingImages(
            id = trackingId,
            request = TrackingUpdateRequest(images, thumbnailImage)
        )
        if (resp.isSuccessful) {
            resp.body()?.message == null
        } else {
            Log.e("TrackingRepository", "이미지 등록 실패: ${resp.code()} ${resp.message()}")
            throw Exception("이미지 등록 실패: ${resp.code()} ${resp.message()}")
        }
    }

    suspend fun deleteTracking(trackingId: String): Boolean = withContext(Dispatchers.IO) {
        val resp = api.deleteTracking(trackingId)
        if (resp.isSuccessful) {
            resp.body()?.message == null
        } else {
            throw Exception("삭제 실패: ${resp.code()} ${resp.message()}")
        }
    }




}
