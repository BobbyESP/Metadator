package com.bobbyesp.ui.common.pages

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.twotone.BugReport
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bobbyesp.ui.R
import com.bobbyesp.ui.components.button.BackButton
import com.bobbyesp.ui.motion.DefaultBoundsTransform
import com.bobbyesp.ui.motion.EmphasizedAccelerateEasing
import com.bobbyesp.ui.motion.EmphasizedDecelerateEasing
import com.bobbyesp.ui.motion.EmphasizedEasing
import com.bobbyesp.ui.motion.MotionConstants.DURATION
import com.bobbyesp.ui.motion.MotionConstants.DURATION_ENTER
import com.bobbyesp.ui.motion.MotionConstants.DURATION_EXIT_SHORT

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ErrorPage(modifier: Modifier = Modifier, throwable: Throwable, onRetry: () -> Unit) {
  var showFullscreenError by remember { mutableStateOf(false) }
  SharedTransitionLayout(
      modifier = modifier.background(MaterialTheme.colorScheme.background),
  ) {
    AnimatedContent(
        transitionSpec = {
          fadeIn(
              tween(
                  durationMillis = DURATION_ENTER,
                  delayMillis = DURATION_EXIT_SHORT,
                  easing = EmphasizedDecelerateEasing)) togetherWith
              fadeOut(
                  tween(
                      durationMillis = DURATION_EXIT_SHORT,
                      easing = EmphasizedAccelerateEasing)) using
              SizeTransform { _, _ -> tween(durationMillis = DURATION, easing = EmphasizedEasing) }
        },
        targetState = showFullscreenError,
        label = "Error Page animated content transition") { wantsFullscreen ->
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (wantsFullscreen) {
              ExpandedErrorPage(
                  modifier = modifier,
                  throwable = throwable,
                  animatedVisibilityScope = this@AnimatedContent,
                  onMinimize = { showFullscreenError = false })
            } else {
              MinimizedErrorPage(
                  modifier = modifier,
                  throwable = throwable,
                  animatedVisibilityScope = this@AnimatedContent,
                  onCardClicked = { showFullscreenError = true },
                  onRetry = onRetry)
            }
          }
        }
  }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SharedTransitionScope.MinimizedErrorPage(
    modifier: Modifier = Modifier,
    throwable: Throwable,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onCardClicked: () -> Unit,
    onRetry: () -> Unit,
) {
  Column(
      modifier = modifier.padding(8.dp),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            modifier = Modifier.size(48.dp),
            imageVector = Icons.Rounded.WarningAmber,
            contentDescription = stringResource(id = R.string.error),
            tint = MaterialTheme.colorScheme.error)
        Text(
            text = stringResource(id = R.string.unknown_error_title),
            style =
                MaterialTheme.typography.titleLarge.copy(
                    color = MaterialTheme.colorScheme.onBackground),
            fontWeight = FontWeight.SemiBold,
        )
        PrimaryStacktraceCard(
            modifier =
                Modifier.sharedBounds(
                        boundsTransform = DefaultBoundsTransform,
                        enter =
                            fadeIn(
                                tween(
                                    durationMillis = DURATION_ENTER,
                                    delayMillis = DURATION_EXIT_SHORT,
                                    easing = EmphasizedDecelerateEasing)),
                        exit =
                            fadeOut(
                                tween(
                                    durationMillis = DURATION_EXIT_SHORT,
                                    easing = EmphasizedAccelerateEasing)),
                        sharedContentState =
                            rememberSharedContentState(key = "stacktraceCardBounds"),
                        animatedVisibilityScope = animatedVisibilityScope,
                        placeHolderSize = SharedTransitionScope.PlaceHolderSize.animatedSize,
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .fillMaxWidth(),
            errorType =
                throwable::class.simpleName ?: stringResource(id = R.string.unknown_error_title),
            methodFailed =
                throwable.localizedMessage ?: stringResource(id = R.string.unknown_error_title),
            line = throwable.stackTrace.firstOrNull()?.lineNumber ?: 0,
            onClick = onCardClicked)
        Button(onClick = onRetry) { Text(text = stringResource(id = R.string.retry)) }
      }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SharedTransitionScope.ExpandedErrorPage(
    modifier: Modifier = Modifier,
    animatedVisibilityScope: AnimatedVisibilityScope,
    throwable: Throwable,
    onMinimize: () -> Unit,
) {
  BackHandler { onMinimize() }
  Column(
      modifier =
          modifier
              .sharedBounds(
                  boundsTransform = DefaultBoundsTransform,
                  enter =
                      fadeIn(
                          tween(
                              durationMillis = DURATION_ENTER,
                              delayMillis = DURATION_EXIT_SHORT,
                              easing = EmphasizedDecelerateEasing)),
                  exit =
                      fadeOut(
                          tween(
                              durationMillis = DURATION_EXIT_SHORT,
                              easing = EmphasizedAccelerateEasing)),
                  sharedContentState = rememberSharedContentState(key = "stacktraceCardBounds"),
                  animatedVisibilityScope = animatedVisibilityScope,
                  placeHolderSize = SharedTransitionScope.PlaceHolderSize.animatedSize,
              )
              .fillMaxSize(),
  ) {
    Row(
        modifier =
            Modifier.background(MaterialTheme.colorScheme.surfaceContainer)
                .systemBarsPadding()
                .fillMaxWidth()
                .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
        verticalAlignment = Alignment.CenterVertically) {
          CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
            BackButton(onClick = { onMinimize() })
            Text(
                modifier = Modifier,
                text = stringResource(id = R.string.unknown_error_title),
                style = MaterialTheme.typography.titleMedium,
                overflow = TextOverflow.Ellipsis)
          }
        }
    StackTraceViewer(modifier = Modifier.fillMaxWidth(), throwable = throwable)
  }
}

@Composable
fun StackTraceViewer(modifier: Modifier = Modifier, throwable: Throwable) {
  Column(
      modifier = modifier.verticalScroll(rememberScrollState()).fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(4.dp)) {
        throwable.stackTrace.forEachIndexed { index, element ->
          Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "${index + 1}",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(32.dp).padding(6.dp).alpha(0.72f))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = element.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                overflow = TextOverflow.Clip)
          }
          HorizontalDivider()
        }
      }
}

@Composable
private fun PrimaryStacktraceCard(
    modifier: Modifier = Modifier,
    errorType: String,
    methodFailed: String,
    line: Int,
    onClick: () -> Unit = {}
) {
  Surface(
      modifier = modifier,
      onClick = onClick,
      shape = MaterialTheme.shapes.small,
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 8.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Icon(
                  imageVector = Icons.TwoTone.BugReport,
                  contentDescription = stringResource(id = R.string.error),
                  modifier =
                      Modifier.size(48.dp)
                          .clip(MaterialTheme.shapes.small)
                          .background(MaterialTheme.colorScheme.primaryContainer)
                          .padding(4.dp),
                  tint = MaterialTheme.colorScheme.primary)
              Column(
                  modifier = Modifier.fillMaxWidth(),
              ) {
                Text(
                    modifier = Modifier,
                    text = errorType.uppercase(),
                    style =
                        MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                            letterSpacing = 1.sp),
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = methodFailed,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    overflow = TextOverflow.Ellipsis)

                Text(
                    text = stringResource(R.string.line, line),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Normal,
                    fontFamily = FontFamily.Monospace,
                    overflow = TextOverflow.Ellipsis)
              }
            }
      }
}

@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun ErrorPagePrev() {
  MaterialTheme(colorScheme = darkColorScheme()) {
    ErrorPage(
        modifier = Modifier.background(MaterialTheme.colorScheme.background),
        throwable = Exception("An error occurred"),
        onRetry = {})
  }
}

@Preview
@Composable
private fun ErrorPagePrevWhite() {
  MaterialTheme {
    ErrorPage(
        modifier = Modifier.background(MaterialTheme.colorScheme.background),
        throwable = Exception("An error occurred"),
        onRetry = {})
  }
}

@Preview
@Composable
private fun PrimaryStacktraceCardPrev() {
  MaterialTheme {
    PrimaryStacktraceCard(errorType = "Error", methodFailed = "Method failed", line = 1)
  }
}

@Preview
@Composable
private fun PrimaryStacktraceCardPrevDark() {
  MaterialTheme(colorScheme = darkColorScheme()) {
    PrimaryStacktraceCard(errorType = "Error", methodFailed = "Method failed", line = 1)
  }
}
