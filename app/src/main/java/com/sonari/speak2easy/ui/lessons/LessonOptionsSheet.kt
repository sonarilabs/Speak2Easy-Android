package com.sonari.speak2easy.ui.lessons

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sonari.speak2easy.ui.theme.SonariFonts
import com.sonari.speak2easy.ui.theme.SonariTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonOptionsSheet(
    source: PracticeSource,
    onDismiss: () -> Unit,
    onStart: (PracticeOptions) -> Unit,
) {
    val c = SonariTheme.colors
    var options by remember { mutableStateOf(PracticeOptions()) }
    // Skip the half-expanded snap point so the sheet opens fully — no manual drag needed.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = c.surfacePrimary,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(source.headerLabel, style = SonariFonts.monoTiny, color = c.accent)
            Text(source.title, style = SonariFonts.monoLarge, color = c.textPrimary)
            Text(source.itemCountLabel, style = SonariFonts.monoSmall, color = c.textSecondary)

            Spacer(Modifier.height(12.dp))

            OptionToggle("Sequential order", "Practice in order, not shuffled", options.isSequential) {
                options = options.copy(isSequential = it)
            }
            OptionToggle("Hands-free mode", "Auto-play, then auto-record each item", options.isHandsFree) {
                options = options.copy(isHandsFree = it)
            }
            // iOS parity: Auto-play is a sub-option of Hands-free. Focus mode disables it because
            // hearing the pronunciation defeats the "no hints" purpose.
            if (options.isHandsFree) {
                SubOptionToggle(
                    title = "Play pronunciation out loud",
                    checked = options.autoPlayAudio && !options.isFocusMode,
                    enabled = !options.isFocusMode,
                ) {
                    options = options.copy(autoPlayAudio = it)
                }
            }
            OptionToggle("Focus mode", "Hide hints until you ask", options.isFocusMode) { on ->
                // Turning focus mode on forces the audio hint off.
                options = options.copy(isFocusMode = on, autoPlayAudio = if (on) false else options.autoPlayAudio)
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { onStart(options) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.buttonText),
            ) {
                Text("START", style = SonariFonts.monoSmall)
            }
        }
    }
}

@Composable
private fun SubOptionToggle(
    title: String,
    checked: Boolean,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit,
) {
    val c = SonariTheme.colors
    val alpha = if (enabled) 1f else 0.4f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
            .background(c.surfaceSecondary, RoundedCornerShape(10.dp))
            .padding(PaddingValues(horizontal = 14.dp, vertical = 6.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = SonariFonts.monoTiny,
            color = c.textPrimary.copy(alpha = alpha),
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = c.buttonText,
                checkedTrackColor = c.accent,
                uncheckedTrackColor = c.background,
            ),
        )
    }
}

@Composable
private fun OptionToggle(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val c = SonariTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = SonariFonts.monoSmall, color = c.textPrimary)
            Text(subtitle, style = SonariFonts.monoTiny, color = c.textTertiary, modifier = Modifier.padding(top = 2.dp))
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = c.buttonText,
                checkedTrackColor = c.accent,
                uncheckedTrackColor = c.surfaceSecondary,
            ),
        )
    }
}
