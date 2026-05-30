package com.example.speak2easy.ui.onboarding

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.speak2easy.ui.theme.SonariFonts
import com.example.speak2easy.ui.theme.SonariTheme

@Composable
fun LearningPreferencesScreen(viewModel: OnboardingViewModel, onBack: () -> Unit) {
    val c = SonariTheme.colors
    val form = viewModel.form

    OnboardingScaffold(
        title = "Your goals",
        subtitle = "Step 2 of 2",
        onBack = onBack,
        footer = {
            viewModel.errorMessage?.let { Text(it, style = SonariFonts.monoCaption, color = c.error) }
            SonariPrimaryButton("FINISH", loading = viewModel.isSubmitting, onClick = viewModel::submit)
        },
    ) {
        LabeledDropdown("Japanese level", JapaneseLevel.entries.toList(), form.japaneseLevel, { it.display }) { v ->
            viewModel.update { it.copy(japaneseLevel = v) }
        }
        LabeledDropdown("Learning goal", LearningGoal.entries.toList(), form.learningGoal, { it.display }) { v ->
            viewModel.update { it.copy(learningGoal = v) }
        }
        LabeledDropdown("Daily goal", listOf(5, 10, 15, 20, 30), form.dailyGoalMinutes, { "$it minutes / day" }) { v ->
            viewModel.update { it.copy(dailyGoalMinutes = v) }
        }
        LabeledDropdown("Practice time", PracticeTime.entries.toList(), form.preferredPracticeTime, { it.display }) { v ->
            viewModel.update { it.copy(preferredPracticeTime = v) }
        }
    }
}
