package com.ssafy.a705.feature.mypage

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun PaginationBar( // ← public
    currentPage: Int,
    totalPages: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (totalPages <= 1) return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        (0 until totalPages).forEach { idx ->
            val selected = idx == currentPage
            TextButton(
                onClick = { onSelect(idx) },
                colors = ButtonDefaults.textButtonColors(
                    containerColor = if (selected) Color(0xFF1F3B73) else Color.Transparent,
                    contentColor = if (selected) Color.White else LocalContentColor.current
                ),
                modifier = Modifier.padding(horizontal = 4.dp)
            ) { Text("${idx + 1}") }
        }
    }
}