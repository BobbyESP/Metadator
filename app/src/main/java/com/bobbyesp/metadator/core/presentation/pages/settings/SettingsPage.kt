package com.bobbyesp.metadator.core.presentation.pages.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.bobbyesp.metadator.R
import com.bobbyesp.metadator.core.presentation.common.LocalNavController
import com.bobbyesp.metadator.core.presentation.common.Route
import com.bobbyesp.ui.components.preferences.SettingsGroup
import com.bobbyesp.ui.components.preferences.SettingsItem
import com.bobbyesp.ui.components.topbar.ColumnWithCollapsibleTopBar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsPage(onBackPressed: () -> Unit) {
  val navController = LocalNavController.current
  var collapseFraction by remember { mutableFloatStateOf(0f) }

  val mainSettingsGroup: List<SettingsItem> =
      listOf(
          SettingsItem(
              title = stringResource(id = R.string.general),
              supportingText = stringResource(id = R.string.general_description),
              icon = Icons.Rounded.Settings,
              onClick = { navController.navigate(Route.SettingsNavigator.Settings.General) }),
          SettingsItem(
              title = stringResource(id = R.string.appearance),
              supportingText = stringResource(id = R.string.appearance_description),
              icon = Icons.Rounded.Brush,
              onClick = { navController.navigate(Route.SettingsNavigator.Settings.Appearance) }),
      )

  val infoSettingsGroup: List<SettingsItem> =
      listOf(
          SettingsItem(
              title = stringResource(id = R.string.about),
              supportingText = stringResource(id = R.string.about_description),
              icon = Icons.Rounded.Info,
              onClick = { navController.navigate(Route.SettingsNavigator.Settings.About) }))
  ColumnWithCollapsibleTopBar(
      topBarContent = {
        IconButton(
            onClick = onBackPressed,
            modifier =
                Modifier.align(Alignment.BottomStart)
                    .padding(horizontal = 12.dp, vertical = 4.dp)) {
              Icon(
                  imageVector = Icons.Rounded.ArrowBackIosNew,
                  contentDescription = stringResource(id = com.bobbyesp.ui.R.string.back))
            }

        Text(
            text = stringResource(id = R.string.settings),
            style = MaterialTheme.typography.displaySmall,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium,
            modifier =
                Modifier.align(Alignment.Center).padding(horizontal = 16.dp).graphicsLayer {
                  val scale = lerp(0.7f, 1f, collapseFraction)
                  scaleX = scale
                  scaleY = scale
                })
      },
      collapseFraction = { collapseFraction = it },
      contentPadding = PaddingValues(horizontal = 32.dp),
      contentHorizontalAlignment = Alignment.CenterHorizontally,
      contentVerticalArrangement = Arrangement.spacedBy(16.dp),
      modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
        SettingsGroup(modifier = Modifier, items = mainSettingsGroup)

        SettingsGroup(modifier = Modifier, items = infoSettingsGroup)

        Text(
            text = stringResource(id = R.string.made_with_love_by),
            modifier = Modifier.padding(horizontal = 12.dp).fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
}
