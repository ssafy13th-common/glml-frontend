package com.ssafy.a705.imageS3

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.net.URLConnection
import javax.inject.Inject

class ImageRepository @Inject constructor(
    private val api: ImageApi
) {

    suspend fun fetchPresignedUrls(
        fileNames: List<String>,
        domain: String
    ): List<PresignedData> = withContext(Dispatchers.IO) {
        val resp = api.getPresignedUrls(ImagePresignRequest(fileNames, domain))

        if (!resp.isSuccessful) {
            val body = resp.errorBody()?.string()
            throw Exception("프리사인드 URL 요청 실패: ${resp.code()} ${resp.message()} | body=$body")
        }

        val body = resp.body() ?: error("presigned-urls | empty body")
        body.data?.presignedUrls ?: emptyList()
    }

    suspend fun uploadFileToPresignedUrl(
        presignedUrl: String,
        file: File,
        mime: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val realMime = mime ?: guessMime(file.name)
        val body = file.asRequestBody(realMime.toMediaType())
        val req  = Request.Builder()
            .url(presignedUrl)
            .header("Content-Type", realMime)
            .put(body)
            .build()

        client.newCall(req).execute().use { resp ->
            val ok = resp.isSuccessful

            if (!ok) {
                val err = resp.body?.string()
                Log.e("ImageRepository",
                    "S3 PUT 실패 code=${resp.code} msg=${resp.message} body=$err url=${presignedUrl.take(120)}...")
            } else {
                Log.d("ImageRepository", "S3 PUT 성공 code=${resp.code}")
            }

            ok
        }
    }

    fun guessMime(fileName: String): String {
        return URLConnection.guessContentTypeFromName(fileName) ?: "application/octet-stream"
    }
}