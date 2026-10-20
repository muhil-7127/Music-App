import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
}

// Apply Google Services only if google-services.json exists in this module
val hasGoogleServicesConfig = file("google-services.json").exists()
if (hasGoogleServicesConfig) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "com.muhil.music"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.muhil.music"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    signingConfigs {
        getByName("debug") {
            keyAlias = "androiddebugkey"
            keyPassword = "android"
            storePassword = "android"
            storeFile = file("${System.getProperty("user.home")}/.android/debug.keystore")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        jvmToolchain(21)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/NOTICE.md"
            excludes += "META-INF/LICENSE.md"
        }
    }

    lint {
        abortOnError = false
    }
}

dependencies {
    // Core AndroidX
    coreLibraryDesugaring(libs.desugaring)
    implementation(libs.activity)
    implementation(libs.appcompat)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.datastore)

    // Compose
    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.util)
    implementation(libs.compose.ui.tooling)
    implementation(libs.compose.animation)
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("androidx.navigation:navigation-compose:2.9.0")

    // Material 3
    implementation(libs.material3)
    implementation(libs.palette)

    // Lifecycle / ViewModel
    implementation(libs.viewmodel)
    implementation(libs.viewmodel.compose)
    implementation(libs.lifecycle.process)

    // Hilt DI
    implementation(libs.hilt)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation)

    // Media3 ExoPlayer + MediaSession
    implementation(libs.media3)
    implementation(libs.media3.session)
    implementation(libs.media3.ui)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:34.19.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")

    // Image loading
    implementation(libs.coil)
    implementation(libs.coil.network.okhttp)

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation(libs.coroutines.guava)
    implementation(libs.concurrent.futures)

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")

    // Shimmer
    implementation(libs.shimmer)

    // Timber logging
    implementation(libs.timber)

    // Lottie animations
    implementation(libs.lottie.compose)

    // Haze (blurred backgrounds)
    implementation(libs.haze)

    // Guava
    implementation(libs.guava)

    testImplementation(libs.junit)
}
