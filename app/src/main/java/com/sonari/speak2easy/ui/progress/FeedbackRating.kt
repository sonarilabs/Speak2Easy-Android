package com.sonari.speak2easy.ui.progress

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sonari.speak2easy.data.remote.dto.FeedbackRating
import com.sonari.speak2easy.data.remote.dto.SessionFeedback
import com.sonari.speak2easy.ui.feedback.MessageField
import com.sonari.speak2easy.ui.theme.SonariFonts
import com.sonari.speak2easy.ui.theme.SonariTheme
import kotlinx.coroutines.launch

/** Why a learner said the pronunciation feedback was wrong. [apiValue] matches the backend. */
enum class FeedbackReportReason(val apiValue: String, val title: String, val icon: ImageVector) {
    DIDNT_MATCH("didnt_match_pronunciation", "It didn't match my pronunciation", Icons.Outlined.GraphicEq),
    WRONG_SOUND("wrong_sound", "It identified the wrong sound", Icons.Outlined.GpsFixed),
    WAS_CORRECT("pronunciation_was_correct", "My pronunciation was correct", Icons.Outlined.CheckCircle),
    CORRECTION_INCORRECT("correction_incorrect", "The correction seemed incorrect", Icons.Outlined.WarningAmber),
    TOO_VAGUE("too_vague", "The feedback was too vague", Icons.AutoMirrored.Outlined.HelpOutline),
    OTHER("other", "Other", Icons.Outlined.MoreHoriz),
}

/** Thumbs up / down answering "Was this feedback helpful?". [rating] is null until the learner votes. */
@Composable
fun FeedbackThumbs(rating: FeedbackRating?, onRate: (isHelpful: Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        Thumb(isHelpful = true, selected = rating?.isHelpful == true, onClick = { onRate(true) })
        Thumb(isHelpful = false, selected = rating?.isHelpful == false, onClick = { onRate(false) })
    }
}

@Composable
private fun Thumb(isHelpful: Boolean, selected: Boolean, onClick: () -> Unit) {
    val c = SonariTheme.colors
    val tint = if (isHelpful) c.success else c.error
    val icon = when {
        isHelpful && selected -> Icons.Filled.ThumbUp
        isHelpful -> Icons.Outlined.ThumbUp
        selected -> Icons.Filled.ThumbDown
        else -> Icons.Outlined.ThumbDown
    }
    IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (selected) tint.copy(alpha = 0.12f) else c.surfacePrimary.copy(alpha = 0f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = if (isHelpful) "Feedback was helpful" else "Feedback was not helpful",
                tint = if (selected) tint else c.textTertiary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/**
 * "Help us improve" sheet shown after a thumbs-down: pick what seemed wrong, optionally
 * explain, then a thank-you state. [onSubmit] reports back whether the answer was saved.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackReportSheet(
    onSubmit: (reason: FeedbackReportReason, note: String, onResult: (Boolean) -> Unit) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = SonariTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var reason by rememberSaveable { mutableStateOf<FeedbackReportReason?>(null) }
    var note by rememberSaveable { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var showThanks by rememberSaveable { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val close: () -> Unit = {
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = c.background) {
        if (showThanks) {
            ThanksContent(onDone = close)
        } else {
            ReportForm(
                reason = reason,
                onReason = { reason = it },
                note = note,
                onNote = { note = it },
                isSubmitting = isSubmitting,
                error = error,
                onCancel = close,
                onSubmit = {
                    val picked = reason ?: return@ReportForm
                    isSubmitting = true
                    error = null
                    onSubmit(picked, note) { saved ->
                        isSubmitting = false
                        if (saved) showThanks = true else error = "Couldn't send your feedback. Please try again."
                    }
                },
            )
        }
    }
}

@Composable
private fun ReportForm(
    reason: FeedbackReportReason?,
    onReason: (FeedbackReportReason) -> Unit,
    note: String,
    onNote: (String) -> Unit,
    isSubmitting: Boolean,
    error: String?,
    onCancel: () -> Unit,
    onSubmit: () -> Unit,
) {
    val c = SonariTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
    ) {
        Box(Modifier.fillMaxWidth()) {
            Text(
                "Help us improve",
                style = SonariFonts.monoMedium,
                color = c.textPrimary,
                modifier = Modifier.align(Alignment.Center),
            )
            IconButton(onClick = onCancel, modifier = Modifier.align(Alignment.CenterEnd)) {
                Icon(Icons.Filled.Close, contentDescription = "Close", tint = c.textSecondary)
            }
        }

        Spacer(Modifier.height(12.dp))
        Text("What seemed wrong with the feedback?", style = SonariFonts.monoSmall, color = c.textSecondary)
        Spacer(Modifier.height(12.dp))

        FeedbackReportReason.entries.forEach { option ->
            ReasonRow(option, selected = reason == option) { onReason(option) }
        }

        Spacer(Modifier.height(20.dp))
        Text("Anything else you'd like us to know? (optional)", style = SonariFonts.monoSmall, color = c.textSecondary)
        Spacer(Modifier.height(8.dp))
        MessageField(
            value = note,
            onChange = onNote,
            placeholder = "Type your feedback here...",
            maxChars = SessionDetailViewModel.MAX_NOTE_CHARS,
            minHeight = 110.dp,
        )
        Text(
            "${note.length}/${SessionDetailViewModel.MAX_NOTE_CHARS}",
            style = SonariFonts.monoTiny,
            color = c.textTertiary,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        )

        error?.let {
            Text(it, style = SonariFonts.monoCaption, color = c.error, modifier = Modifier.padding(top = 8.dp))
        }

        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PillButton(
                text = "Cancel",
                filled = false,
                enabled = !isSubmitting,
                onClick = onCancel,
                modifier = Modifier.weight(1f),
            )
            PillButton(
                text = "Submit Feedback",
                filled = true,
                enabled = reason != null && !isSubmitting,
                loading = isSubmitting,
                onClick = onSubmit,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ReasonRow(option: FeedbackReportReason, selected: Boolean, onSelect: () -> Unit) {
    val c = SonariTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(vertical = 6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(c.surfacePrimary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(option.icon, contentDescription = null, tint = if (selected) c.accent else c.textSecondary, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(option.title, style = SonariFonts.monoSmall, color = c.textPrimary, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        Icon(
            if (selected) Icons.Filled.RadioButtonChecked else Icons.Outlined.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (selected) c.accent else c.textTertiary,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun ThanksContent(onDone: () -> Unit) {
    val c = SonariTheme.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 32.dp)
            .padding(top = 48.dp, bottom = 32.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(width = 200.dp, height = 120.dp)) {
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .border(BorderStroke(2.dp, c.accent.copy(alpha = 0.45f)), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = c.accent, modifier = Modifier.size(52.dp))
            }
            Text("✦", color = c.accent, style = SonariFonts.monoSmall, modifier = Modifier.offset(x = (-74).dp, y = (-34).dp))
            Text("✦", color = c.accent, style = SonariFonts.monoCaption, modifier = Modifier.offset(x = 72.dp, y = 38.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text("Thanks for letting us know!", style = SonariFonts.monoMedium, color = c.textPrimary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(
            "We'll use your feedback to make pronunciation feedback more accurate and helpful.",
            style = SonariFonts.monoSmall,
            color = c.textSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(40.dp))
        PillButton(text = "Got it", filled = true, enabled = true, onClick = onDone, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun PillButton(
    text: String,
    filled: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
) {
    val c = SonariTheme.colors
    val shape = RoundedCornerShape(26.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(52.dp)
            .clip(shape)
            .background(
                when {
                    !filled -> c.surfacePrimary
                    enabled || loading -> c.accent
                    else -> c.accent.copy(alpha = 0.4f)
                },
            )
            .then(if (filled) Modifier else Modifier.border(1.dp, c.border, shape))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
    ) {
        if (loading) {
            CircularProgressIndicator(color = c.buttonText, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
        } else {
            Text(
                text,
                style = SonariFonts.monoSmall,
                fontWeight = FontWeight.Bold,
                color = if (filled) c.buttonText else c.accent,
                maxLines = 1,
            )
        }
    }
}

/**
 * General feedback about a session's pronunciation feedback. Shown at the bottom of the
 * session detail once the learner has rated at least one item.
 */
@Composable
fun GeneralFeedbackCard(
    existing: SessionFeedback?,
    onSubmit: (comment: String, onResult: (Boolean) -> Unit) -> Unit,
) {
    val c = SonariTheme.colors
    var text by rememberSaveable { mutableStateOf("") }
    var isEditing by rememberSaveable { mutableStateOf(false) }
    var isSending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(c.surfacePrimary)
            .border(1.dp, c.border, RoundedCornerShape(12.dp))
            .padding(16.dp),
    ) {
        if (existing != null && !isEditing) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = c.success, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Thanks for your feedback on this session", style = SonariFonts.monoSmall, color = c.textPrimary)
                    Text(existing.comment, style = SonariFonts.monoCaption, color = c.textTertiary, maxLines = 2, modifier = Modifier.padding(top = 4.dp))
                }
                Text(
                    "Edit",
                    style = SonariFonts.monoCaption,
                    color = c.accent,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            text = existing.comment
                            error = null
                            isEditing = true
                        }
                        .padding(4.dp),
                )
            }
        } else {
            GeneralFeedbackForm(
                text = text,
                onText = { text = it },
                isEditing = isEditing,
                isSending = isSending,
                error = error,
                onCancel = {
                    isEditing = false
                    error = null
                },
                onSend = {
                    isSending = true
                    error = null
                    onSubmit(text) { saved ->
                        isSending = false
                        if (saved) {
                            isEditing = false
                            text = ""
                        } else {
                            error = "Couldn't send your feedback. Please try again."
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun GeneralFeedbackForm(
    text: String,
    onText: (String) -> Unit,
    isEditing: Boolean,
    isSending: Boolean,
    error: String?,
    onCancel: () -> Unit,
    onSend: () -> Unit,
) {
    val c = SonariTheme.colors
    Column {
        Row(Modifier.fillMaxWidth()) {
            Text("GENERAL FEEDBACK", style = SonariFonts.monoCaption, color = c.textSecondary, modifier = Modifier.weight(1f))
            Text("${text.length}/${SessionDetailViewModel.MAX_NOTE_CHARS}", style = SonariFonts.monoCaption, color = c.textTertiary)
        }
        Spacer(Modifier.height(8.dp))
        Text("Anything else about the feedback in this session?", style = SonariFonts.monoSmall, color = c.textPrimary)
        Spacer(Modifier.height(10.dp))
        MessageField(
            value = text,
            onChange = onText,
            placeholder = "Type your feedback here...",
            maxChars = SessionDetailViewModel.MAX_NOTE_CHARS,
            minHeight = 96.dp,
        )
        error?.let {
            Text(it, style = SonariFonts.monoCaption, color = c.error, modifier = Modifier.padding(top = 8.dp))
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isEditing) {
                Text(
                    "Cancel",
                    style = SonariFonts.monoSmall,
                    color = c.textSecondary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onCancel)
                        .padding(4.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            PillButton(
                text = "SEND",
                filled = true,
                enabled = text.isNotBlank() && !isSending,
                loading = isSending,
                onClick = onSend,
                modifier = Modifier.width(110.dp),
            )
        }
    }
}
