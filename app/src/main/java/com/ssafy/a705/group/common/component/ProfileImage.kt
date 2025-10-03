package com.ssafy.a705.group.common.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun ProfileImage(url: String, modifier: Modifier = Modifier) {
    AsyncImage(
        model = url,
        contentDescription = "Member profile",
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(Color.LightGray),
        contentScale = ContentScale.Crop
    )
}
