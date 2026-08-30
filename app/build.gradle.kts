plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.dateswitcher"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.dateswitcher"
        minSdk = 26
        targetSdk = 37
        versionCode = 2
        versionName = "2.0"
    }
}
