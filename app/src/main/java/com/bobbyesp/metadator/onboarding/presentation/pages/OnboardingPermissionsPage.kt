package com.bobbyesp.metadator.onboarding.presentation.pages

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.rememberTransition
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import com.bobbyesp.metadator.R
import com.bobbyesp.metadator.mediaplayer.presentation.pages.mediaplayer.player.AnimatedTextContentTransformation
import com.bobbyesp.metadator.onboarding.domain.PermissionItem
import com.bobbyesp.metadator.onboarding.presentation.components.OnboardingScreenHeader
import com.bobbyesp.ui.components.others.AdditionalInformation
import com.bobbyesp.ui.util.isDeviceInLandscape
import com.bobbyesp.utilities.ui.permission.PermissionType
import com.bobbyesp.utilities.ui.permission.PermissionType.Companion.toPermissionType
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun OnboardingPermissionsPage(
    neededPermissions: List<PermissionType>,
    onNextClick: () -> Unit = {},
) {

    val shouldShowRationale = neededPermissions.any {
        !rememberPermissionState(it.permission).status.isGranted
    }

    val allPermissionsGranted = neededPermissions.all {
        rememberPermissionState(it.permission).status.isGranted
    }

    val permissions = neededPermissions.map {
        val storagePermissionState = rememberPermissionState(it.permission)

        PermissionItem(
            permission = it.permission.toPermissionType(),
            icon = it.toPermissionIcon(),
            isGranted = storagePermissionState.status.isGranted,
            onClick = { storagePermissionState.launchPermissionRequest() }
        )
    }


    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            val transitionState = remember {
                MutableTransitionState(allPermissionsGranted)
            }

            LaunchedEffect(allPermissionsGranted) {
                transitionState.targetState = allPermissionsGranted
            }

            val transition = rememberTransition(transitionState = transitionState)


            Button(
                modifier = Modifier
                    .padding(16.dp),
                onClick = {
                    //if they are all granted, go to the next page, otherwise request the permissions
                    if (allPermissionsGranted) {
                        onNextClick()
                    } else {
                        permissions.fastForEach {
                            it.onClick()
                        }
                    }
                },
            ) {
                transition.AnimatedContent(
                    transitionSpec = { AnimatedTextContentTransformation }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val textId =
                            if (it) R.string.finish else com.bobbyesp.utilities.R.string.grant
                        val icon =
                            if (it) Icons.AutoMirrored.Rounded.KeyboardArrowRight else Icons.Rounded.Check

                        Text(text = stringResource(id = textId))
                        Icon(
                            modifier = Modifier,
                            imageVector = icon,
                            contentDescription = stringResource(id = textId)
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        if (isDeviceInLandscape()) {

            val scrollState = rememberScrollState()

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OnboardingScreenHeader(
                    modifier = Modifier.padding(16.dp).weight(0.4f),
                    title = stringResource(R.string.permissions),
                    description = stringResource(R.string.permissions_description),
                    icon = Icons.Rounded.Security
                )

                PermissionsScreenContent(
                    modifier = Modifier.weight(0.6f),
                    permissions = permissions,
                    shouldShowRationale = shouldShowRationale,
                    scrollState = scrollState
                )

            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OnboardingScreenHeader(
                    modifier = Modifier.padding(16.dp),
                    title = stringResource(R.string.permissions),
                    description = stringResource(R.string.permissions_description),
                    icon = Icons.Rounded.Security
                )
                PermissionsScreenContent(
                    modifier = Modifier,
                    permissions = permissions,
                    shouldShowRationale = shouldShowRationale,
                    scrollState = rememberScrollState()
                )
            }
        }
    }
}

@Composable
fun PermissionsScreenContent(
    modifier: Modifier = Modifier,
    permissions: List<PermissionItem>,
    shouldShowRationale: Boolean,
    scrollState: ScrollState
) {
    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            PermissionItemsGroup(
                permissionItems = permissions,
                modifier = Modifier.padding(16.dp)
            )
        }

        AnimatedContent(
            targetState = shouldShowRationale,
            modifier = Modifier
        ) { visible ->
            if (visible) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    HorizontalDivider()

                    AdditionalInformation(
                        modifier = Modifier,
                        text = stringResource(R.string.permissions_additional_information),
                        fontFamily = FontFamily.Monospace
                    )
                }

            }
        }
    }
}

@Preview
@Composable
private fun OnboardingPermissionsPagePreview() {
    val neededPermissions = listOf(
        PermissionType.READ_EXTERNAL_STORAGE,
        PermissionType.READ_MEDIA_AUDIO
    )
    MaterialTheme {
        OnboardingPermissionsPage(
            neededPermissions = neededPermissions
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PermissionItemButton(
    permissionItem: PermissionItem,
    modifier: Modifier = Modifier,
) {
    val animatedBackgroundColor by animateColorAsState(
        targetValue = if (!permissionItem.isGranted) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
    )

    val animatedTextColor by animateColorAsState(
        targetValue = if (!permissionItem.isGranted) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onPrimary
    )

    Row(
        modifier = modifier
            .background(color = MaterialTheme.colorScheme.surfaceContainer)
            .combinedClickable(
                onClick = permissionItem.onClick
            )
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(animatedBackgroundColor)
                .padding(8.dp),
            imageVector = permissionItem.icon,
            tint = animatedTextColor,
            contentDescription = null
        )

        Column {
            Text(
                text = permissionItem.permission.toPermissionString(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = permissionItem.permission.toPermissionDescription(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PermissionItemsGroup(
    permissionItems: List<PermissionItem>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        permissionItems.fastForEachIndexed { index, item ->
            PermissionItemButton(
                permissionItem = item,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(
                        when {
                            permissionItems.size == 1 -> {
                                MaterialTheme.shapes.extraLarge
                            }

                            index == 0 -> {
                                MaterialTheme.shapes.extraLarge.copy(
                                    bottomStart = MaterialTheme.shapes.medium.bottomStart,
                                    bottomEnd = MaterialTheme.shapes.medium.bottomEnd
                                )
                            }

                            index == permissionItems.lastIndex -> {
                                MaterialTheme.shapes.extraLarge.copy(
                                    topStart = MaterialTheme.shapes.medium.topStart,
                                    topEnd = MaterialTheme.shapes.medium.topEnd
                                )
                            }

                            else -> {
                                MaterialTheme.shapes.medium
                            }
                        }
                    )
            )
        }
    }
}