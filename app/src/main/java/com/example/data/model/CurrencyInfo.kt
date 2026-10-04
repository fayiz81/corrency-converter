package com.example.data.model

data class CurrencyInfo(
    val code: String,
    val name: String,
    val symbol: String,
    val flag: String
)

object CurrencyCatalog {
    val supportedCurrencies: List<CurrencyInfo> = listOf(
        CurrencyInfo("USD", "United States Dollar", "$", "🇺🇸"),
        CurrencyInfo("EUR", "Euro", "€", "🇪🇺"),
        CurrencyInfo("GBP", "British Pound", "£", "🇬🇧"),
        CurrencyInfo("JPY", "Japanese Yen", "¥", "🇯🇵"),
        CurrencyInfo("CAD", "Canadian Dollar", "CA$", "🇨🇦"),
        CurrencyInfo("AUD", "Australian Dollar", "AU$", "🇦🇺"),
        CurrencyInfo("CHF", "Swiss Franc", "CHF", "🇨🇭"),
        CurrencyInfo("CNY", "Chinese Renminbi", "¥", "🇨🇳"),
        CurrencyInfo("INR", "Indian Rupee", "₹", "🇮🇳"),
        CurrencyInfo("BRL", "Brazilian Real", "R$", "🇧🇷"),
        CurrencyInfo("BGN", "Bulgarian Lev", "лв", "🇧🇬"),
        CurrencyInfo("CZK", "Czech Koruna", "Kč", "🇨🇿"),
        CurrencyInfo("DKK", "Danish Krone", "kr", "🇩🇰"),
        CurrencyInfo("HKD", "Hong Kong Dollar", "HK$", "🇭🇰"),
        CurrencyInfo("HUF", "Hungarian Forint", "Ft", "🇭🇺"),
        CurrencyInfo("IDR", "Indonesian Rupiah", "Rp", "🇮🇩"),
        CurrencyInfo("ILS", "Israeli Shekel", "₪", "🇮🇱"),
        CurrencyInfo("ISK", "Icelandic Króna", "kr", "🇮🇸"),
        CurrencyInfo("KRW", "South Korean Won", "₩", "🇰🇷"),
        CurrencyInfo("MXN", "Mexican Peso", "MX$", "🇲🇽"),
        CurrencyInfo("MYR", "Malaysian Ringgit", "RM", "🇲🇾"),
        CurrencyInfo("NOK", "Norwegian Krone", "kr", "🇳🇴"),
        CurrencyInfo("NZD", "New Zealand Dollar", "NZ$", "🇳🇿"),
        CurrencyInfo("PHP", "Philippine Peso", "₱", "🇵🇭"),
        CurrencyInfo("PLN", "Polish Złoty", "zł", "🇵🇱"),
        CurrencyInfo("RON", "Romanian Leu", "lei", "🇷🇴"),
        CurrencyInfo("SEK", "Swedish Krona", "kr", "🇸🇪"),
        CurrencyInfo("SGD", "Singapore Dollar", "S$", "🇸🇬"),
        CurrencyInfo("THB", "Thai Baht", "฿", "🇹🇭"),
        CurrencyInfo("TRY", "Turkish Lira", "₺", "🇹🇷"),
        CurrencyInfo("ZAR", "South African Rand", "R", "🇿🇦")
    )

    private val catalogMap: Map<String, CurrencyInfo> = supportedCurrencies.associateBy { it.code }

    fun getCurrency(code: String, fallbackName: String? = null): CurrencyInfo {
        return catalogMap[code] ?: CurrencyInfo(
            code = code,
            name = fallbackName ?: code,
            symbol = getFallbackSymbol(code),
            flag = getFlagForCurrency(code)
        )
    }

    private fun getFallbackSymbol(code: String): String {
        return when (code.uppercase()) {
            "USD", "AUD", "CAD", "NZD", "SGD", "HKD", "MXN" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            "JPY", "CNY" -> "¥"
            "INR" -> "₹"
            "KRW" -> "₩"
            "RUB" -> "₽"
            "TRY" -> "₺"
            "ILS" -> "₪"
            "THB" -> "฿"
            "BRL" -> "R$"
            "PHP" -> "₱"
            "CHF" -> "CHF"
            else -> code
        }
    }

    private fun getFlagForCurrency(currencyCode: String): String {
        if (currencyCode.length < 2) return "🌐"
        if (currencyCode.equals("EUR", ignoreCase = true)) return "🇪🇺"
        // Most currencies have first 2 letters corresponding to ISO 3166-1 country code
        val countryCode = currencyCode.substring(0, 2).uppercase()
        val firstChar = Character.codePointAt(countryCode, 0) - 0x41 + 0x1F1E6
        val secondChar = Character.codePointAt(countryCode, 1) - 0x41 + 0x1F1E6
        return if (countryCode[0] in 'A'..'Z' && countryCode[1] in 'A'..'Z') {
            String(Character.toChars(firstChar)) + String(Character.toChars(secondChar))
        } else {
            "🌐"
        }
    }
}
