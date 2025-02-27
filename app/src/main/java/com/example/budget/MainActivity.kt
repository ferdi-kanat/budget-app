package com.example.budget

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
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
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigationrail.NavigationRailView
import com.google.android.material.navigation.NavigationBarView
import android.view.MenuItem
import androidx.appcompat.app.AppCompatDelegate
import android.content.res.Configuration
import android.content.Context
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.database.sqlite.SQLiteConstraintException
import com.github.mikephil.charting.BuildConfig
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import android.app.DatePickerDialog

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
        private const val PREFS_NAME = "BudgetPrefs"
        private const val DARK_MODE_KEY = "dark_mode"
    }
    private var tempSelectedBank: String? = null
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var progressIndicator: CircularProgressIndicator
    private lateinit var searchView: SearchView
    private var tempExportType: ExportType? = null
    private lateinit var bottomNavigationView: BottomNavigationView
    private var navigationRailView: NavigationRailView? = null
    private var startDate: String? = null
    private var endDate: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Kaydedilmiş gece modu tercihini uygula
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean(DARK_MODE_KEY, false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
        
        setContentView(R.layout.activity_main)
        DatabaseProvider.initialize(this)
        initializeViews()
        setupListeners()
        initializeAdapter()
        setupNavigationViews()
    }

    private fun initializeViews() {
        textViewResult = findViewById(R.id.textViewResult)
        textViewDate = findViewById(R.id.textViewDate)
        recyclerView = findViewById(R.id.recyclerViewTransactions)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        progressIndicator = findViewById(R.id.progressIndicator)
        searchView = findViewById(R.id.searchBar)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        navigationRailView = findViewById(R.id.navigationRail)

        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            setItemViewCacheSize(20)
            setHasFixedSize(true)
        }
        setupSearch()
        setupSwipeRefresh()
        setupDateFilter()

        // Navigation'ı başlangıçta home seçili olarak ayarla
        bottomNavigationView.selectedItemId = R.id.menu_home
        navigationRailView?.selectedItemId = R.id.menu_home
    }

    private fun initializeAdapter() {
        transactionAdapter = TransactionAdapter(mutableListOf())
        recyclerView.adapter = transactionAdapter
        setupTransactionClickListener()
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
                    .filter { transaction ->
                        // Apply date filter if exists
                        val matchesDateFilter = if (startDate != null && endDate != null) {
                            val transactionDate = transaction.date
                            transactionDate in startDate!!..endDate!!
                        } else true

                        // Apply search filter if exists
                        val matchesSearch = searchView.query?.toString()?.let { query ->
                            transaction.description.contains(query, ignoreCase = true) ||
                            transaction.amount.toString().contains(query)
                        } ?: true

                        matchesDateFilter && matchesSearch
                    }

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
        ).show()
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
        val spinnerCategory = dialogView.findViewById<Spinner>(R.id.spinnerCategory)
        val buttonSaveTransaction = dialogView.findViewById<Button>(R.id.buttonSaveTransaction)

        // Kategori spinner'ını ayarla
        val categories = TransactionCategory.values()
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            categories.map { it.displayName }
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = adapter

        // Varsayılan olarak günün tarihini göster
        editTextDate.setText(getCurrentDate())

        buttonSaveTransaction.setOnClickListener {
            val description = editTextDescription.text.toString()
            val amount = editTextAmount.text.toString().toDoubleOrNull() ?: 0.0
            val date = editTextDate.text.toString()
            val selectedCategory = categories[spinnerCategory.selectedItemPosition]

            if (description.isBlank()) {
                editTextDescription.error = "Açıklama gerekli"
                return@setOnClickListener
            }

            if (amount == 0.0) {
                editTextAmount.error = "Geçerli bir miktar girin"
                return@setOnClickListener
            }

            saveTransaction(description, amount, date, selectedCategory)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun saveTransaction(
        description: String,
        amount: Double,
        date: String,
        category: TransactionCategory
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            val transactionEntity = TransactionEntity(
                transactionId = UUID.randomUUID().toString(),
                date = date,
                time = getCurrentTime(),
                description = description,
                amount = amount,
                balance = null,
                bankName = "Manuel Giriş",
                category = category
            )

            DatabaseProvider.getTransactionDao().insertTransaction(transactionEntity)

            withContext(Dispatchers.Main) {
                refreshRecyclerView()
                showSuccess("İşlem kaydedildi")
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

    private fun log(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, message)
        }
    }

    //pdf dosyasını okuma fonksiyonu
    @SuppressLint("SetTextI18n")
    fun handlePdfFile(uri: Uri) {
        showLoading()
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val pdfText = pdfParser.extractText(uri, contentResolver)
                log("DEBUG", "PDF Text: $pdfText")
                
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
                                log("DEBUG", "Extracted transactions: $transactions")
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
                log("PDF_ERROR", "PDF işleme hatası: ${e.message}")
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
        try {
            showLoading()
            val dao = DatabaseProvider.getTransactionDao()
            
            // Her bir işlemi kontrol et ve güncelle/ekle
            transactions.forEach { transaction ->
                val entity = TransactionEntity(
                    transactionId = transaction.transactionId,
                    date = transaction.date,
                    time = transaction.time,
                    description = transaction.description,
                    amount = transaction.amount.toDouble(),
                    balance = transaction.balance.toDoubleOrNull() ?: 0.0,
                    bankName = transaction.bankName,
                    category = TransactionCategory.fromDescription(transaction.description)
                )

                try {
                    dao.insertTransaction(entity)
                } catch (e: SQLiteConstraintException) {
                    // Eğer işlem zaten varsa, güncelle
                    Log.d("DATABASE", "Transaction already exists, updating: ${entity.transactionId}")
                }
            }

            withContext(Dispatchers.Main) {
                hideLoading()
                loadTransactions()
                showSuccess("İşlemler başarıyla kaydedildi")
            }
        } catch (e: Exception) {
            Log.e("SAVE_ERROR", "Kaydetme hatası", e)
            withContext(Dispatchers.Main) {
                hideLoading()
                showError("Kaydetme hatası: ${e.localizedMessage}")
            }
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
            log("ANALYTICS", "Transaction count: ${transactions.size}")
            
            val analytics = TransactionAnalytics().analyzeTransactions(transactions)
            log("ANALYTICS", "Monthly breakdown: ${analytics.monthlyBreakdown}")
            log("ANALYTICS", "Category breakdown: ${analytics.categoryBreakdown}")

            withContext(Dispatchers.Main) {
                val intent = Intent(this@MainActivity, AnalyticsActivity::class.java)
                intent.putExtra("analytics", analytics as Parcelable)
                startActivity(intent)
            }
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

    private interface NavigationActions {
        fun onAnalyticsSelected()
        fun onSettingsSelected()
    }

    private fun setupNavigationViews() {
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        navigationRailView = findViewById(R.id.navigationRail)

        val isLargeScreen = resources.configuration.screenWidthDp >= 600
        val navigationHandler = createNavigationHandler()

        try {
            setupNavigationBasedOnScreenSize(isLargeScreen, navigationHandler)
        } catch (e: Exception) {
            showError("Navigation yapısı kurulurken hata oluştu: ${e.message}")
        }
    }

    private fun createNavigationHandler(): NavigationBarView.OnItemSelectedListener {
        val navigationActions = object : NavigationActions {
            override fun onAnalyticsSelected() = navigateToAnalytics()
            override fun onSettingsSelected() = showSettingsDialog()
        }
        return NavigationHandler(navigationActions)
    }

    private fun setupNavigationBasedOnScreenSize(
        isLargeScreen: Boolean,
        navigationHandler: NavigationBarView.OnItemSelectedListener
    ) {
        if (isLargeScreen) {
            bottomNavigationView.visibility = View.GONE
            navigationRailView?.apply {
                visibility = View.VISIBLE
                setOnItemSelectedListener(navigationHandler)
                selectedItemId = R.id.menu_home
            }
        } else {
            navigationRailView?.visibility = View.GONE
            bottomNavigationView.apply {
                visibility = View.VISIBLE
                setOnItemSelectedListener(navigationHandler)
                selectedItemId = R.id.menu_home
            }
        }
    }

    private fun navigateToAnalytics() {
        lifecycleScope.launch(Dispatchers.IO) {
            val transactions = DatabaseProvider.getTransactionDao().getAllTransactions()
            val analytics = TransactionAnalytics().analyzeTransactions(transactions)

            withContext(Dispatchers.Main) {
                val intent = Intent(this@MainActivity, AnalyticsActivity::class.java)
                intent.putExtra("analytics", analytics as Parcelable)
                startActivity(intent)
            }
        }
    }

    private fun showSettingsDialog() {
        val options = arrayOf("Veritabanını Temizle", "Dışa Aktar", "Gece Modu", "İptal")
        AlertDialog.Builder(this)
            .setTitle("Ayarlar")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> clearDatabase()
                    1 -> showExportDialog()
                    2 -> toggleDarkMode()
                }
            }
            .show()
    }

    private fun toggleDarkMode() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isDarkMode = resources.configuration.uiMode and 
                        Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        
        prefs.edit().putBoolean(DARK_MODE_KEY, !isDarkMode).apply()
        
        AppCompatDelegate.setDefaultNightMode(
            if (!isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    private class NavigationHandler(private val actions: NavigationActions) : 
        NavigationBarView.OnItemSelectedListener {
        
        override fun onNavigationItemSelected(item: MenuItem): Boolean {
            return when (item.itemId) {
                R.id.menu_home -> true  // Ana sayfa zaten aktif
                R.id.menu_analytics -> {
                    actions.onAnalyticsSelected()
                    true
                }
                R.id.menu_settings -> {
                    actions.onSettingsSelected()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupTransactionClickListener() {
        transactionAdapter.setOnItemClickListener { transaction ->
            val bottomSheet = TransactionDetailsBottomSheet.newInstance(transaction)
            bottomSheet.show(supportFragmentManager, "TransactionDetails")
        }
    }

    private fun setupDateFilter() {
        findViewById<MaterialButton>(R.id.buttonDateFilter).setOnClickListener {
            showDateFilterDialog()
        }
    }

    private fun showDateFilterDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_date_filter, null)
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.date_filter)
            .setView(dialogView)
            .create()

        val editTextStartDate = dialogView.findViewById<TextInputEditText>(R.id.editTextStartDate)
        val editTextEndDate = dialogView.findViewById<TextInputEditText>(R.id.editTextEndDate)

        // Set current filter values if they exist
        editTextStartDate.setText(startDate ?: "")
        editTextEndDate.setText(endDate ?: "")

        // Setup date pickers
        editTextStartDate.setOnClickListener {
            showDatePicker(editTextStartDate)
        }

        editTextEndDate.setOnClickListener {
            showDatePicker(editTextEndDate)
        }

        // Setup buttons
        dialogView.findViewById<MaterialButton>(R.id.buttonClearFilter).setOnClickListener {
            startDate = null
            endDate = null
            loadTransactions()
            dialog.dismiss()
        }

        dialogView.findViewById<MaterialButton>(R.id.buttonApplyFilter).setOnClickListener {
            startDate = editTextStartDate.text?.toString()
            endDate = editTextEndDate.text?.toString()
            loadTransactions()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showDatePicker(editText: TextInputEditText) {
        val calendar = Calendar.getInstance()
        
        // If there's existing text, parse it
        editText.text?.toString()?.let { dateStr ->
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
                editText.setText(String.format("%02d.%02d.%04d", dayOfMonth, month + 1, year))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
}