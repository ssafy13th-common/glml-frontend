package com.ssafy.a705.feature.group.photo

import android.content.Context
import android.net.Uri
import com.ssafy.a705.common.imageS3.ImageRepository
import com.ssafy.a705.common.network.GroupApiService
import com.ssafy.a705.common.network.GroupImageDto
import com.ssafy.a705.common.network.GroupImagesPostRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class GroupPhotoRepository @Inject constructor(
    private val groupApiService: GroupApiService,
    private val imageRepository: ImageRepository,
    @ApplicationContext private val context: Context
) {

    suspend fun getGroupImages(groupId: Long, cursorId: Long? = null, page: Int = 0, size: Int = 20): List<GroupImageDto> {
        return withContext(Dispatchers.IO) {
            try {
                val response = groupApiService.getGroupImages(groupId, cursorId, page, size)
                response.data?.images ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    suspend fun uploadImages(
        groupId: Long,
        imageUris: List<Uri>,
        onProgress: ((Int) -> Unit)? = null
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                onProgress?.invoke(10) // 시작

                // 1. 파일명 생성 및 매핑
                val nameMap = imageUris.mapIndexed { idx, uri ->
                    val fileName = "${idx}.jpg"
                    fileName to uri
                }.toMap()
                onProgress?.invoke(20) // 파일명 생성 완료

                // 2. Presigned URL 요청
                val fileNames = nameMap.keys.toList()
                val presignedUrls = imageRepository.fetchPresignedUrls(fileNames, "groups")
                onProgress?.invoke(40) // Presigned URL 요청 완료

                // 3. 각 이미지를 S3에 업로드
                val uploadedImageUrls = mutableListOf<String>()
                val totalImages = imageUris.size

                presignedUrls.forEach { presignedData ->
                    val localFileName = nameMap.keys.firstOrNull {
                        presignedData.fileName.endsWith(it, ignoreCase = true)
                    }

                    if (localFileName != null) {
                        val uri = nameMap[localFileName]!!
                        val file = uriToFile(uri)
                        if (file != null) {
                            val mime = imageRepository.guessMime(file.name)
                            val success = imageRepository.uploadFileToPresignedUrl(
                                presignedData.presignedUrl,
                                file,
                                mime
                            )
                            if (success) {
                                uploadedImageUrls.add(presignedData.fileName)
                            }
                        }
                    }
                }

                onProgress?.invoke(90) // S3 업로드 완료

                // 4. 업로드 성공한 이미지 URL들을 백엔드에 전달
                if (uploadedImageUrls.isNotEmpty()) {
                    val postRequest = GroupImagesPostRequest(uploadedImageUrls)
                    groupApiService.addGroupImages(groupId, postRequest)
                    onProgress?.invoke(100) // 완료
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                println("이미지 업로드 실패: ${e.message}")
                false
            }
        }
    }

    suspend fun deleteImage(groupId: Long, imageId: Long): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                groupApiService.deleteGroupImage(groupId, imageId)
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    private fun uriToFile(uri: Uri): File? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            if (inputStream == null) return null

            val file = File(context.cacheDir, "temp_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)

            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }

            file
        } catch (e: Exception) {
            println("Uri to File 변환 실패: ${e.message}")
            null
        }
    }


}
