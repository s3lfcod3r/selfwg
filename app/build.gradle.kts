import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Release-Signatur aus keystore.properties (liegt im Projekt-Root, NICHT im
// Repo). Fehlt die Datei, signiert der Release-Build mit dem Debug-Keystore,
// damit nichts bricht, solange noch kein Release-Key erzeugt wurde.
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) load(FileInputStream(keystorePropsFile))
}

android {
    namespace = "com.selfwg.app"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.selfwg.app"
        minSdk = 26
        // targetSdk 34: vermeidet das erzwungene Edge-to-Edge von Android 15,
        // damit die System-Navigationsleiste nicht ueber der App liegt.
        targetSdk = 34
        versionCode = 6
        versionName = "1.2.3"
    }
    signingConfigs {
        // Debug-Keystore (Passwort "android" ist Android-Standard, kein Secret).
        create("self") {
            storeFile = rootProject.file("../debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        if (keystorePropsFile.exists()) {
            create("release") {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("self")
        }
        debug {
            signingConfig = signingConfigs.getByName("self")
        }
    }
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    packaging {
        resources { excludes += setOf("/META-INF/{AL2.0,LGPL2.1}") }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.security:security-crypto:1.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // WireGuard-Tunnel-Bibliothek (bringt den nativen Kern libwg-go.so mit).
    implementation("com.wireguard.android:tunnel:1.0.20230706")
    // Schlanker QR-Scanner fuer den WG-Easy-QR-Code.
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
}
