package com.sonari.speak2easy.ui.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.sonari.speak2easy.util.TextSanitizer
import java.time.Year

@Composable
fun PersonalInfoScreen(viewModel: OnboardingViewModel, onNext: () -> Unit) {
    val form = viewModel.form
    val years = remember { (Year.now().value - 5 downTo Year.now().value - 80).toList() }
    val selectedCountry = Countries.firstOrNull { it.code == form.countryCode }
    val selectedState = if (form.countryCode == "US") UsStates.firstOrNull { it.code == form.state } else null

    OnboardingScaffold(
        title = "About you",
        subtitle = "Step 1 of 2",
        footer = {
            SonariPrimaryButton("CONTINUE", enabled = form.isPersonalInfoComplete, onClick = onNext)
        },
    ) {
        // Display name — sanitizer trims digits/symbols + collapses whitespace + caps at 40 chars.
        SonariTextField(form.displayName, { v ->
            viewModel.update { it.copy(displayName = TextSanitizer.cleanName(v)) }
        }, "Display name")

        LabeledDropdown("Gender", Gender.entries.toList(), form.gender, { it.display }) { g ->
            viewModel.update { it.copy(gender = g) }
        }

        // Birth month/year start unselected — forces a real pick so we don't get 2000-01 skew.
        LabeledDropdown(
            label = "Birth month",
            options = (1..12).toList(),
            selected = form.birthMonth,
            optionLabel = { MonthNames[it - 1] },
            placeholder = "Select month",
        ) { m -> viewModel.update { it.copy(birthMonth = m) } }

        LabeledDropdown(
            label = "Birth year",
            options = years,
            selected = form.birthYear,
            optionLabel = { it.toString() },
            placeholder = "Select year",
        ) { y -> viewModel.update { it.copy(birthYear = y) } }

        LabeledDropdown("Country", Countries, selectedCountry, { "${it.flag}  ${it.name}" }) { country ->
            // Clear state/city if switching away from US so the request body stays clean.
            viewModel.update {
                if (country.code == "US") it.copy(countryCode = country.code)
                else it.copy(countryCode = country.code, state = null, city = null)
            }
        }

        // US-only extras — backend accepts state/city up to 100 chars each.
        if (form.countryCode == "US") {
            LabeledDropdown(
                label = "State",
                options = UsStates,
                selected = selectedState,
                optionLabel = { it.name },
                placeholder = "Select state",
            ) { s ->
                // Clear the city when state changes — the previous selection almost certainly
                // doesn't exist in the new state's list. Keep the state code, drop city/Other.
                viewModel.update {
                    if (it.state == s.code) it
                    else it.copy(state = s.code, city = null)
                }
            }

            // City dropdown only after the user picks a state, so the filtered list has
            // meaningful entries instead of just OTHER. Each state's top metros come from
            // the iOS USCity table — full list in OnboardingModels.UsCities.
            val state = form.state
            if (!state.isNullOrEmpty()) {
                val cityOptions = remember(state) { citiesFor(state) }
                val matchedCity = cityOptions.firstOrNull { it.name == form.city && it !== UsCityOther }
                var isOtherSelected by remember(state) {
                    mutableStateOf(!form.city.isNullOrEmpty() && matchedCity == null)
                }
                LabeledDropdown(
                    label = "City",
                    options = cityOptions,
                    selected = when {
                        isOtherSelected -> UsCityOther
                        matchedCity != null -> matchedCity
                        else -> null
                    },
                    optionLabel = { it.name },
                    placeholder = "Select city",
                ) { picked ->
                    if (picked === UsCityOther) {
                        isOtherSelected = true
                        viewModel.update { it.copy(city = "") }
                    } else {
                        isOtherSelected = false
                        viewModel.update { it.copy(city = picked.name) }
                    }
                }
                if (isOtherSelected) {
                    // Letters / space / hyphen / apostrophe / period only — digits and stray
                    // symbols are stripped on every keystroke (cleanCity). The CONTINUE button
                    // gate (isPersonalInfoComplete → isValidCity) also enforces ≥3 chars.
                    SonariTextField(form.city ?: "", { v ->
                        viewModel.update { it.copy(city = TextSanitizer.cleanCity(v)) }
                    }, "City name (letters only, min 3)")
                }
            }
        }
    }
}
