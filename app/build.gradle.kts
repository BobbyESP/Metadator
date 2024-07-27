import java.util.Properties

val isFullBuild: Boolean by rootProject.extra

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.android.kotlin)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.compose.compiler)
}

if (isFullBuild) {
    apply(plugin = libs.plugins.google.gms.get().pluginId)
    apply(plugin = "com.google.firebase.crashlytics")
    apply<CrashlyticsBuildConfig>()
}

val commitSignature = providers.exec {
    commandLine("git", "rev-parse", "--short", "HEAD")
}.standardOutput.asText.get().substringBefore("\n")

val currentVersion: Version = Version.Beta(
    versionMajor = 1,
    versionMinor = 0,
    versionPatch = 0,
    versionBuild = 7
)

val localProperties = Properties().apply {
    load(project.rootDir.resolve("local.properties").inputStream())
}

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
            applicationIdSuffix = ".debug"
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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
        freeCompilerArgs = listOf("-Xcontext-receivers", "-XXLanguage:+ExplicitBackingFields")
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeCompiler {
        enableStrongSkippingMode = true
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
        outputs.all {
            (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName =
                "Metadator-${defaultConfig.versionName}-${name}.apk"
        }
    }

}
ksp {
    arg(RoomSchemaArgProvider(File(projectDir, "schemas")))
}

dependencies {
    implementation(project(":app:utilities"))
    implementation(project(":app:ui"))
//---------------Core----------------//
    implementation(libs.bundles.core) //⚠️ This contains core kotlinx libraries, lifecycle runtime and Activity Compose support

//---------------User Interface---------------//
//Core UI libraries
    api(platform(libs.compose.bom.canary))

//Accompanist libraries
    implementation(libs.bundles.accompanist)

//Compose libraries
    implementation(libs.bundles.compose)
    implementation(libs.materialKolor)
//Pagination
    implementation(libs.bundles.pagination)

//-------------------Network-------------------//
    implementation(libs.bundles.ktor)

//    //---------------Media3---------------//
    implementation(libs.bundles.media3)
    implementation(libs.androidx.media3.datasource.okhttp)
    implementation(project(":app:mediaplayer"))

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
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.taglib)
    implementation(libs.scrollbar)
    implementation(libs.lyricfier)
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

sealed class Version(
    open val versionMajor: Int,
    val versionMinor: Int,
    val versionPatch: Int,
    val versionBuild: Int = 0,
    val commitId: String = ""
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

    class Stable(versionMajor: Int, versionMinor: Int, versionPatch: Int) :
        Version(versionMajor, versionMinor, versionPatch) {
        override fun toVersionName(): String = "${versionMajor}.${versionMinor}.${versionPatch}"
    }

    class ReleaseCandidate(
        versionMajor: Int, versionMinor: Int, versionPatch: Int, versionBuild: Int
    ) : Version(versionMajor, versionMinor, versionPatch, versionBuild) {
        override fun toVersionName(): String =
            "${versionMajor}.${versionMinor}.${versionPatch}-rc.$versionBuild"
    }

    class Beta(versionMajor: Int, versionMinor: Int, versionPatch: Int, versionBuild: Int) :
        Version(versionMajor, versionMinor, versionPatch, versionBuild) {
        override fun toVersionName(): String =
            "${versionMajor}.${versionMinor}.${versionPatch}-beta.$versionBuild"
    }

    class Alpha(
        versionMajor: Int, versionMinor: Int, versionPatch: Int, commitId: String
    ) : Version(versionMajor, versionMinor, versionPatch, commitId = commitId) {
        override fun toVersionName(): String =
            "${versionMajor}.${versionMinor}.${versionPatch}-alpha.$commitId"
    }
}