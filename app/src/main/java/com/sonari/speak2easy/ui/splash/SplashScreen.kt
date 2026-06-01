package com.sonari.speak2easy.ui.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonari.speak2easy.R
import com.sonari.speak2easy.ui.components.Mascot
import com.sonari.speak2easy.ui.components.MascotImage
import com.sonari.speak2easy.ui.theme.SonariFonts
import com.sonari.speak2easy.ui.theme.SonariTheme

/** Loading screen, mirroring iOS SplashView (wordmark logo + mascot + tagline + spinner). */
@Composable
fun SplashScreen() {
    val c = SonariTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.background)
            .padding(horizontal = 20.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.speak2easy_logo),
            contentDescription = "Speak2Easy",
            contentScale = ContentScale.Fit,
            // Full width to make the wordmark as large as possible.
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(48.dp))
        // Shrunk further to ensure the wordmark logo is the dominant element.
        MascotImage(mascot = Mascot.Studying, height = 90.dp)
        Text(
            "More lessons & languages coming soon",
            style = SonariFonts.monoSmall.copy(letterSpacing = 1.2.sp),
            color = c.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp),
        )
        CircularProgressIndicator(color = c.accent, modifier = Modifier.padding(top = 22.dp))
    }
}
