// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false

    // KSP : 주석 프로세서
    alias(libs.plugins.devtools.ksp) apply false
    // Dagger : 의존성 주입
    alias(libs.plugins.dagger.hilt.android) apply false
}