package com.ssafy.a705.tracking

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
fun TrackingPermissionRequester(
    onGranted: () -> Unit
) {
    val context = LocalContext.current

    var fineGranted by remember { mutableStateOf(false) }
    var backgroundRequested by remember { mutableStateOf(false) }

    // Foreground 권한 요청
    val foregroundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true &&
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        fineGranted = granted

        if (!granted) {
            Toast.makeText(context, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    // Background 권한 요청 : 포그라운드 허용 후에 요청해야 함
    val backgroundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onGranted()
        } else {
            Toast.makeText(context, "백그라운드 위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        // 보유 권한 확인
        val fineAlready = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseAlready = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val backgroundAlready = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else true

        // 포그라운드 권한 없으면 권한 요청
        if (!fineAlready || !coarseAlready) {
            foregroundLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
        // 백그라운드 권한이 없으면 권한 요청
        else if (!backgroundAlready && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !backgroundRequested) {
            backgroundRequested = true
            backgroundLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
        // 모든 권한이 있어야 콜백 수행
        else {
            onGranted()
        }
    }
}
