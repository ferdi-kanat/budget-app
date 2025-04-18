package com.example.budget.parsers

import android.content.ContentResolver
import android.net.Uri
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import com.example.budget.MainActivity.Transaction
import java.text.SimpleDateFormat
import java.util.*

class ExcelParser {

    fun readXLSXFile(uri: Uri, contentResolver: ContentResolver): List<Transaction> {
        val inputStream = contentResolver.openInputStream(uri)
        val workbook = XSSFWorkbook(inputStream)
        val sheet = workbook.getSheetAt(0)
        val transactions = mutableListOf<Transaction>()
        var rowIndex = 0

        for (row in sheet) {
            if (row.rowNum == 0) continue // Başlık satırını atla

            val date = getCellValue(row.getCell(0))
            val receiptNumber = getCellValue(row.getCell(1))
            val description = getCellValue(row.getCell(2))
            val rawAmount = getCellValue(row.getCell(3))
            val balance = getCellValue(row.getCell(4))

            // Gereksiz satırları atla
            if (date.isEmpty() || rawAmount.isEmpty() || description.isEmpty()) {
                continue
            }
            // Başlık satırlarını atla
            if (date.equals("Tarih", ignoreCase = true) &&
                receiptNumber.equals("Fiş No", ignoreCase = true) &&
                description.equals("Açıklama", ignoreCase = true)) {
                continue
            }

            // Convert amount to proper format but preserve the original sign
            val amount = try {
                rawAmount.replace(",", ".").toDouble().toString()
            } catch (e: Exception) {
                rawAmount
            }

            transactions.add(
                Transaction(
                    date = date,
                    time = "", // Empty string for time
                    transactionId = receiptNumber, // Using receipt number as transactionId
                    amount = amount,
                    balance = balance,
                    description = description,
                    bankName = "Bankkart" // Always set as Bankkart for Excel imports
                ).also { rowIndex++ }
            )
        }

        workbook.close()
        
        // Sort transactions by date in ascending order (oldest to newest)
        // For same dates, maintain the original order from the Excel file (which is newest first for Bankkart)
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        return transactions.sortedWith(compareBy<Transaction> { transaction ->
            try {
                dateFormat.parse(transaction.date)
            } catch (e: Exception) {
                // If date parsing fails, put it at the end
                Date(Long.MAX_VALUE)
            }
        }.thenByDescending { // For same dates, keep the original order (newest first)
            transactions.indexOf(it)
        })
    }

    private fun getCellValue(cell: Cell?): String {
        return when (cell?.cellType) {
            CellType.STRING -> cell.stringCellValue
            CellType.NUMERIC -> {
                if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                    dateFormat.format(cell.dateCellValue)
                } else {
                    cell.numericCellValue.toString()
                }
            }
            CellType.BOOLEAN -> cell.booleanCellValue.toString()
            else -> ""
        }
    }
}
