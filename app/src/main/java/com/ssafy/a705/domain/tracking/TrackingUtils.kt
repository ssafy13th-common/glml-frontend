package com.ssafy.a705.domain.tracking

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.util.Log
import androidx.core.graphics.createBitmap
import androidx.exifinterface.media.ExifInterface
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.LatLngBounds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

fun captureAndSaveThumbnailToTempFile(
    context: Context,
    mapView: View,
    onResult: (File?) -> Unit
) {
    captureMapViewToBitmap(mapView) { bitmap ->
        if (bitmap != null) {
            val file = saveThumbnailTemp(context, bitmap)
            onResult(file)
        } else {
            Log.e("Thumbnail", "❌ PixelCopy 실패")
            onResult(null)
        }
    }
}

private fun captureMapViewToBitmap(mapView: View, onComplete: (Bitmap?) -> Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val surfaceView = findSurfaceView(mapView)
        if (surfaceView == null) {
            onComplete(null)
            return
        }

        val bitmap = createBitmap(surfaceView.width, surfaceView.height)

        try {
            PixelCopy.request(
                surfaceView,
                bitmap,
                { result ->
                    if (result == PixelCopy.SUCCESS) {
                        onComplete(bitmap)
                    } else {
                        onComplete(null)
                    }
                },
                Handler(Looper.getMainLooper())
            )
        } catch (e: Exception) {
            e.printStackTrace()
            onComplete(null)
        }
    } else {
        onComplete(null)
    }
}

private fun findSurfaceView(view: View): SurfaceView? {
    if (view is SurfaceView) return view
    if (view is ViewGroup) {
        for (i in 0 until view.childCount) {
            findSurfaceView(view.getChildAt(i))?.let { return it }
        }
    }
    return null
}

private fun saveThumbnailTemp(context: Context, bitmap: Bitmap): File {
    val tempFile = File.createTempFile("thumbnail_", ".jpg", context.cacheDir)
    FileOutputStream(tempFile).use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
    }
    Log.d("ThumbnailTemp", "📸 임시 썸네일 저장 경로: ${tempFile.absolutePath}")
    return tempFile
}

fun calculateBoundsFromSnapshots(snapshots: List<TrackingSnapshot>): LatLngBounds? {
    if (snapshots.size < 2) return null

    val builder = LatLngBounds.Builder()
    snapshots.forEach {
        builder.include(LatLng.from(it.latitude, it.longitude))
    }
    return builder.build()
}

private val SAFE = Regex("[^a-z0-9._-]")
fun sanitizeName(name: String): String {
    val base = name.substringBeforeLast('.').lowercase().replace(SAFE, "_")
    val ext  = name.substringAfterLast('.', "jpg").lowercase()
    return "$base.$ext"
}

fun Float.dpToPx(context: Context): Float =
    this * context.resources.displayMetrics.density
