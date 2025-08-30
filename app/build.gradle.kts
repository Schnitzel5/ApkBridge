import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

val keyAlias: String = gradleLocalProperties(rootDir, providers).getProperty("keyAlias")
val keyPassword: String = gradleLocalProperties(rootDir, providers).getProperty("keyPassword")
val storeFile: String = gradleLocalProperties(rootDir, providers).getProperty("storeFile")
val storePassword: String = gradleLocalProperties(rootDir, providers).getProperty("storePassword")

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "me.schnitzel.apkbridge"
    compileSdk = 36

    defaultConfig {
        applicationId = "me.schnitzel.apkbridge"
        minSdk = 26
        targetSdk = 36
        versionCode = 7
        versionName = "1.6"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            keyAlias = keyAlias
            keyPassword = keyPassword
            storeFile = storeFile?.let { file(it) }
            storePassword = storePassword
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs += "-Xcontext-receivers"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.nanohttpd)
    implementation(libs.navigation.compose)
    implementation(libs.nanohttpd.nanolets)
    implementation(libs.injekt)
    implementation(libs.jsoup)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.json.okio)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.logging.interceptor)
    implementation(libs.okhttp)
    implementation(libs.okhttp.brotli)
    implementation(libs.okio)
    implementation(libs.rxjava)
    implementation(libs.rxandroid)
    implementation(libs.injekt)
    implementation(libs.jackson)
    implementation(libs.jackson.kotlin)
    implementation(libs.androidx.core.ktx)
    implementation(libs.preference)
    implementation(libs.preference.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}