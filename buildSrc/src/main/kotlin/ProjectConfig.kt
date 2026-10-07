import org.gradle.api.JavaVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/** SDK levels and JVM target shared by every module. */
object ProjectConfig {
    const val namespace = "com.bobbyesp.metadator"

    /**
     * Android 8.0. Notification channels, adaptive icons and `java.time` are always there, and the
     * devices below it are a rounding error of the 1.x install base.
     */
    const val minSdk = 26

    const val compileSdk = 37

    const val targetSdk = 37

    val javaVersion = JavaVersion.VERSION_17
    val jvmTarget = JvmTarget.JVM_17
}
