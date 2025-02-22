package com.bobbyesp.metadator.onboarding.domain

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.vector.ImageVector
import com.bobbyesp.utilities.ui.permissions.PermissionType

@Stable
data class PermissionItem(
    val permission: PermissionType,
    val icon: ImageVector,
    val isGranted: Boolean,
    val onClick: () -> Unit,
)
