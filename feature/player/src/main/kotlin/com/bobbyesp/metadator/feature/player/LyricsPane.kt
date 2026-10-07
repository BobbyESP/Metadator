/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.feature.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.CubicBezierEasing
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
import androidx.compose.ui.graphics.drawscope.translate
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
 * through that word in step with it, with a soft edge. The rest waits, dimmer, in [waiting].
 *
 * Each word also rises a little as it is sung and stays up, and glows while it is sung and for a
 * moment after: a line is colored along its length, but shines only where the voice is, and more on
 * a note that is held. The glow is the word drawn again with a blurred shadow, which falls behind
 * its letters, from a layout of its own. A shadow on the whole line would light every word at once,
 * and one cut to a word would show the cut.
 *
 * Words move apart from each other, so the line is drawn a word at a time: each row of the text is
 * split between its words, halfway through the space between two, and each part is the whole layout
 * clipped to it and moved. Left to right is assumed.
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
    val count = words.size
    // Where each word is: its row, and its left and right edges on it. A word broken over two
    // rows counts as far as the end of the first.
    val rows = IntArray(count)
    val lefts = FloatArray(count)
    val rights = FloatArray(count)
    for (i in 0 until count) {
        val first = words[i].start.coerceIn(0, lastChar)
        val last = (words[i].end - 1).coerceIn(first, lastChar)
        rows[i] = layout.getLineForOffset(first)
        lefts[i] = layout.getBoundingBox(first).left
        rights[i] =
            if (layout.getLineForOffset(last) == rows[i]) layout.getBoundingBox(last).right
            else layout.getLineRight(rows[i])
    }

    val index = words.indexOfLast { it.startMs <= positionMs }
    val sung =
        when {
            index < 0 -> 0f
            words[index].endMs <= words[index].startMs -> 1f
            else ->
                ((positionMs - words[index].startMs).toFloat() /
                        (words[index].endMs - words[index].startMs))
                    .coerceIn(0f, 1f)
        }
    val sweepX = if (index < 0) 0f else lerp(lefts[index], rights[index], sung)
    val sweepRow = if (index < 0) -1 else rows[index]

    val unsung = waiting.copy(alpha = waiting.alpha * lerp(1f, WAITING_ALPHA, focus))
    val feather = SweepFeather.toPx()
    val rise = WordRise.toPx() * focus

    for (row in 0 until layout.lineCount) {
        val rowTop = layout.getLineTop(row)
        val rowBottom = layout.getLineBottom(row)
        var previous = -1
        for (i in 0 until count) {
            if (rows[i] != row) continue
            var next = i + 1
            while (next < count && rows[next] != row) next++
            // Text that is in no word, before the first of a row or after the last, goes with it.
            val partLeft = if (previous < 0) 0f else (rights[previous] + lefts[i]) / 2f
            val partRight = if (next >= count) size.width else (rights[i] + lefts[next]) / 2f
            translate(top = -rise * words[i].riseAt(positionMs)) {
                clipRect(partLeft, rowTop, partRight, rowBottom) {
                    when {
                        i > index -> drawText(layout, color = unsung)
                        i < index || sung >= 1f -> drawText(layout, color = lit)
                        else -> {
                            clipRect(right = sweepX) {
                                // The edge of the color is soft: it fades into the waiting
                                // one just before where it has got to.
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
                            clipRect(left = sweepX) { drawText(layout, color = unsung) }
                        }
                    }
                }
            }
            previous = i
        }
        // A row with no word of its own: the tail of one broken over two rows.
        if (previous < 0) {
            clipRect(top = rowTop, bottom = rowBottom) {
                drawText(layout, color = if (row <= sweepRow) lit else unsung)
            }
        }
    }

    val blur = GlowBlur.toPx()
    for (i in 0 until count) {
        val strength = words[i].glowAt(positionMs) * focus
        if (strength <= 0f) continue
        val origin =
            Offset(lefts[i], layout.getLineTop(rows[i]) - rise * words[i].riseAt(positionMs))
        val shadow = Shadow(lit.copy(alpha = GLOW_ALPHA * strength), Offset.Zero, blur)
        if (i == index && sung < 1f) {
            // The word being sung glows as far as it has been sung, and a little ahead. Its
            // fill stops with the color underneath, or it would light the rest of the word.
            clipRect(-blur, -blur, sweepX + blur, size.height + blur) {
                drawText(
                    wordLayouts[i],
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
            drawText(wordLayouts[i], color = lit, topLeft = origin, shadow = shadow)
        }
    }
}

/**
 * For how long a word is sung, as far as its glow and its rise go. The last word of a line with no
 * end of its own lasts until the next line, which can be a whole instrumental break away, and
 * nobody holds a note that long.
 */
private val TimedWord.heldMs: Float
    get() = (endMs - startMs).toFloat().coerceIn(0f, LONGEST_NOTE_MS)

/**
 * How much a word glows at [positionMs], from 0 to 1. It comes up as the word starts, quickly for a
 * short one and through the first half of a held note, so that a long note swells. It fades once
 * the word is over, slowly at the end. A held note also glows more than a passing word: it is where
 * the voice stays.
 */
private fun TimedWord.glowAt(positionMs: Long): Float {
    if (positionMs < startMs) return 0f
    val held = heldMs
    val weight =
        lerp(
            PASSING_WORD_GLOW,
            1f,
            ((held - PASSING_WORD_MS) / (HELD_NOTE_MS - PASSING_WORD_MS)).coerceIn(0f, 1f),
        )
    val sungFor = (positionMs - startMs).toFloat().coerceAtMost(held)
    val reached = (sungFor / maxOf(GLOW_RISE_MS, held / 2f)).coerceIn(MIN_GLOW, 1f) * weight
    val over = positionMs - startMs - held
    if (over <= 0f) return reached
    val left = 1f - over / GLOW_FADE_MS
    return if (left <= 0f) 0f else reached * left * left
}

/**
 * How far up a word has risen at [positionMs]: 0 before it is sung and 1 once it is up, where it
 * stays for as long as the line is the one being sung. On the way it goes a little past 1 and comes
 * back ([RiseEasing]), which is what makes it a hop rather than a slide.
 */
private fun TimedWord.riseAt(positionMs: Long): Float {
    if (positionMs <= startMs) return 0f
    val riseMs = heldMs.coerceIn(SHORTEST_RISE_MS, LONGEST_RISE_MS)
    return RiseEasing.transform(((positionMs - startMs) / riseMs).coerceIn(0f, 1f))
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
 * A word's glow. Subtle: it is there to show where the voice is, not to be looked at. It takes at
 * least [GLOW_RISE_MS] to come up and [GLOW_FADE_MS] to go, and even the shortest word shows
 * [MIN_GLOW] of what it would reach.
 */
private const val GLOW_ALPHA = 0.7f
private const val GLOW_RISE_MS = 160f
private const val GLOW_FADE_MS = 900f
private const val MIN_GLOW = 0.35f

/**
 * A word sung in passing glows [PASSING_WORD_GLOW] of what a held note does; in between, in
 * proportion. Nothing counts as held for longer than [LONGEST_NOTE_MS].
 */
private const val PASSING_WORD_MS = 250f
private const val HELD_NOTE_MS = 1_200f
private const val PASSING_WORD_GLOW = 0.5f
private const val LONGEST_NOTE_MS = 2_500f

/**
 * A word takes as long to rise as it takes to sing, within these: any faster it would twitch, and
 * any slower a held note would drift up instead of hopping.
 */
private const val SHORTEST_RISE_MS = 260f
private const val LONGEST_RISE_MS = 420f

/**
 * Quick off the mark, a little too far, then back: a "back out" curve. An easing and not one of the
 * motion scheme's springs, because this is not an animation that runs: where a word is depends only
 * on where the song is, so that seeking, pausing and playing draw the same frame.
 */
private val RiseEasing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)

/** How much of the pane, at its top and at its bottom, what scrolls fades out over. */
private const val EDGE_FADE = 0.1f

private val GlowBlur = 12.dp

/** How far a word rises as it is sung. Barely: enough to be felt, not to be seen moving. */
private val WordRise = 3.dp
private val SweepFeather = 24.dp
