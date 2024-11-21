package com.bobbyesp.metadator.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Square
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.bobbyesp.metadator.App
import com.bobbyesp.metadator.R
import com.bobbyesp.metadator.presentation.common.DestinationInfo
import com.bobbyesp.metadator.presentation.common.LocalNavController
import com.bobbyesp.metadator.presentation.common.mainNavigators
import com.bobbyesp.metadator.presentation.common.qualifiedName
import com.bobbyesp.ui.components.tags.RoundedTag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun ModalNavigationDrawerWrapper(
    modifier: Modifier = Modifier,
    drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed),
    gesturesEnabled: Boolean = false,
    currentNavigator: String? = null,
    navController: NavController = LocalNavController.current,
    scope: CoroutineScope = rememberCoroutineScope(),
    content: @Composable () -> Unit,
) {
    ModalNavigationDrawer(
        modifier = modifier,
        drawerState = drawerState,
        gesturesEnabled = gesturesEnabled,
        drawerContent = {
            ModalDrawerSheet(
                drawerState = drawerState,
                modifier = Modifier,
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    text = stringResource(id = R.string.app_name).uppercase(),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        letterSpacing = 4.sp,
                    ),
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .verticalScroll(
                            rememberScrollState()
                        )
                ) {
                    mainNavigators.forEach { route ->
                        val actualNavigator = route.qualifiedName()
                        val isSelected = currentNavigator == actualNavigator
                        val destinationInfo = DestinationInfo.fromRoute(route)

                        NavigationDrawerItem(
                            label = {
                            Text(
                                text = stringResource(
                                    id = destinationInfo?.title ?: R.string.unknown
                                )
                            )
                        }, selected = isSelected, onClick = {
                            if (isSelected) {
                                scope.launch {
                                    drawerState.close()
                                }
                            } else {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                                scope.launch {
                                    drawerState.close()
                                }
                            }
                        }, icon = {
                            Icon(
                                imageVector = destinationInfo?.icon ?: Icons.Rounded.Square,
                                contentDescription = destinationInfo?.title?.let {
                                    stringResource(id = it)
                                })
                        }, modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                }

                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                ) {
                    Row(
                        modifier = Modifier, verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            modifier = Modifier.padding(16.dp),
                            text = stringResource(id = R.string.app_name).uppercase() + " " + App.appVersion,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            overflow = TextOverflow.Ellipsis
                        )
                        RoundedTag(
                            text = if (App.isPlayStoreBuild) "PLAY STORE" else "FOSS",
                            shape = MaterialTheme.shapes.small
                        )
                    }
//                            Surface(
//                                modifier = Modifier
//                                    .semantics { role = Role.Tab }
//                                    .height(56.dp)
//                                    .fillMaxWidth(),
//                                onClick = {
//                                    navController.navigate(Route.SettingsNavigator.Settings)
//                                    scope.launch {
//                                        drawerState.close()
//                                    }
//                                },
//                                color = Color.Transparent
//                            ) {
//                                Row(
//                                    Modifier.padding(start = 16.dp, end = 24.dp),
//                                    verticalAlignment = Alignment.CenterVertically
//                                ) {
//                                    Icon(
//                                        imageVector = Icons.Rounded.Settings,
//                                        contentDescription = stringResource(id = R.string.settings)
//                                    )
//                                    Spacer(Modifier.width(12.dp))
//                                    Text(
//                                        text = stringResource(id = R.string.settings),
//                                        style = MaterialTheme.typography.bodyMedium,
//                                        fontWeight = FontWeight.Bold,
//                                        fontFamily = FontFamily.Monospace,
//                                        overflow = TextOverflow.Ellipsis
//                                    )
//                                }
//                            }
                }
            }
        },
        content = content
    )
}