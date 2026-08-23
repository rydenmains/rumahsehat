plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

import java.util.Properties

val localProps = Properties().apply {
    val f = file("../local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

// Kredensial diambil dari ENV / local.properties (keduanya tidak masuk git).
// Release build WAJIB gagal kalau password keystore tidak tersedia (tidak ada fallback debug key).
val keystorePassword = System.getenv("RS_KEYSTORE_PASSWORD")
    ?: localProps.getProperty("keystore.password")
val apiToken = System.getenv("RS_API_TOKEN")
    ?: localProps.getProperty("api.token")
    ?: "".also { println("WARNING: RS_API_TOKEN/api.token kosong — sinkronisasi backend akan ditolak.") }
// DSN Sentry opsional. Kosong = crash reporter mati (no-op), app tetap jalan normal.
val sentryDsn = System.getenv("SENTRY_DSN")
    ?: localProps.getProperty("sentry.dsn")
    ?: ""

android {
    namespace = "com.rumahsehat"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.rumahsehat"
        minSdk = 23
        targetSdk = 35
        versionCode = 9
        versionName = "1.6.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }

        buildConfigField("String", "API_TOKEN", "\"$apiToken\"")
        buildConfigField("String", "SENTRY_DSN", "\"$sentryDsn\"")
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
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    afterEvaluate {
        gradle.taskGraph.whenReady {
            val buildsRelease = allTasks.any { it.project.name == "app" && it.name.contains("Release") }
            if (buildsRelease && keystorePassword == null) {
                throw GradleException(
                    "Release build membutuhkan RS_KEYSTORE_PASSWORD / keystore.password di local.properties"
                )
            }
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
        compose = true
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

    // Crash reporting (aktif hanya kalau SENTRY_DSN diisi)
    implementation(libs.sentry.android)

    // Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.runtime.livedata)
    implementation(libs.androidx.activity.compose)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
