import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Machine-specific config (SDK path, backend URL, OAuth client ID) lives in
// local.properties, which is gitignored. See local.properties.example.
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.reader().use { localProperties.load(it) }
}

fun localProperty(name: String, default: String = ""): String =
    localProperties.getProperty(name)?.trim()?.removeSurrounding("\"") ?: default

// Release signing is optional at configuration time so debug builds never
// break on a fresh clone; only the assembleRelease task fails, with a clear
// message, if these are missing. See local.properties.example.
val releaseStoreFile = localProperty("RELEASE_STORE_FILE")
val releaseStorePassword = localProperty("RELEASE_STORE_PASSWORD")
val releaseKeyAlias = localProperty("RELEASE_KEY_ALIAS")
val releaseKeyPassword = localProperty("RELEASE_KEY_PASSWORD")
val hasReleaseSigning = releaseStoreFile.isNotBlank() &&
    releaseStorePassword.isNotBlank() &&
    releaseKeyAlias.isNotBlank() &&
    releaseKeyPassword.isNotBlank()

android {
    namespace = "com.example.cpen321application"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.mlam24.cpen321application"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Baked in at build time from local.properties — never hard-code URLs or
        // OAuth IDs in source. Emulator reaches the host via 10.0.2.2, not localhost.
        buildConfigField(
            "String",
            "API_BASE_URL",
            "\"${localProperty("API_BASE_URL", "http://10.0.2.2:3000")}\""
        )
        buildConfigField(
            "String",
            "GOOGLE_CLIENT_ID",
            "\"${localProperty("GOOGLE_CLIENT_ID")}\""
        )
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
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
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

// Pins the JDK used to compile Kotlin and Java so the build does not depend on
// whichever JDK happens to be on the developer's PATH. Gradle downloads this
// JDK if it is missing (see the foojay resolver in settings.gradle.kts).
kotlin {
    jvmToolchain(17)
}

afterEvaluate {
    if (!hasReleaseSigning) {
        tasks.matching { it.name == "assembleRelease" }.configureEach {
            doFirst {
                throw GradleException(
                    "Release signing config missing: set RELEASE_STORE_FILE, " +
                        "RELEASE_STORE_PASSWORD, RELEASE_KEY_ALIAS, and RELEASE_KEY_PASSWORD " +
                        "in local.properties (see local.properties.example)."
                )
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}