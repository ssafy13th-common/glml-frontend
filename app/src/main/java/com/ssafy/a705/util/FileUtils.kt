package com.ssafy.a705.util

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

object FileUtils {

    /**
     * URI를 임시 파일로 복사하고 로컬 경로를 반환.
     * - 기존 프로젝트에서 사용하던 함수 호환 유지를 위해 그대로 제공
     */
    fun copyUriToTempFile(context: Context, uri: Uri): String? {
        return try {
            val fileName = queryDisplayName(context.contentResolver, uri)
                ?: "temp_${System.currentTimeMillis()}"
            val input = context.contentResolver.openInputStream(uri) ?: return null
            val outFile = File(context.cacheDir, fileName)
            FileOutputStream(outFile).use { output -> input.copyTo(output) }
            outFile.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    /**
     * ContentResolver에서 MIME 추론. 실패 시 파일 확장자로 추정, 그래도 없으면 octet-stream.
     */
    fun resolveMimeType(context: Context, uri: Uri): String {
        val resolver = context.contentResolver
        val detected = resolver.getType(uri)
        if (!detected.isNullOrBlank()) return detected

        val name = queryDisplayName(resolver, uri)
        val byExt = when {
            name?.endsWith(".jpg", true) == true || name?.endsWith(".jpeg", true) == true -> "image/jpeg"
            name?.endsWith(".png", true) == true -> "image/png"
            name?.endsWith(".webp", true) == true -> "image/webp"
            name?.endsWith(".heic", true) == true || name?.endsWith(".heif", true) == true -> "image/heic"
            else -> null
        }
        return byExt ?: "application/octet-stream"
    }

    /**
     * URI를 멀티파트(키=receipt)로 변환.
     * - 영수증 분석 API에 바로 넘길 수 있게 구성
     */
    fun uriToReceiptPart(context: Context, uri: Uri, formKey: String = "receipt"): MultipartBody.Part? {
        return try {
            val fileName = queryDisplayName(context.contentResolver, uri)
                ?: "receipt_${System.currentTimeMillis()}"
            val mime = resolveMimeType(context, uri)

            val input = context.contentResolver.openInputStream(uri) ?: return null
            val outFile = File(context.cacheDir, fileName)
            FileOutputStream(outFile).use { output -> input.copyTo(output) }

            val body = outFile.asRequestBody(mime.toMediaType())
            MultipartBody.Part.createFormData(formKey, outFile.name, body)
        } catch (_: Exception) {
            null
        }
    }

    private fun queryDisplayName(resolver: ContentResolver, uri: Uri): String? {
        return try {
            var name: String? = null
            val cursor: Cursor? = resolver.query(uri, null, null, null, null)
            cursor?.use {
                val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx != -1 && it.moveToFirst()) name = it.getString(idx)
            }
            name
        } catch (_: Exception) {
            null
        }
    }
}
