package com.sonari.speak2easy.ui.feedback

import android.graphics.ImageDecoder
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sonari.speak2easy.di.LocalAppContainer
import com.sonari.speak2easy.ui.theme.SonariFonts
import com.sonari.speak2easy.ui.theme.SonariTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Full-screen Report / Feedback form. Mirrors iOS [FeedbackView]:
 * - Category pills (Bug / Feature / Feedback)
 * - Multiline message with 1000-char counter
 * - Up to 3 photo attachments via PhotoPicker
 * - SEND posts to /users/{userId}/feedback
 * - Success state with checkmark, then back to caller on dismiss
 */
@Composable
fun FeedbackScreen(
    initialCategory: FeedbackCategory = FeedbackCategory.GENERAL,
    sourceScreen: String,
    onDismiss: () -> Unit,
) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val c = SonariTheme.colors
    val haptics = container.hapticsManager
    val viewModel: FeedbackViewModel = viewModel(
        factory = FeedbackViewModel.Factory(
            initialCategory = initialCategory,
            currentScreen = sourceScreen,
            feedbackRepo = container.feedbackRepository,
            authRepo = container.authRepository,
            appContext = context.applicationContext,
        ),
    )
    val state = viewModel.state

    BackHandler { onDismiss() }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(
            maxItems = FeedbackViewModel.MAX_IMAGES,
        ),
    ) { uris ->
        if (uris.isNotEmpty()) {
            haptics.playSelection()
            viewModel.addAttachments(uris)
        }
    }

    LaunchedEffect(state.sent) {
        if (state.sent) haptics.playCorrect()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.background)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        // Header — close button + title
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Close", tint = c.textPrimary, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.weight(1f))
            Text("HELP & REPORT", style = SonariFonts.monoLarge, color = c.textPrimary)
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.size(40.dp))  // mirror the close button to keep title centered
        }

        Spacer(Modifier.height(20.dp))

        if (state.sent) {
            SuccessState(onDismiss = onDismiss)
        } else {
            FormBody(
                state = state,
                attachments = viewModel.attachments,
                onCategory = viewModel::setCategory,
                onMessage = viewModel::setMessage,
                onAddPhotos = {
                    photoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                onRemovePhoto = viewModel::removeAttachment,
                onSubmit = {
                    haptics.playSelection()
                    viewModel.submit()
                },
            )
        }
    }
}

// region — Form body
@Composable
private fun FormBody(
    state: FeedbackUiState,
    attachments: List<Uri>,
    onCategory: (FeedbackCategory) -> Unit,
    onMessage: (String) -> Unit,
    onAddPhotos: () -> Unit,
    onRemovePhoto: (Int) -> Unit,
    onSubmit: () -> Unit,
) {
    val c = SonariTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        SectionLabel("Category")
        Spacer(Modifier.height(8.dp))
        CategoryRow(selected = state.category, onChange = onCategory)

        Spacer(Modifier.height(24.dp))

        SectionLabel("Message")
        Spacer(Modifier.height(8.dp))
        MessageField(
            value = state.message,
            onChange = onMessage,
            placeholder = when (state.category) {
                FeedbackCategory.BUG -> "What went wrong? Steps to reproduce help us a lot."
                FeedbackCategory.FEATURE -> "What would you like to see in Speak2Easy?"
                FeedbackCategory.GENERAL -> "Share your thoughts about the app."
            },
        )
        Text(
            "${state.message.length} / ${FeedbackViewModel.MAX_MESSAGE_CHARS}",
            style = SonariFonts.monoTiny,
            color = c.textTertiary,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            textAlign = TextAlign.End,
        )

        Spacer(Modifier.height(24.dp))

        SectionLabel("Attachments  (up to ${FeedbackViewModel.MAX_IMAGES})")
        Spacer(Modifier.height(8.dp))
        AttachmentRow(
            attachments = attachments,
            isProcessing = state.isProcessingImages,
            onAdd = onAddPhotos,
            onRemove = onRemovePhoto,
        )

        Spacer(Modifier.height(24.dp))

        state.errorMessage?.let {
            Text(it, style = SonariFonts.monoCaption, color = c.error, modifier = Modifier.padding(bottom = 12.dp))
        }

        SendButton(
            enabled = state.message.isNotBlank() && !state.isSubmitting,
            isSubmitting = state.isSubmitting,
            onClick = onSubmit,
        )

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = SonariFonts.monoCaption, color = SonariTheme.colors.textSecondary)
}

@Composable
private fun CategoryRow(selected: FeedbackCategory, onChange: (FeedbackCategory) -> Unit) {
    val c = SonariTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        FeedbackCategory.entries.forEach { cat ->
            val active = cat == selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (active) c.accent else c.surfacePrimary)
                    .border(1.dp, if (active) c.accent else c.border, RoundedCornerShape(12.dp))
                    .clickable { onChange(cat) }
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    cat.icon,
                    contentDescription = null,
                    tint = if (active) c.buttonText else c.textSecondary,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    cat.shortLabel,
                    style = SonariFonts.monoCaption,
                    color = if (active) c.buttonText else c.textPrimary,
                )
            }
        }
    }
}

@Composable
private fun MessageField(value: String, onChange: (String) -> Unit, placeholder: String) {
    val c = SonariTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 160.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(c.surfacePrimary)
            .border(1.dp, c.border, RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = { onChange(it.take(FeedbackViewModel.MAX_MESSAGE_CHARS)) },
            modifier = Modifier.fillMaxSize(),
            textStyle = TextStyle(color = c.textPrimary, fontSize = 14.sp),
            cursorBrush = SolidColor(c.accent),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )
        if (value.isEmpty()) {
            Text(
                placeholder,
                style = SonariFonts.monoCaption,
                color = c.textTertiary,
            )
        }
    }
}

@Composable
private fun AttachmentRow(
    attachments: List<Uri>,
    isProcessing: Boolean,
    onAdd: () -> Unit,
    onRemove: (Int) -> Unit,
) {
    val c = SonariTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        attachments.forEachIndexed { index, uri ->
            AttachmentThumbnail(uri = uri, onRemove = { onRemove(index) }, modifier = Modifier.weight(1f))
        }
        if (attachments.size < FeedbackViewModel.MAX_IMAGES) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(96.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(c.surfacePrimary)
                    .border(1.dp, c.border, RoundedCornerShape(12.dp))
                    .clickable(enabled = !isProcessing) { onAdd() },
                contentAlignment = Alignment.Center,
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(color = c.accent, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                } else {
                    Icon(Icons.Filled.Add, contentDescription = "Add photo", tint = c.textSecondary)
                }
            }
        }
        // Fill remaining slots with invisible weight to keep thumbnails consistent width.
        val emptySlots = FeedbackViewModel.MAX_IMAGES - attachments.size - 1
        if (emptySlots > 0) {
            repeat(emptySlots.coerceAtLeast(0)) { Spacer(Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun AttachmentThumbnail(uri: Uri, onRemove: () -> Unit, modifier: Modifier = Modifier) {
    val c = SonariTheme.colors
    val context = LocalContext.current
    var bitmap by remember(uri) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(uri) {
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    val longest = maxOf(info.size.width, info.size.height)
                    if (longest > 256) {
                        val s = 256f / longest
                        decoder.setTargetSize((info.size.width * s).toInt(), (info.size.height * s).toInt())
                    }
                }.asImageBitmap()
            }.getOrNull()
        }
    }
    Box(
        modifier = modifier
            .height(96.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(c.surfacePrimary)
            .border(1.dp, c.border, RoundedCornerShape(12.dp)),
    ) {
        bitmap?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = c.accent, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
        }
        // Remove ✕ chip in the top-right corner.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(22.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable { onRemove() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun SendButton(enabled: Boolean, isSubmitting: Boolean, onClick: () -> Unit) {
    val c = SonariTheme.colors
    val alpha = if (enabled) 1f else 0.4f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(c.accent.copy(alpha = alpha))
            .clickable(enabled = enabled, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isSubmitting) {
            CircularProgressIndicator(color = c.buttonText, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
        } else {
            Text("SEND FEEDBACK", style = SonariFonts.monoSmall, color = c.buttonText, fontWeight = FontWeight.Bold)
        }
    }
}
// endregion

// region — Success state
@Composable
private fun SuccessState(onDismiss: () -> Unit) {
    val c = SonariTheme.colors
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        SuccessGlyph(icon = Icons.Filled.CheckCircle, tint = c.success)
        Spacer(Modifier.height(20.dp))
        Text("FEEDBACK SENT", style = SonariFonts.monoLarge, color = c.textPrimary)
        Spacer(Modifier.height(8.dp))
        Text(
            "Thanks — we read every message.",
            style = SonariFonts.monoSmall,
            color = c.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
        )
        Spacer(Modifier.height(28.dp))
        Row(
            modifier = Modifier
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(c.accent)
                .clickable(onClick = onDismiss)
                .padding(horizontal = 28.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("DONE", style = SonariFonts.monoSmall, color = c.buttonText, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SuccessGlyph(icon: ImageVector, tint: Color) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(40.dp))
            .background(tint.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(48.dp))
    }
}
// endregion
