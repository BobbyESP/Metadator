/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.feature.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.VerticalAlignCenter
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.bobbyesp.metadator.core.designsystem.component.PlaceholderCard
import com.bobbyesp.metadator.core.designsystem.theme.Spacing
import com.bobbyesp.metadator.core.domain.lyrics.TrackLyrics
import com.bobbyesp.metadator.lyrics.api.SongLyrics
import com.bobbyesp.metadator.lyrics.api.TimedLine
import com.bobbyesp.metadator.lyrics.api.TimedWord
import com.bobbyesp.metadator.player.api.PlaybackState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

/**
 * The lyrics of the song that is playing, as its file has them: following the song where they are
 * timed, word by word where each word is, and as plain text to read where they are not.
 *
 * @param lyrics null while they are being read
 * @param compact whether there is little height, which calls for smaller text
 * @param bottomClearance the room whatever floats over the bottom of the pane takes
 * @param onEdit asked to open the editor, which is where lyrics are added or looked up
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun LyricsPane(
    lyrics: TrackLyrics?,
    playback: PlaybackState,
    onSeek: (Long) -> Unit,
    onEdit: () -> Unit,
    compact: Boolean,
    bottomClearance: Dp,
    modifier: Modifier = Modifier,
) {
    Crossfade(
        targetState = lyrics,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        modifier = modifier,
        label = "Lyrics",
    ) { shown ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when (shown) {
                null -> LoadingIndicator()
                TrackLyrics.None ->
                    Notice(bottomClearance) {
                        PlaceholderCard(
                            title = stringResource(R.string.no_lyrics),
                            description = stringResource(R.string.no_lyrics_description),
                            icon = Icons.Rounded.Lyrics,
                            actionText = stringResource(R.string.edit_tags),
                            actionIcon = Icons.Rounded.Edit,
                            onAction = onEdit,
                        )
                    }
                TrackLyrics.Unreadable ->
                    Notice(bottomClearance) {
                        PlaceholderCard(
                            title = stringResource(R.string.lyrics_unreadable),
                            description = stringResource(R.string.lyrics_unreadable_description),
                            icon = Icons.Rounded.ErrorOutline,
                            isError = true,
                        )
                    }
                is TrackLyrics.Available ->
                    when (val song = shown.lyrics) {
                        is SongLyrics.Plain -> PlainLyrics(song.text, compact, bottomClearance)
                        is SongLyrics.Synced ->
                            SyncedLyrics(song.lines, playback, onSeek, compact, bottomClearance)
                    }
            }
        }
    }
}

/** Scrolls, because a card with a button is taller than a phone on its side. */
@Composable
private fun Notice(bottomClearance: Dp, content: @Composable () -> Unit) {
    Box(
        Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = Spacing.large, bottom = Spacing.large + bottomClearance),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun PlainLyrics(text: String, compact: Boolean, bottomClearance: Dp) {
    Text(
        text,
        style =
            if (compact) MaterialTheme.typography.titleMedium
            else MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier =
            Modifier.fillMaxSize()
                .fadingEdges()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.medium)
                .padding(top = Spacing.huge, bottom = Spacing.huge * 2 + bottomClearance),
    )
}

/**
 * Timed lyrics. The line being sung rests a third of the way down, and the list scrolls each new
 * one there. Scrolling it by hand stops that, so the lyrics can be read ahead, until the user asks
 * to go back or leaves them alone for a while. Tapping a line plays from it.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SyncedLyrics(
    lines: List<TimedLine>,
    playback: PlaybackState,
    onSeek: (Long) -> Unit,
    compact: Boolean,
    bottomClearance: Dp,
) {
    val clock = rememberPlaybackClock(playback)
    // Derived, so that a clock that changes every frame recomposes only when the line does.
    val activeIndex by remember(lines, clock) { derivedStateOf { lines.indexAt(clock.positionMs) } }
    val listState = rememberLazyListState()
    val motion = MaterialTheme.motionScheme
    // An effects spec, not a spatial one: a list that bounced past the line would be hard to read.
    val scrollSpec = motion.slowEffectsSpec<Float>()
    var following by remember(lines) { mutableStateOf(true) }

    LaunchedEffect(listState) {
        listState.interactionSource.interactions.collectLatest { interaction ->
            when (interaction) {
                is DragInteraction.Start -> following = false
                is DragInteraction.Stop,
                is DragInteraction.Cancel -> {
                    delay(RESUME_FOLLOWING_MS)
                    following = true
                }
            }
        }
    }
    LaunchedEffect(lines, activeIndex, following) {
        if (!following) return@LaunchedEffect
        val target = activeIndex.coerceAtLeast(0)
        val onScreen = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == target }
        // An item's offset is from where the content starts, below the padding: the place a line
        // rests at is offset zero.
        if (onScreen != null) listState.animateScrollBy(onScreen.offset.toFloat(), scrollSpec)
        else listState.animateScrollToItem(target)
    }

    val style =
        if (compact) MaterialTheme.typography.headlineSmallEmphasized
        else MaterialTheme.typography.headlineMediumEmphasized
    BoxWithConstraints(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().fadingEdges(),
            contentPadding =
                PaddingValues(
                    top = maxHeight * RESTING_FRACTION,
                    bottom = maxHeight * (1f - RESTING_FRACTION) + bottomClearance,
                ),
            verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
        ) {
            itemsIndexed(lines, key = { index, _ -> index }) { index, line ->
                LyricLine(
                    line = line,
                    active = index == activeIndex,
                    clock = clock,
                    style = style,
                    onClick = {
                        following = true
                        onSeek(line.startMs)
                    },
                )
            }
        }
        AnimatedVisibility(
            visible = !following,
            enter =
                scaleIn(motion.fastSpatialSpec(), initialScale = 0.8f) +
                    fadeIn(motion.fastEffectsSpec()),
            exit =
                scaleOut(motion.fastEffectsSpec(), targetScale = 0.8f) +
                    fadeOut(motion.fastEffectsSpec()),
            modifier =
                Modifier.align(Alignment.BottomCenter)
                    .padding(bottom = Spacing.large + bottomClearance),
        ) {
            FilledTonalButton(onClick = { following = true }, shapes = ButtonDefaults.shapes()) {
                Icon(
                    Icons.Rounded.VerticalAlignCenter,
                    null,
                    Modifier.size(ButtonDefaults.IconSize),
                )
                Text(
                    stringResource(R.string.back_to_current_line),
                    Modifier.padding(start = ButtonDefaults.IconSpacing),
                )
            }
        }
    }
}

/**
 * One line. The one being sung comes forward, opaque, at full size and in the primary color, and
 * the rest step back. Where its words are timed, the color also moves across it with the song and
 * each word glows as it is sung: see [drawSweep].
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LyricLine(
    line: TimedLine,
    active: Boolean,
    clock: PlaybackClock,
    style: TextStyle,
    onClick: () -> Unit,
) {
    val focus by
        animateFloatAsState(
            targetValue = if (active) 1f else 0f,
            animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
            label = "LyricFocus",
        )
    val text = MaterialTheme.colorScheme.onSurface
    val primary = MaterialTheme.colorScheme.primary
    var layout by remember(line) { mutableStateOf<TextLayoutResult?>(null) }
    // Each word measured on its own, to be drawn again over itself with its glow. Only when the
    // line first needs them: most lines are never the one being sung while they are on screen.
    val measurer = rememberTextMeasurer()
    val wordLayouts =
        remember(line, style, measurer) {
            lazy(LazyThreadSafetyMode.NONE) {
                line.words.map { word ->
                    val from = word.start.coerceIn(0, line.text.length)
                    val to = word.end.coerceIn(from, line.text.length)
                    measurer.measure(line.text.substring(from, to), style, softWrap = false)
                }
            }
        }
    Text(
        line.text,
        style = style,
        color = text,
        onTextLayout = { layout = it },
        modifier =
            Modifier.fillMaxWidth()
                .graphicsLayer {
                    alpha = lerp(REST_ALPHA, 1f, focus)
                    val scale = lerp(REST_SCALE, 1f, focus)
                    scaleX = scale
                    scaleY = scale
                    // From the edge the text starts at, so the lines stay aligned as they grow.
                    transformOrigin = TransformOrigin(0f, 0.5f)
                    // Not through a buffer the size of the line, which would cut the glow off.
                    compositingStrategy = CompositingStrategy.ModulateAlpha
                }
                // No ripple: the answer to a tap is the song jumping there.
                .clickable(interactionSource = null, indication = null, onClick = onClick)
                .padding(horizontal = Spacing.medium, vertical = Spacing.small)
                .drawWithContent {
                    val measured = layout
                    // The color arrives with the focus, so it is drawn here rather than given
                    // to the text, which would recompose on every frame of the change.
                    val lit = lerp(text, primary, focus)
                    when {
                        measured == null || focus <= 0f -> drawContent()
                        line.words.isEmpty() -> drawText(measured, color = lit)
                        // The clock is read only here and only by the line in focus: nothing
                        // else is drawn again on every frame.
                        else ->
                            drawSweep(
                                layout = measured,
                                words = line.words,
                                wordLayouts = wordLayouts.value,
                                positionMs = clock.positionMs,
                                focus = focus,
                                lit = lit,
                                waiting = text,
                            )
                    }
                },
    )
}

/**
 * Draws a line as far as it has been sung: in the [lit] color up to the word being sung, and
 * through that word in step with it, with a soft edge. The rest waits, dimmer, in the [waiting]
 * color.
 *
 * Each word also glows while it is sung and for a moment after, then the glow fades: a line is lit
 * along its length, but shines only where the voice is. The glow is the word drawn again over
 * itself with a blurred shadow, from a layout of its own. A shadow on the whole line would light
 * every word at once, and one cut to a word would show the cut.
 *
 * The text is drawn one visual line at a time, each clipped to itself, since a line that wraps is
 * sung through its first row before its second. Left to right is assumed.
 *
 * @param wordLayouts each of [words] measured alone, in the line's style
 * @param focus how far into focus the line is, which is how strong all of this is
 */
private fun DrawScope.drawSweep(
    layout: TextLayoutResult,
    words: List<TimedWord>,
    wordLayouts: List<TextLayoutResult>,
    positionMs: Long,
    focus: Float,
    lit: Color,
    waiting: Color,
) {
    val lastChar = layout.layoutInput.text.length - 1
    if (lastChar < 0) return
    val index = words.indexOfLast { it.startMs <= positionMs }
    val word = words[index.coerceAtLeast(0)]
    val first = word.start.coerceIn(0, lastChar)
    val last = (word.end - 1).coerceIn(first, lastChar)
    val sweepLine = layout.getLineForOffset(first)
    val from = layout.getBoundingBox(first).left
    // A word broken over two rows is swept to the end of the first.
    val to =
        if (layout.getLineForOffset(last) == sweepLine) layout.getBoundingBox(last).right
        else layout.getLineRight(sweepLine)
    val sung =
        when {
            index < 0 -> 0f
            word.endMs <= word.startMs -> 1f
            else ->
                ((positionMs - word.startMs).toFloat() / (word.endMs - word.startMs)).coerceIn(
                    0f,
                    1f,
                )
        }
    val sweepX = lerp(from, to, sung)

    val unsung = waiting.copy(alpha = waiting.alpha * lerp(1f, WAITING_ALPHA, focus))
    val feather = SweepFeather.toPx()

    for (row in 0 until layout.lineCount) {
        val rowTop = layout.getLineTop(row)
        val rowBottom = layout.getLineBottom(row)
        when {
            row < sweepLine ->
                clipRect(top = rowTop, bottom = rowBottom) { drawText(layout, color = lit) }
            row > sweepLine ->
                clipRect(top = rowTop, bottom = rowBottom) { drawText(layout, color = unsung) }
            else -> {
                clipRect(top = rowTop, right = sweepX, bottom = rowBottom) {
                    // The edge of the color is soft: it fades into the waiting one just before
                    // where it has got to.
                    drawText(
                        layout,
                        brush =
                            Brush.horizontalGradient(
                                0f to lit,
                                1f to unsung,
                                startX = sweepX - feather,
                                endX = sweepX,
                            ),
                    )
                }
                clipRect(left = sweepX, top = rowTop, bottom = rowBottom) {
                    drawText(layout, color = unsung)
                }
            }
        }
    }

    val blur = GlowBlur.toPx()
    words.forEachIndexed { wordIndex, glowing ->
        val strength = glowing.glowAt(positionMs) * focus
        if (strength <= 0f) return@forEachIndexed
        val start = glowing.start.coerceIn(0, lastChar)
        val origin =
            Offset(
                layout.getBoundingBox(start).left,
                layout.getLineTop(layout.getLineForOffset(start)),
            )
        val shadow = Shadow(lit.copy(alpha = GLOW_ALPHA * strength), Offset.Zero, blur)
        if (wordIndex == index && sung < 1f) {
            // The word being sung glows as far as it has been sung, and a little ahead. Its
            // fill stops with the color underneath, or it would light the rest of the word.
            clipRect(-blur, -blur, sweepX + blur, size.height + blur) {
                drawText(
                    wordLayouts[wordIndex],
                    brush =
                        Brush.horizontalGradient(
                            0f to lit,
                            1f to lit.copy(alpha = 0f),
                            startX = sweepX - feather - origin.x,
                            endX = sweepX - origin.x,
                        ),
                    topLeft = origin,
                    shadow = shadow,
                )
            }
        } else {
            drawText(wordLayouts[wordIndex], color = lit, topLeft = origin, shadow = shadow)
        }
    }
}

/**
 * How much a word glows at [positionMs], from 0 to 1: it comes up quickly as the word starts, holds
 * while it is sung and fades once it is over, slowly at the end. A word too short to come up all
 * the way fades from where it got to.
 */
private fun TimedWord.glowAt(positionMs: Long): Float {
    if (positionMs < startMs) return 0f
    val sungFor = (minOf(positionMs, endMs) - startMs).toFloat()
    val reached = (sungFor / GLOW_RISE_MS).coerceIn(MIN_GLOW, 1f)
    if (positionMs <= endMs) return reached
    val left = 1f - (positionMs - endMs) / GLOW_FADE_MS
    return if (left <= 0f) 0f else reached * left * left
}

/** The line being sung at [positionMs]: the last one that has started, or -1 before the first. */
private fun List<TimedLine>.indexAt(positionMs: Long): Int {
    var low = 0
    var high = lastIndex
    while (low <= high) {
        val middle = (low + high) ushr 1
        if (this[middle].startMs <= positionMs) low = middle + 1 else high = middle - 1
    }
    return high
}

/** Lets what scrolls fade out at the top and the bottom instead of being cut. */
private fun Modifier.fadingEdges(): Modifier = graphicsLayer {
    compositingStrategy = CompositingStrategy.Offscreen
}
    .drawWithContent {
        drawContent()
        drawRect(
            Brush.verticalGradient(
                0f to Color.Transparent,
                EDGE_FADE to Color.Black,
                1f - EDGE_FADE to Color.Black,
                1f to Color.Transparent,
            ),
            blendMode = BlendMode.DstIn,
        )
    }

/** How far down the pane the line being sung rests. */
private const val RESTING_FRACTION = 0.3f

/** How long after the user lets go of the list it follows the song again. */
private const val RESUME_FOLLOWING_MS = 4_000L

/** A line out of focus: dimmer and a little smaller. */
private const val REST_ALPHA = 0.35f
private const val REST_SCALE = 0.94f

/** The part of the line in focus that has not been sung yet. */
private const val WAITING_ALPHA = 0.4f

/**
 * A word's glow. Subtle: it is there to show where the voice is, not to be looked at. It takes
 * [GLOW_RISE_MS] to come up and [GLOW_FADE_MS] to go, and even the shortest word shows [MIN_GLOW].
 */
private const val GLOW_ALPHA = 0.6f
private const val GLOW_RISE_MS = 160f
private const val GLOW_FADE_MS = 900f
private const val MIN_GLOW = 0.35f
private const val EDGE_FADE = 0.1f

private val GlowBlur = 12.dp
private val SweepFeather = 24.dp
