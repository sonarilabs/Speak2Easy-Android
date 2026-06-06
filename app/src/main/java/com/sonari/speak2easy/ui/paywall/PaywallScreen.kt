package com.sonari.speak2easy.ui.paywall

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonari.speak2easy.R
import com.sonari.speak2easy.ui.theme.SonariFonts
import com.sonari.speak2easy.ui.theme.SonariTheme

@Composable
fun PaywallScreen(viewModel: PaywallViewModel) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val c = SonariTheme.colors
    val context = LocalContext.current
    val activity = context.findActivity()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.background)
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(20.dp))
        Image(
            painter = painterResource(R.drawable.speak2easy_logo),
            contentDescription = "Speak2Easy",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth(0.72f)
                .widthIn(max = 320.dp),
        )

        Spacer(Modifier.height(34.dp))
        Text(
            "UNLOCK SPEAK2EASY",
            style = SonariFonts.monoLarge,
            color = c.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(24.dp))
        FeaturePanel()

        Spacer(Modifier.height(34.dp))
        Text(
            ui.productTitle,
            style = SonariFonts.monoSmall,
            color = c.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            ui.priceLabel,
            fontSize = 38.sp,
            fontWeight = FontWeight.Black,
            color = c.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(c.accent)
                .padding(horizontal = 28.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(ui.trialLabel, style = SonariFonts.monoMedium, color = c.buttonText)
        }

        Spacer(Modifier.height(26.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (ui.primaryButtonEnabled) c.accent else c.surfaceSecondary)
                .clickable(enabled = ui.primaryButtonEnabled) {
                    if (activity != null) viewModel.startPurchase(activity)
                },
            contentAlignment = Alignment.Center,
        ) {
            if (ui.isPurchasing) {
                CircularProgressIndicator(color = c.buttonText, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
            } else {
                Text(
                    "START FREE TRIAL >>",
                    style = SonariFonts.monoMedium,
                    color = if (ui.primaryButtonEnabled) c.buttonText else c.textTertiary,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable(enabled = !ui.isRestoring && !ui.isPurchasing) { viewModel.restorePurchases() }
                .padding(vertical = 10.dp),
        ) {
            if (ui.isRestoring || ui.isLoadingStatus) {
                CircularProgressIndicator(color = c.accent, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
                Spacer(Modifier.size(8.dp))
            }
            Text(
                "Already subscribed? Restore Purchases",
                style = SonariFonts.monoCaption,
                color = c.textSecondary,
                textAlign = TextAlign.Center,
            )
        }

        ui.infoMessage?.let {
            Text(
                it,
                style = SonariFonts.monoCaption,
                color = c.success,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
            )
        }
        ui.errorMessage?.let {
            Text(
                it,
                style = SonariFonts.monoCaption,
                color = c.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(22.dp))
    }
}

@Composable
private fun FeaturePanel() {
    val c = SonariTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(c.cardBackground)
            .border(1.dp, c.border, RoundedCornerShape(16.dp))
            .padding(horizontal = 18.dp, vertical = 8.dp),
    ) {
        PaywallFeature(Icons.AutoMirrored.Filled.MenuBook, "Unlimited Lessons")
        PaywallFeature(Icons.Filled.GraphicEq, "Speech Recognition")
        PaywallFeature(Icons.Filled.BarChart, "Progress Tracking")
        PaywallFeature(Icons.Filled.Translate, "All Hiragana & Katakana")
        PaywallFeature(Icons.AutoMirrored.Filled.LibraryBooks, "Vocabulary Sets", showDivider = false)
    }
}

@Composable
private fun PaywallFeature(icon: ImageVector, label: String, showDivider: Boolean = true) {
    val c = SonariTheme.colors
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = c.accent, modifier = Modifier.size(24.dp))
            Text(
                label,
                style = SonariFonts.monoMedium,
                color = c.textPrimary,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
            )
            Icon(Icons.Filled.Check, contentDescription = null, tint = c.success, modifier = Modifier.size(24.dp))
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(c.divider),
            )
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
