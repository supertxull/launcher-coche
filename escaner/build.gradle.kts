plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Escáner del coche: escucha (sin enviar nada) los mensajes del MCU, avisos de fábrica y ajustes.

android {
    namespace = "com.radiocoche.escaner"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.radiocoche.escaner"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        create("coche") {
            storeFile = file("../app/radio.keystore")
            storePassword = "radiocoche"
            keyAlias = "radio"
            keyPassword = "radiocoche"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("coche")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("coche")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}
