package com.example.budget

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.net.Uri
import android.util.Log
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.budget.parsers.ExcelParser
import com.example.budget.parsers.PDFParser
import com.example.budget.utils.RegexPatterns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import android.os.Parcelable
import android.widget.EditText
import com.example.budget.utils.ExportUtils
import kotlinx.parcelize.Parcelize
import java.text.SimpleDateFormat
import com.example.budget.analytics.TransactionAnalytics
import com.example.budget.analytics.AnalyticsActivity

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {

    private lateinit var textViewResult: TextView
    private val pdfRequestCode = 1
    private val xlsxRequestCode = 2
    private val editRequestCode = 100
    private lateinit var textViewDate: TextView
    private val pdfParser = PDFParser()
    private lateinit var recyclerView: RecyclerView
    private lateinit var transactionAdapter: TransactionAdapter
    private val supportedBanks = mapOf(
        "VakıfBank" to "Dönem Borcunuz",
        "Bankkart" to "Dönem Borcu TL"
    )
    companion object {
        private const val EXCEL_EXPORT_REQUEST_CODE = 3
        private const val PDF_EXPORT_REQUEST_CODE = 4
    }
    private var tempSelectedBank: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkPermissions()

        val buttonLoadPDF: Button = findViewById(R.id.buttonLoadPDF)
        val buttonLoadXLSX: Button = findViewById(R.id.buttonLoadXLSX)
        val buttonClearDatabase: Button = findViewById(R.id.buttonClearDatabase)
        DatabaseProvider.initialize(applicationContext)
        val buttonAddTransaction: Button = findViewById(R.id.buttonAddTransaction)
        buttonAddTransaction.setOnClickListener {
            showAddTransactionDialog()
        }

        textViewResult = findViewById(R.id.textViewResult)
        textViewDate = findViewById(R.id.textViewDate)
        buttonLoadPDF.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "application/pdf"
            startActivityForResult(intent, pdfRequestCode)
        }
        buttonLoadXLSX.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            startActivityForResult(intent, xlsxRequestCode) // xlsxRequestCode = 2
        }
        buttonClearDatabase.setOnClickListener {
            clearDatabase()
        }
        recyclerView = findViewById(R.id.recyclerViewTransactions)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val buttonExport: Button = findViewById(R.id.buttonExport)
        buttonExport.setOnClickListener {
            showExportDialog()
        }
        // Verileri yüklemek için LifecycleScope başlat
        lifecycleScope.launch(Dispatchers.IO) {
            val transactions = DatabaseProvider.getTransactionDao().getAllTransactions()
            withContext(Dispatchers.Main) {
                transactionAdapter = TransactionAdapter(transactions)
                recyclerView.adapter = transactionAdapter
            }
        }

        val buttonAnalytics: Button = findViewById(R.id.buttonAnalytics)
        buttonAnalytics.setOnClickListener {
            launchAnalytics()
        }
    }

    @SuppressLint("SetTextI18n")
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null) {
            when (requestCode) {
                pdfRequestCode -> processPdfText(data.data ?: return)
                xlsxRequestCode -> processExcelFile(data.data ?: return)
                EXCEL_EXPORT_REQUEST_CODE, PDF_EXPORT_REQUEST_CODE -> {
                    val uri = data.data ?: return
                    lifecycleScope.launch(Dispatchers.IO) {
                        val transactions = DatabaseProvider.getTransactionDao().getAllTransactions()
                        val exportUtils = ExportUtils()

                        try {
                            if (requestCode == EXCEL_EXPORT_REQUEST_CODE) {
                                exportUtils.exportToExcel(this@MainActivity, transactions, uri, tempSelectedBank)
                            } else {
                                exportUtils.exportToPDF(this@MainActivity, transactions, uri, tempSelectedBank)
                            }

                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    this@MainActivity,
                                    "Dışa aktarma başarılı",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    this@MainActivity,
                                    "Dışa aktarma başarısız: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                        // Reset the temporary selected bank
                        tempSelectedBank = null
                    }
                }
                editRequestCode -> {
                    val updatedTransactions = data.getParcelableArrayListExtra<Transaction>("updatedTransactions")
                    if (updatedTransactions != null) {
                        lifecycleScope.launch(Dispatchers.IO) {
                            saveTransactionsToDatabase(updatedTransactions)
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@MainActivity, "İşlemler kaydedildi", Toast.LENGTH_SHORT).show()
                                refreshRecyclerView()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showAddTransactionDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_transaction, null)
        val dialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.gelir_gider_ekle))
            .setView(dialogView)
            .create()

        val editTextDescription = dialogView.findViewById<EditText>(R.id.editTextDescription)
        val editTextAmount = dialogView.findViewById<EditText>(R.id.editTextAmount)
        val editTextDate = dialogView.findViewById<EditText>(R.id.editTextDate)
        val buttonSaveTransaction = dialogView.findViewById<Button>(R.id.buttonSaveTransaction)

        // Varsayılan olarak günün tarihini göster
        editTextDate.setText(getCurrentDate())

        // Tarih seçici
        editTextDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(this,
                { _, year, month, day ->
                    val selectedDate = String.format("%02d.%02d.%04d", day, month + 1, year)
                    editTextDate.setText(selectedDate)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        buttonSaveTransaction.setOnClickListener {
            val description = editTextDescription.text.toString()
            val amount = editTextAmount.text.toString().toDoubleOrNull() ?: 0.0
            val date = editTextDate.text.toString()

            if (description.isBlank()) {
                editTextDescription.error = "Açıklama gerekli"
                return@setOnClickListener
            }

            if (amount == 0.0) {
                editTextAmount.error = "Geçerli bir miktar girin"
                return@setOnClickListener
            }

            saveTransaction(description, amount, date)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun saveTransaction(description: String, amount: Double, date: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val transaction = Transaction(
                date = date,
                time = getCurrentTime(),
                transactionId = UUID.randomUUID().toString(),
                amount = amount.toString(),
                balance = "0.0",
                description = description
            )

            val transactionEntity = TransactionEntity(
                date = transaction.date,
                time = transaction.time,
                transactionId = transaction.transactionId,
                amount = amount,
                balance = null,
                description = description,
                bankName = "Manuel Giriş"
            )

            DatabaseProvider.getTransactionDao().insertTransaction(transactionEntity)

            withContext(Dispatchers.Main) {
                refreshRecyclerView()
                Toast.makeText(this@MainActivity, "İşlem kaydedildi", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun getCurrentTime(): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }

    //pdf dosyasını okuma fonksiyonu
    @SuppressLint("SetTextI18n")
    fun processPdfText(uri: Uri) {
        val pdfText = pdfParser.extractText(uri, contentResolver)
        for ((bankName, keyword) in supportedBanks) {
            if (pdfText.contains(bankName, ignoreCase = true)) {
                if (pdfText.contains(keyword)) {
                    handleCreditCardStatement(bankName, pdfText)
                } else {
                    val transactions = extractTransactions(pdfText)
                    launchEditActivity(transactions)
                }
                return
            }
        }
        textViewResult.text = "Bu bankayı desteklemiyoruz."
        textViewDate.text = ""
    }

    //excel dosyasını okuma fonksiyonu
    private fun processExcelFile(uri: Uri) {
        try {
            val excelParser = ExcelParser()
            val transactions = excelParser.readXLSXFile(uri, contentResolver)
            launchEditActivity(transactions, "Bankkart")
        } catch (e: Exception) {
            Log.e("EXCEL_ERROR", "Excel dosyası okunurken hata oluştu: ${e.message}")
            Toast.makeText(this, "Excel dosyası okunamadı!", Toast.LENGTH_LONG).show()
        }
    }
    //veritabanına kaydetme işlemi
    private suspend fun saveTransactionsToDatabase(transactions: List<Transaction>) {
        val dao = DatabaseProvider.getTransactionDao()
        transactions.forEach { transaction ->
            dao.insertTransaction(
                TransactionEntity(
                    date = transaction.date,
                    time = transaction.time,
                    transactionId = transaction.transactionId,
                    amount = transaction.amount.toDouble(),
                    balance = transaction.balance.toDouble(),
                    description = transaction.description,
                    bankName = transaction.bankName
                )
            )
        }
        withContext(Dispatchers.Main) {
            Toast.makeText(this@MainActivity, "${transactions.size} işlem veritabanına kaydedildi!", Toast.LENGTH_SHORT).show()
            refreshRecyclerView()
        }
    }

    private fun refreshRecyclerView() {
        lifecycleScope.launch(Dispatchers.IO) {
            val updatedTransactions = DatabaseProvider.getTransactionDao().getAllTransactions()

            val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

            // Tarihleri Date objesine çevirerek sıralama yap
            val sortedTransactions = updatedTransactions.sortedWith(
                compareByDescending<TransactionEntity> { dateFormat.parse(it.date) }
                    .thenByDescending { it.time }
            )
            withContext(Dispatchers.Main) {
                transactionAdapter.updateData(sortedTransactions)
            }
        }
    }

    private fun clearDatabase() {
        lifecycleScope.launch(Dispatchers.IO) {
            DatabaseProvider.getTransactionDao().deleteAllTransactions()
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "Veritabanı temizlendi!", Toast.LENGTH_SHORT).show()
                refreshRecyclerView()
            }
        }
    }
    //vakıfbank toplam tutarı pdften çekme
    private fun findTotalAmountVakif(text: String): String? {
        val regex = Regex(RegexPatterns.VAKIFBANK_TOTAL_AMOUNT)
        // VakıfBank formatı
        val match = regex.find(text)
        val rawAmount = match?.groups?.get(1)?.value
        return rawAmount?.replace(",", "")
    }
    //vakıfbank son ödeme tarihini pdften çekme
    private fun extractDueDateVakif(text: String): String? {
        val dateRegex = Regex(RegexPatterns.VAKIFBANK_DUE_DATE)

        val match = dateRegex.find(text)
        return match?.groups?.get(1)?.value
    }
    //ziraat bankası dönem borcunu pdften çekme
    private fun findTotalAmountZiraat(text: String): String? {
        val regex = Regex(RegexPatterns.ZIRAAT_TOTAL_AMOUNT) // Ziraat Bankası formatı
        val match = regex.find(text)
        val rawAmount = match?.groups?.get(1)?.value
        return rawAmount?.replace(".", "")?.replace(",", ".")
    }
    //ziraat bankası son ödeme tarihi pdften çekme
    private fun extractDueDateZiraat(text: String): String? {
        val dateRegex = Regex(RegexPatterns.ZIRAAT_DUE_DATE)
        val match = dateRegex.find(text)
        return match?.groups?.get(1)?.value
    }
    @Parcelize
    data class Transaction(
        val date: String,
        val time: String,
        val transactionId: String,
        var amount: String,
        val balance: String,
        var description: String,
        val bankName: String = "VakıfBank" // Add default value for backward compatibility
    ) : Parcelable

    private fun extractTransactions(text: String): List<Transaction> {
        val transactionRegex = Regex(RegexPatterns.TRANSACTION_DETAILS)
        val transactions = mutableListOf<Transaction>()

        transactionRegex.findAll(text).forEach { matchResult ->
            val (dateTime, transactionId, amount, balance, description) = matchResult.destructured
            val (date, time) = dateTime.split(" ") // Tarih ve saati ayır
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
    @SuppressLint("SetTextI18n")
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
        textViewResult.text = "Dönem Borcu ($bankName): $totalAmount TL"
        textViewDate.text = "Son Ödeme Tarihi ($bankName): $lastDate"
    }
    /*private fun handleBankTransactions(bankName: String, pdfText: String) {
        val transactions = extractTransactions(pdfText)
        lifecycleScope.launch(Dispatchers.IO) {
            saveTransactionsToDatabase(transactions, bankName)
        }
    }*/
    private fun showExportDialog() {
        // First, get unique bank names from database
        lifecycleScope.launch(Dispatchers.IO) {
            val transactions = DatabaseProvider.getTransactionDao().getAllTransactions()
            val bankNames = transactions.map { it.bankName }.distinct()

            withContext(Dispatchers.Main) {
                val options = mutableListOf("Tüm Bankalar")
                options.addAll(bankNames)

                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Banka Seçin")
                    .setItems(options.toTypedArray()) { _, bankIndex ->
                        val selectedBank = if (bankIndex == 0) null else options[bankIndex]
                        showExportFormatDialog(selectedBank)
                    }
                    .show()
            }
        }
    }

    private fun showExportFormatDialog(selectedBank: String?) {
        val options = arrayOf("Excel'e Aktar", "PDF'e Aktar")
        AlertDialog.Builder(this)
            .setTitle("Dışa Aktarma Formatı")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> exportToExcel(selectedBank)
                    1 -> exportToPDF(selectedBank)
                }
            }
            .show()
    }

    private fun exportToExcel(selectedBank: String?) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            val filename = if (selectedBank != null) {
                "transactions_${selectedBank}_${getCurrentDateTime()}.xlsx"
            } else {
                "transactions_all_banks_${getCurrentDateTime()}.xlsx"
            }
            putExtra(Intent.EXTRA_TITLE, filename)
        }
        startActivityForResult(intent, EXCEL_EXPORT_REQUEST_CODE)
        // Store selected bank for use in onActivityResult
        tempSelectedBank = selectedBank
    }

    private fun exportToPDF(selectedBank: String?) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
            val filename = if (selectedBank != null) {
                "transactions_${selectedBank}_${getCurrentDateTime()}.pdf"
            } else {
                "transactions_all_banks_${getCurrentDateTime()}.pdf"
            }
            putExtra(Intent.EXTRA_TITLE, filename)
        }
        startActivityForResult(intent, PDF_EXPORT_REQUEST_CODE)
        // Store selected bank for use in onActivityResult
        tempSelectedBank = selectedBank
    }

    private fun getCurrentDateTime(): String {
        return SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    }
    private fun launchEditActivity(transactions: List<Transaction>, bankName: String = "VakıfBank") {
        val intent = Intent(this, EditTransactionsActivity::class.java)
        intent.putExtra("transactions", ArrayList(transactions))
        intent.putExtra("bankName", bankName)
        startActivityForResult(intent, editRequestCode)
    }
    // İzin kontrolü
    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) { // Android 12 ve öncesi
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
        } else { // Android 13 ve sonrası
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
    // İzin sonucu işlemi
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

    private fun launchAnalytics() {
        lifecycleScope.launch(Dispatchers.IO) {
            val transactions = DatabaseProvider.getTransactionDao().getAllTransactions()
            val analytics = TransactionAnalytics().analyzeTransactions(transactions)
            
            val intent = Intent(this@MainActivity, AnalyticsActivity::class.java)
            intent.putExtra("analytics", analytics)
            startActivity(intent)
        }
    }
}