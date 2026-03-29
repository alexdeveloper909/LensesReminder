import org.gradle.api.GradleException
import java.io.File
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

fun Project.optionalProperty(name: String): String? {
    return providers.gradleProperty(name).orNull?.takeIf { it.isNotBlank() }
}

fun firstNonBlank(vararg values: String?): String? = values.firstOrNull { !it.isNullOrBlank() }

val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("keystore.properties")

if (keystorePropertiesFile.isFile) {
    keystorePropertiesFile.inputStream().use { input ->
        keystoreProperties.load(input)
    }
}

val releaseKeystorePath = firstNonBlank(
    project.optionalProperty("releaseKeystorePath"),
    project.optionalProperty("android.injected.signing.store.file"),
    providers.environmentVariable("ANDROID_KEYSTORE_PATH").orNull,
    keystoreProperties.getProperty("storeFile")
)
val releaseKeystorePassword = firstNonBlank(
    project.optionalProperty("releaseKeystorePassword"),
    project.optionalProperty("android.injected.signing.store.password"),
    providers.environmentVariable("ANDROID_KEYSTORE_PASSWORD").orNull,
    keystoreProperties.getProperty("storePassword")
)
val releaseKeyAlias = firstNonBlank(
    project.optionalProperty("releaseKeyAlias"),
    project.optionalProperty("android.injected.signing.key.alias"),
    providers.environmentVariable("ANDROID_KEY_ALIAS").orNull,
    keystoreProperties.getProperty("keyAlias")
)
val releaseKeyPassword = firstNonBlank(
    project.optionalProperty("releaseKeyPassword"),
    project.optionalProperty("android.injected.signing.key.password"),
    providers.environmentVariable("ANDROID_KEY_PASSWORD").orNull,
    keystoreProperties.getProperty("keyPassword"),
    releaseKeystorePassword
)
val releaseSigningRequested = gradle.startParameter.taskNames.any { taskName ->
    taskName.contains("release", ignoreCase = true)
}

val missingReleaseSigningInputs = buildList {
    if (releaseKeystorePath == null) add("releaseKeystorePath / ANDROID_KEYSTORE_PATH")
    if (releaseKeystorePassword == null) add("releaseKeystorePassword / ANDROID_KEYSTORE_PASSWORD")
    if (releaseKeyAlias == null) add("releaseKeyAlias / ANDROID_KEY_ALIAS")
    if (releaseKeyPassword == null) add("releaseKeyPassword / ANDROID_KEY_PASSWORD")
}

if (releaseSigningRequested && missingReleaseSigningInputs.isNotEmpty()) {
    throw GradleException(
        "Release signing is not configured. Missing: ${missingReleaseSigningInputs.joinToString()}."
    )
}

android {
    namespace = "com.alex.lensesreminder"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.alex.lensesreminder"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val keystorePath = releaseKeystorePath
            if (keystorePath != null) {
                val keystoreFile = rootProject.file(keystorePath)
                if (releaseSigningRequested && !keystoreFile.isFile) {
                    throw GradleException(
                        "Release keystore file was not found at: $keystorePath"
                    )
                }
                storeFile = keystoreFile
            }
            storePassword = releaseKeystorePassword
            keyAlias = releaseKeyAlias
            keyPassword = releaseKeyPassword
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
        arg("room.generateKotlin", "true")
    }
}

dependencies {
    coreLibraryDesugaring(libs.androidx.core.desugaring)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.google.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    ksp(libs.androidx.room.compiler)
    ksp(libs.google.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.app.cash.turbine)
    testImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
