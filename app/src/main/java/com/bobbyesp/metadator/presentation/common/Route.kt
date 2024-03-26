package com.bobbyesp.metadator.presentation.common

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.ui.graphics.vector.ImageVector
import com.bobbyesp.metadator.R
import dagger.hilt.android.qualifiers.ApplicationContext

sealed class Route(
    val route: String,
    @StringRes val title: Int? = null,
    val icon: ImageVector? = null,
) {
    data object MainHost : Route("main_host")

    data object MetadatorNavigator : Route(
        "metadator_navigator",
        title = R.string.app_name,
        icon = Icons.Default.ContentPaste
    ) {
        data object HomePage :
            Route(
                "home",
                title = R.string.home,
                icon = Icons.Default.ContentPaste
            )
    }
}

val routesToShowInBottomBar = listOf(
    Route.MetadatorNavigator,
)

fun Route.getTitle(@ApplicationContext context: Context): String? {
    return title?.let { context.getString(it) }
}

enum class NavArg(val key: String) {

}