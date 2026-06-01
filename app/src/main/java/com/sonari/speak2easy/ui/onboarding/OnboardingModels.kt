package com.sonari.speak2easy.ui.onboarding

import com.sonari.speak2easy.data.remote.dto.OnboardingRequest
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

enum class PracticeTime(
    val value: String,
    val display: String,
    val shortLabel: String,
    val timeRange: String,
    /**
     * Lower-bound hour of the tile's window — used both as the daily reminder hour
     * (local AlarmManager) and the backend's `reminder_time`. Morning fires at 6am,
     * Afternoon at noon, Evening at 6pm. Flexible has no window so we default to 8pm,
     * the most common "evening study" slot for learners without a strict schedule.
     */
    val lowerBoundHour: Int,
) {
    MORNING("morning", "Morning (6am – 12pm)", "Morning", "6am – 12pm", 6),
    AFTERNOON("afternoon", "Afternoon (12pm – 6pm)", "Afternoon", "12pm – 6pm", 12),
    EVENING("evening", "Evening (6pm – 12am)", "Evening", "6pm – 12am", 18),
    FLEXIBLE("flexible", "Flexible / No preference", "Flexible", "Anytime", 20);

    /** HH:mm:ss string for the backend's `daily_reminder_time` payload. */
    val reminderTime: String get() = "%02d:00:00".format(lowerBoundHour)

    /** Minute-of-day for [com.sonari.speak2easy.data.prefs.SonariPreferences.reminderMinuteOfDay]. */
    val reminderMinuteOfDay: Int get() = lowerBoundHour * 60
}

data class Country(val code: String, val name: String, val flag: String)

/** Full ISO-3166-1 alpha-2 list. Sorted by display name. Flag emoji computed from the country code. */
val Countries: List<Country> = listOf(
    "AF" to "Afghanistan", "AX" to "Åland Islands", "AL" to "Albania", "DZ" to "Algeria",
    "AS" to "American Samoa", "AD" to "Andorra", "AO" to "Angola", "AI" to "Anguilla",
    "AQ" to "Antarctica", "AG" to "Antigua and Barbuda", "AR" to "Argentina", "AM" to "Armenia",
    "AW" to "Aruba", "AU" to "Australia", "AT" to "Austria", "AZ" to "Azerbaijan",
    "BS" to "Bahamas", "BH" to "Bahrain", "BD" to "Bangladesh", "BB" to "Barbados",
    "BY" to "Belarus", "BE" to "Belgium", "BZ" to "Belize", "BJ" to "Benin",
    "BM" to "Bermuda", "BT" to "Bhutan", "BO" to "Bolivia", "BA" to "Bosnia and Herzegovina",
    "BW" to "Botswana", "BV" to "Bouvet Island", "BR" to "Brazil", "IO" to "British Indian Ocean Territory",
    "BN" to "Brunei", "BG" to "Bulgaria", "BF" to "Burkina Faso", "BI" to "Burundi",
    "CV" to "Cabo Verde", "KH" to "Cambodia", "CM" to "Cameroon", "CA" to "Canada",
    "KY" to "Cayman Islands", "CF" to "Central African Republic", "TD" to "Chad", "CL" to "Chile",
    "CN" to "China", "CX" to "Christmas Island", "CC" to "Cocos (Keeling) Islands", "CO" to "Colombia",
    "KM" to "Comoros", "CG" to "Congo", "CD" to "Congo (DRC)", "CK" to "Cook Islands",
    "CR" to "Costa Rica", "CI" to "Côte d'Ivoire", "HR" to "Croatia", "CU" to "Cuba",
    "CW" to "Curaçao", "CY" to "Cyprus", "CZ" to "Czechia", "DK" to "Denmark",
    "DJ" to "Djibouti", "DM" to "Dominica", "DO" to "Dominican Republic", "EC" to "Ecuador",
    "EG" to "Egypt", "SV" to "El Salvador", "GQ" to "Equatorial Guinea", "ER" to "Eritrea",
    "EE" to "Estonia", "SZ" to "Eswatini", "ET" to "Ethiopia", "FK" to "Falkland Islands",
    "FO" to "Faroe Islands", "FJ" to "Fiji", "FI" to "Finland", "FR" to "France",
    "GF" to "French Guiana", "PF" to "French Polynesia", "TF" to "French Southern Territories",
    "GA" to "Gabon", "GM" to "Gambia", "GE" to "Georgia", "DE" to "Germany",
    "GH" to "Ghana", "GI" to "Gibraltar", "GR" to "Greece", "GL" to "Greenland",
    "GD" to "Grenada", "GP" to "Guadeloupe", "GU" to "Guam", "GT" to "Guatemala",
    "GG" to "Guernsey", "GN" to "Guinea", "GW" to "Guinea-Bissau", "GY" to "Guyana",
    "HT" to "Haiti", "HM" to "Heard & McDonald Islands", "VA" to "Holy See", "HN" to "Honduras",
    "HK" to "Hong Kong", "HU" to "Hungary", "IS" to "Iceland", "IN" to "India",
    "ID" to "Indonesia", "IR" to "Iran", "IQ" to "Iraq", "IE" to "Ireland",
    "IM" to "Isle of Man", "IL" to "Israel", "IT" to "Italy", "JM" to "Jamaica",
    "JP" to "Japan", "JE" to "Jersey", "JO" to "Jordan", "KZ" to "Kazakhstan",
    "KE" to "Kenya", "KI" to "Kiribati", "KP" to "North Korea", "KR" to "South Korea",
    "KW" to "Kuwait", "KG" to "Kyrgyzstan", "LA" to "Laos", "LV" to "Latvia",
    "LB" to "Lebanon", "LS" to "Lesotho", "LR" to "Liberia", "LY" to "Libya",
    "LI" to "Liechtenstein", "LT" to "Lithuania", "LU" to "Luxembourg", "MO" to "Macao",
    "MG" to "Madagascar", "MW" to "Malawi", "MY" to "Malaysia", "MV" to "Maldives",
    "ML" to "Mali", "MT" to "Malta", "MH" to "Marshall Islands", "MQ" to "Martinique",
    "MR" to "Mauritania", "MU" to "Mauritius", "YT" to "Mayotte", "MX" to "Mexico",
    "FM" to "Micronesia", "MD" to "Moldova", "MC" to "Monaco", "MN" to "Mongolia",
    "ME" to "Montenegro", "MS" to "Montserrat", "MA" to "Morocco", "MZ" to "Mozambique",
    "MM" to "Myanmar", "NA" to "Namibia", "NR" to "Nauru", "NP" to "Nepal",
    "NL" to "Netherlands", "NC" to "New Caledonia", "NZ" to "New Zealand", "NI" to "Nicaragua",
    "NE" to "Niger", "NG" to "Nigeria", "NU" to "Niue", "NF" to "Norfolk Island",
    "MK" to "North Macedonia", "MP" to "Northern Mariana Islands", "NO" to "Norway",
    "OM" to "Oman", "PK" to "Pakistan", "PW" to "Palau", "PS" to "Palestine",
    "PA" to "Panama", "PG" to "Papua New Guinea", "PY" to "Paraguay", "PE" to "Peru",
    "PH" to "Philippines", "PN" to "Pitcairn Islands", "PL" to "Poland", "PT" to "Portugal",
    "PR" to "Puerto Rico", "QA" to "Qatar", "RE" to "Réunion", "RO" to "Romania",
    "RU" to "Russia", "RW" to "Rwanda", "BL" to "Saint Barthélemy", "SH" to "Saint Helena",
    "KN" to "Saint Kitts and Nevis", "LC" to "Saint Lucia", "MF" to "Saint Martin",
    "PM" to "Saint Pierre and Miquelon", "VC" to "Saint Vincent and the Grenadines",
    "WS" to "Samoa", "SM" to "San Marino", "ST" to "São Tomé and Príncipe", "SA" to "Saudi Arabia",
    "SN" to "Senegal", "RS" to "Serbia", "SC" to "Seychelles", "SL" to "Sierra Leone",
    "SG" to "Singapore", "SX" to "Sint Maarten", "SK" to "Slovakia", "SI" to "Slovenia",
    "SB" to "Solomon Islands", "SO" to "Somalia", "ZA" to "South Africa",
    "GS" to "South Georgia & South Sandwich Is.", "SS" to "South Sudan", "ES" to "Spain",
    "LK" to "Sri Lanka", "SD" to "Sudan", "SR" to "Suriname", "SJ" to "Svalbard and Jan Mayen",
    "SE" to "Sweden", "CH" to "Switzerland", "SY" to "Syria", "TW" to "Taiwan",
    "TJ" to "Tajikistan", "TZ" to "Tanzania", "TH" to "Thailand", "TL" to "Timor-Leste",
    "TG" to "Togo", "TK" to "Tokelau", "TO" to "Tonga", "TT" to "Trinidad and Tobago",
    "TN" to "Tunisia", "TR" to "Türkiye", "TM" to "Turkmenistan", "TC" to "Turks and Caicos Islands",
    "TV" to "Tuvalu", "UG" to "Uganda", "UA" to "Ukraine", "AE" to "United Arab Emirates",
    "GB" to "United Kingdom", "US" to "United States", "UM" to "U.S. Minor Outlying Islands",
    "UY" to "Uruguay", "UZ" to "Uzbekistan", "VU" to "Vanuatu", "VE" to "Venezuela",
    "VN" to "Vietnam", "VG" to "Virgin Islands (British)", "VI" to "Virgin Islands (U.S.)",
    "WF" to "Wallis and Futuna", "EH" to "Western Sahara", "YE" to "Yemen",
    "ZM" to "Zambia", "ZW" to "Zimbabwe",
).map { (code, name) -> Country(code, name, flagEmoji(code)) }.sortedBy { it.name }

/** Convert an ISO alpha-2 country code into its emoji flag (regional indicator pair). */
private fun flagEmoji(code: String): String {
    if (code.length != 2) return ""
    val base = 0x1F1E6 - 'A'.code
    val first = code[0].uppercaseChar().code + base
    val second = code[1].uppercaseChar().code + base
    return String(Character.toChars(first)) + String(Character.toChars(second))
}

data class UsState(val code: String, val name: String)

/** US states + DC, sorted by name. Backend stores `state` as free-text up to 100 chars. */
val UsStates: List<UsState> = listOf(
    "AL" to "Alabama", "AK" to "Alaska", "AZ" to "Arizona", "AR" to "Arkansas",
    "CA" to "California", "CO" to "Colorado", "CT" to "Connecticut", "DE" to "Delaware",
    "DC" to "District of Columbia", "FL" to "Florida", "GA" to "Georgia", "HI" to "Hawaii",
    "ID" to "Idaho", "IL" to "Illinois", "IN" to "Indiana", "IA" to "Iowa",
    "KS" to "Kansas", "KY" to "Kentucky", "LA" to "Louisiana", "ME" to "Maine",
    "MD" to "Maryland", "MA" to "Massachusetts", "MI" to "Michigan", "MN" to "Minnesota",
    "MS" to "Mississippi", "MO" to "Missouri", "MT" to "Montana", "NE" to "Nebraska",
    "NV" to "Nevada", "NH" to "New Hampshire", "NJ" to "New Jersey", "NM" to "New Mexico",
    "NY" to "New York", "NC" to "North Carolina", "ND" to "North Dakota", "OH" to "Ohio",
    "OK" to "Oklahoma", "OR" to "Oregon", "PA" to "Pennsylvania", "RI" to "Rhode Island",
    "SC" to "South Carolina", "SD" to "South Dakota", "TN" to "Tennessee", "TX" to "Texas",
    "UT" to "Utah", "VT" to "Vermont", "VA" to "Virginia", "WA" to "Washington",
    "WV" to "West Virginia", "WI" to "Wisconsin", "WY" to "Wyoming",
).map { (code, name) -> UsState(code, name) }.sortedBy { it.name }

data class UsCity(val name: String, val stateCode: String?)

/** Sentinel meaning "let me type my own city" — shown last in the dropdown. */
val UsCityOther = UsCity("Other (type below)", null)

/**
 * Major US cities indexed by state, ported verbatim from iOS `USCity.allCities`. Each list
 * is the top ~3–8 cities per state by population/relevance — covers the common case without
 * exploding the dropdown. The OTHER sentinel is appended by [citiesFor] when the state is
 * known but the user wants to type something custom.
 */
val UsCities: List<UsCity> = listOf(
    // Alabama
    UsCity("Birmingham", "AL"), UsCity("Montgomery", "AL"), UsCity("Huntsville", "AL"),
    UsCity("Mobile", "AL"), UsCity("Tuscaloosa", "AL"),
    // Alaska
    UsCity("Anchorage", "AK"), UsCity("Fairbanks", "AK"), UsCity("Juneau", "AK"),
    // Arizona
    UsCity("Phoenix", "AZ"), UsCity("Tucson", "AZ"), UsCity("Mesa", "AZ"),
    UsCity("Scottsdale", "AZ"), UsCity("Tempe", "AZ"),
    // Arkansas
    UsCity("Little Rock", "AR"), UsCity("Fayetteville", "AR"), UsCity("Fort Smith", "AR"),
    // California
    UsCity("Los Angeles", "CA"), UsCity("San Francisco", "CA"), UsCity("San Diego", "CA"),
    UsCity("San Jose", "CA"), UsCity("Sacramento", "CA"), UsCity("Oakland", "CA"),
    UsCity("Fresno", "CA"), UsCity("Long Beach", "CA"), UsCity("Irvine", "CA"),
    UsCity("Santa Monica", "CA"), UsCity("Palo Alto", "CA"), UsCity("Berkeley", "CA"),
    // Colorado
    UsCity("Denver", "CO"), UsCity("Colorado Springs", "CO"), UsCity("Aurora", "CO"),
    UsCity("Boulder", "CO"), UsCity("Fort Collins", "CO"),
    // Connecticut
    UsCity("Hartford", "CT"), UsCity("New Haven", "CT"), UsCity("Stamford", "CT"),
    UsCity("Bridgeport", "CT"),
    // Delaware
    UsCity("Wilmington", "DE"), UsCity("Dover", "DE"), UsCity("Newark", "DE"),
    // Florida
    UsCity("Miami", "FL"), UsCity("Orlando", "FL"), UsCity("Tampa", "FL"),
    UsCity("Jacksonville", "FL"), UsCity("Fort Lauderdale", "FL"),
    UsCity("St. Petersburg", "FL"), UsCity("Tallahassee", "FL"), UsCity("Gainesville", "FL"),
    // Georgia
    UsCity("Atlanta", "GA"), UsCity("Savannah", "GA"), UsCity("Augusta", "GA"),
    UsCity("Athens", "GA"), UsCity("Macon", "GA"),
    // Hawaii
    UsCity("Honolulu", "HI"), UsCity("Hilo", "HI"), UsCity("Kailua", "HI"),
    // Idaho
    UsCity("Boise", "ID"), UsCity("Idaho Falls", "ID"), UsCity("Nampa", "ID"),
    // Illinois
    UsCity("Chicago", "IL"), UsCity("Aurora", "IL"), UsCity("Naperville", "IL"),
    UsCity("Springfield", "IL"), UsCity("Evanston", "IL"),
    // Indiana
    UsCity("Indianapolis", "IN"), UsCity("Fort Wayne", "IN"), UsCity("South Bend", "IN"),
    UsCity("Bloomington", "IN"),
    // Iowa
    UsCity("Des Moines", "IA"), UsCity("Cedar Rapids", "IA"), UsCity("Iowa City", "IA"),
    // Kansas
    UsCity("Wichita", "KS"), UsCity("Kansas City", "KS"), UsCity("Topeka", "KS"),
    UsCity("Lawrence", "KS"),
    // Kentucky
    UsCity("Louisville", "KY"), UsCity("Lexington", "KY"), UsCity("Bowling Green", "KY"),
    // Louisiana
    UsCity("New Orleans", "LA"), UsCity("Baton Rouge", "LA"), UsCity("Shreveport", "LA"),
    // Maine
    UsCity("Portland", "ME"), UsCity("Augusta", "ME"), UsCity("Bangor", "ME"),
    // Maryland
    UsCity("Baltimore", "MD"), UsCity("Annapolis", "MD"), UsCity("Rockville", "MD"),
    UsCity("Bethesda", "MD"),
    // Massachusetts
    UsCity("Boston", "MA"), UsCity("Cambridge", "MA"), UsCity("Worcester", "MA"),
    UsCity("Springfield", "MA"),
    // Michigan
    UsCity("Detroit", "MI"), UsCity("Grand Rapids", "MI"), UsCity("Ann Arbor", "MI"),
    UsCity("Lansing", "MI"),
    // Minnesota
    UsCity("Minneapolis", "MN"), UsCity("St. Paul", "MN"), UsCity("Rochester", "MN"),
    UsCity("Duluth", "MN"),
    // Mississippi
    UsCity("Jackson", "MS"), UsCity("Gulfport", "MS"), UsCity("Biloxi", "MS"),
    // Missouri
    UsCity("Kansas City", "MO"), UsCity("St. Louis", "MO"), UsCity("Springfield", "MO"),
    UsCity("Columbia", "MO"),
    // Montana
    UsCity("Billings", "MT"), UsCity("Missoula", "MT"), UsCity("Helena", "MT"),
    // Nebraska
    UsCity("Omaha", "NE"), UsCity("Lincoln", "NE"),
    // Nevada
    UsCity("Las Vegas", "NV"), UsCity("Reno", "NV"), UsCity("Henderson", "NV"),
    // New Hampshire
    UsCity("Manchester", "NH"), UsCity("Concord", "NH"), UsCity("Nashua", "NH"),
    // New Jersey
    UsCity("Newark", "NJ"), UsCity("Jersey City", "NJ"), UsCity("Trenton", "NJ"),
    UsCity("Princeton", "NJ"), UsCity("Hoboken", "NJ"),
    // New Mexico
    UsCity("Albuquerque", "NM"), UsCity("Santa Fe", "NM"), UsCity("Las Cruces", "NM"),
    // New York
    UsCity("New York City", "NY"), UsCity("Buffalo", "NY"), UsCity("Rochester", "NY"),
    UsCity("Albany", "NY"), UsCity("Syracuse", "NY"), UsCity("Yonkers", "NY"),
    // North Carolina
    UsCity("Charlotte", "NC"), UsCity("Raleigh", "NC"), UsCity("Durham", "NC"),
    UsCity("Greensboro", "NC"), UsCity("Asheville", "NC"),
    // North Dakota
    UsCity("Fargo", "ND"), UsCity("Bismarck", "ND"),
    // Ohio
    UsCity("Columbus", "OH"), UsCity("Cleveland", "OH"), UsCity("Cincinnati", "OH"),
    UsCity("Toledo", "OH"), UsCity("Akron", "OH"),
    // Oklahoma
    UsCity("Oklahoma City", "OK"), UsCity("Tulsa", "OK"), UsCity("Norman", "OK"),
    // Oregon
    UsCity("Portland", "OR"), UsCity("Eugene", "OR"), UsCity("Salem", "OR"),
    UsCity("Bend", "OR"),
    // Pennsylvania
    UsCity("Philadelphia", "PA"), UsCity("Pittsburgh", "PA"), UsCity("Harrisburg", "PA"),
    UsCity("Allentown", "PA"),
    // Rhode Island
    UsCity("Providence", "RI"), UsCity("Warwick", "RI"), UsCity("Newport", "RI"),
    // South Carolina
    UsCity("Charleston", "SC"), UsCity("Columbia", "SC"), UsCity("Greenville", "SC"),
    // South Dakota
    UsCity("Sioux Falls", "SD"), UsCity("Rapid City", "SD"),
    // Tennessee
    UsCity("Nashville", "TN"), UsCity("Memphis", "TN"), UsCity("Knoxville", "TN"),
    UsCity("Chattanooga", "TN"),
    // Texas
    UsCity("Houston", "TX"), UsCity("Dallas", "TX"), UsCity("Austin", "TX"),
    UsCity("San Antonio", "TX"), UsCity("Fort Worth", "TX"), UsCity("El Paso", "TX"),
    UsCity("Plano", "TX"), UsCity("Arlington", "TX"),
    // Utah
    UsCity("Salt Lake City", "UT"), UsCity("Provo", "UT"), UsCity("Park City", "UT"),
    // Vermont
    UsCity("Burlington", "VT"), UsCity("Montpelier", "VT"),
    // Virginia
    UsCity("Virginia Beach", "VA"), UsCity("Richmond", "VA"), UsCity("Norfolk", "VA"),
    UsCity("Arlington", "VA"), UsCity("Alexandria", "VA"),
    // Washington
    UsCity("Seattle", "WA"), UsCity("Spokane", "WA"), UsCity("Tacoma", "WA"),
    UsCity("Bellevue", "WA"), UsCity("Olympia", "WA"),
    // West Virginia
    UsCity("Charleston", "WV"), UsCity("Huntington", "WV"),
    // Wisconsin
    UsCity("Milwaukee", "WI"), UsCity("Madison", "WI"), UsCity("Green Bay", "WI"),
    // Wyoming
    UsCity("Cheyenne", "WY"), UsCity("Casper", "WY"), UsCity("Jackson", "WY"),
    // Washington D.C.
    UsCity("Washington", "DC"),
)

/**
 * Cities filtered to [stateCode], alphabetized, with the OTHER sentinel always last.
 * Returns just the OTHER option if no state is selected — keeps the dropdown usable
 * even before the user picks a state (though we hide the City field in that case).
 */
fun citiesFor(stateCode: String?): List<UsCity> {
    if (stateCode.isNullOrEmpty()) return listOf(UsCityOther)
    return UsCities.filter { it.stateCode == stateCode }.sortedBy { it.name } + UsCityOther
}

val MonthNames = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
)

/** Mutable onboarding form, mirroring iOS `OnboardingData` (streamlined for the MVP). */
data class OnboardingForm(
    val displayName: String = "",
    val gender: Gender = Gender.PREFER_NOT_TO_SAY,
    val birthYear: Int? = null,
    val birthMonth: Int? = null,
    val countryCode: String = "",
    val state: String? = null,
    val city: String? = null,
    val nativeLanguage: String = "en",
    val japaneseLevel: JapaneseLevel = JapaneseLevel.BEGINNER,
    val learningGoal: LearningGoal = LearningGoal.TRAVEL,
    val dailyGoalMinutes: Int = 10,
    val preferredPracticeTime: PracticeTime = PracticeTime.FLEXIBLE,
    val referralCode: String = "",
) {
    val isPersonalInfoComplete: Boolean
        get() {
            // Cap matches TextSanitizer.MAX_NAME_CHARS — keep them in sync if the limit changes.
            val nameOk = displayName.trim().length in 2..20
            val dateOk = birthYear != null && birthMonth != null
            val countryOk = countryCode.isNotEmpty()
            val usExtrasOk = if (countryCode != "US") true else {
                val stateOk = !state.isNullOrBlank()
                // ≥3 letter-only chars. Applies to both dropdown picks (Nashville, LA, etc.,
                // all pass naturally) and the typed-in Other path. Blocks "" / "St" / "NYC123".
                val cityOk = !city.isNullOrBlank() &&
                    com.sonari.speak2easy.util.TextSanitizer.isValidCity(city)
                stateOk && cityOk
            }
            return nameOk && dateOk && countryOk && usExtrasOk
        }

    fun toRequest(): OnboardingRequest = OnboardingRequest(
        displayName = displayName.trim(),
        birthYear = birthYear ?: 0,
        birthMonth = birthMonth ?: 0,
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
        city = city?.takeIf { it.isNotBlank() },
        state = state?.takeIf { it.isNotBlank() },
        referredByCode = referralCode.trim().uppercase().ifEmpty { null },
    )
}
