package com.ssafy.a705.feature.record.map.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity

@Composable
fun MapMenu(
    modifier: Modifier = Modifier,
    onWriteClick: () -> Unit,
    onListClick: () -> Unit,
    onColorClick: () -> Unit,
    isColored : Boolean,
    onMeasuredWidthDp: (Float) -> Unit = {}
) {
    val density = LocalDensity.current

    Box(modifier = modifier.zIndex(100f)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 말풍선 본체
            Row(
                modifier = Modifier
                    .wrapContentWidth()
                    .height(48.dp)
                    .background(color = Color(0xFF2D92FF), shape = RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp)
                    .onGloballyPositioned { coords ->
                        val wDp = with(density) { coords.size.width.toDp().value }
                        onMeasuredWidthDp(wDp)
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MenuTextButton("글 작성", onWriteClick)

                if(isColored) {
                    DividerLine()
                    MenuTextButton("글 목록", onListClick)
                    DividerLine()
                    MenuTextButton("색 변경", onColorClick)
                }
            }

            // 꼬리 삼각형
            Canvas(
                modifier = Modifier
                    .size(width = 20.dp, height = 10.dp)
            ) {
                val path = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width / 2f, size.height)
                    lineTo(size.width, 0f)
                    close()
                }
                drawPath(path = path, color = Color(0xFF2D92FF))
            }
        }
    }
}

@Composable
fun MenuTextButton(text: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
        colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
fun DividerLine() {
    Spacer(
        modifier = Modifier
            .width(1.dp)
            .height(24.dp)
            .background(Color.White.copy(alpha = 0.5f))
    )
}
