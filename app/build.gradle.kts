import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
}

val localPropsFile = rootProject.file("local.properties")
val localProps = Properties()
if (localPropsFile.exists()) {
    FileInputStream(localPropsFile).use { fis -> localProps.load(fis) }
}
val telegramToken: String = localProps.getProperty("TELEGRAM_BOT_TOKEN", "")
val telegramChat: String = localProps.getProperty("TELEGRAM_CHAT_ID", "")
val ipinfoToken: String = localProps.getProperty("IPINFO_TOKEN", "")
val googleMapsApiKey: String = localProps.getProperty("GOOGLE_MAPS_API_KEY", "")


android {
    namespace = "com.visualguard.finnalproject"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.visualguard.finnalproject"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "TELEGRAM_BOT_TOKEN", "\"$telegramToken\"")
        buildConfigField("String", "TELEGRAM_CHAT_ID", "\"$telegramChat\"")
        buildConfigField("String", "IPINFO_TOKEN", "\"$ipinfoToken\"")
        buildConfigField("String", "GOOGLE_MAPS_API_KEY", "\"$googleMapsApiKey\"")

    }

    buildFeatures {
        buildConfig = true
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation ("com.google.mlkit:text-recognition:16.0.1")


    implementation ("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:okhttp-sse:4.12.0")

    implementation ("androidx.media:media:1.7.0")

    implementation ("com.google.mlkit:barcode-scanning:17.2.0")

    implementation ("androidx.camera:camera-core:1.3.3")
    implementation ("androidx.camera:camera-camera2:1.3.3")
    implementation ("androidx.camera:camera-lifecycle:1.3.3")
    implementation ("androidx.camera:camera-view:1.3.3")

    implementation ("com.google.android.gms:play-services-location:21.0.1")
    implementation ("com.squareup.okhttp3:okhttp:4.12.0")

    // Google Maps - sử dụng version mới nhất
    implementation ("com.google.android.gms:play-services-maps:18.2.0")
    implementation ("com.google.android.gms:play-services-location:21.0.1")


}