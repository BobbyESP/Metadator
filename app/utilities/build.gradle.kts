plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.android.kotlin)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ktfmt.gradle)
}

android {
    namespace = "com.bobbyesp.utilities"
    compileSdk = 35

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeCompiler { reportsDestination = layout.buildDirectory.dir("compose_compiler") }
    kotlinOptions { jvmTarget = "21" }
}

ktfmt {
    // Google style - 2 space indentation & automatically adds/removes trailing commas
    // googleStyle()

    // KotlinLang style - 4 space indentation - From
    // https://kotlinlang.org/docs/coding-conventions.html
    kotlinLangStyle()
}

dependencies {
    implementation(libs.core.ktx)
    implementation(project(":core-utilities"))
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.bundles.compose)
    implementation(libs.paging.compose)
    implementation(libs.paging.runtime)
    implementation(libs.coil)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.bundles.coroutines)
    implementation(libs.bundles.accompanist)

    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.test.junit4)
    implementation(libs.compose.tooling.preview)
    debugImplementation(libs.compose.tooling)
    debugImplementation(libs.compose.test.manifest)
}
