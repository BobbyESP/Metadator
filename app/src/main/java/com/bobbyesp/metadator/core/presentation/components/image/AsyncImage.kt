package com.bobbyesp.metadator.core.presentation.components.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.bobbyesp.ui.components.others.Placeholder
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil.CoilImage
import com.skydoves.landscapist.coil.CoilImageState
import com.skydoves.landscapist.coil.LocalCoilImageLoader
import java.io.FileNotFoundException

@Composable
fun AsyncImage(
    modifier: Modifier = Modifier,
    imageModel: Any? = null,
    imageModifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.small,
    placeholder: ImageVector? = null,
    context: Context = LocalContext.current,
    imageLoader: ImageLoader? = LocalCoilImageLoader.current,
    imageOptions: ImageOptions =
        ImageOptions(contentDescription = null, contentScale = ContentScale.Crop),
    requestListener: (() -> ImageRequest.Listener)? = null,
    onSuccessData: (CoilImageState.Success) -> Unit = { _ -> },
) {
    val imageUrl by remember(imageModel) { mutableStateOf(imageModel) }

    CoilImage(
        modifier = modifier.clip(shape),
        imageModel = { imageUrl },
        imageOptions = imageOptions,
        onImageStateChanged = { state ->
            if (state is CoilImageState.Success) {
                onSuccessData(state)
            }
        },
        requestListener = requestListener,
        loading = {
            Placeholder(
                modifier = imageModifier.fillMaxSize(),
                icon = placeholder ?: Icons.Rounded.MusicNote,
                colorful = false,
                contentDescription = "Song cover placeholder",
            )
        },
        failure = { error ->
            val icon =
                if (error.reason is FileNotFoundException) {
                    Icons.Rounded.MusicNote
                } else {
                    Icons.Rounded.ErrorOutline
                }
            Placeholder(
                modifier = imageModifier.fillMaxSize(),
                icon = icon,
                colorful = false,
                contentDescription = "Song cover failed to load",
            )
        },
        imageLoader = { imageLoader ?: ImageLoader(context) },
    )
}

@Composable
fun loadBitmapFromUrl(url: String): Bitmap? {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    val context = LocalContext.current

    LaunchedEffect(url) {
        val imageLoader = ImageLoader(context)
        val request =
            ImageRequest.Builder(context)
                .data(url)
                .target { drawable ->
                    if (drawable is BitmapDrawable) {
                        bitmap = drawable.bitmap
                    }
                }
                .build()

        val result = (imageLoader.execute(request) as SuccessResult).drawable
        if (result is BitmapDrawable) {
            bitmap = result.bitmap
        }
    }

    return bitmap
}
