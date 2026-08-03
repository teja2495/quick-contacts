plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.tk.quickcontacts"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.tk.quickcontacts"
        minSdk = 24
        targetSdk = 36
        versionCode = 15
        versionName = "2.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            buildConfigField("boolean", "DISABLE_RECENT_CALLS", "false")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        create("googlePlayRelease") {
            initWith(getByName("release"))
            matchingFallbacks += listOf("release")
            buildConfigField("boolean", "DISABLE_RECENT_CALLS", "true")
        }
        debug {
            buildConfigField("boolean", "DISABLE_RECENT_CALLS", "false")
            applicationIdSuffix = ".debug"
            isDebuggable = true
            isJniDebuggable = true
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }
    adbOptions {
        installOptions("--user", "0")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

tasks.register("app-release") {
    group = "build"
    description = "Builds the standard release APK."
    dependsOn("assembleRelease")
}

tasks.register("google-play-release") {
    group = "build"
    description = "Builds the Google Play APK and AAB without recent-calls access."
    dependsOn("assembleGooglePlayRelease", "bundleGooglePlayRelease")
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.coil.compose)
    implementation("com.google.code.gson:gson:2.10.1")
    implementation(libs.compose.reorderable)
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.libphonenumber)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
