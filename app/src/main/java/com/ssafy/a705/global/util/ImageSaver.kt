package com.ssafy.a705.global.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import coil.ImageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object ImageSaver {

    fun saveImageFromCache(
        context: Context,
        imageUrl: String,
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val imageLoader = ImageLoader(context)

            // 캐시에서 가져오기
            val request = ImageRequest.Builder(context)
                .data(imageUrl)
                .allowHardware(false)
                .diskCachePolicy(CachePolicy.ENABLED)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .build()

            val result = imageLoader.execute(request)
            val bitmap = (result as? SuccessResult)
                ?.drawable
                ?.let { it as? BitmapDrawable }
                ?.bitmap

            val success = bitmap?.let { saveBitmapToGallery(context, it) } ?: false

            showToast(context, success)
        }
    }

    private fun saveBitmapToGallery(context: Context, bitmap: Bitmap): Boolean {
        return try {
            val filename = "tracking_${System.currentTimeMillis()}.jpg"
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/TrackingApp")
            }

            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let {
                resolver.openOutputStream(it)?.use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun showToast(context: Context, success: Boolean) {
        withContext(Dispatchers.Main) {
            Toast.makeText(
                context,
                if (success) "이미지가 저장되었습니다" else "이미지 저장 실패. 새로고침 후 다시 시도해 주세요.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
