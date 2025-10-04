package com.ssafy.a705.feature.mypage.ui.viewmodel

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.a705.common.imageS3.ImageRepository
import com.ssafy.a705.feature.mypage.domain.usecase.UpdateProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ProfilePhotoViewModel @Inject constructor(
    private val imageRepo: ImageRepository,          // imageS3 - 수정 없음 (그대로 인용)
    private val updateProfileUseCase: UpdateProfileUseCase,
    @ApplicationContext private val app: Context
) : ViewModel() {

    /**
     * 갤러리에서 받은 Uri로:
     * 1) 로컬 캐시 복사
     * 2) presign 받기
     * 3) S3 PUT 업로드
     * 4) 서버에 profileUrl PATCH
     */
    fun pickAndUpload(
        email: String,
        uri: Uri,
        domain: String = "members",
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val cr = app.contentResolver
                val local = copyUriToCache(cr, uri)

                val mimeFromCr = cr.getType(uri)              // e.g. "image/jpeg"
                val ext = mimeToExt(mimeFromCr)               // jpg/png/webp 등
                val fileName = "${UUID.randomUUID()}.$ext"

                val presigns = imageRepo.fetchPresignedUrls(listOf(fileName), domain)
                val p = presigns.first()

                val contentType = extToMime(ext)
                val ok = imageRepo.uploadFileToPresignedUrl(p.presignedUrl, local, contentType)
                if (!ok) error("upload failed")

                val publicUrl = p.presignedUrl.substringBefore("?")
// presigned 전체 URL → path만 추출해서 앞의 '/' 제거 → S3 Key로 변환
                val key = publicUrl.toUri().path?.trimStart('/') ?: publicUrl
                updateProfileUseCase(email, key)
            }.onSuccess {
                withContext(Dispatchers.Main) { onResult(true) }
            }.onFailure {
                withContext(Dispatchers.Main) { onResult(false) }
            }
        }
    }

    // ───── helpers ─────

    private fun copyUriToCache(cr: ContentResolver, uri: Uri): File {
        val input = cr.openInputStream(uri) ?: error("URI open 실패")
        val file = File.createTempFile("pick_", ".tmp", app.cacheDir) // 캐시 파일 확장자는 무관
        file.outputStream().use { out -> input.copyTo(out) }
        input.close()
        return file
    }

    private fun File.extensionOr(default: String): String {
        val dot = name.lastIndexOf('.')
        return if (dot >= 0) name.substring(dot + 1) else default
    }

    // MIME -> 확장자
    private fun mimeToExt(mime: String?): String = when (mime?.lowercase()) {
        "image/jpeg", "image/jpg" -> "jpg"
        "image/png" -> "png"
        "image/webp" -> "webp"
        else -> "jpg"  // 모르면 jpg로
    }

    // 확장자 -> MIME
    private fun extToMime(ext: String): String = when (ext.lowercase()) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "webp" -> "image/webp"
        else -> "image/jpeg"
    }

}