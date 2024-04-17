package com.bobbyesp.metadator.presentation.common

import android.content.Context
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Handyman
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.ui.graphics.vector.ImageVector
import com.bobbyesp.metadator.App.Companion.json
import com.bobbyesp.metadator.R
import com.bobbyesp.metadator.model.ParcelableSong
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.encodeToString

sealed class Route(
    val route: String,
    @StringRes val title: Int? = null,
    val icon: ImageVector? = null,
) {
    data object MainHost : Route("main_host")

    data object MetadatorNavigator : Route(
        "metadator_navigator",
        title = R.string.home,
        icon = Icons.Rounded.Home
    ) {
        data object Home :
            Route(
                "home",
                title = R.string.home,
                icon = Icons.Rounded.Home
            ) {
        }
    }

    data object MediaplayerNavigator : Route(
        "mediaplayer_navigator",
        title = R.string.mediaplayer,
        icon = Icons.Rounded.PlayArrow
    ) {
        data object Mediaplayer :
            Route(
                "mediaplayer",
                title = R.string.mediaplayer,
                icon = Icons.Rounded.PlayArrow
            ) {
        }
    }

    data object UtilitiesNavigator :
        Route(
            "utilities",
            title = R.string.utilities,
            icon = Icons.Rounded.Handyman
        ) {
        data object TagEditor :
            Route(
                "utilities/tag_editor/{${NavArgs.TagEditorSelectedSong.key}}",
                title = R.string.tag_editor,
                icon = Icons.Rounded.Edit
            ) {
            fun createRoute(parcelableSong: ParcelableSong) =
                "utilities/tag_editor/${
                    Uri.encode(
                        json.encodeToString<ParcelableSong>(
                            parcelableSong
                        )
                    )
                }"
        }
    }
}

val routesToNavigate = listOf(
    Route.MetadatorNavigator,
    Route.MediaplayerNavigator
)

fun Route.getTitle(@ApplicationContext context: Context): String? {
    return title?.let { context.getString(it) }
}

enum class NavArgs(val key: String) {
    SelectedSong(key = "selectedSong"),
    TagEditorSelectedSong(key = "tagEditorSelectedSong"),
}