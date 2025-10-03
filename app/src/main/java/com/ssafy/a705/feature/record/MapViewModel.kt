package com.ssafy.a705.feature.record

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val repository: MapRepository
) : ViewModel() {

    private val _colorMap = mutableStateOf<Map<String, String>>(emptyMap())
    val colorMap: State<Map<String, String>> = _colorMap

    init {
        loadMapColors()
    }

    fun loadMapColors() {
        viewModelScope.launch {
            try {
                val result = repository.getMapColors()  // 예: (지역코드 -> "#AABBCC")
                _colorMap.value = result
            } catch (e: Exception) {
                Log.e("MapViewModel", "Failed to load colors", e)
            }
        }
    }

    // code가 지역 코드, hex가 바꾼 색
    fun updateColor(code: String, hex: String) {
        val newColorWithoutHashtag = if (hex.startsWith("#")) hex.substring(1) else hex
        val newColorHex = if(newColorWithoutHashtag.length == 8) hex.substring(3) else newColorWithoutHashtag

        val prevColor = _colorMap.value[code] ?: "#FFFFFFFF"
        val alpha = if(prevColor == "#FFFFFFFF") "80" else prevColor.substring(1,3)
        val fixed = "#$alpha$newColorHex"

        _colorMap.value = _colorMap.value.toMutableMap().apply { this[code] = fixed }

        viewModelScope.launch {
            runCatching { repository.updateMapColor(code, fixed) }
                .onFailure {
                    // 업데이트 실패 시 롤백
                    _colorMap.value = _colorMap.value.toMutableMap().apply {
                        if (prevColor == "#FFFFFFFF") remove(code) else put(code, prevColor)
                    }
                }
        }
    }
}
