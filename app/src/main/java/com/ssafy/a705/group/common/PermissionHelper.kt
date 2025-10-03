package com.ssafy.a705.group.common

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

object PermissionHelper {

    fun getGalleryPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES) // Android 13+
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE) // Android 12↓
        }
    }

    // 갤러리 권한 허용 여부 확인
    fun hasGalleryPermission(context: Context): Boolean {
        return getGalleryPermissions().all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    // 갤러리 권한 요청 실행
    fun requestGalleryPermission(launcher: ActivityResultLauncher<Array<String>>) {
        launcher.launch(getGalleryPermissions())
    }
}

/**
 * 갤러리 권한 요청 버튼 (Compose)
 * - 권한이 이미 허용 되었으면 onGranted() 실행
 * - 권한 거부 시 onDenied() 실행
 */
@Composable
fun GalleryPermissionRequestButton(
    onGranted: () -> Unit = {}, // 권한 허용 시 실행
    onDenied: () -> Unit = {}   // 권한 거부 시 실행
) {
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.all { it }) {
            Toast.makeText(context, "갤러리 권한 허용됨", Toast.LENGTH_SHORT).show()
            onGranted()
        } else {
            Toast.makeText(context, "갤러리 권한이 거부되었습니다.", Toast.LENGTH_SHORT).show()
            onDenied()
        }
    }

    Button(onClick = {
        if (PermissionHelper.hasGalleryPermission(context)) {
            Toast.makeText(context, "이미 갤러리 권한이 있습니다.", Toast.LENGTH_SHORT).show()
            onGranted()
        } else {
            PermissionHelper.requestGalleryPermission(permissionLauncher)
        }
    }) {
        Text("갤러리 권한 요청")
    }
}
