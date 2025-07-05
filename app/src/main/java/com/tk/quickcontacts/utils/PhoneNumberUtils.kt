package com.tk.quickcontacts.utils

import android.content.Context
import android.telephony.TelephonyManager
import java.util.Locale

/**
 * Utility functions for phone number operations
 */
object PhoneNumberUtils {
    
    /**
     * Format a phone number for display
     */
    fun formatPhoneNumber(phoneNumber: String): String {
        val cleaned = phoneNumber.replace(Regex("[^+\\d]"), "")
        
        return when {
            // US/Canada number with +1 country code
            cleaned.startsWith("+1") && cleaned.length == 12 -> {
                val digits = cleaned.substring(2)
                "+1 (${digits.substring(0, 3)}) ${digits.substring(3, 6)}-${digits.substring(6)}"
            }
            // US/Canada number without country code (10 digits)
            !cleaned.startsWith("+") && cleaned.length == 10 -> {
                "(${cleaned.substring(0, 3)}) ${cleaned.substring(3, 6)}-${cleaned.substring(6)}"
            }
            // US/Canada number with country code but no +
            cleaned.startsWith("1") && cleaned.length == 11 -> {
                val digits = cleaned.substring(1)
                "+1 (${digits.substring(0, 3)}) ${digits.substring(3, 6)}-${digits.substring(6)}"
            }
            // International number with country code
            cleaned.startsWith("+") && cleaned.length > 7 -> {
                val countryCode = cleaned.substring(0, cleaned.length - 10)
                val localNumber = cleaned.substring(cleaned.length - 10)
                if (localNumber.length == 10) {
                    "$countryCode (${localNumber.substring(0, 3)}) ${localNumber.substring(3, 6)}-${localNumber.substring(6)}"
                } else {
                    cleaned // Fallback to cleaned number
                }
            }
            // Other formats - just return cleaned
            else -> cleaned
        }
    }
    
    /**
     * Normalize a phone number for comparison
     */
    fun normalizePhoneNumber(phoneNumber: String): String {
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
    }
    
    /**
     * Get user's country code
     */
    fun getUserCountryCode(context: Context): String {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        
        // Try to get country from SIM card first
        var countryCode = telephonyManager.simCountryIso
        
        // If SIM country is not available, try network country
        if (countryCode.isNullOrEmpty()) {
            countryCode = telephonyManager.networkCountryIso
        }
        
        // If both are empty, fall back to device locale
        if (countryCode.isNullOrEmpty()) {
            countryCode = Locale.getDefault().country
        }
        
        return countryCode.uppercase()
    }
    
    /**
     * Extract country code from phone number
     */
    fun getCountryCodeFromPhoneNumber(phoneNumber: String): String? {
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
                "268" to "SZ", // Swaziland
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
                "380" to "UA", // Ukraine
                "381" to "RS", // Serbia
                "382" to "ME", // Montenegro
                "383" to "XK", // Kosovo
                "385" to "HR", // Croatia
                "386" to "SI", // Slovenia
                "387" to "BA", // Bosnia and Herzegovina
                "389" to "MK", // North Macedonia
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
        
        return null
    }
    
    /**
     * Determine if a phone number is international
     */
    fun isInternationalNumber(context: Context, phoneNumber: String, isDetectionEnabled: Boolean = true): Boolean {
        // If international detection is disabled, always return false (treat as domestic)
        if (!isDetectionEnabled) {
            return false
        }
        
        val userCountryCode = getUserCountryCode(context)
        val phoneCountryCode = getCountryCodeFromPhoneNumber(phoneNumber)
        
        // If we can't determine the phone's country code, assume it's domestic
        if (phoneCountryCode == null) {
            return false
        }
        
        // Compare country codes
        return phoneCountryCode != userCountryCode
    }
} 