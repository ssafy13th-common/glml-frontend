package com.ssafy.a705.feature.record.diary

import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.ssafy.a705.common.imageS3.ImageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
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
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeParseException

data class RecordCreateUiState(
    val code: String = "",
    val location: String = "",
    val startDate: String? = null,
    val endDate: String? = null,
    val photoUris: List<Uri> = emptyList(),
    val diary: String = "",
    val isSaving: Boolean = false,
    val error: String? = null
)

data class RecordDetailUiState(
    val loading: Boolean = false,
    val data: RecordDetailItem? = null,
    val error: String? = null
)

data class RecordUpdateUiState(
    val diaryId: Long? = null,

    val locationCode: Int? = null,
    val locationName: String? = null,
    val startedAtDisplay: String = "", // yyyy.MM.dd
    val endedAtDisplay: String = "",
    val content: String = "",

    val keepServerKeys: List<String> = mutableListOf(),
    val newPhotoUris: List<Uri> = emptyList(),

    val isSaving: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class RecordViewModel @Inject constructor(
    private val repository: RecordRepository,
    private val imageRepository: ImageRepository
) : ViewModel() {
    // mutableStateOf : Compose에서 값이 바뀌면 UI를 다시 그려주도록 "관찰 가능한 상태"로 만듦
    private val _createState = MutableStateFlow(RecordCreateUiState())
    val createState: StateFlow<RecordCreateUiState> = _createState

    private val _updateState = MutableStateFlow(RecordUpdateUiState())
    val updateState: StateFlow<RecordUpdateUiState> = _updateState

    private val _detail = MutableStateFlow(RecordDetailUiState())
    val detail: StateFlow<RecordDetailUiState> = _detail

    private val _locationCode = MutableStateFlow<Int?>(null)
    private val _refreshTick = MutableStateFlow(0)

    private val KST: ZoneId = ZoneId.of("Asia/Seoul")

    @OptIn(ExperimentalCoroutinesApi::class)
    val diaries = combine(_locationCode, _refreshTick) { code, _ -> code }
        .flatMapLatest { code ->
            Pager(
                config = PagingConfig(
                    pageSize = 10,
                    prefetchDistance = 2,
                    enablePlaceholders = false
                ),
                pagingSourceFactory = {
                    repository.diaryPagingSource(
                        pageSize = 10,
                        locationCode = code
                    )
                }
            ).flow
        }
        .cachedIn(viewModelScope)

    fun setCode(code: String) {
        _createState.update { it.copy(code = code) }
    }

    fun setLocation(location: String) {
        _createState.update { it.copy(location = location) }
    }

    fun setStartDate(date: String) {
        _createState.update { it.copy(startDate = date) }
    }

    fun setEndDate(date: String) {
        _createState.update { it.copy(endDate = date) }
    }

    fun addPhotos(uri: List<Uri>) {
        _createState.update { it.copy(photoUris = it.photoUris + uri) }
    }

    fun removePhoto(uri: Uri) {
        _createState.update { it.copy(photoUris = it.photoUris - uri) }
    }

    fun setDiary(text: String) {
        _createState.update { it.copy(diary = text) }
    }

    fun reset() {
        _createState.value = RecordCreateUiState()
    }

    fun setLocationFilter(code: Int?) {
        _locationCode.value = code
    }

    fun refreshList() {
        _refreshTick.update { it + 1 }
    }

    fun setUpdateStartedAt(date: String) = _updateState.update { it.copy(startedAtDisplay = date) }
    fun setUpdateEndedAt(date: String)   = _updateState.update { it.copy(endedAtDisplay = date) }
    fun setUpdateContent(text: String)   = _updateState.update { it.copy(content = text) }
    fun setUpdateLocationName(name: String) = _updateState.update { it.copy(locationName = name) }
    fun addNewPhotos(uris: List<Uri>) {
        _updateState.update { it.copy(newPhotoUris = it.newPhotoUris + uris) }
    }

    fun removeNewPhoto(uri: Uri) {
        _updateState.update { it.copy(newPhotoUris = it.newPhotoUris.filterNot { u -> u == uri }) }
    }

    fun removeKeep(key: String) = _updateState.update {
        it.copy(keepServerKeys = it.keepServerKeys.filterNot { k -> k == key })
    }

    fun createRecordsWithUploads(
        context: Context,
        onFinished: (Boolean, Long?, String?) -> Unit
    ) {
        val s = _createState.value

        val codeInt = s.code.toIntOrNull()
        val started = toIso(s.startDate)
        val ended = toIso(s.endDate)

        validateCreateInputs(codeInt, started, ended)?.let { msg ->
            onFinished(false, null, msg); return
        }

        _createState.update { it.copy(isSaving = true, error = null) }

        viewModelScope.launch(Dispatchers.IO) {
            val files = s.photoUris.mapNotNull { uri -> copyUriToCache(context, uri) }

            try {
                if (files.isEmpty()) {
                    Log.d("RrecordMV", "사진 없어서 넘어감")
                    val req = RecordCreateRequest(
                        locationCode = codeInt!!,
                        startedAt = started!!,
                        endedAt = ended!!,
                        content = s.diary.ifBlank { null },
                        imageUrls = emptyList()
                    )
                    Log.d("RrecordMV", "req: $req")
                    val newId = repository.createRecord(req).getOrThrow()
                    withContext(Dispatchers.Main) {
                        _createState.update { it.copy(isSaving = false) }
                        refreshList()
                        reset()
                        onFinished(true, newId, null)
                    }
                    return@launch
                }

                Log.d("RrecordMV", "사진 있음")

                val nameMap: Map<String, File> = files
                    .mapIndexed { idx, f -> "${idx}.${f.extension.ifBlank { "jpg" }}".lowercase() to f }
                    .toMap()
                val presigns = imageRepository.fetchPresignedUrls(nameMap.keys.toList(), "diaries")

                val semaphore = Semaphore(permits = 1)
                coroutineScope {
                    presigns.map { p ->
                        async {
                            semaphore.withPermit {
                                val local = nameMap.keys.firstOrNull {
                                    p.fileName.endsWith(it, ignoreCase = true)
                                } ?: error("프리사인 매칭 실패: ${p.fileName}")
                                val file = nameMap[local]!!
                                val mime = imageRepository.guessMime(file.name)
                                imageRepository.uploadFileToPresignedUrl(p.presignedUrl, file, mime)
                            }
                        }
                    }.awaitAll()
                }

                val serverKeys = nameMap.keys.map { local ->
                    presigns.first { it.fileName.endsWith(local, ignoreCase = true) }.fileName
                }

                val req = RecordCreateRequest(
                    locationCode = codeInt!!,
                    startedAt = started!!,
                    endedAt = ended!!,
                    content = s.diary.ifBlank { null },
                    imageUrls = serverKeys
                )
                val newId = repository.createRecord(req).getOrThrow()

                withContext(Dispatchers.Main) {
                    _createState.update { it.copy(isSaving = false) }
                    refreshList()
                    reset()
                    onFinished(true, newId, null)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _createState.update { it.copy(isSaving = false, error = e.message) }
                    onFinished(false, null, e.message)
                }
            } finally {
                files.forEach { runCatching { it.delete() } }
            }
        }
    }

    fun updateDiaryWithUploads(
        context: Context,
        onFinished: (Boolean, String?) -> Unit
    ) {
        val s = _updateState.value

        val diaryId = s.diaryId ?: return onFinished(false, "수정 대상이 없습니다.")
        val locationCode = s.locationCode
        val started = toIso(s.startedAtDisplay)
        val ended = toIso(s.endedAtDisplay)

        validateCreateInputs(locationCode, started, ended)?.let { msg ->
            onFinished(false, msg); return
        }

        _updateState.update { it.copy(isSaving = true, error = null) }

        viewModelScope.launch(Dispatchers.IO) {
            val files = s.newPhotoUris.mapNotNull { uri -> copyUriToCache(context, uri) }
            try {
                // 새 파일 프리사인 업로드
                val newServerKeys: List<String> =
                    if (files.isNotEmpty()) {
                        val nameMap: Map<String, File> = files
                            .mapIndexed { idx, f -> "${idx}.${f.extension.ifBlank { "jpg" }}".lowercase() to f }
                            .toMap()
                        val presigns = imageRepository.fetchPresignedUrls(nameMap.keys.toList(), "diaries")

                        Log.d("RrecordMV", "업데이트 사진 있음")

                        val semaphore = Semaphore(permits = 1)
                        // 업로드
                        coroutineScope {
                            presigns.map { p ->
                                async {
                                    semaphore.withPermit {
                                        val local = nameMap.keys.firstOrNull {
                                            p.fileName.endsWith(it, ignoreCase = true)
                                        } ?: error("프리사인 매칭 실패: ${p.fileName}")
                                        val file = nameMap[local]!!
                                        val mime = imageRepository.guessMime(file.name)

                                        imageRepository.uploadFileToPresignedUrl(
                                            p.presignedUrl,
                                            file,
                                            mime
                                        )
                                    }
                                }
                            }.awaitAll()
                        }

                        // 서버 저장 키 추출
                        nameMap.keys.map { local ->
                            presigns.first {
                                it.fileName.endsWith(
                                    local,
                                    ignoreCase = true
                                )
                            }.fileName
                        }
                    } else emptyList()

                // PUT 바디 구성
                val req = RecordUpdateRequest(
                    locationCode = locationCode!!,
                    startedAt = started!!,
                    endedAt = ended!!,
                    content = s.content.ifBlank { null },
                    keepImageUrls = s.keepServerKeys.toList().map(::urlToKey),
                    newImageUrls = newServerKeys
                )

                Log.d("RecordVM", "keep: ${req.keepImageUrls}\nnew: ${req.newImageUrls}")

                // 서버 호출
                repository.updateRecord(diaryId, req).getOrThrow()

                withContext(Dispatchers.Main) {
                    _updateState.update { it.copy(isSaving = false) }
                    refreshList()
                    loadDiaryDetail(diaryId)
                    onFinished(true, null)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _updateState.update { it.copy(isSaving = false, error = e.message) }
                    onFinished(false, e.message)
                }
            } finally {
                files.forEach { runCatching { it.delete() } }
            }
        }
    }

    private fun toIso(date: String?): String? {
        return date?.replace(".", "-")
    }
    private fun toRaw(date: String?): String? {
        return date?.replace("-", ".")
    }

    private fun validateCreateInputs(
        codeInt: Int?,
        startedIso: String?,
        endedIso: String?
    ): String? {
        if (codeInt == null) return "지역 코드가 올바르지 않습니다."
        if (startedIso.isNullOrBlank()) return "여행 시작 일자가 입력되지 않았습니다."
        if (endedIso.isNullOrBlank()) return "여행 도착 일자가 입력되지 않았습니다."

        val start = try { LocalDate.parse(startedIso) } catch (_: DateTimeParseException) {
            return "시작 일자 형식이 올바르지 않습니다. (예: 2025.08.17)"
        }
        val end = try { LocalDate.parse(endedIso) } catch (_: DateTimeParseException) {
            return "도착 일자 형식이 올바르지 않습니다. (예: 2025.08.17)"
        }

        if (start.isAfter(end)) return "시작 일자는 도착 일자보다 늦을 수 없습니다."
        if (start.isAfter(LocalDate.now(KST))) return "시작 일자는 오늘 이후로 설정할 수 없습니다."
        return null
    }

    private fun copyUriToCache(context: Context, uri: Uri): File? = try {
        val ext = context.contentResolver.getType(uri)?.let { mime ->
            MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)
        } ?: "jpg"
        val out = File.createTempFile("upload_", ".$ext", context.cacheDir)
        context.contentResolver.openInputStream(uri)?.use { input ->
            out.outputStream().use { output -> input.copyTo(output) }
        }
        out
    } catch (_: Exception) {
        null
    }

    // 단일 레코드 반환
    fun loadDiaryDetail(id: Long) {
        _detail.value = RecordDetailUiState(loading = true)
        viewModelScope.launch(Dispatchers.IO) {
            repository.getDiaryDetail(id)
                .onSuccess {d ->
                    _detail.value = RecordDetailUiState(data = d)
                    _updateState.update {
                        it.copy(
                            diaryId = id,
                            locationCode = 11000,                   // todo : 하드코딩 및 속성 제거
                            locationName = d.location,
                            startedAtDisplay = toRaw(d.startedAt)!!,
                            endedAtDisplay   = toRaw(d.endedAt)!!,
                            content          = d.content ?: "",
                            keepServerKeys   = (d.imageUrls ?: emptyList()),
                            newPhotoUris     = emptyList()
                        )
                    }
                    Log.d("RecordVM", "${d.imageUrls}")
                }
                .onFailure { _detail.value = RecordDetailUiState(error = it.message) }
        }
    }

    fun deleteDiary(
        diaryId: Long,
        onFinished: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.deleteRecord(diaryId)
            withContext(Dispatchers.Main) {
                result.onSuccess {
                    refreshList()
                    onFinished(true, null)
                }.onFailure { e ->
                    onFinished(false, e.message)
                }
            }
        }
    }

    private fun urlToKey(s: String): String {
        return try {
            val path = s.toUri().path ?: s
            path.trimStart('/')
        } catch (_: Exception) {
            s
        }
    }
}