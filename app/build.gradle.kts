val isFullBuild: Boolean by rootProject.extra

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
}

if (isFullBuild) {
    apply(plugin = libs.plugins.google.gms.get().pluginId)
    apply(plugin = "com.google.firebase.crashlytics")
}

val currentVersion: Version = Version.Beta(
    versionMajor = 0,
    versionMinor = 0,
    versionPatch = 1,
    versionBuild = 0
)

android {
    namespace = "com.bobbyesp.metadator"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.bobbyesp.metadator"
        minSdk = 24
        targetSdk = 34

        versionCode = currentVersion.toVersionCode()
        versionName = currentVersion.toVersionName()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
        }
    }

    flavorDimensionList.add("version")

    productFlavors {
        create("playstore") {
            dimension = "version"
        }

        create("foss") {
            dimension = "version"
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

ksp {
    arg(RoomSchemaArgProvider(File(projectDir, "schemas")))
}

dependencies {
    implementation(project(":color"))
    implementation(project(":app:utilities"))
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
//    implementation(libs.markdown)

//-------------------Image Loading-------------------//
    implementation(libs.landscapist.coil)

//-------------------FIREBASE-------------------//
    "playstoreImplementation"(platform(libs.firebase.bom))
    "playstoreImplementation"(libs.firebase.analytics)
    "playstoreImplementation"(libs.firebase.crashlytics)

//-------------------Utilities-------------------//
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.qrcode.kotlin.android)
    implementation(libs.profileinstaller)
    implementation(libs.kotlinx.datetime)
    implementation(libs.taglib)

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
        return listOf("room.schemaLocation=${schemaDir.path}")
    }
}

sealed class Version(
    open val versionMajor: Int,
    val versionMinor: Int,
    val versionPatch: Int,
    val versionBuild: Int = 0
) {
    abstract fun toVersionName(): String
    fun toVersionCode(): Int {
        val minorExtraDigit = if (versionMinor > 9) {
            (versionMinor / 10).toString()
        } else {
            ""
        }

        return "$versionMajor$minorExtraDigit$versionPatch$versionBuild".toInt()
    }

    class Beta(versionMajor: Int, versionMinor: Int, versionPatch: Int, versionBuild: Int) :
        Version(versionMajor, versionMinor, versionPatch, versionBuild) {
        override fun toVersionName(): String =
            "${versionMajor}.${versionMinor}.${versionPatch}-beta.$versionBuild"
    }

    class Stable(versionMajor: Int, versionMinor: Int, versionPatch: Int) :
        Version(versionMajor, versionMinor, versionPatch) {
        override fun toVersionName(): String =
            "${versionMajor}.${versionMinor}.${versionPatch}"
    }

    class ReleaseCandidate(
        versionMajor: Int,
        versionMinor: Int,
        versionPatch: Int,
        versionBuild: Int
    ) :
        Version(versionMajor, versionMinor, versionPatch, versionBuild) {
        override fun toVersionName(): String =
            "${versionMajor}.${versionMinor}.${versionPatch}-rc.$versionBuild"
    }
}