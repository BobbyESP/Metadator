package com.bobbyesp.metadator.presentation.pages.settings.modules

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import com.bobbyesp.metadator.App.Companion.preferences
import com.bobbyesp.metadator.R
import com.bobbyesp.metadator.presentation.common.LocalNavController
import com.bobbyesp.metadator.util.preferences.PreferencesKeys.MARQUEE_TEXT
import com.bobbyesp.metadator.util.preferences.booleanState
import com.bobbyesp.ui.components.button.BackButton
import com.bobbyesp.ui.components.preferences.PreferenceSwitch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralSettingsPage() {
    val useMarqueeText = MARQUEE_TEXT.booleanState

    val navController = LocalNavController.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        state = rememberTopAppBarState(),
        canScroll = { true }
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.general),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    BackButton {
                        navController.popBackStack()
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = paddingValues
        ) {
            item {
                PreferenceSwitch(
                    title = stringResource(R.string.marquee_text),
                    description = stringResource(R.string.marquee_text_description),
                    isChecked = useMarqueeText.value,
                    onClick = {
                        useMarqueeText.value = !useMarqueeText.value
                        preferences.updateValue(MARQUEE_TEXT, useMarqueeText.value)
                    }
                )
            }
        }
    }
}