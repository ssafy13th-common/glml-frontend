package com.ssafy.a705.feature.record.map.ui

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.a705.feature.record.map.domain.repository.MapRepository
import com.ssafy.a705.feature.record.map.domain.usecase.GetMapColorsUseCase
import com.ssafy.a705.feature.record.map.domain.usecase.UpdateMapColorUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
//    private val repository: MapRepository
    private val getColors: GetMapColorsUseCase,
    private val updateColor: UpdateMapColorUseCase
) : ViewModel() {

    private val _colorMap = mutableStateOf<Map<String, String>>(emptyMap())
    val colorMap: State<Map<String, String>> = _colorMap

    init {
        loadMapColors()
    }

    fun loadMapColors() {
        viewModelScope.launch {
            try {
                val result = getColors.invoke()  // 예: (지역코드 -> "#AABBCC")
                _colorMap.value = result
            } catch (e: Exception) {
                Log.e("MapViewModel", "Failed to load colors", e)
            }
        }
    }

    // code가 지역 코드, hex가 바꾼 색
    fun updateColor(code: String, hex: String) {
        val newColorWithoutHashtag = hex.removePrefix("#")
        val newColorHex = if(newColorWithoutHashtag.length == 8) newColorWithoutHashtag.drop(2) else newColorWithoutHashtag

        val prevColor = _colorMap.value[code] ?: "#FFFFFFFF"
        val alpha = if(prevColor == "#FFFFFFFF") "80" else prevColor.substring(1,3)
        val fixed = "#$alpha$newColorHex"

        _colorMap.value = _colorMap.value.toMutableMap().apply { this[code] = fixed }

        viewModelScope.launch {
            runCatching { updateColor.invoke(code, fixed) }
                .onFailure {
                    // 업데이트 실패 시 롤백
                    _colorMap.value = _colorMap.value.toMutableMap().apply {
                        if (prevColor == "#FFFFFFFF") remove(code) else put(code, prevColor)
                    }
                }
        }
    }
}
