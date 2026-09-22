import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    // Firebase config processing. Safe to keep enabled; requires google-services.json.
    alias(libs.plugins.google.services)
}

// Read the API base URL and (optionally) the signing config from local files
// that are NOT committed to source control.
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val apiBaseUrl: String = (localProps.getProperty("KINPLUS_API_BASE_URL")
    ?: "https://kinplus-api.azurewebsites.net/")

android {
    namespace = "za.co.kinplus.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "za.co.kinplus.app"
        minSdk = 26          // NFR-06: Android 8.0 and above
        targetSdk = 34
        versionCode = 2      // Part 2 build (PoE feature set); bumped from Part 1 planning
        versionName = "1.0-poe"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Exposed to code as BuildConfig.API_BASE_URL so the endpoint is not hard-coded.
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")

        // The Android OAuth client's server (web) client id, used by Google SSO.
        // Provide it in local.properties; a blank default keeps debug builds compiling.
        buildConfigField(
            "String",
            "GOOGLE_WEB_CLIENT_ID",
            "\"${localProps.getProperty("KINPLUS_GOOGLE_WEB_CLIENT_ID") ?: ""}\""
        )

        // Google Maps API key (Home screen). Provide it in local.properties as
        // KINPLUS_GOOGLE_MAPS_API_KEY; a blank value means the map tiles won't
        // load, but the app still builds and runs.
        manifestPlaceholders["MAPS_API_KEY"] = localProps.getProperty("KINPLUS_GOOGLE_MAPS_API_KEY") ?: ""
    }

    // Release signing is read from keystore.properties when present (see PLAY_STORE.md).
    val keystoreProps = Properties().apply {
        val f = rootProject.file("keystore.properties")
        if (f.exists()) f.inputStream().use { load(it) }
    }
    signingConfigs {
        if (keystoreProps.isNotEmpty()) {
            create("release") {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            // No applicationIdSuffix: keeps a single Firebase client (google-services.json) valid.
            isDebuggable = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (keystoreProps.isNotEmpty()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        // Opt in once, project-wide, to the experimental Compose APIs used across
        // the screens (Material 3 top bars, FlowRow, coroutine flow operators).
        freeCompilerArgs = freeCompilerArgs + listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
        )
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    // ----- Core / lifecycle -----
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // ----- Jetpack Compose (BOM keeps versions aligned) -----
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // ----- Dependency injection (Hilt) -----
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    // ----- Networking (Retrofit + OkHttp) -----
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // ----- Offline cache + background sync -----
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.work.runtime.ktx)
    implementation(libs.datastore.preferences)
    implementation(libs.security.crypto)

    // ----- Firebase (Auth + Cloud Messaging) -----
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.analytics)

    // ----- Google SSO (Credential Manager) + location + maps -----
    implementation(libs.play.services.auth)
    implementation(libs.play.services.location)
    implementation(libs.play.services.maps)
    implementation(libs.maps.compose)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)

    // ----- Coroutines -----
    implementation(libs.coroutines.android)
    implementation(libs.coroutines.play.services)

    // ----- Unit tests -----
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.coroutines.test)

    // ----- Instrumented tests -----
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
}
