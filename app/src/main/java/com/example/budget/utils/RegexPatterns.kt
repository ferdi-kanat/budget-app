package com.example.budget.utils

object RegexPatterns {
    const val VAKIFBANK_TOTAL_AMOUNT = "Dönem Borcunuz\\s*:\\s*([0-9,]+\\.[0-9]+)"
    const val VAKIFBANK_DUE_DATE = "Son Ödeme Tarihi\\s*[:\\-]?\\s*(\\d{2}\\.\\d{2}\\.\\d{4})"
    const val ZIRAAT_TOTAL_AMOUNT = "Dönem Borcu TL\\s*:\\s*([0-9,.]+) TL"
    const val ZIRAAT_DUE_DATE = "Son Ödeme Tarihi\\s*:\\s*(\\d{2}/\\d{2}/\\d{4})"
    const val TRANSACTION_DETAILS = "(\\d{2}\\.\\d{2}\\.\\d{4} \\d{2}:\\d{2})\\s+(\\d+)\\s+([\\-\\d.,]+)\\s+([\\-\\d.,]+)\\s+(.*)"
}
