plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlin.ksp)
}

android {
    namespace = "com.bobbyesp.metadator"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.bobbyesp.metadator"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    //---------------Core----------------//
    implementation(libs.bundles.core) //⚠️ This contains core kotlinx libraries, lifecycle runtime and Activity Compose support

    //---------------User Interface---------------//
    //Core UI libraries
    implementation(platform(libs.compose.bom))

    //Accompanist libraries
    implementation(libs.bundles.accompanist)

    //Compose libraries
    implementation(libs.bundles.compose)
    implementation(libs.material)

    //Pagination
    implementation(libs.bundles.pagination)

    //-------------------Network-------------------//
    implementation(libs.bundles.ktor)

//    //---------------Media3---------------//
//    implementation(libs.bundles.media3)
//    implementation(libs.androidx.media3.datasource.okhttp)


    //---------------Dependency Injection---------------//
    implementation(libs.bundles.hilt)
    ksp(libs.hilt.ext.compiler)
    ksp(libs.hilt.compiler)

    //-------------------Database-------------------//
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.room.paging)
    annotationProcessor(libs.room.compiler)

    //-------------------Key-value Storage-------------------//
    implementation(libs.mmkv)

    //-------------------Markdown-------------------//
    implementation(libs.markdown)

    //-------------------Image Loading-------------------//
    implementation(libs.landscapist.coil)

    //-------------------FIREBASE-------------------//
//    implementation(platform(libs.firebase.bom))
//    implementation(libs.firebase.analytics)
//    implementation(libs.firebase.crashlytics)

    //-------------------Utilities-------------------//
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.qrcode.kotlin.android)
    implementation(libs.profileinstaller)
    implementation(libs.kotlinx.datetime)

    //-------------------Testing-------------------//
    //Android testing libraries
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)

    //Compose testing and tooling libraries
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.test.junit4)
    implementation(libs.compose.tooling.preview)
    debugImplementation(libs.compose.tooling)
    debugImplementation(libs.compose.test.manifest)
}