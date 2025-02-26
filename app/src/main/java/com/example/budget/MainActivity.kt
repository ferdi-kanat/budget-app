package com.example.budget

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.budget.analytics.AnalyticsActivity
import com.example.budget.analytics.TransactionAnalytics
import com.example.budget.parsers.ExcelParser
import com.example.budget.parsers.PDFParser
import com.example.budget.utils.ExportUtils
import com.example.budget.utils.RegexPatterns
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import java.text.SimpleDateFormat
import java.util.*
import androidx.appcompat.widget.SearchView
import android.app.Activity
import android.util.Log

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
        private const val EDIT_TRANSACTIONS_REQUEST = 100
    }
    private var tempSelectedBank: String? = null
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var progressIndicator: CircularProgressIndicator
    private lateinit var searchView: SearchView
    private var tempExportType: ExportType? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        DatabaseProvider.initialize(this)
        initializeViews()
        setupListeners()
        initializeAdapter()
    }

    private fun initializeViews() {
        textViewResult = findViewById(R.id.textViewResult)
        textViewDate = findViewById(R.id.textViewDate)
        recyclerView = findViewById(R.id.recyclerViewTransactions)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        progressIndicator = findViewById(R.id.progressIndicator)
        searchView = findViewById(R.id.searchBar)

        recyclerView.layoutManager = LinearLayoutManager(this)
        setupSearch()
        setupSwipeRefresh()
    }

    private fun initializeAdapter() {
        transactionAdapter = TransactionAdapter(mutableListOf())
        recyclerView.adapter = transactionAdapter
        loadTransactions()
    }

    private fun setupListeners() {
        setupButtons()
        checkPermissions()
    }

    private fun setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener {
            loadTransactions()
        }
    }

    private fun setupSearch() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { filterTransactions(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let { filterTransactions(it) }
                return true
            }
        })
    }

    private fun setupButtons() {
        findViewById<Button>(R.id.buttonAddTransaction).setOnClickListener {
            showAddTransactionDialog()
        }

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

        findViewById<Button>(R.id.buttonClearDatabase).setOnClickListener {
            showClearDatabaseConfirmation()
        }

        findViewById<Button>(R.id.buttonExport).setOnClickListener {
            showExportDialog()
        }

        findViewById<Button>(R.id.buttonAnalytics).setOnClickListener {
            launchAnalytics()
        }
    }

    private fun showLoading() {
        progressIndicator.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        progressIndicator.visibility = View.GONE
        swipeRefreshLayout.isRefreshing = false
    }

    private fun loadTransactions() {
        showLoading()
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val transactions = DatabaseProvider.getTransactionDao().getAllTransactions()
                withContext(Dispatchers.Main) {
                    if (transactions.isEmpty()) {
                        showError("No transactions found")
                    } else {
                        transactionAdapter.updateData(transactions)
                        recyclerView.scrollToPosition(0)
                    }
                    hideLoading()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    hideLoading()
                    showError("Error loading transactions: ${e.message}")
                }
            }
        }
    }

    private fun filterTransactions(query: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val filteredTransactions = DatabaseProvider.getTransactionDao()
                .getAllTransactions()
                .filter { transaction ->
                    transaction.description.contains(query, ignoreCase = true) ||
                    transaction.amount.toString().contains(query)
                }
            withContext(Dispatchers.Main) {
                transactionAdapter.updateData(filteredTransactions)
            }
        }
    }

    private fun showError(message: String) {
        Snackbar.make(
            findViewById(android.R.id.content),
            message,
            Snackbar.LENGTH_LONG
        ).setAction("Retry") {
            loadTransactions()
        }.show()
    }

    private fun showClearDatabaseConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.clear_database_title)
            .setMessage(R.string.clear_database_message)
            .setPositiveButton(R.string.clear) { _, _ -> clearDatabase() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                EDIT_TRANSACTIONS_REQUEST -> {
                    @Suppress("DEPRECATION")
                    data?.getParcelableArrayListExtra<Transaction>("updatedTransactions")?.let { transactions ->
                        lifecycleScope.launch {
                            try {
                                saveTransactionsToDatabase(transactions)
                                refreshRecyclerView()
                                showSuccess("İşlemler başarıyla kaydedildi")
                            } catch (e: Exception) {
                                Log.e("SAVE_ERROR", "Kaydetme hatası", e)
                                showError("Kaydetme hatası: ${e.message}")
                            }
                        }
                    }
                }
                pdfRequestCode -> {
                    data?.data?.let { uri -> handlePdfFile(uri) }
                }
                xlsxRequestCode -> {
                    data?.data?.let { uri -> handleXlsxFile(uri) }
                }
                EXCEL_EXPORT_REQUEST_CODE, PDF_EXPORT_REQUEST_CODE -> {
                    data?.data?.let { uri ->
                        lifecycleScope.launch(Dispatchers.IO) {
                            try {
                                val transactions = DatabaseProvider.getTransactionDao().getAllTransactions()
                                handleExport(uri, transactions)
                                tempSelectedBank = null
                                withContext(Dispatchers.Main) {
                                    showSuccess("Dışa aktarma başarılı")
                                }
                            } catch (e: Exception) {
                                Log.e("EXPORT_ERROR", "Dışa aktarma hatası", e)
                                withContext(Dispatchers.Main) {
                                    showError("Dışa aktarma hatası: ${e.message}")
                                }
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
    fun handlePdfFile(uri: Uri) {
        showLoading()
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val pdfText = pdfParser.extractText(uri, contentResolver)
                Log.d("DEBUG", "PDF Text: $pdfText") // Debug için PDF içeriğini logla
                
                var bankFound = false
                // Check for supported banks first
                for ((bankName, keyword) in supportedBanks) {
                    if (pdfText.contains(bankName, ignoreCase = true)) {
                        bankFound = true
                        withContext(Dispatchers.Main) {
                            if (pdfText.contains(keyword)) {
                                handleCreditCardStatement(bankName, pdfText)
                            } else {
                                val transactions = extractTransactions(pdfText)
                                Log.d("DEBUG", "Extracted transactions: $transactions") // Debug için işlemleri logla
                                if (transactions.isNotEmpty()) {
                                    launchEditActivity(transactions)
                                } else {
                                    showError("İşlem bulunamadı")
                                }
                            }
                            hideLoading()
                        }
                        break  // Banka bulunduğunda döngüden çık
                    }
                }
                
                // Sadece banka bulunamadığında hata mesajı göster
                if (!bankFound) {
                    withContext(Dispatchers.Main) {
                        hideLoading()
                        showError("Bu bankayı desteklemiyoruz.")
                    }
                }
            } catch (e: Exception) {
                Log.e("PDF_ERROR", "PDF işleme hatası", e) // Detaylı hata logu
                withContext(Dispatchers.Main) {
                    hideLoading()
                    showError("PDF işleme hatası: ${e.message}")
                }
            }
        }
    }

    //excel dosyasını okuma fonksiyonu
    private fun handleXlsxFile(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val excelParser = ExcelParser()
                val transactions = excelParser.readXLSXFile(uri, contentResolver)
                launchEditActivity(transactions, "Bankkart")
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showError(e.message ?: "Error processing XLSX file")
                }
            }
        }
    }
    //veritabanına kaydetme işlemi
    private suspend fun saveTransactionsToDatabase(transactions: List<Transaction>) {
        showLoading()
        val dao = DatabaseProvider.getTransactionDao()
        transactions.forEach { transaction ->
            val entity = TransactionEntity(
                transactionId = transaction.transactionId,
                date = transaction.date,
                time = transaction.time,
                description = transaction.description,
                amount = transaction.amount.toDouble(),
                balance = transaction.balance.toDoubleOrNull() ?: 0.0,
                bankName = transaction.bankName
            )
            dao.insertTransaction(entity)
        }
        withContext(Dispatchers.Main) {
            hideLoading()
            loadTransactions()
            showSuccess("Transactions saved successfully")
        }
    }

    private fun refreshRecyclerView() {
        lifecycleScope.launch(Dispatchers.IO) {
            val transactions = DatabaseProvider.getTransactionDao().getAllTransactions()
            withContext(Dispatchers.Main) {
                transactionAdapter.updateData(transactions)
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
                    0 -> {
                        tempExportType = ExportType.EXCEL
                        exportToExcel(selectedBank)
                    }
                    1 -> {
                        tempExportType = ExportType.PDF
                        exportToPDF(selectedBank)
                    }
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

    private fun handleExport(uri: Uri, transactions: List<TransactionEntity>) {
        try {
            when (tempExportType) {
                ExportType.EXCEL -> ExportUtils().exportToExcel(this@MainActivity, transactions, uri, tempSelectedBank)
                ExportType.PDF -> ExportUtils().exportToPDF(this@MainActivity, transactions, uri, tempSelectedBank)
                null -> throw IllegalStateException("Export type not set")
            }
        } catch (e: Exception) {
            Log.e("EXPORT_ERROR", "Export failed", e)
            throw e
        } finally {
            tempExportType = null
            tempSelectedBank = null
        }
    }

    private enum class ExportType {
        EXCEL,
        PDF
    }

    private fun showSuccess(message: String) {
        Snackbar.make(
            findViewById(android.R.id.content),
            message,
            Snackbar.LENGTH_SHORT
        ).show()
    }
}