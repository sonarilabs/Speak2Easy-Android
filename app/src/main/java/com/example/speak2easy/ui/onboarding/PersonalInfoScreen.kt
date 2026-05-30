package com.example.speak2easy.ui.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.time.Year

@Composable
fun PersonalInfoScreen(viewModel: OnboardingViewModel, onNext: () -> Unit) {
    val form = viewModel.form
    val years = remember { (Year.now().value - 5 downTo Year.now().value - 80).toList() }
    val selectedCountry = Countries.firstOrNull { it.code == form.countryCode }

    OnboardingScaffold(
        title = "About you",
        subtitle = "Step 1 of 2",
        footer = {
            SonariPrimaryButton("CONTINUE", enabled = form.isPersonalInfoComplete, onClick = onNext)
        },
    ) {
        SonariTextField(form.displayName, { v -> viewModel.update { it.copy(displayName = v) } }, "Display name")
        LabeledDropdown("Gender", Gender.entries.toList(), form.gender, { it.display }) { g ->
            viewModel.update { it.copy(gender = g) }
        }
        LabeledDropdown("Birth month", (1..12).toList(), form.birthMonth, { MonthNames[it - 1] }) { m ->
            viewModel.update { it.copy(birthMonth = m) }
        }
        LabeledDropdown("Birth year", years, form.birthYear, { it.toString() }) { y ->
            viewModel.update { it.copy(birthYear = y) }
        }
        LabeledDropdown("Country", Countries, selectedCountry, { "${it.flag}  ${it.name}" }) { country ->
            viewModel.update { it.copy(countryCode = country.code) }
        }
        SonariTextField(form.referralCode, { v -> viewModel.update { it.copy(referralCode = v) } }, "Referral code (optional)")
    }
}
