plugins {
    id("com.android.library") version "8.6.1"
    id("org.jetbrains.kotlin.android") version "1.9.25"
}

group = "community.openbase"
version = "0.1.0"

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
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    api("com.squareup.okhttp3:okhttp:4.12.0")
    api("com.squareup.moshi:moshi:1.15.1")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
}
