plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "org.ligi.passandroid"
    compileSdk = 37

    defaultConfig {
        applicationId = "org.ligi.passandroid"
        minSdk = 29
        targetSdk = 36
        versionCode = 373
        versionName = "3.7.3"
        testInstrumentationRunner = "org.ligi.passandroid.AppReplacingRunner"
        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-project.txt")
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

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
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
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.core.ktx)
    implementation(libs.coroutines.android)
    implementation(libs.datastore.preferences)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
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
