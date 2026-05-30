package com.example.speak2easy.ui.onboarding

import com.example.speak2easy.data.remote.dto.OnboardingRequest
import java.util.TimeZone

enum class Gender(val value: String, val display: String) {
    MALE("male", "Male"),
    FEMALE("female", "Female"),
    NON_BINARY("non-binary", "Non-binary"),
    PREFER_NOT_TO_SAY("prefer-not-to-say", "Prefer not to say"),
}

enum class JapaneseLevel(val value: String, val display: String) {
    BEGINNER("beginner", "Beginner — No experience"),
    INTERMEDIATE("intermediate", "Intermediate — Knows basic sounds"),
    ADVANCED("advanced", "Advanced — Comfortable with sounds"),
}

enum class LearningGoal(val value: String, val display: String) {
    TRAVEL("travel", "Travel to Japan"),
    ACADEMIC("academic", "Academic / Study"),
    BUSINESS("business", "Business / Professional"),
    CULTURAL("cultural", "Cultural Interest"),
    PERSONAL("personal", "Personal Interest"),
    ANIME("anime-manga", "Anime / Manga / Games"),
    OTHER("other", "Other"),
}

enum class PracticeTime(val value: String, val display: String, val reminderTime: String) {
    MORNING("morning", "Morning (6am – 12pm)", "08:00:00"),
    AFTERNOON("afternoon", "Afternoon (12pm – 6pm)", "13:00:00"),
    EVENING("evening", "Evening (6pm – 12am)", "19:00:00"),
    FLEXIBLE("flexible", "Flexible / No preference", "09:00:00"),
}

data class Country(val code: String, val name: String, val flag: String)

val Countries: List<Country> = listOf(
    Country("US", "United States", "🇺🇸"),
    Country("GB", "United Kingdom", "🇬🇧"),
    Country("CA", "Canada", "🇨🇦"),
    Country("AU", "Australia", "🇦🇺"),
    Country("JP", "Japan", "🇯🇵"),
    Country("IN", "India", "🇮🇳"),
    Country("DE", "Germany", "🇩🇪"),
    Country("FR", "France", "🇫🇷"),
    Country("BR", "Brazil", "🇧🇷"),
    Country("MX", "Mexico", "🇲🇽"),
    Country("ES", "Spain", "🇪🇸"),
    Country("IT", "Italy", "🇮🇹"),
    Country("NL", "Netherlands", "🇳🇱"),
    Country("SE", "Sweden", "🇸🇪"),
    Country("KR", "South Korea", "🇰🇷"),
    Country("CN", "China", "🇨🇳"),
    Country("SG", "Singapore", "🇸🇬"),
    Country("PH", "Philippines", "🇵🇭"),
    Country("ID", "Indonesia", "🇮🇩"),
    Country("VN", "Vietnam", "🇻🇳"),
).sortedBy { it.name }

val MonthNames = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
)

/** Mutable onboarding form, mirroring iOS `OnboardingData` (streamlined for the MVP). */
data class OnboardingForm(
    val displayName: String = "",
    val gender: Gender = Gender.PREFER_NOT_TO_SAY,
    val birthYear: Int = 2000,
    val birthMonth: Int = 1,
    val countryCode: String = "",
    val nativeLanguage: String = "en",
    val japaneseLevel: JapaneseLevel = JapaneseLevel.BEGINNER,
    val learningGoal: LearningGoal = LearningGoal.TRAVEL,
    val dailyGoalMinutes: Int = 10,
    val preferredPracticeTime: PracticeTime = PracticeTime.FLEXIBLE,
    val referralCode: String = "",
) {
    val isPersonalInfoComplete: Boolean
        get() = displayName.trim().length in 1..50 && countryCode.isNotEmpty()

    fun toRequest(): OnboardingRequest = OnboardingRequest(
        displayName = displayName.trim(),
        birthYear = birthYear,
        birthMonth = birthMonth,
        gender = gender.value,
        countryCode = countryCode,
        timezone = TimeZone.getDefault().id,
        nativeLanguage = nativeLanguage,
        targetLanguages = listOf("ja"),
        japaneseLevel = japaneseLevel.value,
        learningGoal = learningGoal.value,
        dailyGoalMinutes = dailyGoalMinutes,
        preferredPracticeTime = preferredPracticeTime.value,
        dailyReminderEnabled = true,
        dailyReminderTime = preferredPracticeTime.reminderTime,
        referredByCode = referralCode.trim().uppercase().ifEmpty { null },
    )
}
