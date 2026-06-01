package com.sonari.speak2easy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonari.speak2easy.ui.theme.SonariFonts
import com.sonari.speak2easy.ui.theme.SonariTheme

/**
 * Reusable "Quick Guide" tooltip sheet — mascot banner + heading + body copy. Mirrors the
 * iOS pattern of explaining unfamiliar Japanese terms (Gojūon, dakuon, etc.) without taking the
 * user out of context. Use anywhere a section needs a one-tap "what does this mean?" affordance.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickGuideSheet(
    title: String,
    body: String,
    introText: String = "These notes explain Japanese terms you'll see across lessons and charts.",
    onDismiss: () -> Unit,
) {
    val c = SonariTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = c.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Mascot banner card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(c.surfacePrimary)
                    .border(1.dp, c.accent.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                MascotImage(mascot = Mascot.Thinking, height = 96.dp)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                    Text("QUICK GUIDE", style = SonariFonts.monoCaption, color = c.accent)
                    Text(introText, style = SonariFonts.monoSmall, color = c.textPrimary)
                }
            }

            // Heading
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = c.textPrimary)

            // Body
            Text(body, style = SonariFonts.monoSmall, color = c.textSecondary)

            Spacer(Modifier.padding(top = 4.dp))
        }
    }
}
