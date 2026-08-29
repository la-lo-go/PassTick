plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
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

    flavorDimensions += listOf("maps", "analytics", "distribution")
    productFlavors {
        create("withMaps") { dimension = "maps" }
        create("noMaps") { dimension = "maps" }
        create("withAnalytics") { dimension = "analytics" }
        create("noAnalytics") { dimension = "analytics" }
        create("forFDroid") { dimension = "distribution" }
        create("forPlay") { dimension = "distribution" }
        create("forAmazon") { dimension = "distribution" }
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
        viewBinding = true
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
        baseline = file("lint-baseline.xml")
        warning += setOf("MissingTranslation", "InvalidPackage")
        disable += "NullSafeMutableLiveData"
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

androidComponents {
    beforeVariants(selector().all()) { variant ->
        val flavors = variant.productFlavors.associate { it.first to it.second }
        val maps = flavors["maps"]
        val analytics = flavors["analytics"]
        val distribution = flavors["distribution"]
        variant.enable = when {
            distribution == "forFDroid" -> maps == "noMaps" && analytics == "noAnalytics"
            distribution == "forPlay" -> maps == "withMaps" && analytics == "withAnalytics"
            distribution == "forAmazon" -> maps == "withMaps" && analytics == "withAnalytics"
            else -> false
        }
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }
}

dependencies {
    compileOnly(libs.jsr305)
    implementation(platform(libs.compose.bom))
    androidTestImplementation(platform(libs.compose.bom))

    implementation(libs.activity.compose)
    implementation(libs.annotation)
    implementation(libs.appcompat)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.material3)
    implementation(libs.compose.material3.adaptive)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.core.ktx)
    implementation(libs.coroutines.android)
    implementation(libs.datastore.preferences)
    implementation(libs.fragment.ktx)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.kotpref)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.service)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.material)
    implementation(libs.moshi)
    implementation(libs.moshi.kotlin)
    implementation(libs.navigation3.runtime)
    implementation(libs.navigation3.ui)
    implementation(libs.okhttp)
    implementation(libs.okio)
    implementation(libs.preference.ktx)
    implementation(libs.threetenabp)
    implementation(libs.timber)
    implementation(libs.webkit)
    implementation(libs.zip4j)
    implementation(libs.zxing)

    implementation("com.github.permissions-dispatcher:permissionsdispatcher-ktx:4.8.0")
    implementation("com.github.ligi:ExtraCompats:1.0")
    implementation("com.github.ligi:KAXT:1.0")
    implementation("com.github.ligi:KAXTUI:1.0")
    implementation("com.github.ligi:tracedroid:4.1")
    implementation("com.larswerkman:HoloColorPicker:1.5")
    implementation("net.i2p.android.ext:floatingactionbutton:1.10.1") {
        exclude(group = "com.android.support", module = "support-v4")
    }

    add("forPlayImplementation", "com.github.ligi.snackengage:snackengage-playrate:0.30")
    add("forFDroidImplementation", "com.github.ligi.snackengage:snackengage-playrate:0.30")
    add("forAmazonImplementation", "com.github.ligi.snackengage:snackengage-amazonrate:0.30")
    add("withAnalyticsImplementation", "com.google.android.gms:play-services-analytics:18.1.1")
    add("withMapsImplementation", "com.google.android.gms:play-services-maps:19.2.0")

    testImplementation(libs.junit4)
    testImplementation(libs.assertj)
    testImplementation(libs.json)
    testImplementation(libs.mockito)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.threetenbp)

    androidTestImplementation(libs.compose.ui.test)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.test.core)
    androidTestImplementation(libs.test.ext.junit)
    debugImplementation(libs.compose.ui.test.manifest)
    debugImplementation(libs.compose.ui.tooling)
}
