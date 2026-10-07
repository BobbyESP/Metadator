package com.bobbyesp.metadator.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.bobbyesp.metadator.core.designsystem.component.NavigationItem
import com.bobbyesp.metadator.core.designsystem.component.RadioItem
import com.bobbyesp.metadator.core.designsystem.component.SectionHeader
import com.bobbyesp.metadator.core.designsystem.component.SwitchItem
import com.bobbyesp.metadator.core.designsystem.component.readableWidth
import com.bobbyesp.metadator.core.designsystem.theme.GroupShapes
import com.bobbyesp.metadator.core.designsystem.theme.Spacing
import com.bobbyesp.metadator.core.designsystem.theme.isDynamicColorSupported
import com.bobbyesp.metadator.core.model.MultiValueMode
import com.bobbyesp.metadator.core.model.PaletteStyle
import com.bobbyesp.metadator.core.model.ThemeMode
import com.bobbyesp.metadator.core.model.UserSettings
import com.bobbyesp.metadator.core.navigation.About
import com.bobbyesp.metadator.core.navigation.AppearanceSettings
import com.bobbyesp.metadator.core.navigation.EditorSettings
import com.bobbyesp.metadator.core.navigation.LookupSettings
import com.bobbyesp.metadator.core.navigation.Navigator
import com.bobbyesp.metadator.core.navigation.Settings
import com.bobbyesp.metadator.core.ui.viewmodel.LocalSnackbarHostState
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

private const val SOURCE_URL = "https://github.com/BobbyESP/Metadator"
private const val ISSUES_URL = "https://github.com/BobbyESP/Metadator/issues"
private const val PLAY_URL = "https://play.google.com/store/apps/details?id=com.bobbyesp.metadator"

/** Seeds offered when wallpaper colors are off: the 1.x default first. */
private val SeedColors =
    listOf(UserSettings.DEFAULT_SEED_COLOR, 0xFF6750A4.toInt(), 0xFF006A6A.toInt(), 0xFF8B5000.toInt(), 0xFFB3261E.toInt(), 0xFF386A20.toInt(), 0xFF984061.toInt(), 0xFF00639B.toInt())

fun EntryProviderScope<NavKey>.settingsSection(navigator: Navigator) {
    entry<Settings> {
        SettingsPage(stringResource(R.string.settings), onBack = navigator::goBack) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                    val pages =
                        listOf(
                            Triple(R.string.appearance, R.string.appearance_description, Icons.Rounded.Palette) to AppearanceSettings,
                            Triple(R.string.editor, R.string.editor_description, Icons.Rounded.EditNote) to EditorSettings,
                            Triple(R.string.lookup, R.string.lookup_description, Icons.Rounded.Public) to LookupSettings,
                            Triple(R.string.about, R.string.about_description, Icons.Rounded.Info) to About,
                        )
                    pages.forEachIndexed { index, (labels, key) ->
                        NavigationItem(
                            title = stringResource(labels.first),
                            supportingText = stringResource(labels.second),
                            icon = labels.third,
                            onClick = { navigator.goTo(key) },
                            shapes = GroupShapes.listItemShapes(index, pages.size),
                        )
                    }
                }
            }
        }
    }

    entry<AppearanceSettings> {
        val viewModel: SettingsViewModel = koinViewModel()
        val settings by viewModel.settings.collectAsStateWithLifecycle()
        SettingsPage(stringResource(R.string.appearance), onBack = navigator::goBack) {
            appearance(settings, viewModel::update)
        }
    }

    entry<EditorSettings> {
        val viewModel: SettingsViewModel = koinViewModel()
        val settings by viewModel.settings.collectAsStateWithLifecycle()
        val snackbar = LocalSnackbarHostState.current
        val scope = androidx.compose.runtime.rememberCoroutineScope()
        val cleared = stringResource(R.string.history_cleared)
        SettingsPage(stringResource(R.string.editor), onBack = navigator::goBack) {
            editor(settings, viewModel::update) {
                viewModel.clearHistory { scope.launch { snackbar.showSnackbar(cleared) } }
            }
        }
    }

    entry<LookupSettings> {
        val viewModel: SettingsViewModel = koinViewModel()
        val settings by viewModel.settings.collectAsStateWithLifecycle()
        SettingsPage(stringResource(R.string.lookup), onBack = navigator::goBack) {
            item { SectionHeader(stringResource(R.string.lookup_sources)) }
            item {
                Text(
                    stringResource(R.string.lookup_sources_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = Spacing.small, end = Spacing.small, bottom = Spacing.medium),
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                    viewModel.providers.forEachIndexed { index, provider ->
                        SwitchItem(
                            title = provider.name,
                            supportingText =
                                stringResource(
                                    when (provider.id) {
                                        UserSettings.PROVIDER_MUSICBRAINZ -> R.string.provider_musicbrainz_description
                                        UserSettings.PROVIDER_DEEZER -> R.string.provider_deezer_description
                                        else -> R.string.provider_other_description
                                    }
                                ),
                            icon = Icons.Rounded.Public,
                            checked = provider.id in settings.enabledProviders,
                            onCheckedChange = { viewModel.toggleProvider(provider.id) },
                            shapes = GroupShapes.listItemShapes(index, viewModel.providers.size),
                        )
                    }
                }
            }
        }
    }

    entry<About> {
        val viewModel: SettingsViewModel = koinViewModel()
        val uriHandler = LocalUriHandler.current
        val info = viewModel.appInfo
        SettingsPage(stringResource(R.string.about), onBack = navigator::goBack) {
            item {
                Text(
                    stringResource(R.string.version, info.versionName),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(Spacing.small),
                )
            }
            item {
                val rows = buildList {
                    add(Triple(R.string.source_code, R.string.source_code_description, Icons.Rounded.Code) to SOURCE_URL)
                    add(Triple(R.string.report_issue, R.string.report_issue_description, Icons.Rounded.BugReport) to ISSUES_URL)
                    if (info.isPlayStoreBuild) add(Triple(R.string.rate, R.string.rate_description, Icons.Rounded.Star) to PLAY_URL)
                }
                Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                    rows.forEachIndexed { index, (labels, url) ->
                        NavigationItem(
                            title = stringResource(labels.first),
                            supportingText = stringResource(labels.second),
                            icon = labels.third,
                            onClick = { uriHandler.openUri(url) },
                            shapes = GroupShapes.listItemShapes(index, rows.size),
                            showChevron = false,
                        )
                    }
                }
            }
            item { SectionHeader(stringResource(R.string.credits)) }
            item {
                Text(
                    stringResource(R.string.credits_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Spacing.small),
                )
            }
            item {
                Box(Modifier.fillMaxWidth().padding(Spacing.huge), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.made_by),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SettingsPage(title: String, onBack: () -> Unit, content: LazyListScope.() -> Unit) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack, shapes = IconButtonDefaults.shapes()) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back))
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding() + Spacing.huge,
                    start = Spacing.screen,
                    end = Spacing.screen,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item { Box(Modifier.readableWidth()) }
            content()
        }
    }
}

private fun LazyListScope.appearance(settings: UserSettings, update: ((UserSettings) -> UserSettings) -> Unit) {
    item { SectionHeader(stringResource(R.string.theme), Modifier.readableWidth()) }
    item {
        val modes = listOf(ThemeMode.FollowSystem to R.string.theme_system, ThemeMode.Light to R.string.theme_light, ThemeMode.Dark to R.string.theme_dark)
        Column(Modifier.readableWidth(), verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
            modes.forEachIndexed { index, (mode, label) ->
                RadioItem(
                    title = stringResource(label),
                    supportingText = null,
                    selected = settings.themeMode == mode,
                    onClick = { update { it.copy(themeMode = mode) } },
                    shapes = GroupShapes.listItemShapes(index, modes.size),
                )
            }
        }
    }
    item { SectionHeader(stringResource(R.string.colors), Modifier.readableWidth()) }
    item {
        val dynamicAvailable = isDynamicColorSupported()
        Column(Modifier.readableWidth(), verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
            SwitchItem(
                title = stringResource(R.string.dynamic_color),
                supportingText = stringResource(R.string.dynamic_color_description),
                icon = Icons.Rounded.Wallpaper,
                checked = settings.useDynamicColor && dynamicAvailable,
                enabled = dynamicAvailable,
                onCheckedChange = { checked -> update { it.copy(useDynamicColor = checked) } },
                shapes = GroupShapes.listItemShapes(0, 3),
            )
            SwitchItem(
                title = stringResource(R.string.pure_black),
                supportingText = stringResource(R.string.pure_black_description),
                icon = Icons.Rounded.Contrast,
                checked = settings.pureBlack,
                onCheckedChange = { checked -> update { it.copy(pureBlack = checked) } },
                shapes = GroupShapes.listItemShapes(1, 3),
            )
            SwitchItem(
                title = stringResource(R.string.color_from_cover),
                supportingText = stringResource(R.string.color_from_cover_description),
                icon = Icons.Rounded.Image,
                checked = settings.colorEditorFromArtwork,
                onCheckedChange = { checked -> update { it.copy(colorEditorFromArtwork = checked) } },
                shapes = GroupShapes.listItemShapes(2, 3),
            )
        }
    }
    if (!settings.useDynamicColor || !isDynamicColorSupported()) {
        item { SectionHeader(stringResource(R.string.seed_color), Modifier.readableWidth()) }
        item { SeedColorPicker(settings.seedColor) { color -> update { it.copy(seedColor = color) } } }
        item { SectionHeader(stringResource(R.string.palette_style), Modifier.readableWidth()) }
        item { PaletteStylePicker(settings.paletteStyle) { style -> update { it.copy(paletteStyle = style) } } }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SeedColorPicker(selected: Int, onSelect: (Int) -> Unit) {
    FlowRow(
        modifier = Modifier.readableWidth().padding(horizontal = Spacing.small),
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        SeedColors.forEach { color ->
            val isSelected = color == selected
            Surface(
                onClick = { onSelect(color) },
                shape = CircleShape,
                color = Color(color),
                border = if (isSelected) BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface) else null,
                modifier = Modifier.size(52.dp),
            ) {
                if (isSelected) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PaletteStylePicker(selected: PaletteStyle, onSelect: (PaletteStyle) -> Unit) {
    FlowRow(
        modifier = Modifier.readableWidth().padding(horizontal = Spacing.small),
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        PaletteStyle.entries.forEach { style ->
            FilterChip(
                selected = style == selected,
                onClick = { onSelect(style) },
                label = { Text(stringResource(style.label)) },
            )
        }
    }
}

private val PaletteStyle.label: Int
    get() =
        when (this) {
            PaletteStyle.TonalSpot -> R.string.palette_tonal_spot
            PaletteStyle.Neutral -> R.string.palette_neutral
            PaletteStyle.Vibrant -> R.string.palette_vibrant
            PaletteStyle.Expressive -> R.string.palette_expressive
            PaletteStyle.Rainbow -> R.string.palette_rainbow
            PaletteStyle.FruitSalad -> R.string.palette_fruit_salad
            PaletteStyle.Monochrome -> R.string.palette_monochrome
            PaletteStyle.Fidelity -> R.string.palette_fidelity
            PaletteStyle.Content -> R.string.palette_content
        }

private fun LazyListScope.editor(
    settings: UserSettings,
    update: ((UserSettings) -> UserSettings) -> Unit,
    onClearHistory: () -> Unit,
) {
    item { SectionHeader(stringResource(R.string.multi_values), Modifier.readableWidth()) }
    item {
        Column(Modifier.readableWidth(), verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
            RadioItem(
                title = stringResource(R.string.multi_separate),
                supportingText = stringResource(R.string.multi_separate_description),
                selected = settings.multiValueMode == MultiValueMode.Separate,
                onClick = { update { it.copy(multiValueMode = MultiValueMode.Separate) } },
                shapes = GroupShapes.listItemShapes(0, 2),
            )
            RadioItem(
                title = stringResource(R.string.multi_joined),
                supportingText = stringResource(R.string.multi_joined_description),
                selected = settings.multiValueMode == MultiValueMode.Joined,
                onClick = { update { it.copy(multiValueMode = MultiValueMode.Joined) } },
                shapes = GroupShapes.listItemShapes(1, 2),
            )
        }
    }
    item {
        OutlinedTextField(
            value = settings.multiValueSeparator,
            onValueChange = { value -> if (value.isNotEmpty() && value.length <= 5) update { it.copy(multiValueSeparator = value) } },
            label = { Text(stringResource(R.string.separator)) },
            supportingText = { Text(stringResource(R.string.separator_description)) },
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.readableWidth().padding(top = Spacing.medium),
        )
    }
    item { Box(Modifier.padding(top = Spacing.small)) }
    item {
        SwitchItem(
            title = stringResource(R.string.warn_small_cover),
            supportingText = stringResource(R.string.warn_small_cover_description),
            icon = Icons.Rounded.Image,
            checked = settings.warnSmallArtwork,
            onCheckedChange = { checked -> update { it.copy(warnSmallArtwork = checked) } },
            modifier = Modifier.readableWidth(),
        )
    }
    item { SectionHeader(stringResource(R.string.history), Modifier.readableWidth()) }
    item {
        NavigationItem(
            title = stringResource(R.string.clear_history),
            supportingText = stringResource(R.string.clear_history_description),
            icon = Icons.Rounded.DeleteSweep,
            onClick = onClearHistory,
            showChevron = false,
            modifier = Modifier.readableWidth(),
        )
    }
}

