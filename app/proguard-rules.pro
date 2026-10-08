# Navigation keys are restored from saved state by their serializers.
-keepclassmembers @kotlinx.serialization.Serializable class com.bobbyesp.metadator.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.bobbyesp.metadator.**$$serializer { *; }

# Ktor and OkHttp reference optional platform classes.
-dontwarn org.slf4j.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
