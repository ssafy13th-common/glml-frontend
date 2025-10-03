package com.ssafy.a705.feature.record.diary

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.ssafy.a705.R

@Composable
fun RecordCard(item: RecordListItem) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val cellPx = with(density) { 80.dp.roundToPx() }

    val model = remember(item.thumbnailUrl, cellPx) {
        ImageRequest.Builder(context)
            .data(item.thumbnailUrl)
            .size(cellPx, cellPx)   // ★ 썸네일 픽셀 사이즈로 다운샘플
            .crossfade(false)       // 초기 지연 줄이기
            .build()
    }

    // 가로 방향 구성
    Row(modifier = Modifier.fillMaxWidth()) {
        // 이미지 영역
        SubcomposeAsyncImage(
            model = model,
            contentDescription = null,
            modifier = Modifier
                .size(80.dp)
                .aspectRatio(1f)
                .background(Color.Transparent),
            contentScale = ContentScale.Crop,
            loading = {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(strokeWidth = 2.dp)
                }
            },
            error = {
                Image(
                    painter = painterResource(R.drawable.default_img),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        )

        Spacer(modifier = Modifier.width(12.dp))    // 이미지와 텍스트 사이 간격

        Column(modifier = Modifier.weight(1f)) {
            Text(item.location, style = MaterialTheme.typography.titleMedium)
            Text(item.startedAt, style = MaterialTheme.typography.bodySmall)
            item.summary?.let {
                Text(it,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
            }
        }
    }
}