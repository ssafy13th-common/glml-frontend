import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)

    alias(libs.plugins.devtools.ksp)
    alias(libs.plugins.dagger.hilt.android)

    kotlin("kapt")
}

android {
    namespace = "com.ssafy.a705"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ssafy.a705"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        val localProperties = Properties().apply {
            val localPropsFile = rootProject.file("local.properties")
            if (localPropsFile.exists()) {
                load(FileInputStream(localPropsFile))
            }
        }

        val kakaoNativeAppKey = localProperties.getProperty("KAKAO_NATIVE_APP_KEY") ?: ""
        val kakaoRestApiKey = localProperties.getProperty("KAKAO_REST_API_KEY") ?: ""

        defaultConfig {
            buildConfigField("String", "KAKAO_NATIVE_APP_KEY", "\"$kakaoNativeAppKey\"")
            buildConfigField("String", "KAKAO_REST_API_KEY", "\"$kakaoRestApiKey\"")
        }

        ndk {
            abiFilters.add("arm64-v8a")
            abiFilters.add("armeabi-v7a")
//            abiFilters.add("x86")
            abiFilters.add("x86_64")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.play.services.maps)
    implementation(libs.androidx.exifinterface)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging.interceptor)

    // 카카오 맵 SDK
    implementation(libs.kakao.map)  // Gradle에서 -을 .으로 자동 변환함
    // Gson : GeoJSON 구조화된 파싱 가능
    implementation(libs.gson)
    // Coil : 이미지 다운로드 및 로딩, 캐싱 지원
    implementation(libs.coil)
    implementation(libs.coil.compose)
    // KSP : 코드 생성 지원
    ksp(libs.hilt.compiler)
    // Hilt : 의존성 주입
    implementation(libs.hilt.android)
    // Hilt Navigation : Hilt로 ViewModel 주입 지원
    implementation(libs.hilt.navigation.compose)
    // Color Picker : 색상 선택
    implementation(libs.colorpicker.compose)
    // GMS Location : GPS 트래킹
    implementation(libs.gms.location)
    // 권한 요청 UI 제공
    implementation(libs.accompanist.permissions)
    // DataStore : 데이터 저장 - 앱 종료 시(stopService)에도 값을 저장해둠
    implementation(libs.datastore.preferences)
    // Paging : 페이지네이션
    implementation(libs.paging.runtime)
    implementation(libs.paging.compose)
    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    // Retrofit + Gson 변환 + 네트워크 로그 출력(OkHttp3)
    implementation(libs.retrofit)
//    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    // Hilt Navigation Compose
    implementation(libs.hilt.navigation.compose)
    // Paging : 페이지네이션
    implementation(libs.paging.runtime)
    implementation(libs.paging.compose)
    // Kakao 유저 sdk
    implementation(libs.kakao.user)
    // moshi 의존성 채팅
    implementation(libs.moshi)
    implementation(libs.moshi.kotlin)

}