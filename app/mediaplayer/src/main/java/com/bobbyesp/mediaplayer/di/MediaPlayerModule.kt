import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import com.bobbyesp.mediaplayer.data.repository.MusicScannerImpl
import com.bobbyesp.mediaplayer.domain.repository.MusicScanner
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

@OptIn(UnstableApi::class)
val mediaplayerInternalsModule: Module = module {
    single {
        ExoPlayer.Builder(androidContext())
            .setSeekBackIncrementMs(5000)
            .setSeekForwardIncrementMs(5000)
            .setHandleAudioBecomingNoisy(true)
            .setTrackSelector(DefaultTrackSelector(androidContext()))
            .setAudioAttributes(get<AudioAttributes>(), true)
            .build()
    }

    single {
        AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()
    }

    single { (callback: MediaLibrarySession.Callback) ->
        MediaLibrarySession.Builder(get<MediaLibraryService>(), get<ExoPlayer>(), callback).build()
    }

    single<MusicScanner> { MusicScannerImpl(context = androidContext()) }
}
