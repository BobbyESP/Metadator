plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.android.kotlin)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ktfmt.gradle)
}

android {
    namespace = "com.bobbyesp.mediaplayer"
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
                "proguard-rules.pro"
            )
        }
    }
    libraryVariants.all {
        val variantName = name
        sourceSets {
            getByName("main") {
                java.srcDir(File("build/generated/ksp/$variantName/kotlin"))
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
}

ktfmt {
    // Google style - 2 space indentation & automatically adds/removes trailing commas
    //googleStyle()

    // KotlinLang style - 4 space indentation - From https://kotlinlang.org/docs/coding-conventions.html
    kotlinLangStyle()
}

ksp {
    arg("KOIN_CONFIG_CHECK", "true")
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.core.appcompat)
    implementation(libs.androidx.legacy.support.v4)
    implementation(project(":core-utilities")) // Needed MediaSessionCompat.Token

    // Todo create a top level utilities

    //DI (Dependency Injection - Koin)
    implementation(libs.bundles.koin)

    //Media3
    implementation(libs.bundles.media3)

    //KotlinX Serialization
    implementation(libs.kotlinx.serialization.json)

    //Coil
    implementation(libs.coil)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
}