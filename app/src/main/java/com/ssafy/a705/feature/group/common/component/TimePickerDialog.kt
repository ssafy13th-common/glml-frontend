package com.ssafy.a705.feature.group.common.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    initialHour: Int = 12,
    initialMinute: Int = 30,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(
                modifier = Modifier
                    .background(Color.White)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val timeState = rememberTimePickerState(
                    initialHour = initialHour,
                    initialMinute = initialMinute,
                    is24Hour = false
                )

                TimePicker(state = timeState)

                Button(
                    onClick = {
                        val hour = timeState.hour
                        val minute = timeState.minute
                        val formattedTime = String.format(
                            "%02d:%02d %s",
                            if (hour % 12 == 0) 12 else hour % 12,
                            minute,
                            if (hour < 12) "AM" else "PM"
                        )
                        onConfirm(formattedTime)
                    },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text("확인")
                }
            }
        }
    }
}
