package com.ssafy.a705.sign

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ssafy.a705.controller.viewmodel.PhoneVerifyViewModel
import com.ssafy.a705.navigation.Screen

@Composable
fun PhoneVerifyScreen(
    nextRoute: String? = null,
    onVerified: (String?) -> Unit,
    vm: PhoneVerifyViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(ui.error) { ui.error?.let { snackbar.showSnackbar(it) } }
    LaunchedEffect(ui.message) { ui.message?.let { snackbar.showSnackbar(it) } }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .padding(20.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("전화번호 인증", style = MaterialTheme.typography.titleLarge)

            OutlinedTextField(
                value = ui.phone,
                onValueChange = vm::setPhone,
                label = { Text("전화번호 (숫자만)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { vm.sendSms() },
                    enabled = !ui.sending && !ui.sent,
                ) {
                    if (ui.sending) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text("인증번호 전송")
                }

                if (ui.sent) {
                    Text(if (ui.secondsLeft > 0) "남은 시간 ${ui.secondsLeft}s" else "만료됨")
                    TextButton(
                        onClick = { vm.resend() },
                        enabled = ui.secondsLeft == 0
                    ) { Text("재전송") }
                }
            }

            if (ui.sent) {
                OutlinedTextField(
                    value = ui.code,
                    onValueChange = vm::setCode,
                    label = { Text("인증코드 6자리") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        vm.verify {
                        val safeRoute = nextRoute?.takeIf { it.isNotBlank() } ?: Screen.With.route
                        onVerified(safeRoute)
                        }
                              },
                    enabled = !ui.verifying && ui.code.length == 6,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (ui.verifying) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text("인증하기")
                }
            }

            Spacer(Modifier.weight(1f))
        }
    }
}