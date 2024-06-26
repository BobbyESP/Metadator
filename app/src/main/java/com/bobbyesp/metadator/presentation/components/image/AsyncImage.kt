package com.bobbyesp.metadator.presentation.components.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.bobbyesp.metadator.R
import com.bobbyesp.ui.components.others.PlaceholderCreator
import com.skydoves.landscapist.coil.LocalCoilImageLoader

@Composable
fun AsyncImageImpl(
    model: Any,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: Float = DefaultAlpha,
    transform: (AsyncImagePainter.State) -> AsyncImagePainter.State = AsyncImagePainter.DefaultTransform,
    onState: ((AsyncImagePainter.State) -> Unit)? = null,
    colorFilter: ColorFilter? = null,
    filterQuality: FilterQuality = DrawScope.DefaultFilterQuality,
    isPreview: Boolean = false,
    context: Context = LocalContext.current
) {
    // Create an ImageLoader if it doesn't exist yet and remember it with the current context.
    val imageLoader = LocalCoilImageLoader.current

    if (isPreview) {
        Image(
            painter = painterResource(R.drawable.metadator_logo_foreground),
            contentDescription = contentDescription,
            modifier = modifier,
            alignment = alignment,
            contentScale = contentScale,
            alpha = alpha,
            colorFilter = colorFilter,
        )
    } else {
        AsyncImage(
            model = model,
            imageLoader = imageLoader ?: ImageLoader.Builder(context).build(),
            onState = onState,
            filterQuality = filterQuality,
            transform = transform,
            contentDescription = contentDescription,
            modifier = modifier,
            alignment = alignment,
            contentScale = contentScale,
            alpha = alpha,
            colorFilter = colorFilter,
        )
    }
}


@Composable
fun AsyncImageImpl(
    model: Any,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    error: Painter? = null,
    placeholder: Painter? = null,
    fallback: Painter? = null,
    onLoading: ((AsyncImagePainter.State.Loading) -> Unit)? = null,
    onSuccess: ((AsyncImagePainter.State.Success) -> Unit)? = null,
    onError: ((AsyncImagePainter.State.Error) -> Unit)? = null,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null,
    filterQuality: FilterQuality = DrawScope.DefaultFilterQuality,
) {

    val context = LocalContext.current

    val imageLoader = LocalCoilImageLoader.current

    val placeholderPainter = placeholder ?: painterResource(R.drawable.metadator_logo_foreground)

    AsyncImage(
        model = model,
        imageLoader = imageLoader ?: ImageLoader.Builder(context).build(),
        filterQuality = filterQuality,
        onError = onError,
        onLoading = onLoading,
        onSuccess = onSuccess,
        fallback = fallback,
        error = error,
        placeholder = placeholderPainter,
        contentDescription = contentDescription,
        modifier = modifier,
        alignment = alignment,
        contentScale = contentScale,
        alpha = alpha,
        colorFilter = colorFilter
    )
}

@Composable
fun ArtworkAsyncImage(
    modifier: Modifier = Modifier,
    imageModifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.small,
    artworkPath: Any? = null
) {
    var showArtwork by remember { mutableStateOf(true) }

    val model by remember(artworkPath) {
        mutableStateOf(artworkPath)
    }

    LaunchedEffect(model) {
        showArtwork = model != null
    }

    if (model != null && showArtwork) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center,
        ) {
            AsyncImageImpl(
                modifier = imageModifier
                    .fillMaxSize()
                    .clip(shape),
                model = model!!,
                onState = { state ->
                    //if it was successful, don't show the placeholder, else show it
                    showArtwork =
                        state !is AsyncImagePainter.State.Error && state !is AsyncImagePainter.State.Empty
                },
                contentDescription = "Song cover",
                contentScale = ContentScale.Crop,
                isPreview = false
            )
        }
    } else {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center,
        ) {
            PlaceholderCreator(
                modifier = imageModifier
                    .fillMaxSize()
                    .clip(shape),
                icon = Icons.Rounded.MusicNote,
                colorful = false,
                contentDescription = "Song cover placeholder"
            )
        }
    }
}


@Composable
fun loadBitmapFromUrl(url: String): Bitmap? {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    val context = LocalContext.current

    LaunchedEffect(url) {
        val imageLoader = ImageLoader(context)
        val request = ImageRequest.Builder(context).data(url).target { drawable ->
            if (drawable is BitmapDrawable) {
                bitmap = drawable.bitmap
            }
        }.build()

        val result = (imageLoader.execute(request) as SuccessResult).drawable
        if (result is BitmapDrawable) {
            bitmap = result.bitmap
        }
    }

    return bitmap
}
