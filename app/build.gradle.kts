plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Release signing comes from `~/.gradle/gradle.properties` (user-level, never committed).
// If the file isn't present we silently fall back to the debug keystore so CI / fresh
// clones still build; the resulting AAB is then only usable for local verification, not
// Play upload. See RELEASE.md for the four properties this looks for.
val releaseKeystoreFile = providers.gradleProperty("SPEAK2EASY_KEYSTORE_FILE").orNull

android {
    namespace = "com.sonari.speak2easy"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        // Installed package name on device — matches the Android OAuth client registered with
        // Google and the Play Store entry. Same value as `namespace` above (kept in sync).
        applicationId = "com.sonari.speak2easy"
        minSdk = 30
        targetSdk = 36
        versionCode = 4
        versionName = "1.0.4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (releaseKeystoreFile != null) {
            create("release") {
                storeFile = file(releaseKeystoreFile)
                storePassword = providers.gradleProperty("SPEAK2EASY_KEYSTORE_PASSWORD").get()
                keyAlias = providers.gradleProperty("SPEAK2EASY_KEY_ALIAS").get()
                keyPassword = providers.gradleProperty("SPEAK2EASY_KEY_PASSWORD").get()
            }
        }
    }

    buildTypes {
        release {
            // R8 obfuscates class/method/package names and strips dead code, so a decompiled
            // release APK reads like minified Java — much harder to reverse-engineer than the
            // 1:1 Kotlin you'd get with minify off. Resource shrinking removes unused drawables
            // and strings the optimizer can prove are unreachable.
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
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
    buildFeatures {
        compose = true
        // Needed for BuildConfig.DEBUG gating in AppContainer (HTTP body logging only in debug).
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.google.play.billing.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
