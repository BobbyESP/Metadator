import com.bobbyesp.metadator.MainActivity
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase

fun MainActivity.setupFirebase() {
    Firebase.crashlytics.setCrashlyticsCollectionEnabled(true)
}