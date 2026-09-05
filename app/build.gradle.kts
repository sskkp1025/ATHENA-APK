plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.athenaz.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.athenaz.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    // 안드로이드 기본 코어 및 UI 라이브러리
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    
    // 🚀 Vultr 서버 통신(SSH) 및 비동기 처리를 위한 라이브러리
    implementation("com.jcraft:jsch:0.1.55")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}
