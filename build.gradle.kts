plugins {
    id("com.android.library") version "8.6.1"
    id("org.jetbrains.kotlin.android") version "1.9.25"
}

group = "community.openbase"
version = "0.2.0"

android {
    namespace = "community.openbase.allauth.client"
    compileSdk = 36

    defaultConfig {
        minSdk = 23
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        // Compose compiler extension compatible with Kotlin 1.9.25.
        kotlinCompilerExtensionVersion = "1.5.15"
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    api("com.squareup.okhttp3:okhttp:4.12.0")
    api("com.squareup.moshi:moshi:1.15.1")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

    // Jetpack Compose auth UI. Versions match the Openbase Android app
    // (Compose BOM 2025.01.00) so consumers do not get version skew.
    val composeBom = platform("androidx.compose:compose-bom:2025.01.00")
    api(composeBom)
    api("androidx.compose.ui:ui")
    api("androidx.compose.foundation:foundation")
    api("androidx.compose.material3:material3")
    api("androidx.compose.runtime:runtime")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
}
