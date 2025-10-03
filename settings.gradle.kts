pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // 카카오맵 SDK 저장소
        maven(url = uri("https://devrepo.kakao.com/nexus/repository/kakaomap-releases/"))
        // 카카오 로그인 저장소
        maven(url = uri("https://devrepo.kakao.com/nexus/content/groups/public/"))
    }
}

rootProject.name = "A705"
include(":app")
