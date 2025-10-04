package com.ssafy.a705.feature.board.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.ssafy.a705.R

private const val S3_BASE = "https://glmlbucket.s3.ap-northeast-2.amazonaws.com/"

@Composable
fun Avatar(
    imageUrl: String?,
    nameFallback: String,
    size: Dp
) {
    val baseModifier = Modifier.size(size).clip(CircleShape)

    if (imageUrl.isNullOrBlank()) {
        DefaultAvatar(size)      // ← 이니셜 없이 빈 원
        return
    }

    // members 키 → 풀 URL, http → https
    val safeUrl = imageUrl.trim().let { raw ->
        when {
            raw.startsWith("https://") -> raw
            raw.startsWith("http://")  -> raw.replaceFirst("http://", "https://")
            raw.startsWith("/members/")-> S3_BASE + raw.trimStart('/')
            raw.startsWith("members/") -> S3_BASE + raw
            else -> raw
        }
    }

    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(safeUrl)
            .crossfade(true)
            .build(),
        contentDescription = "$nameFallback 프로필",
        modifier = baseModifier,
        contentScale = ContentScale.Crop,
        loading = { DefaultAvatar(size) },
        error   = { DefaultAvatar(size) }
    )
}

@Composable
private fun DefaultAvatar(size: Dp) {
    Image(
        painter = painterResource(id = R.drawable.profile),
        contentDescription = "기본 프로필",
        modifier = Modifier
            .size(size)
            .clip(CircleShape),
        contentScale = ContentScale.Crop
    )
}