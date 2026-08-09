plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

import java.util.Properties

val localProps = Properties().apply {
    val f = file("../local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

// Kredensial diambil dari ENV / local.properties (keduanya tidak masuk git).
// Tanpa password, release build tetap jalan dengan debug key (untuk CI/F-Droid).
val keystorePassword = System.getenv("RS_KEYSTORE_PASSWORD")
    ?: localProps.getProperty("keystore.password")
val apiToken = System.getenv("RS_API_TOKEN")
    ?: localProps.getProperty("api.token")
    ?: "".also { println("WARNING: RS_API_TOKEN/api.token kosong — sinkronisasi backend akan ditolak.") }

android {
    namespace = "com.rumahsehat"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.rumahsehat"
        minSdk = 23
        targetSdk = 35
        versionCode = 6
        versionName = "1.3.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }

        buildConfigField("String", "API_TOKEN", "\"$apiToken\"")
    }

    signingConfigs {
        create("release") {
            storeFile = file("../rumahsehat-upload.keystore")
            storePassword = keystorePassword
            keyAlias = "rumahsehat"
            keyPassword = keystorePassword
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            if (keystorePassword != null) {
                signingConfig = signingConfigs.getByName("release")
            } else {
                // ponytail: tanpa password keystore upload, fallback ke debug key
                // supaya APK tetap bisa di-install & di-index (ganti di local.properties
                // kalau mau cert asli: keystore.password=...)
                signingConfig = signingConfigs.getByName("debug")
            }
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    flavorDimensions += "role"
    productFlavors {
        create("user") {
            dimension = "role"
            applicationIdSuffix = ".user"
            versionNameSuffix = "-user"
        }
    }

    applicationVariants.all {
        resValue("string", "app_name", "Rumah Sehat")
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    sourceSets {
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.viewpager2)
    implementation(libs.material)
    
    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
