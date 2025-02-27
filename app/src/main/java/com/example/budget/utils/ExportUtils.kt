package com.example.budget.utils

import android.content.Context
import android.net.Uri
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import com.example.budget.TransactionEntity
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import java.text.SimpleDateFormat
import java.util.*

class ExportUtils {
    fun exportToExcel(context: Context, transactions: List<TransactionEntity>, uri: Uri, selectedBank: String? = null) {
        val filteredTransactions = if (selectedBank != null) {
            transactions.filter { it.bankName == selectedBank }
        } else {
            transactions
        }

        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Transactions")

        // Create header row
        val headerRow = sheet.createRow(0)
        val headers = arrayOf("Tarih", "Saat", "İşlem No", "Tutar", "Bakiye", "Açıklama", "Banka")
        headers.forEachIndexed { index, header ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(header)
            sheet.setColumnWidth(index, 15 * 256)
        }

        // Fill data rows
        filteredTransactions.forEachIndexed { index, transaction ->
            val row = sheet.createRow(index + 1)
            row.createCell(0).setCellValue(transaction.date)
            row.createCell(1).setCellValue(transaction.time)
            row.createCell(2).setCellValue(transaction.transactionId)
            row.createCell(3).setCellValue(transaction.amount)
            row.createCell(4).setCellValue(transaction.balance ?: 0.0)
            row.createCell(5).setCellValue(transaction.description)
            row.createCell(6).setCellValue(transaction.bankName)
        }

        // Set description column wider
        sheet.setColumnWidth(5, 30 * 256)

        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            workbook.write(outputStream)
        }
        workbook.close()
    }

    fun exportToPDF(context: Context, transactions: List<TransactionEntity>, uri: Uri, selectedBank: String? = null) {
        val filteredTransactions = if (selectedBank != null) {
            transactions.filter { it.bankName == selectedBank }
        } else {
            transactions
        }

        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            val pdfWriter = PdfWriter(outputStream)
            val pdfDocument = PdfDocument(pdfWriter)
            val document = Document(pdfDocument)

            // Add title
            document.add(Paragraph("İşlem Geçmişi"))
            document.add(Paragraph("Oluşturulma Tarihi: ${getCurrentDateTime()}"))

            // Create table
            val table = Table(floatArrayOf(1f, 1f, 1f, 1f, 1f, 2f, 1f))

            // Add headers
            arrayOf("Tarih", "Saat", "İşlem No", "Tutar", "Bakiye", "Açıklama", "Banka").forEach {
                table.addCell(Cell().add(Paragraph(it)))
            }

            // Add data
            filteredTransactions.forEach { transaction ->
                table.addCell(Cell().add(Paragraph(transaction.date)))
                table.addCell(Cell().add(Paragraph(transaction.time)))
                table.addCell(Cell().add(Paragraph(transaction.transactionId)))
                table.addCell(Cell().add(Paragraph(transaction.amount.toString())))
                table.addCell(Cell().add(Paragraph(transaction.balance?.toString() ?: "")))
                table.addCell(Cell().add(Paragraph(transaction.description)))
                table.addCell(Cell().add(Paragraph(transaction.bankName)))
            }

            document.add(table)
            document.close()
        }
    }

    private fun getCurrentDateTime(): String {
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }
}