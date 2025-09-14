plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")

}

android {
    namespace = "fr.ikao.IkaoParis"
    compileSdk = 36

    defaultConfig {
        applicationId = "fr.ikao.IkaoParis"
        minSdk = 24
        targetSdk = 36
        versionCode = 2
        versionName = "1.0"

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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.crashlytics.buildtools)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Import the Firebase BoM
    implementation(platform(libs.firebase.bom))

    // When using the BoM, don't specify versions in Firebase dependencies
    implementation(libs.com.google.firebase.firebase.analytics)
    // Import Firebase FCM (Firebase Cloud Messaging
    implementation(libs.google.firebase.messaging)

    implementation (libs.firebase.config.ktx)
    implementation (libs.firebase.analytics.ktx)


    // Add the dependencies for any other desired Firebase products
    // https://firebase.google.com/docs/android/setup#available-libraries
}