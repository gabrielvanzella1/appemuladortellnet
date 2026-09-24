import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

// Credenciais de assinatura de release — arquivo local, nunca versionado (ver .gitignore).
// Sem esse arquivo (ex.: outra maquina/CI sem a keystore), o release simplesmente
// nao fica assinado em vez de quebrar o build.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.logisticapp.emuladortelnet"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.logisticapp.emuladortelnet"
        minSdk = 24
        targetSdk = 34
        versionCode = 4
        versionName = "1.1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
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
