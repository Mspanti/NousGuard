// app/build.gradle.kts
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt") // For Room Persistence Library annotation processing
}

android {
    namespace = "pant.com.nousguard"
    compileSdk = 35 // Already updated to 35

    defaultConfig {
        applicationId = "pant.com.nousguard"
        minSdk = 26 // Already updated to 26
        targetSdk = 35 // Already updated to 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14" // Updated for Kotlin 1.9.24
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    // Keep this block to explicitly force the version, just in case.
    // This should help resolve the biometric-ktx:1.1.0 issue.

}

dependencies {

    // 1. Android KTX and Core Libraries
    implementation("androidx.core:core-ktx:1.13.1") // Latest stable
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3") // Latest stable
    implementation("androidx.activity:activity-compose:1.9.0") // Latest stable

    // 2. Jetpack Compose UI Components
    // BOM (Bill of Materials) for Compose dependencies - ensures all Compose libs use compatible versions
    implementation(platform("androidx.compose:compose-bom:2024.04.00")) // Example BOM version
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    // Lifecycle ViewModel KTX for viewModel() composable
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.3") // Updated to latest stable
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.biometric.ktx)
    implementation(libs.androidx.navigation.common.android)
    implementation(libs.androidx.foundation.layout.android)
    implementation(libs.generativeai)
    implementation(libs.androidx.navigation.compose.android) // Updated to latest stable
    kapt("androidx.lifecycle:lifecycle-compiler:2.8.3") // Updated to latest stable
    implementation("androidx.compose.material:material-icons-extended:1.6.8")
    // 3. TensorFlow Lite (On-Device ML)
    // Core TFLite library for running ML models on device
    implementation ("org.tensorflow:tensorflow-lite-select-tf-ops:2.15.0")
    implementation("org.tensorflow:tensorflow-lite:2.16.1") // Latest stable (as per previous)
    // TFLite GPU delegate (optional, for GPU acceleration if available on device)
    implementation("org.tensorflow:tensorflow-lite-gpu:2.16.1") // Latest stable (as per previous)
    // TFLite Text Task Library (for text-specific ML tasks like sentiment analysis, NLP)
    implementation("org.tensorflow:tensorflow-lite-task-text:0.4.0") // Latest stable (as per previous)
    implementation("androidx.biometric:biometric-ktx:1.4.0-alpha02")
    // 4. Room Persistence Library (Local Database)
    implementation("androidx.room:room-runtime:2.6.1") // Latest stable
    kapt("androidx.room:room-compiler:2.6.1") // Latest stable
    implementation("androidx.room:room-ktx:2.6.1") // Latest stable

    implementation ("com.google.code.gson:gson:2.10.1")

    // Removed: implementation(libs.androidx.foundation.layout.android)
    // This is part of androidx.compose.foundation, which is already pulled in by compose-bom.
    // Explicitly adding it is redundant and can cause issues if versions mismatch.

    // 5. SQLCipher (for Encrypted SQLite Database)
    implementation("net.zetetic:android-database-sqlcipher:4.5.3") // Latest stable (as per previous)
    implementation("androidx.sqlite:sqlite-ktx:2.4.0") // Latest stable


    // 7. Kotlin Coroutines (for Asynchronous Programming)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0") // Latest stable
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0") // Latest stable

    // 8. Testing Dependencies (for unit and UI tests)
    testImplementation("junit:junit:4.13.2") // Latest stable
    androidTestImplementation("androidx.test.ext:junit:1.1.5") // Latest stable
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1") // Latest stable
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.04.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4") // Latest stable
    debugImplementation("androidx.compose.ui:ui-tooling") // Latest stable
    // Removed: debugImplementation(libs.ui.test.manifest)
    // This is part of androidx.compose.ui:ui-test-manifest, which is already pulled in by ui-test-junit4
    // or ui-tooling in debug builds. Explicitly adding it is redundant.
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}