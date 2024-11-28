import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import java.util.Properties

val isGoogleMobileServicesBuild: Boolean by rootProject.extra

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.android.kotlin)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.compose.compiler)
}

val localProperties = Properties().apply {
    load(project.rootDir.resolve("local.properties").inputStream())
}

android {
    namespace = "com.bobbyesp.metadator"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.bobbyesp.metadator"
        minSdk = 24
        targetSdk = 35

        versionCode = rootProject.extra["versionCode"] as Int
        versionName = rootProject.extra["versionName"] as String

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        manifestPlaceholders["redirectHostName"] = "metadator"
        manifestPlaceholders["redirectSchemeName"] = "metadator"
    }

    buildTypes {
        release {
            buildConfigField(
                "String", "CLIENT_ID", "\"${localProperties.getProperty("CLIENT_ID")}\""
            )
            buildConfigField(
                "String", "CLIENT_SECRET", "\"${localProperties.getProperty("CLIENT_SECRET")}\""
            )
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
        debug {
            buildConfigField(
                "String", "CLIENT_ID", "\"${localProperties.getProperty("CLIENT_ID")}\""
            )
            buildConfigField(
                "String", "CLIENT_SECRET", "\"${localProperties.getProperty("CLIENT_SECRET")}\""
            )
            isMinifyEnabled = false
//            applicationIdSuffix = ".debug"
        }
    }

    flavorDimensionList.add("version")

    productFlavors {
        create("playstore") {
            dimension = "version"
            apply(plugin = libs.plugins.google.gms.get().pluginId)
            apply(plugin = libs.plugins.firebase.crashlytics.get().pluginId)
            configure<CrashlyticsExtension> {
                mappingFileUploadEnabled = true
                nativeSymbolUploadEnabled = true
            }
        }

        create("foss") {
            dimension = "version"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
        //freeCompilerArgs = listOf("-Xcontext-receivers", "-XXLanguage:+ExplicitBackingFields")
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeCompiler {
        reportsDestination = layout.buildDirectory.dir("compose_compiler")
    }
    kotlin {
        sourceSets.all {
            languageSettings {
                languageVersion = "2.0"
            }
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    applicationVariants.all {
        val variantName = name
        sourceSets {
            getByName("main") {
                java.srcDir(File("build/generated/ksp/$variantName/kotlin"))
            }
        }
        outputs.all {
            (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName =
                "Metadator-${defaultConfig.versionName}-${name}.apk"
        }
    }

}

ksp {
    arg(RoomSchemaArgProvider(File(projectDir, "schemas")))
    arg("KOIN_CONFIG_CHECK", "true")
}

dependencies {
    implementation(project(":app:utilities"))
    implementation(project(":app:ui"))
//---------------Core----------------//
    implementation(libs.bundles.core) //⚠️ This contains core kotlinx libraries, lifecycle runtime and Activity Compose support
    implementation(libs.bundles.coroutines)

//---------------User Interface---------------//
//Core UI libraries
    api(platform(libs.compose.bom))

//Accompanist libraries
    implementation(libs.bundles.accompanist)

//Compose libraries
    implementation(libs.bundles.compose)
    implementation(libs.materialKolor)
//Pagination
    implementation(libs.bundles.pagination)

//-------------------Network-------------------//
    implementation(libs.bundles.ktor)

    //---------------Media3---------------//
    implementation(libs.bundles.media3)
    implementation(project(":app:mediaplayer"))

//---------------Dependency Injection---------------//
    implementation(libs.bundles.koin)

//-------------------Database-------------------//
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.room.paging)
    annotationProcessor(libs.room.compiler)

//-------------------Key-value Storage-------------------//
    implementation(libs.mmkv)

//-------------------Image Loading-------------------//
    implementation(libs.landscapist.coil)

//-------------------FIREBASE-------------------//
    "playstoreApi"(platform(libs.firebase.bom))
    "playstoreImplementation"(libs.firebase.analytics)
    "playstoreImplementation"(libs.firebase.crashlytics)

//-------------------Utilities-------------------//
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.profileinstaller)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.taglib)
    implementation(libs.scrollbar)
    implementation(libs.spotify.api.android)
    implementation(project(":crashhandler"))

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

class RoomSchemaArgProvider(
    @get:InputDirectory @get:PathSensitive(PathSensitivity.RELATIVE) val schemaDir: File
) : CommandLineArgumentProvider {

    override fun asArguments(): Iterable<String> {
        if (!schemaDir.exists()) {
            schemaDir.mkdirs()
        }
        return listOf("room.schemaLocation=${schemaDir.path}")
    }
}