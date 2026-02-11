package com.tk.quickcontacts.utils

import android.content.Context
import android.telephony.TelephonyManager
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import java.util.Locale
import java.util.regex.Pattern

/**
 * Utility functions for phone number operations
 */
object PhoneNumberUtils {

    private val phoneNumberUtil = PhoneNumberUtil.getInstance()
    
    // Phone number validation patterns
    private val PHONE_PATTERN = Pattern.compile("^[+]?[0-9\\s\\-\\(\\)]{7,20}$")
    private val DIGITS_ONLY_PATTERN = Pattern.compile("^[0-9]+$")
    
    // Shared map for ISO to dialing code
    private val countryToDialing = mapOf(
        "US" to "+1", // US/Canada
        "CA" to "+1",
        "GB" to "+44",
        "DE" to "+49",
        "FR" to "+33",
        "IT" to "+39",
        "ES" to "+34",
        "RU" to "+7",
        "JP" to "+81",
        "KR" to "+82",
        "CN" to "+86",
        "IN" to "+91",
        "AU" to "+61",
        "BR" to "+55",
        "MX" to "+52",
        "NL" to "+31",
        "SE" to "+46",
        "NO" to "+47",
        "DK" to "+45",
        "CH" to "+41",
        "AT" to "+43",
        "BE" to "+32",
        "PT" to "+351",
        "GR" to "+30",
        "PL" to "+48",
        "CZ" to "+420",
        "HU" to "+36",
        "TR" to "+90",
        "IL" to "+972",
        "SA" to "+966",
        "AE" to "+971",
        "SG" to "+65",
        "MY" to "+60",
        "TH" to "+66",
        "VN" to "+84",
        "ID" to "+62",
        "PH" to "+63",
        "ZA" to "+27",
        "NG" to "+234",
        "EG" to "+20",
        // ... (add more as needed)
    )
    
    /**
     * Validate if a phone number is in a valid format
     */
    fun isValidPhoneNumber(phoneNumber: String): Boolean {
        if (phoneNumber.isBlank()) return false
        
        // Check basic format
        if (!PHONE_PATTERN.matcher(phoneNumber).matches()) {
            return false
        }
        
        // Extract digits only
        val digitsOnly = phoneNumber.replace(Regex("[^\\d]"), "")
        
        // Check length requirements
        return when {
            digitsOnly.startsWith("+") -> digitsOnly.length in 8..15 // International format
            digitsOnly.startsWith("1") && digitsOnly.length == 11 -> true // US/Canada with country code
            digitsOnly.length == 10 -> true // Standard 10-digit number
            digitsOnly.length in 7..15 -> true // Other valid lengths
            else -> false
        }
    }
    
    /**
     * Compare two number strings (handles country code vs no country code).
     */
    fun isSameNumber(number1: String, number2: String): Boolean {
        if (number1.isBlank() || number2.isBlank()) return number1.isBlank() && number2.isBlank()
        return normalizePhoneNumber(number1) == normalizePhoneNumber(number2)
    }

    /**
     * Format a phone number for display (alias for formatPhoneNumber).
     */
    fun formatPhoneNumberForDisplay(phoneNumber: String): String = formatPhoneNumber(phoneNumber)

    /**
     * Safely format a phone number for display using libphonenumber (same as quick-search).
     */
    fun formatPhoneNumber(phoneNumber: String): String {
        if (phoneNumber.isBlank()) return phoneNumber

        val trimmed = phoneNumber.trim()
        val digits = trimmed.filter { it.isDigit() }
        if (digits.isEmpty()) return phoneNumber

        val region = Locale.getDefault().country
        val regionCode = if (region.isNullOrBlank()) "ZZ" else region
        val parseRegion = if (trimmed.startsWith("+")) "ZZ" else regionCode

        return try {
            val parsed = phoneNumberUtil.parse(trimmed, parseRegion)
            val format = if (trimmed.startsWith("+")) {
                PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL
            } else {
                PhoneNumberUtil.PhoneNumberFormat.NATIONAL
            }
            phoneNumberUtil.format(parsed, format)
        } catch (e: NumberParseException) {
            formatPhoneNumberFallback(trimmed, digits)
        }
    }

    private fun formatPhoneNumberFallback(phoneNumber: String, digits: String): String {
        if (phoneNumber.startsWith("+")) {
            return formatInternationalNumber("+$digits")
        }
        if (digits.length == 10) {
            return formatUSNumber(digits)
        }
        return formatInternationalNumber(digits)
    }

    private fun formatInternationalNumber(number: String): String {
        val digits = number.filter { it.isDigit() }
        val hasPlus = number.startsWith("+")
        if (digits.length <= 3) return number
        val formatted = StringBuilder()
        if (hasPlus) formatted.append("+")
        val countryCodeLength = when {
            digits.length >= 10 -> 1
            digits.length >= 9 -> 2
            else -> 3
        }
        formatted.append(digits.substring(0, countryCodeLength)).append(" ")
        val remainingDigits = digits.substring(countryCodeLength)
        var index = 0
        while (index < remainingDigits.length) {
            val chunkSize = if (remainingDigits.length - index <= 4) 4 else 3
            if (index > 0) formatted.append(" ")
            formatted.append(remainingDigits.substring(index, (index + chunkSize).coerceAtMost(remainingDigits.length)))
            index += chunkSize
        }
        return formatted.toString()
    }

    private fun formatUSNumber(digits: String): String {
        if (digits.length != 10) return digits
        return "(${digits.substring(0, 3)}) ${digits.substring(3, 6)}-${digits.substring(6)}"
    }
    
    /**
     * Normalize a phone number for comparison with validation
     */
    fun normalizePhoneNumber(phoneNumber: String): String {
        if (!isValidPhoneNumber(phoneNumber)) {
            return phoneNumber // Return original if invalid
        }
        
        try {
            // Remove all non-digit characters
            val digitsOnly = phoneNumber.replace(Regex("[^\\d]"), "")
            
            return when {
                // Empty or too short
                digitsOnly.length < 7 -> digitsOnly
                
                // US/Canada numbers: 11 digits starting with 1, or 10 digits
                digitsOnly.length == 11 && digitsOnly.startsWith("1") -> {
                    // Remove leading 1 for US/Canada numbers to normalize to 10 digits
                    digitsOnly.substring(1)
                }
                digitsOnly.length == 10 -> {
                    // Already 10 digits, likely US/Canada without country code
                    digitsOnly
                }
                
                // International numbers: keep as is but remove leading country codes for comparison
                digitsOnly.length > 11 -> {
                    // Take last 10 digits for comparison (assumes international format)
                    digitsOnly.takeLast(10)
                }
                
                // Other cases: return as is
                else -> digitsOnly
            }
        } catch (e: Exception) {
            android.util.Log.w("PhoneNumberUtils", "Error normalizing phone number: $phoneNumber", e)
            return phoneNumber.replace(Regex("[^\\d]"), "") // Return digits-only as fallback
        }
    }
    
    /**
     * Get user's country code with error handling
     * @return ISO country code (e.g., "US"), or null if it cannot be determined. UI should prompt user if null.
     */
    fun getUserCountryCode(context: Context): String? {
        return try {
            // Try to get TelephonyManager, but don't fail if not available
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            
            var countryCode: String? = null
            
            // Try to get country from SIM card first (if TelephonyManager is available)
            if (telephonyManager != null) {
                countryCode = telephonyManager.simCountryIso
                
                // If SIM country is not available, try network country
                if (countryCode.isNullOrEmpty()) {
                    countryCode = telephonyManager.networkCountryIso
                }
            }
            
            // If both are empty or TelephonyManager not available, fall back to device locale
            if (countryCode.isNullOrEmpty()) {
                countryCode = Locale.getDefault().country
            }
            
            // Final fallback: try to get from timezone
            if (countryCode.isNullOrEmpty()) {
                countryCode = getCountryCodeFromTimezone()
            }
            
            // Validate country code format
            if (countryCode.isNullOrEmpty() || countryCode.length != 2) {
                return null // No fallback to US
            }
            
            countryCode.uppercase()
        } catch (e: Exception) {
            android.util.Log.w("PhoneNumberUtils", "Error getting user country code", e)
            null // No fallback to US
        }
    }
    
    /**
     * Map ISO country code (e.g., "US") to dialing code (e.g., "+1")
     */
    fun isoToDialingCode(isoCountryCode: String): String? {
        val result = countryToDialing[isoCountryCode.uppercase(Locale.ROOT)]
        return result
    }
    
    /**
     * Extract country code from phone number with validation
     */
    fun getCountryCodeFromPhoneNumber(phoneNumber: String): String? {
        if (!isValidPhoneNumber(phoneNumber)) {
            return null
        }
        
        return try {
            val cleaned = phoneNumber.replace(Regex("[^0-9+]"), "")
            
            if (cleaned.startsWith("+")) {
                // Extract country code from international format
                val numberWithoutPlus = cleaned.substring(1)
                
                // Common country codes (you can expand this list)
                val countryCodes = mapOf(
                    "1" to "US", // US/Canada
                    "44" to "GB", // UK
                    "49" to "DE", // Germany
                    "33" to "FR", // France
                    "39" to "IT", // Italy
                    "34" to "ES", // Spain
                    "7" to "RU", // Russia
                    "81" to "JP", // Japan
                    "82" to "KR", // South Korea
                    "86" to "CN", // China
                    "91" to "IN", // India
                    "61" to "AU", // Australia
                    "55" to "BR", // Brazil
                    "52" to "MX", // Mexico
                    "31" to "NL", // Netherlands
                    "46" to "SE", // Sweden
                    "47" to "NO", // Norway
                    "45" to "DK", // Denmark
                    "41" to "CH", // Switzerland
                    "43" to "AT", // Austria
                    "32" to "BE", // Belgium
                    "351" to "PT", // Portugal
                    "30" to "GR", // Greece
                    "48" to "PL", // Poland
                    "420" to "CZ", // Czech Republic
                    "36" to "HU", // Hungary
                    "90" to "TR", // Turkey
                    "972" to "IL", // Israel
                    "966" to "SA", // Saudi Arabia
                    "971" to "AE", // UAE
                    "65" to "SG", // Singapore
                    "60" to "MY", // Malaysia
                    "66" to "TH", // Thailand
                    "84" to "VN", // Vietnam
                    "62" to "ID", // Indonesia
                    "63" to "PH", // Philippines
                    "27" to "ZA", // South Africa
                    "234" to "NG", // Nigeria
                    "20" to "EG", // Egypt
                    "212" to "MA", // Morocco
                    "213" to "DZ", // Algeria
                    "216" to "TN", // Tunisia
                    "218" to "LY", // Libya
                    "220" to "GM", // Gambia
                    "221" to "SN", // Senegal
                    "222" to "MR", // Mauritania
                    "223" to "ML", // Mali
                    "224" to "GN", // Guinea
                    "225" to "CI", // Ivory Coast
                    "226" to "BF", // Burkina Faso
                    "227" to "NE", // Niger
                    "228" to "TG", // Togo
                    "229" to "BJ", // Benin
                    "230" to "MU", // Mauritius
                    "231" to "LR", // Liberia
                    "232" to "SL", // Sierra Leone
                    "233" to "GH", // Ghana
                    "235" to "TD", // Chad
                    "236" to "CF", // Central African Republic
                    "237" to "CM", // Cameroon
                    "238" to "CV", // Cape Verde
                    "239" to "ST", // São Tomé and Príncipe
                    "240" to "GQ", // Equatorial Guinea
                    "241" to "GA", // Gabon
                    "242" to "CG", // Republic of the Congo
                    "243" to "CD", // Democratic Republic of the Congo
                    "244" to "AO", // Angola
                    "245" to "GW", // Guinea-Bissau
                    "246" to "IO", // British Indian Ocean Territory
                    "247" to "AC", // Ascension Island
                    "248" to "SC", // Seychelles
                    "249" to "SD", // Sudan
                    "250" to "RW", // Rwanda
                    "251" to "ET", // Ethiopia
                    "252" to "SO", // Somalia
                    "253" to "DJ", // Djibouti
                    "254" to "KE", // Kenya
                    "255" to "TZ", // Tanzania
                    "256" to "UG", // Uganda
                    "257" to "BI", // Burundi
                    "258" to "MZ", // Mozambique
                    "260" to "ZM", // Zambia
                    "261" to "MG", // Madagascar
                    "262" to "RE", // Réunion
                    "263" to "ZW", // Zimbabwe
                    "264" to "NA", // Namibia
                    "265" to "MW", // Malawi
                    "266" to "LS", // Lesotho
                    "267" to "BW", // Botswana
                    "268" to "SZ", // Eswatini
                    "269" to "KM", // Comoros
                    "290" to "SH", // Saint Helena
                    "291" to "ER", // Eritrea
                    "297" to "AW", // Aruba
                    "298" to "FO", // Faroe Islands
                    "299" to "GL", // Greenland
                    "350" to "GI", // Gibraltar
                    "352" to "LU", // Luxembourg
                    "353" to "IE", // Ireland
                    "354" to "IS", // Iceland
                    "355" to "AL", // Albania
                    "356" to "MT", // Malta
                    "357" to "CY", // Cyprus
                    "358" to "FI", // Finland
                    "359" to "BG", // Bulgaria
                    "370" to "LT", // Lithuania
                    "371" to "LV", // Latvia
                    "372" to "EE", // Estonia
                    "373" to "MD", // Moldova
                    "374" to "AM", // Armenia
                    "375" to "BY", // Belarus
                    "376" to "AD", // Andorra
                    "377" to "MC", // Monaco
                    "378" to "SM", // San Marino
                    "379" to "VA", // Vatican City
                    "380" to "UA", // Ukraine
                    "381" to "RS", // Serbia
                    "382" to "ME", // Montenegro
                    "383" to "XK", // Kosovo
                    "385" to "HR", // Croatia
                    "386" to "SI", // Slovenia
                    "387" to "BA", // Bosnia and Herzegovina
                    "389" to "MK", // North Macedonia
                    "420" to "CZ", // Czech Republic
                    "421" to "SK", // Slovakia
                    "423" to "LI", // Liechtenstein
                    "500" to "FK", // Falkland Islands
                    "501" to "BZ", // Belize
                    "502" to "GT", // Guatemala
                    "503" to "SV", // El Salvador
                    "504" to "HN", // Honduras
                    "505" to "NI", // Nicaragua
                    "506" to "CR", // Costa Rica
                    "507" to "PA", // Panama
                    "508" to "PM", // Saint Pierre and Miquelon
                    "509" to "HT", // Haiti
                    "590" to "GP", // Guadeloupe
                    "591" to "BO", // Bolivia
                    "592" to "GY", // Guyana
                    "593" to "EC", // Ecuador
                    "594" to "GF", // French Guiana
                    "595" to "PY", // Paraguay
                    "596" to "MQ", // Martinique
                    "597" to "SR", // Suriname
                    "598" to "UY", // Uruguay
                    "599" to "CW", // Curaçao
                    "670" to "TL", // East Timor
                    "672" to "AQ", // Antarctica
                    "673" to "BN", // Brunei
                    "674" to "NR", // Nauru
                    "675" to "PG", // Papua New Guinea
                    "676" to "TO", // Tonga
                    "677" to "SB", // Solomon Islands
                    "678" to "VU", // Vanuatu
                    "679" to "FJ", // Fiji
                    "680" to "PW", // Palau
                    "681" to "WF", // Wallis and Futuna
                    "682" to "CK", // Cook Islands
                    "683" to "NU", // Niue
                    "685" to "WS", // Samoa
                    "686" to "KI", // Kiribati
                    "687" to "NC", // New Caledonia
                    "688" to "TV", // Tuvalu
                    "689" to "PF", // French Polynesia
                    "690" to "TK", // Tokelau
                    "691" to "FM", // Micronesia
                    "692" to "MH", // Marshall Islands
                    "850" to "KP", // North Korea
                    "852" to "HK", // Hong Kong
                    "853" to "MO", // Macau
                    "855" to "KH", // Cambodia
                    "856" to "LA", // Laos
                    "880" to "BD", // Bangladesh
                    "886" to "TW", // Taiwan
                    "960" to "MV", // Maldives
                    "961" to "LB", // Lebanon
                    "962" to "JO", // Jordan
                    "963" to "SY", // Syria
                    "964" to "IQ", // Iraq
                    "965" to "KW", // Kuwait
                    "967" to "YE", // Yemen
                    "968" to "OM", // Oman
                    "970" to "PS", // Palestine
                    "971" to "AE", // UAE
                    "972" to "IL", // Israel
                    "973" to "BH", // Bahrain
                    "974" to "QA", // Qatar
                    "975" to "BT", // Bhutan
                    "976" to "MN", // Mongolia
                    "977" to "NP", // Nepal
                    "992" to "TJ", // Tajikistan
                    "993" to "TM", // Turkmenistan
                    "994" to "AZ", // Azerbaijan
                    "995" to "GE", // Georgia
                    "996" to "KG", // Kyrgyzstan
                    "998" to "UZ" // Uzbekistan
                )
                
                // Try different lengths for country codes (1-4 digits)
                for (length in 1..4) {
                    if (numberWithoutPlus.length >= length) {
                        val possibleCode = numberWithoutPlus.substring(0, length)
                        if (countryCodes.containsKey(possibleCode)) {
                            return countryCodes[possibleCode]
                        }
                    }
                }
            }
            
            null
        } catch (e: Exception) {
            android.util.Log.w("PhoneNumberUtils", "Error extracting country code from: $phoneNumber", e)
            null
        }
    }

    /**
     * Extract dialing code from phone number (e.g., '+1', '+91')
     */
    fun getDialingCodeFromPhoneNumber(phoneNumber: String): String? {
        val cleaned = phoneNumber.replace(Regex("[^0-9+]"), "")
        if (!cleaned.startsWith("+")) return null
        // Try different lengths for country codes (1-4 digits)
        for (length in 1..4) {
            if (cleaned.length > length) {
                val possibleCode = cleaned.substring(1, 1 + length)
                // Check if this code exists in our map
                if (countryToDialing.values.contains("+" + possibleCode)) {
                    return "+" + possibleCode
                }
            }
        }
        return null
    }
    
    /**
     * Clean and validate phone number for safe use
     */
    fun cleanPhoneNumber(phoneNumber: String): String? {
        if (!isValidPhoneNumber(phoneNumber)) {
            return null
        }
        
        return try {
            phoneNumber.replace(Regex("[^+\\d]"), "")
        } catch (e: Exception) {
            android.util.Log.w("PhoneNumberUtils", "Error cleaning phone number: $phoneNumber", e)
            null
        }
    }

    /**
     * Check if a string is a valid country dialing code (e.g., '+1', '+91'). Accepts with or without '+'.
     */
    fun isValidCountryDialingCode(code: String): Boolean {
        val normalized = if (code.startsWith("+")) code else "+$code"
        return countryToDialing.values.contains(normalized)
    }

    /**
     * Get country code from timezone as fallback
     */
    private fun getCountryCodeFromTimezone(): String? {
        return try {
            val timeZone = java.util.TimeZone.getDefault()
            val timeZoneId = timeZone.id
            
            // Map common timezone IDs to country codes
            val timezoneToCountry = mapOf(
                // North America
                "America/New_York" to "US",
                "America/Chicago" to "US",
                "America/Denver" to "US",
                "America/Los_Angeles" to "US",
                "America/Phoenix" to "US",
                "America/Anchorage" to "US",
                "Pacific/Honolulu" to "US",
                "America/Toronto" to "CA",
                "America/Vancouver" to "CA",
                "America/Edmonton" to "CA",
                "America/Winnipeg" to "CA",
                "America/Halifax" to "CA",
                "America/St_Johns" to "CA",
                "America/Mexico_City" to "MX",
                "America/Tijuana" to "MX",
                "America/Monterrey" to "MX",
                
                // Europe
                "Europe/London" to "GB",
                "Europe/Paris" to "FR",
                "Europe/Berlin" to "DE",
                "Europe/Rome" to "IT",
                "Europe/Madrid" to "ES",
                "Europe/Moscow" to "RU",
                "Europe/Amsterdam" to "NL",
                "Europe/Stockholm" to "SE",
                "Europe/Oslo" to "NO",
                "Europe/Copenhagen" to "DK",
                "Europe/Zurich" to "CH",
                "Europe/Vienna" to "AT",
                "Europe/Brussels" to "BE",
                "Europe/Lisbon" to "PT",
                "Europe/Athens" to "GR",
                "Europe/Warsaw" to "PL",
                "Europe/Prague" to "CZ",
                "Europe/Budapest" to "HU",
                "Europe/Istanbul" to "TR",
                "Europe/Dublin" to "IE",
                "Europe/Helsinki" to "FI",
                "Europe/Riga" to "LV",
                "Europe/Tallinn" to "EE",
                "Europe/Vilnius" to "LT",
                "Europe/Sofia" to "BG",
                "Europe/Bucharest" to "RO",
                "Europe/Belgrade" to "RS",
                "Europe/Zagreb" to "HR",
                "Europe/Ljubljana" to "SI",
                "Europe/Bratislava" to "SK",
                "Europe/Luxembourg" to "LU",
                "Europe/Malta" to "MT",
                "Europe/Cyprus" to "CY",
                "Europe/Reykjavik" to "IS",
                "Europe/Kaliningrad" to "RU",
                "Europe/Samara" to "RU",
                "Europe/Yekaterinburg" to "RU",
                "Europe/Novosibirsk" to "RU",
                "Europe/Krasnoyarsk" to "RU",
                "Europe/Irkutsk" to "RU",
                "Europe/Yakutsk" to "RU",
                "Europe/Vladivostok" to "RU",
                "Europe/Magadan" to "RU",
                "Europe/Kamchatka" to "RU",
                
                // Asia
                "Asia/Tokyo" to "JP",
                "Asia/Seoul" to "KR",
                "Asia/Shanghai" to "CN",
                "Asia/Beijing" to "CN",
                "Asia/Hong_Kong" to "CN",
                "Asia/Kolkata" to "IN",
                "Asia/Kolkata" to "IN",
                "Asia/Dhaka" to "BD",
                "Asia/Karachi" to "PK",
                "Asia/Tashkent" to "UZ",
                "Asia/Almaty" to "KZ",
                "Asia/Bishkek" to "KG",
                "Asia/Dushanbe" to "TJ",
                "Asia/Ashgabat" to "TM",
                "Asia/Baku" to "AZ",
                "Asia/Tbilisi" to "GE",
                "Asia/Yerevan" to "AM",
                "Asia/Tehran" to "IR",
                "Asia/Baghdad" to "IQ",
                "Asia/Kuwait" to "KW",
                "Asia/Riyadh" to "SA",
                "Asia/Dubai" to "AE",
                "Asia/Muscat" to "OM",
                "Asia/Qatar" to "QA",
                "Asia/Bahrain" to "BH",
                "Asia/Aden" to "YE",
                "Asia/Amman" to "JO",
                "Asia/Beirut" to "LB",
                "Asia/Damascus" to "SY",
                "Asia/Jerusalem" to "IL",
                "Asia/Gaza" to "PS",
                "Asia/Hebron" to "PS",
                "Asia/Jakarta" to "ID",
                "Asia/Bangkok" to "TH",
                "Asia/Ho_Chi_Minh" to "VN",
                "Asia/Phnom_Penh" to "KH",
                "Asia/Vientiane" to "LA",
                "Asia/Yangon" to "MM",
                "Asia/Singapore" to "SG",
                "Asia/Kuala_Lumpur" to "MY",
                "Asia/Manila" to "PH",
                "Asia/Taipei" to "TW",
                "Asia/Ulaanbaatar" to "MN",
                "Asia/Pyongyang" to "KP",
                "Asia/Macau" to "MO",
                "Asia/Urumqi" to "CN",
                "Asia/Kashgar" to "CN",
                "Asia/Chongqing" to "CN",
                "Asia/Harbin" to "CN",
                "Asia/Kabul" to "AF",
                "Asia/Kathmandu" to "NP",
                "Asia/Thimphu" to "BT",
                "Asia/Colombo" to "LK",
                "Asia/Male" to "MV",
                
                // Australia/Oceania
                "Australia/Sydney" to "AU",
                "Australia/Melbourne" to "AU",
                "Australia/Brisbane" to "AU",
                "Australia/Perth" to "AU",
                "Australia/Adelaide" to "AU",
                "Australia/Darwin" to "AU",
                "Australia/Hobart" to "AU",
                "Pacific/Auckland" to "NZ",
                "Pacific/Wellington" to "NZ",
                "Pacific/Fiji" to "FJ",
                "Pacific/Guam" to "GU",
                "Pacific/Saipan" to "MP",
                "Pacific/Port_Moresby" to "PG",
                "Pacific/Honiara" to "SB",
                "Pacific/Noumea" to "NC",
                "Pacific/Tahiti" to "PF",
                "Pacific/Honolulu" to "US",
                "Pacific/Midway" to "UM",
                "Pacific/Wake" to "UM",
                
                // South America
                "America/Sao_Paulo" to "BR",
                "America/Rio_Branco" to "BR",
                "America/Manaus" to "BR",
                "America/Belem" to "BR",
                "America/Fortaleza" to "BR",
                "America/Recife" to "BR",
                "America/Araguaina" to "BR",
                "America/Maceio" to "BR",
                "America/Bahia" to "BR",
                "America/Sao_Paulo" to "BR",
                "America/Campo_Grande" to "BR",
                "America/Cuiaba" to "BR",
                "America/Porto_Velho" to "BR",
                "America/Boa_Vista" to "BR",
                "America/Manaus" to "BR",
                "America/Eirunepe" to "BR",
                "America/Rio_Branco" to "BR",
                "America/Argentina/Buenos_Aires" to "AR",
                "America/Argentina/Cordoba" to "AR",
                "America/Argentina/Salta" to "AR",
                "America/Argentina/Jujuy" to "AR",
                "America/Argentina/Tucuman" to "AR",
                "America/Argentina/Catamarca" to "AR",
                "America/Argentina/La_Rioja" to "AR",
                "America/Argentina/San_Juan" to "AR",
                "America/Argentina/Mendoza" to "AR",
                "America/Argentina/San_Luis" to "AR",
                "America/Argentina/Rio_Gallegos" to "AR",
                "America/Argentina/Ushuaia" to "AR",
                "America/Santiago" to "CL",
                "America/Punta_Arenas" to "CL",
                "America/Easter" to "CL",
                "America/Asuncion" to "PY",
                "America/Montevideo" to "UY",
                "America/Caracas" to "VE",
                "America/La_Paz" to "BO",
                "America/Lima" to "PE",
                "America/Bogota" to "CO",
                "America/Guayaquil" to "EC",
                "America/Galapagos" to "EC",
                "America/Guatemala" to "GT",
                "America/Belize" to "BZ",
                "America/El_Salvador" to "SV",
                "America/Tegucigalpa" to "HN",
                "America/Managua" to "NI",
                "America/Costa_Rica" to "CR",
                "America/Panama" to "PA",
                "America/Cayman" to "KY",
                "America/Jamaica" to "JM",
                "America/Port-au-Prince" to "HT",
                "America/Santo_Domingo" to "DO",
                "America/Puerto_Rico" to "PR",
                "America/St_Thomas" to "VI",
                "America/St_Lucia" to "LC",
                "America/St_Vincent" to "VC",
                "America/Barbados" to "BB",
                "America/Grenada" to "GD",
                "America/Trinidad" to "TT",
                "America/Guyana" to "GY",
                "America/Paramaribo" to "SR",
                "America/Cayenne" to "GF",
                
                // Africa
                "Africa/Cairo" to "EG",
                "Africa/Tripoli" to "LY",
                "Africa/Tunis" to "TN",
                "Africa/Algiers" to "DZ",
                "Africa/Casablanca" to "MA",
                "Africa/Rabat" to "MA",
                "Africa/El_Aaiun" to "EH",
                "Africa/Nouakchott" to "MR",
                "Africa/Dakar" to "SN",
                "Africa/Banjul" to "GM",
                "Africa/Bissau" to "GW",
                "Africa/Conakry" to "GN",
                "Africa/Freetown" to "SL",
                "Africa/Monrovia" to "LR",
                "Africa/Abidjan" to "CI",
                "Africa/Accra" to "GH",
                "Africa/Lome" to "TG",
                "Africa/Porto-Novo" to "BJ",
                "Africa/Lagos" to "NG",
                "Africa/Douala" to "CM",
                "Africa/Malabo" to "GQ",
                "Africa/Libreville" to "GA",
                "Africa/Brazzaville" to "CG",
                "Africa/Kinshasa" to "CD",
                "Africa/Bangui" to "CF",
                "Africa/Ndjamena" to "TD",
                "Africa/Khartoum" to "SD",
                "Africa/Juba" to "SS",
                "Africa/Addis_Ababa" to "ET",
                "Africa/Asmara" to "ER",
                "Africa/Djibouti" to "DJ",
                "Africa/Mogadishu" to "SO",
                "Africa/Nairobi" to "KE",
                "Africa/Dar_es_Salaam" to "TZ",
                "Africa/Kampala" to "UG",
                "Africa/Bujumbura" to "BI",
                "Africa/Kigali" to "RW",
                "Africa/Luanda" to "AO",
                "Africa/Lubumbashi" to "CD",
                "Africa/Lusaka" to "ZM",
                "Africa/Harare" to "ZW",
                "Africa/Maputo" to "MZ",
                "Africa/Blantyre" to "MW",
                "Africa/Lilongwe" to "MW",
                "Africa/Gaborone" to "BW",
                "Africa/Maseru" to "LS",
                "Africa/Mbabane" to "SZ",
                "Africa/Johannesburg" to "ZA",
                "Africa/Pretoria" to "ZA",
                "Africa/Windhoek" to "NA",
                "Africa/Luanda" to "AO",
                "Africa/Sao_Tome" to "ST",
                "Africa/Brazzaville" to "CG",
                "Africa/Libreville" to "GA",
                "Africa/Douala" to "CM",
                "Africa/Malabo" to "GQ",
                "Africa/Lagos" to "NG",
                "Africa/Porto-Novo" to "BJ",
                "Africa/Lome" to "TG",
                "Africa/Accra" to "GH",
                "Africa/Abidjan" to "CI",
                "Africa/Monrovia" to "LR",
                "Africa/Freetown" to "SL",
                "Africa/Bissau" to "GW",
                "Africa/Banjul" to "GM",
                "Africa/Dakar" to "SN",
                "Africa/Nouakchott" to "MR",
                "Africa/El_Aaiun" to "EH",
                "Africa/Casablanca" to "MA",
                "Africa/Rabat" to "MA",
                "Africa/Algiers" to "DZ",
                "Africa/Tunis" to "TN",
                "Africa/Tripoli" to "LY",
                "Africa/Cairo" to "EG"
            )
            
            timezoneToCountry[timeZoneId]
        } catch (e: Exception) {
            android.util.Log.w("PhoneNumberUtils", "Error getting country from timezone", e)
            null
        }
    }
} 