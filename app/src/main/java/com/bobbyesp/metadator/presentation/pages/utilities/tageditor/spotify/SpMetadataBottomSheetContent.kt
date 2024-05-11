package com.bobbyesp.metadator.presentation.pages.utilities.tageditor.spotify

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpMetadataBottomSheetContent(
    modifier: Modifier = Modifier,
    state: SheetState,
    viewModel: SpMetadataBottomSheetContentViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()

    //TODO: Add the sheet content
}