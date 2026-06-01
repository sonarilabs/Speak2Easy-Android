package com.sonari.speak2easy.ui.feedback

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.ui.graphics.vector.ImageVector

/** Three feedback buckets — values match the backend's `category` field (free-form string). */
enum class FeedbackCategory(val apiValue: String, val shortLabel: String, val icon: ImageVector) {
    BUG("Bug Report", "Bug", Icons.Filled.BugReport),
    FEATURE("Feature Request", "Feature", Icons.Filled.Lightbulb),
    GENERAL("General Feedback", "Feedback", Icons.Filled.ChatBubble),
}
