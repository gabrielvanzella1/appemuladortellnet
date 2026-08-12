plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.logisticapp.emuladortelnet"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.logisticapp.emuladortelnet"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    flavorDimensions += "env"
    productFlavors {
        create("production") {
            dimension = "env"
            buildConfigField("String", "BASE_URL", "\"https://scante.com.br/scante-admin/public\"")
            buildConfigField("String", "API_SECRET", "\"eab28be7c8536e7f5979e5a46b5ec65ba34fdf891d23c5f772e65a4d07057faf\"")
            resValue("string", "app_name", "ScanTE")
        }
        create("sandbox") {
            dimension = "env"
            applicationIdSuffix = ".sandbox"
            versionNameSuffix = "-sandbox"
            buildConfigField("String", "BASE_URL", "\"https://sandbox.scante.com.br/scante-admin/public\"")
            buildConfigField("String", "API_SECRET", "\"c9ce24d6a1e9a583036278736526a82d144e9955b04c9ea490f215f230f592cf\"")
            resValue("string", "app_name", "ScanTE Sandbox")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    kotlin {
        jvmToolchain {
            languageVersion.set(JavaLanguageVersion.of(11))
        }
    }

    buildFeatures {
        compose = false
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    // Android Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Lifecycle
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")

    // Network
    implementation("com.squareup.okhttp3:okhttp:4.11.0")

    // SSH
    implementation("com.jcraft:jsch:0.1.55")

    // JSON
    implementation("com.google.code.gson:gson:2.10.1")

    // Logging
    implementation("com.jakewharton.timber:timber:5.0.1")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}
