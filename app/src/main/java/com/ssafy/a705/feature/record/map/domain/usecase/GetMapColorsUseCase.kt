package com.ssafy.a705.feature.record.map.domain.usecase

import com.ssafy.a705.feature.record.map.domain.repository.MapRepository
import javax.inject.Inject

class GetMapColorsUseCase @Inject constructor(
    private val repo: MapRepository
) {
    suspend operator fun invoke(): Map<String, String> =
        repo.getMapColors()
}