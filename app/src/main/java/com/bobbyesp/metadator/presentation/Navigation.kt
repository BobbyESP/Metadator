package com.bobbyesp.metadator.presentation

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import com.bobbyesp.metadator.presentation.common.LocalNavController
import com.bobbyesp.metadator.presentation.common.LocalSnackbarHostState
import com.bobbyesp.metadator.presentation.common.Route
import com.bobbyesp.ui.motion.animatedComposable

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun Navigator() {
    val navController = LocalNavController.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()

    val currentRootRoute = rememberSaveable(navBackStackEntry, key = "currentRootRoute") {
        mutableStateOf(
            navBackStackEntry?.destination?.parent?.route ?: Route.MetadatorNavigator.route
        )
    }
    val currentRoute = rememberSaveable(navBackStackEntry, key = "currentRoute") {
        mutableStateOf(
            navBackStackEntry?.destination?.route ?: Route.MetadatorNavigator.HomePage.route
        )
    }

    val snackbarHostState = LocalSnackbarHostState.current

    val showSnackbarMessage: suspend (String) -> Unit = { message ->
        snackbarHostState.showSnackbar(message)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState
                ) { dataReceived ->
                    Snackbar(
                        modifier = Modifier,
                        snackbarData = dataReceived,
                        containerColor = MaterialTheme.colorScheme.inverseSurface,
                        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    )
                }
            },
        ) {paddingValues ->
            NavHost(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(paddingValues)
                    .align(Alignment.Center),
                navController = navController,
                startDestination = Route.MetadatorNavigator.route,
                route = Route.MainHost.route,
            ) {
                animatedComposable(Route.MetadatorNavigator.route) {

                }
            }
        }
    }
}