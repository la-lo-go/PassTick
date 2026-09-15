import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

// Release signing values come from the git-ignored root keystore.properties file or from
// PASSTICK_KEYSTORE_FILE, PASSTICK_KEYSTORE_PASSWORD, PASSTICK_KEY_ALIAS, and PASSTICK_KEY_PASSWORD.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use(::load)
}

fun releaseSigningValue(property: String, environment: String): String? =
    keystoreProperties.getProperty(property)?.takeIf(String::isNotBlank)
        ?: System.getenv(environment)?.takeIf(String::isNotBlank)

val releaseStoreFile = releaseSigningValue("storeFile", "PASSTICK_KEYSTORE_FILE")
val releaseStorePassword = releaseSigningValue("storePassword", "PASSTICK_KEYSTORE_PASSWORD")
val releaseKeyAlias = releaseSigningValue("keyAlias", "PASSTICK_KEY_ALIAS")
val releaseKeyPassword = releaseSigningValue("keyPassword", "PASSTICK_KEY_PASSWORD")
val hasReleaseSigning = listOf(
    releaseStoreFile,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { it != null }

android {
    namespace = "org.ligi.passandroid"
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.lalogo.passtick"
        minSdk = 29
        targetSdk = 36
        versionCode = 2
        versionName = "0.1.1"
        testInstrumentationRunner = "org.ligi.passandroid.AppReplacingRunner"
        vectorDrawables.useSupportLibrary = true
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(requireNotNull(releaseStoreFile))
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-project.txt")
            signingConfig = signingConfigs.findByName("release")
        }
        debug {
            isDebuggable = true
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
        viewBinding = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources.excludes += setOf(
            "asm-license.txt",
            "LICENSE",
            "LICENSE.txt",
            "NOTICE",
            "META-INF/maven/com.google.guava/guava/pom.properties",
            "META-INF/maven/com.google.guava/guava/pom.xml",
            "META-INF/lib_release.kotlin_module",
        )
    }

    lint {
        warning += setOf("MissingTranslation", "InvalidPackage")
        disable += "NullSafeMutableLiveData"
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    androidTestImplementation(platform(libs.compose.bom))

    implementation(libs.activity.compose)
    implementation(libs.annotation)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.material3)
    implementation(libs.compose.material3.adaptive)
    implementation(libs.compose.material3.adaptive.layout)
    implementation(libs.compose.material3.adaptive.navigation)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.core.ktx)
    implementation(libs.coroutines.android)
    implementation(libs.datastore.preferences)
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.material.kolor) {
        // Material Kolor ships JetBrains Compose wrappers for prebuilt artifacts; this app builds
        // against the pinned androidx.compose.material3 only.
        exclude(group = "org.jetbrains.compose.material3")
    }
    implementation(libs.moshi)
    implementation(libs.moshi.kotlin)
    implementation(libs.navigation3.runtime)
    implementation(libs.navigation3.ui)
    implementation(libs.okio)
    implementation(libs.threetenabp)
    implementation(libs.timber)
    implementation(libs.zip4j)
    implementation(libs.zxing)

    testImplementation(libs.junit4)
    testImplementation(libs.assertj)
    testImplementation(libs.json)
    testImplementation(libs.mockito)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.threetenbp)

    androidTestImplementation(libs.compose.ui.test)
    androidTestImplementation(libs.assertj)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.mockito)
    androidTestImplementation(libs.threetenbp)
    androidTestImplementation(libs.test.core)
    androidTestImplementation(libs.test.ext.junit)
    androidTestImplementation("com.linkedin.dexmaker:dexmaker-mockito:2.28.4")
    debugImplementation(libs.compose.ui.test.manifest)
    debugImplementation(libs.compose.ui.tooling)
}

val detektCli by configurations.creating

dependencies {
    detektCli(libs.detekt.cli)
}

tasks.register<JavaExec>("detektComplexity") {
    group = "verification"
    description = "Checks Kotlin cyclomatic and cognitive complexity."
    classpath = detektCli
    mainClass.set("io.gitlab.arturbosch.detekt.cli.Main")
    val detektInput = providers.gradleProperty("detektInput")
        .orElse(projectDir.resolve("src/main/java").absolutePath)
    args(
        "--input", detektInput.get(),
        "--config", "${rootProject.projectDir}/config/detekt-complexity.yml",
        "--report", "txt:${layout.buildDirectory.get()}/reports/detekt/complexity.txt",
    )
}
