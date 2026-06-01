package com.sonari.speak2easy.ui.onboarding

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonari.speak2easy.R
import com.sonari.speak2easy.ui.theme.SonariFonts
import com.sonari.speak2easy.ui.theme.SonariTheme

@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit,
    onSignIn: () -> Unit,
    themeLabel: String,
    onToggleTheme: () -> Unit,
) {
    val c = SonariTheme.colors
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.background)
            .systemBarsPadding(),
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp), horizontalArrangement = Arrangement.End) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, c.border, RoundedCornerShape(8.dp))
                    .clickable(onClick = onToggleTheme)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Icon(
                    if (themeLabel == "DARK") Icons.Filled.DarkMode else Icons.Filled.LightMode,
                    contentDescription = null,
                    tint = c.textSecondary,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.size(6.dp))
                Text(themeLabel, style = SonariFonts.monoCaption, color = c.textSecondary)
            }
        }

        Spacer(Modifier.weight(1f))

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Image(
                painter = painterResource(R.drawable.speak2easy_logo),
                contentDescription = "Speak2Easy",
                contentScale = ContentScale.Fit,
                modifier = Modifier.width(320.dp),
            )
            // Hero tagline — bold sans-serif, two lines, second line in accent. Mirrors iOS Welcome.
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "DAWN OF PERFECT",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    color = c.textPrimary,
                )
                Text(
                    "PRONUNCIATION",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    color = c.accent,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, c.border, RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(c.success))
                Text("JAPANESE V1.0", style = SonariFonts.monoCaption, color = c.accent)
                Text("//", style = SonariFonts.monoCaption, color = c.textTertiary)
                Text("MORE COMING", style = SonariFonts.monoCaption, color = c.textSecondary)
            }
        }

        Spacer(Modifier.weight(1f))

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                modifier = Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(14.dp)).background(c.accent).clickable(onClick = onGetStarted),
                contentAlignment = Alignment.Center,
            ) {
                Text("CREATE ACCOUNT >>", style = SonariFonts.monoMedium, color = c.buttonText)
            }
            Box(
                modifier = Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(14.dp)).border(1.5.dp, c.border, RoundedCornerShape(14.dp)).clickable(onClick = onSignIn),
                contentAlignment = Alignment.Center,
            ) {
                Text("SIGN IN >>", style = SonariFonts.monoMedium, color = c.textPrimary)
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 32.dp),
        ) {
            Text("By continuing, you agree to our", fontSize = 12.sp, color = c.textTertiary)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Terms of Use", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = c.accent, modifier = Modifier.clickable { uriHandler.openUri("https://sonarilabs.ai/terms") })
                Text("and", fontSize = 12.sp, color = c.textTertiary)
                Text("Privacy Policy", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = c.accent, modifier = Modifier.clickable { uriHandler.openUri("https://sonarilabs.ai/privacy") })
            }
        }
    }
}
