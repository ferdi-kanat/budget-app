package com.example.budget.parsers

import android.content.ContentResolver
import android.net.Uri
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor

class PDFParser {

    fun extractText(uri: Uri, contentResolver: ContentResolver): String {
        val inputStream = contentResolver.openInputStream(uri)
        val pdfReader = PdfReader(inputStream)
        val pdfDoc = PdfDocument(pdfReader)
        val textBuilder = StringBuilder()

        for (i in 1..pdfDoc.numberOfPages) {
            val text = PdfTextExtractor.getTextFromPage(pdfDoc.getPage(i))
            textBuilder.append(text).append("\n")
        }

        pdfDoc.close()
        return textBuilder.toString()
    }
}
