plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.example.myapplication"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildToolsVersion = "35.0.0"
}

dependencies {
    val roomVersion = "2.6.1"
    val hiltVersion = rootProject.extra["hilt_version"].toString()

    implementation(libs.androidx.core.ktx.v1120)
    implementation(platform(libs.kotlin.bom))
    implementation(libs.androidx.lifecycle.runtime.ktx.v270)
    implementation(libs.androidx.activity.compose.v182)

    // Compose BOM
    implementation(platform(libs.androidx.compose.bom.v20240200))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.material3)
    implementation(libs.androidx.material.icons.extended)

    // Room
    implementation(libs.androidx.room.runtime)
    kapt("androidx.room:room-compiler:$roomVersion")
    implementation(libs.androidx.room.ktx)

    // Lifecycle components
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Kotlin Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Hilt
    implementation(libs.hilt.android)
    kapt("com.google.dagger:hilt-compiler:$hiltVersion")
    implementation(libs.androidx.hilt.navigation.compose)

    // WorkManager with Hilt
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    kapt(libs.androidx.hilt.compiler)
    kapt("com.google.dagger:hilt-android-compiler:$hiltVersion")

    // DataStore
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.datastore.preferences.core)

    // Retrofit & Gson
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.gson)

    // OkHttp
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit.v115)
    androidTestImplementation(libs.androidx.espresso.core.v351)
    androidTestImplementation(platform(libs.androidx.compose.bom.v20240200))
    androidTestImplementation(libs.ui.test.junit4)
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)

    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(libs.androidx.material3.window)
}