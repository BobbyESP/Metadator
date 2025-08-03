package com.bobbyesp.utilities.ui.layouts.lazygrid

/**
 * Data class representing the parameters used to generate a unique key for items in a lazy grid.
 *
 * This class encapsulates the necessary data to identify an item within a lazy grid, enabling
 * features like item persistence during recomposition and efficient item reuse.
 *
 * @property params A string representing additional parameters relevant to the item's identity.
 *   This could include filters, sorting criteria, or any other context-specific data that
 *   distinguishes items within the grid. Defaults to an empty string.
 * @property index The index of the item within the underlying data list. This is a crucial part of
 *   the key as it directly correlates to the item's position in the grid.
 * @property scrollOffset The vertical scroll offset of the lazy grid when this item was initially
 *   composed. This is vital for maintaining item identity during scrolling, as the visible indices
 *   can change while the underlying data and scroll offset remain constant. It helps to distinguish
 *   the same item displayed at different scroll positions.
 */
data class LazyGridKeyParams(val params: String = "", val index: Int, val scrollOffset: Int)
