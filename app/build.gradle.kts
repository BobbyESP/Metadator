plugins {
    id("metadator.android.application")
    id("metadator.android.compose")
}

// Firebase (crash reports) only where its configuration is: a FOSS build, or a fork without the
// file, builds and runs without it.
val hasGoogleServices =
    file("google-services.json").exists() || file("src/playstore/google-services.json").exists()

if (hasGoogleServices) {
    apply(plugin = libs.plugins.google.gms.get().pluginId)
    apply(plugin = libs.plugins.firebase.crashlytics.get().pluginId)
}

val signingStorePath: String? = System.getenv("SIGNING_KEY_STORE_PATH")

android {
    namespace = "com.bobbyesp.metadator"

    defaultConfig {
        applicationId = "com.bobbyesp.metadator"
        versionCode = rootProject.extra["versionCode"] as Int
        versionName = rootProject.extra["versionName"] as String
    }

    signingConfigs {
        if (signingStorePath != null) {
            create("release") {
                storeFile = file(signingStorePath)
                storePassword = System.getenv("SIGNING_STORE_PASSWORD")
                keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (signingStorePath != null) signingConfig = signingConfigs.getByName("release")
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("playstore") { dimension = "distribution" }
        create("foss") { dimension = "distribution" }
    }

    buildFeatures { buildConfig = true }

    androidResources { generateLocaleConfig = true }

    dependenciesInfo {
        // F-Droid cannot read the signed blob Google adds.
        includeInApk = false
        includeInBundle = false
    }

    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

dependencies {
    // Core
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:database"))
    implementation(project(":core:network"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:ui"))
    implementation(project(":core:navigation"))

    // Engines: the one place implementations are named.
    implementation(project(":tags:taglib"))
    implementation(project(":library:mediastore"))
    implementation(project(":lookup:musicbrainz"))
    implementation(project(":lookup:deezer"))
    implementation(project(":lyrics:lrclib"))
    implementation(project(":player:media3"))

    // Features
    implementation(project(":feature:library"))
    implementation(project(":feature:editor"))
    implementation(project(":feature:batch"))
    implementation(project(":feature:player"))
    implementation(project(":feature:settings"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.bundles.lifecycle)
    implementation(libs.bundles.navigation3)
    implementation(libs.bundles.koin.android)
    implementation(libs.bundles.coroutines)
    implementation(libs.androidx.profileinstaller)

    "playstoreImplementation"(platform(libs.firebase.bom))
    "playstoreImplementation"(libs.firebase.analytics)
    "playstoreImplementation"(libs.firebase.crashlytics)
    "playstoreImplementation"(libs.play.review)

    debugImplementation(libs.leakcanary)
}
