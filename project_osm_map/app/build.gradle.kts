plugins {
    id("com.android.application")
}

android {
    namespace = "ru.mpavel.osmmap"
    compileSdk = 37

    defaultConfig {
        applicationId = "ru.mpavel.osmmap"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("org.osmdroid:osmdroid-android:6.1.20")
}
