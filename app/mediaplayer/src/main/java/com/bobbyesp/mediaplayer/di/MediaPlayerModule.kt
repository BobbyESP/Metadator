import android.app.PendingIntent
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import com.bobbyesp.mediaplayer.service.ConnectionHandler
import com.bobbyesp.mediaplayer.service.MediaLibrarySessionCallback
import com.bobbyesp.mediaplayer.service.MediaServiceHandler
import com.bobbyesp.mediaplayer.service.notifications.MediaNotificationManager
import com.bobbyesp.mediaplayer.service.notifications.customLayout.MediaSessionLayoutHandler
import com.bobbyesp.mediaplayer.service.notifications.customLayout.MediaSessionLayoutHandlerImpl
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

@OptIn(UnstableApi::class)
val mediaplayerInternalsModule: Module = module {
    single {
        AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()
    }

    single {
        ExoPlayer.Builder(androidContext())
            .setSeekBackIncrementMs(5000)
            .setSeekForwardIncrementMs(5000)
            .setAudioAttributes(get(), true)
            .setHandleAudioBecomingNoisy(true)
            .setTrackSelector(DefaultTrackSelector(androidContext()))
            .setAudioAttributes(
                get<AudioAttributes>(),
                true
            )
            .build()
    }

    single {
        MediaServiceHandler(
            player = get<ExoPlayer>()
        )
    }

    single { ConnectionHandler() }

    single {
        MediaLibrarySession.Builder(
            androidContext(),
            get<ExoPlayer>(),
            MediaLibrarySessionCallback(androidContext())
        ).setSessionActivity(
            PendingIntent.getActivity(
                androidContext(),
                0,
                androidContext().packageManager.getLaunchIntentForPackage(androidContext().packageName),
                PendingIntent.FLAG_IMMUTABLE
            )
        ).build()
    }

    single<MediaSessionLayoutHandler> {
        MediaSessionLayoutHandlerImpl(
            androidContext(),
            get<MediaLibrarySession>()
        )
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        single {
            MediaNotificationManager(
                context = androidContext(),
                player = get()
            )
        }
    }
}
