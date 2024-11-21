package com.bobbyesp.metadator.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Square
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingAppBarDefaults.ScreenOffset
import androidx.compose.material3.HorizontalFloatingAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.bobbyesp.metadator.R
import com.bobbyesp.metadator.presentation.common.DestinationInfo
import com.bobbyesp.metadator.presentation.common.Route
import com.bobbyesp.metadator.presentation.common.qualifiedName
import com.bobbyesp.ui.components.button.navigation.HorizontalFloatingAppBarItem

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HorizontalFloatingAppBarWrapper(
    visible: Boolean,
    expanded: Boolean,
    mainNavigators: List<Route>,
    currentNavigator: String?,
    navController: NavController,
    bottomInset: Dp
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(220, delayMillis = 90)) +
                scaleIn(
                    initialScale = 0.92f,
                    animationSpec = tween(220, delayMillis = 90)
                ),
        exit = fadeOut(animationSpec = tween(220)) +
                scaleOut(
                    targetScale = 0.92f,
                    animationSpec = tween(220)
                )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .offset(y = -ScreenOffset),
                contentAlignment = Alignment.Center
            ) {
                HorizontalFloatingAppBar(
                    modifier = Modifier
                        .then(
                            when(currentNavigator) {
                                Route.MediaplayerNavigator.Mediaplayer.qualifiedName() -> {
                                    Modifier.offset(y = -bottomInset)
                                }
                                else -> {
                                    Modifier
                                }
                            }
                        )
                        .animateContentSize(animationSpec = spring()),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    expanded = expanded,
                    content = {
                        mainNavigators.forEach { route ->
                            val actualNavigator = route.qualifiedName()
                            val isSelected = currentNavigator == actualNavigator
                            val destinationInfo = DestinationInfo.fromRoute(route)

                            HorizontalFloatingAppBarItem(
                                modifier = Modifier,
                                label = {
                                    Text(
                                        text = stringResource(
                                            id = destinationInfo?.title ?: R.string.unknown
                                        )
                                    )
                                },
                                selected = isSelected,
                                expanded = true,
                                onClick = {
                                    if (!isSelected) {
                                        navController.navigate(route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = destinationInfo?.icon ?: Icons.Rounded.Square,
                                        contentDescription = destinationInfo?.title?.let {
                                            stringResource(id = it)
                                        }
                                    )
                                }
                            )
                        }
                    }
                )
            }
        }
    }
}