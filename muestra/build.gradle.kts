plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// App de muestra para elegir el diseño del launcher en la propia unidad.
// La pantalla es la página muestra.html (generada con generar_muestra.py a partir del lienzo de diseño).
android {
    namespace = "com.radiocoche.muestra"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.radiocoche.muestra"
        minSdk = 21
        targetSdk = 34
        versionCode = 2
        versionName = "1.1"
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
