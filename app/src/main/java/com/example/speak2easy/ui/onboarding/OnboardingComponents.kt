package com.example.speak2easy.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.speak2easy.ui.theme.SonariFonts
import com.example.speak2easy.ui.theme.SonariTheme

@Composable
fun sonariFieldColors(): TextFieldColors {
    val c = SonariTheme.colors
    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = c.textPrimary,
        unfocusedTextColor = c.textPrimary,
        focusedBorderColor = c.accent,
        unfocusedBorderColor = c.border,
        focusedLabelColor = c.accent,
        unfocusedLabelColor = c.textSecondary,
        cursorColor = c.accent,
        focusedContainerColor = c.surfacePrimary,
        unfocusedContainerColor = c.surfacePrimary,
    )
}

@Composable
fun SonariTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = SonariFonts.monoCaption) },
        singleLine = true,
        textStyle = SonariFonts.monoSmall,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = sonariFieldColors(),
        modifier = modifier.fillMaxWidth(),
    )
}

/** Label above a field, with an optional show/hide eye for passwords. Matches iOS AuthOptions fields. */
@Composable
fun SonariLabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
) {
    val c = SonariTheme.colors
    var visible by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Text(label, style = SonariFonts.monoCaption, color = c.textSecondary)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, style = SonariFonts.monoSmall, color = c.textTertiary) },
            singleLine = true,
            textStyle = SonariFonts.monoSmall,
            visualTransformation = if (isPassword && !visible) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            trailingIcon = if (!isPassword) null else {
                {
                    IconButton(onClick = { visible = !visible }) {
                        Icon(
                            if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (visible) "Hide password" else "Show password",
                            tint = c.textTertiary,
                        )
                    }
                }
            },
            colors = sonariFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> LabeledDropdown(
    label: String,
    options: List<T>,
    selected: T?,
    optionLabel: (T) -> String,
    modifier: Modifier = Modifier,
    onSelect: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selected?.let(optionLabel) ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label, style = SonariFonts.monoCaption) },
            textStyle = SonariFonts.monoSmall,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = sonariFieldColors(),
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option), style = SonariFonts.monoSmall) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
fun SonariPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val c = SonariTheme.colors
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.buttonText),
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = c.buttonText, strokeWidth = 2.dp)
        } else {
            Text(text, style = SonariFonts.monoSmall)
        }
    }
}

@Composable
fun SonariSecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = SonariTheme.colors
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
    ) {
        Text(text, style = SonariFonts.monoSmall, color = c.textPrimary)
    }
}

/** Header + scrollable content + pinned footer, used by the multi-field onboarding screens. */
@Composable
fun OnboardingScaffold(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    footer: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = SonariTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.background)
            .systemBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp),
    ) {
        if (onBack != null) {
            TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) {
                Text("← Back", style = SonariFonts.monoCaption, color = c.textSecondary)
            }
        }
        Text(title, style = SonariFonts.monoLarge, color = c.textPrimary, modifier = Modifier.padding(top = 8.dp))
        if (subtitle != null) {
            Text(subtitle, style = SonariFonts.monoCaption, color = c.textSecondary, modifier = Modifier.padding(top = 4.dp))
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            content()
        }
        Column(
            modifier = Modifier.padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            footer()
        }
    }
}
