package com.ssafy.a705.domain.group.common.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 사다리꼴 Shape 정의
private val CustomTabShape: Shape = object : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        return Outline.Generic(Path().apply {
            val width = size.width
            val height = size.height

            with(density) {
                val topToBottomWidthRatio = 4f / 5f
                val cornerRadius = 8.dp.toPx()

                val topWidth = width * topToBottomWidthRatio
                val skewAmount = (width - topWidth) / 2f

                // 상단 좌측 둥근 모서리 이후 시작
                moveTo(skewAmount + cornerRadius, 0f)
                // 상단 직선
                lineTo(width - skewAmount - cornerRadius, 0f)
                // 상단 우측 둥근 모서리
                arcTo(
                    rect = Rect(
                        left = width - skewAmount - cornerRadius * 2,
                        top = 0f,
                        right = width - skewAmount,
                        bottom = cornerRadius * 2
                    ),
                    startAngleDegrees = 270f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
                // 우측 기울어진 직선
                lineTo(width, height)
                // 하단 직선
                lineTo(0f, height)
                // 좌측 기울어진 직선
                lineTo(skewAmount, cornerRadius)
                // 좌측 상단 둥근 모서리
                arcTo(
                    rect = Rect(
                        left = skewAmount,
                        top = 0f,
                        right = skewAmount + cornerRadius * 2,
                        bottom = cornerRadius * 2
                    ),
                    startAngleDegrees = 180f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
                close()
            }
        })
    }
}

// 실제 사다리꼴 탭 컴포즈
@Composable
fun CustomFlagTab(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    backgroundColor: Color
) {
    val textColor = if (selected) Color.Black else Color.White
    val textSize = if(selected) 16.sp else 14.sp

    Box(
        modifier = modifier
            .width(if (selected) 93.dp else 87.dp)
            .height(if (selected) 45.dp else 41.dp)
            .clip(CustomTabShape)
            .background(backgroundColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = textSize,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = textColor
        )
    }
}
