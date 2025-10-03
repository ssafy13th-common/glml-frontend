package com.ssafy.a705.feature.tracking

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.a705.common.imageS3.ImageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import androidx.core.net.toUri
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlin.error

@HiltViewModel
class TrackingViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val trackingPrefs: TrackingPreferenceManager,
    private val repository: TrackingRepository,
    private val imageRepository: ImageRepository
) : ViewModel() {

    val isTracking: StateFlow<Boolean> =
        trackingPrefs.isTracking
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // 실시간 위치 리스트
    private val _locations = MutableStateFlow<List<TrackingSnapshot>>(emptyList())
    val locations: StateFlow<List<TrackingSnapshot>> = _locations

    init {
        // 전역 스토어 구독 : 화면이 바뀌어도 동일한 리스트를 뿌려줌
        viewModelScope.launch {
            TrackingSnapshotStore.snapshots.collect { list ->
                _locations.value = list
            }
        }
    }

    // 선택한 사진 Uri 리스트
    private val _photoUris = MutableStateFlow<List<Uri>>(emptyList())
    val photoUris: StateFlow<List<Uri>> = _photoUris

    // 서버에서 받은 트래킹 경로
    private val _trackPoints = MutableStateFlow<List<TrackingSnapshot>>(emptyList())
    val trackPoints: StateFlow<List<TrackingSnapshot>> = _trackPoints

    // 트래킹 썸네일에 첨부된 사진 Url 리스트
    private val _imageUrls = MutableStateFlow<List<String>>(emptyList())
    val imageUrls: StateFlow<List<String>> = _imageUrls
    
    // 트래킹 썸네일 기록 리스트
    private val _allImages = MutableStateFlow<List<TrackingImageItem>>(emptyList())
    val allImages: StateFlow<List<TrackingImageItem>> = _allImages

    // 로딩 중
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun startTracking() {
        viewModelScope.launch { trackingPrefs.setTracking(true) }
        TrackingSnapshotStore.clear()
        TrackingService.start(appContext)
    }

    fun stopTracking() {
        viewModelScope.launch { trackingPrefs.setTracking(false) }
        TrackingSnapshotStore.clear()
        TrackingService.stop(appContext)
    }

    fun onLocationUpdate(snapshot: TrackingSnapshot) {
        _locations.update { it + snapshot }
    }

    fun addPhotoUris(new: List<Uri>) {
        _photoUris.value = _photoUris.value + new
    }

    fun removePhotoUri(uri: Uri) {
        _photoUris.value = _photoUris.value.filterNot { it == uri }
    }

    fun removeServerImageUrl(key: String) {
        _imageUrls.value = _imageUrls.value.toMutableList().apply { remove(key) }
    }

    fun clearLocations() {
        _locations.value = emptyList()
    }

    fun clearPhotoUris() {
        _photoUris.value = emptyList()
    }

    suspend fun createTracking(
        context: Context,
        thumbnailFile: File
    ): String? {
        return try {
            // S3에 썸네일 올리고 경로(thumbName) 얻기
            val thumbName = uploadThumbnailToS3(thumbnailFile)
            Log.d("TrackingVM", "서버 저장 성공")

            val resp = repository.createTracking(
                snapshots = locations.value,
                thumbnailPath = thumbName
            )

            // 업로드 완료 후 로컬 상태 초기화
            stopTracking()      // SharedPreference·StateFlow
            clearLocations()    // MutableStateFlow<List<…>>
            clearPhotoUris()    // MutableStateFlow<List<Uri>>

            // ID 반환
            resp.data!!.trackingId
        } catch (e: Exception) {
            Log.e("TrackingVM", e.toString())
            null
        }
    }

    private suspend fun uploadThumbnailToS3(
        thumbnailFile: File
    ): String = withContext(Dispatchers.IO) {
        val fileNames = sanitizeName(thumbnailFile.name).lowercase()
        val presigns = imageRepository.fetchPresignedUrls(fileNames = listOf(fileNames), domain = "trackings")
        val p = presigns.first { it.fileName.endsWith(fileNames) }

        imageRepository.uploadFileToPresignedUrl(presignedUrl = p.presignedUrl, file = thumbnailFile)
        p.fileName
    }

    fun fetchTrackingDetail(id: String) = viewModelScope.launch(Dispatchers.IO) {
        _isLoading.value = true

        try {
            // 서버에 detail 데이터 요청
            val detail = repository.fetchTrackingDetail(id) ?: return@launch

            // 받은 trackPoints, images를 각각 StateFlow에 업데이트
            _trackPoints.value = detail.trackPoints
            _imageUrls.value = detail.images

            Log.d("TrackingVM", "imageUrls 수신: ${_imageUrls.value}")
        } catch (e: Exception) {
            Log.e("TrackingVM", "상세 조회 실패", e)
        } finally {
            _isLoading.value = false
        }
    }

    fun fetchAllImages() = viewModelScope.launch(Dispatchers.IO) {
        try {
            _allImages.value = repository.fetchAllTrackingList()
        } catch (e: Exception) {
            Log.e("TrackingVM", "전체 조회 실패", e)
        }
    }

    fun uploadTrackingImages(
        trackingId: String,
        files: List<File>,
        thumbnailFile: File,
        onResult: (Boolean, String?) -> Unit
    ) = viewModelScope.launch(Dispatchers.IO) {
        _isLoading.value = true

        try {
            val nameMap = buildMap {
                files.forEach { put(sanitizeName(it.name), it) }
                put(sanitizeName(thumbnailFile.name), thumbnailFile)
            }
            val fileNames = nameMap.keys.toList()
            val presigns = imageRepository.fetchPresignedUrls(fileNames = fileNames, domain = "trackings")

            val semaphore = Semaphore(permits = 1)
            coroutineScope {
                presigns.map { p ->
                    async {
                        semaphore.withPermit {
                            val localKey = nameMap.keys.firstOrNull {
                                p.fileName.endsWith(it, ignoreCase = true)
                            } ?: error("프리사인 매칭 실패: ${p.fileName}")
                            val file = nameMap[localKey]!!
                            val mime = imageRepository.guessMime(localKey)
                            imageRepository.uploadFileToPresignedUrl(p.presignedUrl, file, mime)
                        }
                    }
                }.awaitAll()
            }

            val serverKeys = presigns.map { it.fileName }
            val thumbKeyOnly = sanitizeName(thumbnailFile.name)
            val thumbnailServerKey = presigns.first { it.fileName.endsWith(thumbKeyOnly) }.fileName

            val keptKeys = imageUrls.value.map{ toKey(it) }                      // removeServerImageUrl 반영된 최신 상태
            val newKeys = serverKeys.filter { it != thumbnailServerKey }
            val merged = (keptKeys + newKeys).distinct()

            Log.d("TrackingMV", "merged: $merged")

            // 서버에 PATCH
            repository.updateTrackingImages(
                trackingId = trackingId,
                images = merged,
                thumbnailImage = thumbnailServerKey
            )

            withContext(Dispatchers.Main) {
                _imageUrls.value = merged
                clearPhotoUris()
                onResult(true, null)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onResult(false, e.message)
            }
        } finally {
            _isLoading.value = false
        }
    }

    fun deleteTracking(
        trackingId: String,
        onResult: (Boolean, String?) -> Unit
    ) = viewModelScope.launch(Dispatchers.IO) {
        _isLoading.value = true
        try {
            val ok = repository.deleteTracking(trackingId)
            withContext(Dispatchers.Main) { onResult(ok, if (ok) null else "삭제 실패") }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) { onResult(false, e.message) }
        } finally {
            _isLoading.value = false
        }
    }

    private fun toKey(value: String): String {
        return if (value.startsWith("http")) {
            val u = value.toUri()
            (u.path ?: "").removePrefix("/")
        } else value
    }
}
