package com.sonari.speak2easy.ui.writing

/** Matches the iOS [WritingMode]: animated guide, traced overlay, or blank free-draw. */
enum class WritingMode(val label: String, val apiValue: String) {
    GUIDE("Guide", "guide"),
    TRACE("Trace", "trace"),
    FREE_DRAW("Free Draw", "freedraw"),
}
