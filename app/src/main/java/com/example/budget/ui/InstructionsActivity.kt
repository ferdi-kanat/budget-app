package com.example.budget.ui

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.budget.R
import com.example.budget.data.AutomaticTransaction
import com.example.budget.data.RepeatPeriod
import com.example.budget.data.dao.AccountDao
import com.example.budget.parsers.ExcelParser
import com.example.budget.parsers.PDFParser
import com.example.budget.DatabaseProvider
import com.example.budget.utils.RegexPatterns
import com.github.mikephil.charting.BuildConfig
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import android.app.AlertDialog

class InstructionsActivity : AppCompatActivity() {

    private lateinit var textViewResult: TextView
    private lateinit var textViewDate: TextView
    private lateinit var editTextAmount: TextInputEditText
    private lateinit var editTextDescription: TextInputEditText
    private lateinit var editTextDate: TextInputEditText
    private lateinit var accountDropdown: AutoCompleteTextView
    private lateinit var repeatDropdown: AutoCompleteTextView
    private lateinit var buttonSaveTransaction: Button
    private val pdfRequestCode = 1
    private val xlsxRequestCode = 2
    private val pdfParser = PDFParser()
    private val supportedBanks = mapOf(
        "VakıfBank" to "Dönem Borcunuz",
        "Bankkart" to "Dönem Borcu TL"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_instructions)

        initializeViews()
        setupButtons()
        setupDropdowns()
        checkPermissions()
    }

    private fun initializeViews() {
        textViewResult = findViewById(R.id.textViewResult)
        textViewDate = findViewById(R.id.textViewDate)
        editTextAmount = findViewById(R.id.editTextAmount)
        editTextDescription = findViewById(R.id.editTextDescription)
        editTextDate = findViewById(R.id.editTextDate)
        accountDropdown = findViewById<AutoCompleteTextView>(R.id.accountDropdown)
        repeatDropdown = findViewById<AutoCompleteTextView>(R.id.repeatDropdown)
        buttonSaveTransaction = findViewById(R.id.buttonSaveTransaction)
    }

    private fun setupButtons() {
        findViewById<Button>(R.id.buttonLoadPDF).setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "application/pdf"
            }
            startActivityForResult(intent, pdfRequestCode)
        }

        findViewById<Button>(R.id.buttonLoadXLSX).setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            }
            startActivityForResult(intent, xlsxRequestCode)
        }

        editTextDate.setOnClickListener {
            showDatePicker()
        }

        buttonSaveTransaction.setOnClickListener {
            saveAutomaticTransaction()
        }
    }

    private fun setupDropdowns() {
        // Setup repeat period dropdown
        val repeatPeriods = RepeatPeriod.values().map { it.name }
        val repeatAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, repeatPeriods)
        repeatDropdown.setAdapter(repeatAdapter)

        // Setup account dropdown
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val accounts = DatabaseProvider.getAccountDao(this@InstructionsActivity).getAllAccounts().first()
                val accountNames = accounts.map { it.accountName }
                
                withContext(Dispatchers.Main) {
                    val accountAdapter = ArrayAdapter(this@InstructionsActivity, android.R.layout.simple_dropdown_item_1line, accountNames)
                    accountDropdown.setAdapter(accountAdapter)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showError("Hesaplar yüklenirken hata oluştu: ${e.message}")
                }
            }
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        
        // If there's existing text, parse it
        editTextDate.text?.toString()?.let { dateStr ->
            if (dateStr.isNotEmpty()) {
                val parts = dateStr.split(".")
                if (parts.size == 3) {
                    calendar.set(parts[2].toInt(), parts[1].toInt() - 1, parts[0].toInt())
                }
            }
        }

        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                editTextDate.setText(String.format("%02d.%02d.%04d", dayOfMonth, month + 1, year))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun saveAutomaticTransaction() {
        val amount = editTextAmount.text?.toString()?.toDoubleOrNull()
        val description = editTextDescription.text?.toString()
        val date = editTextDate.text?.toString()
        val accountName = accountDropdown.text?.toString()
        val repeatPeriodStr = repeatDropdown.text?.toString()

        if (amount == null || amount <= 0) {
            showError("Lütfen geçerli bir tutar girin")
            return
        }

        if (description.isNullOrEmpty()) {
            showError("Lütfen bir açıklama girin")
            return
        }

        if (date.isNullOrEmpty()) {
            showError("Lütfen ödeme tarihi seçin")
            return
        }

        if (accountName.isNullOrEmpty()) {
            showError("Lütfen hesap seçin")
            return
        }

        if (repeatPeriodStr.isNullOrEmpty()) {
            showError("Lütfen tekrar sıklığı seçin")
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val accounts = DatabaseProvider.getAccountDao(this@InstructionsActivity).getAllAccounts().first()
                val account = accounts.find { it.accountName == accountName }
                    ?: throw Exception("Hesap bulunamadı")

                // Convert date string to Long timestamp
                val dateParts = date.split(".")
                val day = dateParts[0].toInt()
                val month = dateParts[1].toInt() - 1 // Calendar months are 0-based
                val year = dateParts[2].toInt()
                
                val calendar = Calendar.getInstance()
                calendar.set(year, month, day, 0, 0, 0)
                val dateTimestamp = calendar.timeInMillis

                // Try to convert accountId from String to Long
                val accountIdLong = try {
                    account.accountId.toLong()
                } catch (e: NumberFormatException) {
                    // If conversion fails, generate a new ID based on the account name
                    account.accountName.hashCode().toLong()
                }

                val transaction = AutomaticTransaction(
                    amount = amount,
                    description = description,
                    paymentDate = dateTimestamp,
                    accountId = accountIdLong,
                    repeatPeriod = RepeatPeriod.fromString(repeatPeriodStr)
                )

                val transactionId = DatabaseProvider.getAutomaticTransactionDao().insertTransaction(transaction)

                withContext(Dispatchers.Main) {
                    showSuccess("Otomatik işlem kaydedildi")
                    clearInputs()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showError("İşlem kaydedilirken hata oluştu: ${e.message}")
                }
            }
        }
    }

    private fun clearInputs() {
        editTextAmount.text?.clear()
        editTextDescription.text?.clear()
        editTextDate.text?.clear()
        accountDropdown.text?.clear()
        repeatDropdown.text?.clear()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                pdfRequestCode -> {
                    data?.data?.let { uri -> handlePdfFile(uri) }
                }
                xlsxRequestCode -> {
                    data?.data?.let { uri -> handleXlsxFile(uri) }
                }
            }
        }
    }

    private fun handlePdfFile(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val pdfText = pdfParser.extractText(uri, contentResolver)
                log("DEBUG", "PDF Text: $pdfText")
                
                var bankFound = false
                for ((bankName, keyword) in supportedBanks) {
                    if (pdfText.contains(bankName, ignoreCase = true)) {
                        bankFound = true
                        withContext(Dispatchers.Main) {
                            if (pdfText.contains(keyword)) {
                                handleCreditCardStatement(bankName, pdfText)
                            } else {
                                showError("Kredi kartı ekstresi bulunamadı")
                            }
                        }
                        break
                    }
                }
                
                if (!bankFound) {
                    withContext(Dispatchers.Main) {
                        showError("Bu bankayı desteklemiyoruz.")
                    }
                }
            } catch (e: Exception) {
                log("PDF_ERROR", "PDF işleme hatası: ${e.message}")
                withContext(Dispatchers.Main) {
                    showError("PDF işleme hatası: ${e.message}")
                }
            }
        }
    }

    private fun handleXlsxFile(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val excelParser = ExcelParser()
                val transactions = excelParser.readXLSXFile(uri, contentResolver)
                if (transactions.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        showSuccess("Excel dosyası başarıyla okundu")
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        showError("İşlem bulunamadı")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showError(e.message ?: "Excel dosyası işlenirken hata oluştu")
                }
            }
        }
    }

    private fun handleCreditCardStatement(bankName: String, pdfText: String) {
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
        textViewResult.text = "Dönem Borcu ($bankName): $totalAmount TL"
        textViewDate.text = "Son Ödeme Tarihi ($bankName): $lastDate"

        // Show dialog to ask if user wants to save as automatic transaction
        AlertDialog.Builder(this)
            .setTitle("Otomatik İşlem Olarak Kaydet")
            .setMessage("Kredi kartı ödemesini otomatik işlem olarak kaydetmek ister misiniz?")
            .setPositiveButton("Evet") { _, _ ->
                // First, get available accounts
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val accounts = DatabaseProvider.getAccountDao(this@InstructionsActivity).getAllAccounts().first()
                        withContext(Dispatchers.Main) {
                            // Show account selection dialog
                            val accountNames = accounts.map { it.accountName }.toTypedArray()
                            AlertDialog.Builder(this@InstructionsActivity)
                                .setTitle("Hesap Seçin")
                                .setItems(accountNames) { _, which ->
                                    val selectedAccount = accounts[which]
                                    
                                    // Convert date string to timestamp
                                    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale("tr"))
                                    val paymentDate = try {
                                        dateFormat.parse(lastDate)?.time ?: System.currentTimeMillis()
                                    } catch (e: Exception) {
                                        System.currentTimeMillis()
                                    }

                                    // Create automatic transaction
                                    val automaticTransaction = AutomaticTransaction(
                                        amount = totalAmount?.replace("TL", "")?.trim()?.toDoubleOrNull() ?: 0.0,
                                        description = "$bankName Kredi Kartı Ödemesi",
                                        paymentDate = paymentDate,
                                        accountId = try {
                                            selectedAccount.accountId.toLong()
                                        } catch (e: NumberFormatException) {
                                            selectedAccount.accountName.hashCode().toLong()
                                        },
                                        repeatPeriod = RepeatPeriod.ONE_TIME,
                                        isActive = true
                                    )

                                    // Save to database
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        try {
                                            DatabaseProvider.getAutomaticTransactionDao().insertTransaction(automaticTransaction)
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(this@InstructionsActivity, "Otomatik işlem kaydedildi", Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (e: Exception) {
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(this@InstructionsActivity, "Otomatik işlem kaydedilemedi: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                                .setNegativeButton("İptal", null)
                                .show()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@InstructionsActivity, "Hesaplar yüklenirken hata oluştu: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Hayır", null)
            .show()
    }

    private fun findTotalAmountVakif(text: String): String? {
        val regex = Regex(RegexPatterns.VAKIFBANK_TOTAL_AMOUNT)
        val match = regex.find(text)
        val rawAmount = match?.groups?.get(1)?.value
        return rawAmount?.replace(",", "")
    }

    private fun extractDueDateVakif(text: String): String? {
        val dateRegex = Regex(RegexPatterns.VAKIFBANK_DUE_DATE)
        val match = dateRegex.find(text)
        return match?.groups?.get(1)?.value
    }

    private fun findTotalAmountZiraat(text: String): String? {
        val regex = Regex(RegexPatterns.ZIRAAT_TOTAL_AMOUNT)
        val match = regex.find(text)
        val rawAmount = match?.groups?.get(1)?.value
        return rawAmount?.replace(".", "")?.replace(",", ".")
    }

    private fun extractDueDateZiraat(text: String): String? {
        val dateRegex = Regex(RegexPatterns.ZIRAAT_DUE_DATE)
        val match = dateRegex.find(text)
        return match?.groups?.get(1)?.value
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    1
                )
            }
        } else {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_IMAGES
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.READ_MEDIA_IMAGES,
                        Manifest.permission.READ_MEDIA_VIDEO,
                        Manifest.permission.READ_MEDIA_AUDIO
                    ),
                    2
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 || requestCode == 2) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "İzin verildi!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "İzin reddedildi!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showError(message: String) {
        Snackbar.make(
            findViewById(android.R.id.content),
            message,
            Snackbar.LENGTH_LONG
        ).show()
    }

    private fun showSuccess(message: String) {
        Snackbar.make(
            findViewById(android.R.id.content),
            message,
            Snackbar.LENGTH_SHORT
        ).show()
    }

    private fun log(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, message)
        }
    }
} 