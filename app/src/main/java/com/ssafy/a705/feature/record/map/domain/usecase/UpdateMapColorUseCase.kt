package com.ssafy.a705.feature.record.map.domain.usecase

import com.ssafy.a705.feature.record.map.domain.repository.MapRepository
import javax.inject.Inject

class UpdateMapColorUseCase @Inject constructor(
    private val repo: MapRepository
) {
    suspend operator fun invoke(code: String, hex: String) {
        require(code.isNotBlank()) { "code required" }
        val clean = hex.removePrefix("#")
        require(clean.length == 6 || clean.length == 8) { "hex format" }
        repo.updateMapColor(code, "#$clean")
    }
}