package com.ssafy.a705.global.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

// 공용 액션 모델
data class MenuAction(
    val label: String,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
    val isDestructive: Boolean = false
)

// 좌우 48.dp 슬롯 확보로 제목 중앙 정렬 고정
@Composable
fun HeaderRow(
    modifier: Modifier = Modifier,
    text: String = "",
    showText: Boolean = true,
    showLeftButton: Boolean = false,
    onLeftClick: () -> Unit = {},
    menuActions: List<MenuAction> = emptyList()
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Leading slot: 항상 48dp 폭 확보
        Box(
            modifier = Modifier.width(48.dp),
            contentAlignment = Alignment.Center
        ) {
            if (showLeftButton) {
                IconButton(onClick = onLeftClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                }
            }
        }

        // Title
        if (showText) {
            Text(
                text = text,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        } else {
            Spacer(Modifier.weight(1f))
        }

        // Trailing slot
        when {
            // 메뉴 1개: TextButton (필요 시 widthIn(min=48.dp)로 감싸 균형 강제 가능)
            menuActions.size == 1 -> {
                val action by rememberUpdatedState(menuActions.first())
                // TextButton 폭이 가변이라도 좌우 48dp 대칭이 크게 깨지지 않음.
                // 더 엄격히 맞추고 싶다면 Box(Modifier.width(48.dp))로 고정 후 아이콘/짧은 라벨 사용 권장.
                TextButton(
                    onClick = action.onClick,
                    enabled = action.enabled
                ) {
                    Text(
                        text = action.label,
                        color = if (action.isDestructive)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary
                    )
                }
            }

            // 메뉴 여러 개: ⋮ + Dropdown (48dp 슬롯)
            menuActions.isNotEmpty() -> {
                Box(
                    modifier = Modifier.width(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "메뉴")
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        val latest by rememberUpdatedState(menuActions)
                        latest.forEach { action ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        action.label,
                                        color = if (action.isDestructive)
                                            MaterialTheme.colorScheme.error
                                        else
                                            LocalContentColor.current
                                    )
                                },
                                onClick = { expanded = false; action.onClick() },
                                enabled = action.enabled
                            )
                        }
                    }
                }
            }

            // 메뉴 없음: Trailing도 48dp 확보 → 완전 대칭
            else -> {
                Spacer(Modifier.width(48.dp))
            }
        }
    }
}
