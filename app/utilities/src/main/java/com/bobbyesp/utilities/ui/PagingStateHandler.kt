package com.bobbyesp.utilities.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.bobbyesp.utilities.R

fun <T : Any> LazyListScope.handlePagingState(
    items: LazyPagingItems<T>?,
    initialLoadingItemCount: Int = 7,
    loadingContent: @Composable LazyItemScope.() -> Unit,
    errorContent: @Composable LazyItemScope.(errorMessage: String?) -> Unit = { errorMessage ->
        // Default error content.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = errorMessage ?: stringResource(R.string.unknown_error),
            )
        }
    }
) {
    items?.let { pagingItems ->
        when {
            pagingItems.loadState.refresh is LoadState.Loading -> {
                // Initial loading state.
                items(initialLoadingItemCount) {
                    loadingContent()
                }
            }

            pagingItems.loadState.append is LoadState.Loading -> {
                // Loading more items state.
                item {
                    loadingContent()
                }
            }

            pagingItems.loadState.refresh is LoadState.Error -> {
                val error = pagingItems.loadState.refresh as LoadState.Error
                item {
                    errorContent(error.error.message)
                }
            }

            pagingItems.loadState.append is LoadState.Error -> {
                val error = pagingItems.loadState.append as LoadState.Error
                item {
                    errorContent(error.error.message)
                }
            }

            else -> {
                // Add a else branch for the case where state is not loading or error.
                // To avoid unexpected behavior in future changes.
            }
        }
    } ?: run {
        // Handle the case where items is null.
        // For example, display an empty state or an error message.
        item {
            errorContent(stringResource(id = R.string.list_items_null))
        }
    }
}