package com.example.budget

import android.net.Uri
import android.widget.Toast
import com.example.budget.parsers.ExcelParser
import com.example.budget.parsers.PDFParser
import com.example.budget.utils.RegexPatterns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

class FileProcessor(private val activity: MainActivity) {

    fun processPdfText(uri: Uri) {
        val pdfParser = PDFParser()
        val pdfText = pdfParser.extractText(uri, activity.contentResolver)
        for ((bankName, keyword) in activity.supportedBanks) {
            if (pdfText.contains(bankName, ignoreCase = true)) {
                if (pdfText.contains(keyword)) {
                    handleCreditCardStatement(bankName, pdfText)
                } else {
                    val transactions = extractTransactions(pdfText)
                    activity.launchEditActivity(transactions)
                }
                return
            }
        }
        activity.textViewResult.text = "Bu bankayı desteklemiyoruz."
        activity.textViewDate.text = ""
    }

    fun processExcelFile(uri: Uri) {
        try {
            val excelParser = ExcelParser()
            val transactions = excelParser.readXLSXFile(uri, activity.contentResolver)
            activity.launchEditActivity(transactions)
        } catch (e: Exception) {
            Toast.makeText(activity, "Excel dosyası okunamadı!", Toast.LENGTH_LONG).show()
        }
    }

    fun handleCreditCardStatement(bankName: String, pdfText: String) {
        val totalAmount = when (bankName) {
            "VakıfBank" -> findTotalAmountVakif(pdfText)
            "Bankkart" -> findTotalAmountZiraat(pdfText)
            else -> ""
        }
        val lastDate = when (bankName) {
            "VakıfBank" -> extractDueDateVakif(pdfText)
            "Bankkart" -> extractDueDateZiraat(pdfText)
            else -> ""
        }
        activity.textViewResult.text = "Dönem Borcu ($bankName): $totalAmount TL"
        activity.textViewDate.text = "Son Ödeme Tarihi ($bankName): $lastDate"
    }

    private fun extractTransactions(text: String): List<Transaction> {
        val transactionRegex = Pattern.compile(RegexPatterns.TRANSACTION_DETAILS)
        val transactions = mutableListOf<Transaction>()
        val matcher = transactionRegex.matcher(text)
        while (matcher.find()) {
            val (dateTime, transactionId, amount, balance, description) = matcher.destructured
            val (date, time) = dateTime.split(" ")
            transactions.add(
                Transaction(
                    date = date,
                    time = time,
                    transactionId = transactionId,
                    amount = amount.replace(".", "").replace(",", "."),
                    balance = balance.replace(".", "").replace(",", "."),
                    description = description.trim()
                )
            )
        }
        return transactions
    }

    private fun findTotalAmountVakif(text: String): String? {
        val regex = Regex(RegexPatterns.VAKIFBANK_TOTAL_AMOUNT)
        val match = regex.find(text)
        val rawAmount = match?.groups?.get(1)?.value
        return rawAmount?.replace(",", "")
    }

    private fun findTotalAmountZiraat(text: String): String? {
        val regex = Regex(RegexPatterns.ZIRAAT_TOTAL_AMOUNT)
        val match = regex.find(text)
        val rawAmount = match?.groups?.get(1)?.value
        return rawAmount?.replace(".", "")?.replace(",", ".")
    }

    private fun extractDueDateVakif(text: String): String? {
        val dateRegex = Regex(RegexPatterns.VAKIFBANK_DUE_DATE)
        val match = dateRegex.find(text)
        return match?.groups?.get(1)?.value
    }

    private fun extractDueDateZiraat(text: String): String? {
        val dateRegex = Regex(RegexPatterns.ZIRAAT_DUE_DATE)
        val match = dateRegex.find(text)
        return match?.groups?.get(1)?.value
    }
}
