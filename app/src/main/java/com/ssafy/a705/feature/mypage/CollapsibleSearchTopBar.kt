package com.ssafy.a705.feature.mypage

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CollapsibleSearchBarStable(
    title: String,
    searchExpanded: Boolean,
    onToggle: () -> Unit,      // 펼침/접기 토글
    query: String,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit = {},   // ← 화면 이탈 콜백 (항상 유지)
    onClear: () -> Unit = { onQueryChange("") }
) {
    val topInsets = WindowInsets.statusBars
    Surface(shadowElevation = 0.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(topInsets)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            AnimatedContent(
                targetState = searchExpanded,
                transitionSpec = {
                    (fadeIn(tween(150)) togetherWith fadeOut(tween(100)))
                        .using(SizeTransform(clip = false))
                },
                label = "search-anim"
            ) { expanded ->
                if (!expanded) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 기본 상태: ← (이탈), 가운데 타이틀, 오른쪽 🔍 (열기)
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "뒤로")
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onToggle) {
                            Icon(Icons.Default.Search, contentDescription = "검색 열기")
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 🔹 검색 펼친 상태에도 ←은 "화면 이탈"
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "뒤로")
                        }
                        Spacer(Modifier.width(8.dp))
                        BasicTextField(
                            value = query,
                            onValueChange = onQueryChange,
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color(0xFFE5E5E5))
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            textStyle = TextStyle(fontSize = 16.sp),
                            decorationBox = { inner ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray)
                                    Spacer(Modifier.width(6.dp))
                                    Box(Modifier.weight(1f)) { inner() }
                                }
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        // 🔹 우측 X는 검색 닫기만
                        IconButton(onClick = { onClear(); onToggle() }) {
                            Icon(Icons.Default.Close, contentDescription = "검색 닫기")
                        }
                    }
                }
            }
        }
    }
}
