package com.ssafy.a705.global.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.graphics.toArgb
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController

@Composable
fun ColorPickerDialog(
    initialColor: Color = Color.White,
    onDismiss: () -> Unit,
    onColorSelected: (String) -> Unit
) {
    var selectedColor by remember { mutableStateOf(initialColor) }
    val controller = rememberColorPickerController()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("색상 선택") },
        text = {
            Column {
                HsvColorPicker(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    controller = controller,
                    onColorChanged = {
                        selectedColor = it.color
                    }
                )
                Text(
                    text = "선택된 색: #${Integer.toHexString(selectedColor.toArgb()).uppercase()}",
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val hex = "#%08X".format(selectedColor.toArgb())
                onColorSelected(hex)
            }) {
                Text("확인")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}
