package com.ssafy.a705.domain.sign

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.ssafy.a705.R

// 1. 처음 시작 화면 (로고 보여주고 자동 이동)
@Composable
fun OnboardingIntroScreen(
    onIntroFinished: () -> Unit
) {
    var step by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        repeat(4) {
            delay(700)
            step++
        }
        onIntroFinished()
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (step) {
            0 -> Image(painterResource(R.drawable.logo_empty), contentDescription = null, Modifier.size(200.dp))
            1 -> Image(painterResource(R.drawable.logo_galae), contentDescription = null, Modifier.size(200.dp))
            2 -> Image(painterResource(R.drawable.logo_malae), contentDescription = null, Modifier.size(200.dp))
            3 -> Image(painterResource(R.drawable.logo_galaemalae), contentDescription = null, Modifier.size(200.dp))
        }
    }
}