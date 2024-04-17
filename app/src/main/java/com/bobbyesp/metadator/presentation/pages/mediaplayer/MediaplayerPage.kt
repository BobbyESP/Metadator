package com.bobbyesp.metadator.presentation.pages.mediaplayer

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bobbyesp.metadator.presentation.components.others.CollapsedPlayerHeight
import com.bobbyesp.metadator.presentation.components.others.MediaplayerSheet
import com.bobbyesp.metadator.presentation.components.others.PlayerAnimationSpec
import com.bobbyesp.ui.components.bottomsheet.draggable.rememberDraggableBottomSheetState

@Composable
fun MediaplayerPage() {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val mediaPlayerSheetState = rememberDraggableBottomSheetState(
            dismissedBound = 0.dp,
            collapsedBound = CollapsedPlayerHeight,
            expandedBound = maxHeight,
            animationSpec = PlayerAnimationSpec,
        )

        Scaffold(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it)
            ) {
                val scope = rememberCoroutineScope()

                Button(onClick = { mediaPlayerSheetState.expandSoft() }) {
                    Text(text = "Expand soft")
                }
            }
        }

        MediaplayerSheet(
            state = mediaPlayerSheetState
        )
    }
}