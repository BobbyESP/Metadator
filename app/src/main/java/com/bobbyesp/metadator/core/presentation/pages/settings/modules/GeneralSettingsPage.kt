package com.bobbyesp.metadator.core.presentation.pages.settings.modules

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
import com.bobbyesp.metadator.R
import com.bobbyesp.metadator.core.data.local.preferences.PreferencesKey.MARQUEE_TEXT_ENABLED
import com.bobbyesp.metadator.core.data.local.preferences.PreferencesKey.REDUCE_SHADOWS
import com.bobbyesp.metadator.core.data.local.preferences.datastore.rememberPreferenceState
import com.bobbyesp.metadator.core.presentation.common.LocalNavController
import com.bobbyesp.ui.components.button.BackButton
import com.bobbyesp.ui.components.preferences.PreferenceSwitch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralSettingsPage() {
  val (useMarqueeText, updateMarqueeText) = rememberPreferenceState(MARQUEE_TEXT_ENABLED)
  val (reduceShadows, updateReduceShadows) = rememberPreferenceState(REDUCE_SHADOWS)

  val navController = LocalNavController.current
  val scrollBehavior =
      TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
          state = rememberTopAppBarState(), canScroll = { true })

  Scaffold(
      modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
      topBar = {
        MediumTopAppBar(
            title = {
              Text(
                  text = stringResource(id = R.string.general),
                  style = MaterialTheme.typography.titleLarge)
            },
            scrollBehavior = scrollBehavior,
            navigationIcon = { BackButton { navController.popBackStack() } })
      }) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = paddingValues) {
              item {
                PreferenceSwitch(
                    title = stringResource(R.string.marquee_text),
                    description = stringResource(R.string.marquee_text_description),
                    isChecked = useMarqueeText.value,
                    thumbContent = null,
                    onClick = { updateMarqueeText(!useMarqueeText.value) })
              }
              item {
                PreferenceSwitch(
                    title = stringResource(R.string.reduce_shadows),
                    description = stringResource(R.string.reduce_shadows_description),
                    isChecked = reduceShadows.value,
                    thumbContent = null,
                    onClick = { updateReduceShadows(!reduceShadows.value) })
              }
            }
      }
}
