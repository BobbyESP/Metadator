import com.bobbyesp.metadator.App
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import com.google.firebase.initialize

fun App.initializeFirebase() {
    Firebase.initialize(this)
}

/**
 * Extension function for MainActivity to enable Crashlytics collection.
 *
 * This function sets the Crashlytics collection to be enabled, allowing Firebase Crashlytics
 * to collect crash reports for the application.
 */
fun setCrashlyticsCollection() {
    Firebase.crashlytics.setCrashlyticsCollectionEnabled(true)
}