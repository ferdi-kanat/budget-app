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
import android.widget.ArrayAdapter
import android.database.sqlite.SQLiteConstraintException
import com.github.mikephil.charting.BuildConfig
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import android.app.DatePickerDialog
import com.example.budget.ui.BudgetGoalsActivity
import com.example.budget.ui.AccountsActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.widget.RadioGroup
import android.widget.AutoCompleteTextView
import com.example.budget.data.AccountEntity
import com.example.budget.utils.AutoBackupManager
import com.example.budget.utils.BackupUtils
import kotlinx.coroutines.flow.first

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
        private const val BACKUP_REQUEST_CODE = 5
        private const val RESTORE_REQUEST_CODE = 6
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
        
        // Initialize database and create default account before setting up UI
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                DatabaseProvider.initialize(this@MainActivity)
                createDefaultAccountIfNeeded()
                
                withContext(Dispatchers.Main) {
                    initializeViews()
                    setupListeners()
                    initializeAdapter()
                    setupNavigationViews()

                    // Handle settings intent
                    if (intent.getBooleanExtra("openSettings", false)) {
                        showSettingsDialog()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showError("Veritabanı başlatılırken hata oluştu: ${e.message}")
                }
            }
        }
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

        // Initialize adapter first
        transactionAdapter = TransactionAdapter(mutableListOf())
        recyclerView.adapter = transactionAdapter
        
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            setItemViewCacheSize(20)
            setHasFixedSize(true)
        }

        // Initialize FAB and set click listener
        findViewById<FloatingActionButton>(R.id.fabAddTransaction).setOnClickListener {
            showAddTransactionDialog()
        }

        setupSearch()
        setupSwipeRefresh()
        setupDateFilter()
        setupTransactionClickListener()

        // Navigation'ı başlangıçta home seçili olarak ayarla
        bottomNavigationView.selectedItemId = R.id.menu_home
        navigationRailView?.selectedItemId = R.id.menu_home
        
        // Load transactions after everything is set up
        loadTransactions()
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

        findViewById<Button>(R.id.buttonExport).setOnClickListener {
            showExportDialog()
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
                BACKUP_REQUEST_CODE -> {
                    data?.data?.let { uri ->
                        lifecycleScope.launch(Dispatchers.IO) {
                            try {
                                val transactions = DatabaseProvider.getTransactionDao().getAllTransactions()
                                BackupUtils.createBackup(this@MainActivity, transactions, uri)
                                withContext(Dispatchers.Main) {
                                    showSuccess("Yedekleme başarılı")
                                }
                            } catch (e: Exception) {
                                Log.e("BACKUP_ERROR", "Backup failed", e)
                                withContext(Dispatchers.Main) {
                                    showError("Yedekleme hatası: ${e.message}")
                                }
                            }
                        }
                    }
                }
                RESTORE_REQUEST_CODE -> {
                    data?.data?.let { uri ->
                        lifecycleScope.launch(Dispatchers.IO) {
                            try {
                                val transactions = BackupUtils.restoreFromBackup(this@MainActivity, uri)
                                
                                // First, clear the database
                                DatabaseProvider.getTransactionDao().deleteAllTransactions()
                                DatabaseProvider.getAccountDao(this@MainActivity).deleteAllAccounts()
                                
                                // Create default account first
                                createDefaultAccountIfNeeded()
                                
                                // Group transactions by bank to create/update accounts
                                val transactionsByBank = transactions.groupBy { it.bankName }
                                
                                // Create/update accounts based on transactions
                                transactionsByBank.forEach { (bankName, bankTransactions) ->
                                    when (bankName) {
                                        "VakıfBank" -> {
                                            createVakifBankAccountIfNeeded(convertToTransactionList(bankTransactions))
                                        }
                                        "Bankkart" -> {
                                            createBankkartAccountIfNeeded(convertToTransactionList(bankTransactions))
                                        }
                                    }
                                }
                                
                                // Insert all transactions
                                DatabaseProvider.getTransactionDao().insertTransactions(transactions)
                                
                                withContext(Dispatchers.Main) {
                                    // Update RecyclerView with new data
                                    refreshRecyclerView()
                                    showSuccess("Geri yükleme başarılı")
                                }
                            } catch (e: Exception) {
                                Log.e("RESTORE_ERROR", "Restore failed", e)
                                withContext(Dispatchers.Main) {
                                    showError("Geri yükleme hatası: ${e.message}")
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
        val radioGroupType = dialogView.findViewById<RadioGroup>(R.id.radioGroupTransactionType)
        val categorySpinner = dialogView.findViewById<AutoCompleteTextView>(R.id.spinnerCategory)
        val buttonSaveTransaction = dialogView.findViewById<Button>(R.id.buttonSaveTransaction)

        // Set up category dropdown
        val categories = TransactionCategory.values()
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            categories.map { it.displayName }
        )
        categorySpinner.setAdapter(adapter)

        // Show current date by default
        editTextDate.setText(getCurrentDate())

        // Handle date selection
        editTextDate.setOnClickListener {
            showDatePicker(editTextDate)
        }

        buttonSaveTransaction.setOnClickListener {
            val description = editTextDescription.text.toString()
            val amountStr = editTextAmount.text.toString()
            val date = editTextDate.text.toString()
            val isIncome = radioGroupType.checkedRadioButtonId == R.id.radioIncome
            val selectedCategory = categories.find { it.displayName == categorySpinner.text.toString() }
                ?: categories[0]

            if (description.isBlank()) {
                editTextDescription.error = "Description is required"
                return@setOnClickListener
            }

            val amount = amountStr.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                editTextAmount.error = "Please enter a positive amount"
                return@setOnClickListener
            }

            // Convert amount to positive or negative based on transaction type
            val finalAmount = if (isIncome) amount else -amount
            
            saveTransaction(description, finalAmount, date, selectedCategory)
            dialog.dismiss()
        }

        dialog.show()
    }

    private suspend fun createDefaultAccountIfNeeded() {
        try {
            val accountDao = DatabaseProvider.getAccountDao(this)
            val accounts = accountDao.getAllAccounts().first()
            
            // Check if default account already exists
            if (accounts.none { it.bankName == "Manuel Giriş" }) {
                // Create new default account
                val account = AccountEntity(
                    accountId = UUID.randomUUID().toString(),
                    accountName = "Manuel İşlemler",
                    bankName = "Manuel Giriş",
                    balance = 0.0,
                    accountType = "Vadesiz",
                    isActive = true
                )
                
                accountDao.insertAccount(account)
                log("ACCOUNT", "Default account created")
            }
        } catch (e: Exception) {
            log("ACCOUNT_ERROR", "Error creating default account: ${e.message}")
            throw e
        }
    }

    private suspend fun updateDefaultAccountBalance(amount: Double) {
        try {
            val accountDao = DatabaseProvider.getAccountDao(this)
            val accounts = accountDao.getAllAccounts().first()
            
            // Find default account
            accounts.find { it.bankName == "Manuel Giriş" }?.let { account ->
                accountDao.updateBalance(account.accountId, amount)
                log("ACCOUNT", "Default account balance updated by: $amount")
            }
        } catch (e: Exception) {
            log("ACCOUNT_ERROR", "Error updating default account balance: ${e.message}")
            throw e
        }
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
                category = category.displayName
            )

            DatabaseProvider.getTransactionDao().insertTransaction(transactionEntity)
            
            // Update default account balance
            updateDefaultAccountBalance(amount)

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
                                    // Create VakıfBank account if it doesn't exist
                                    if (bankName == "VakıfBank") {
                                        createVakifBankAccountIfNeeded(transactions)
                                    }
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

    private suspend fun createVakifBankAccountIfNeeded(transactions: List<Transaction>) {
        try {
            val accountDao = DatabaseProvider.getAccountDao(this)
            val accounts = accountDao.getAllAccounts().first()
            
            // Check if VakıfBank account already exists
            if (accounts.none { it.bankName == "VakıfBank" }) {
                // Get the latest balance from transactions
                val latestBalance = transactions.lastOrNull()?.balance?.toDoubleOrNull() ?: 0.0
                
                // Create new VakıfBank account
                val account = AccountEntity(
                    accountId = UUID.randomUUID().toString(),
                    accountName = "VakıfBank Hesap",
                    bankName = "VakıfBank",
                    balance = latestBalance,
                    accountType = "Vadesiz",
                    isActive = true
                )
                
                accountDao.insertAccount(account)
                log("ACCOUNT", "VakıfBank account created with balance: $latestBalance")
            }
        } catch (e: Exception) {
            log("ACCOUNT_ERROR", "Error creating VakıfBank account: ${e.message}")
            throw e
        }
    }

    //excel dosyasını okuma fonksiyonu
    private fun handleXlsxFile(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val excelParser = ExcelParser()
                val transactions = excelParser.readXLSXFile(uri, contentResolver)
                if (transactions.isNotEmpty()) {
                    createBankkartAccountIfNeeded(transactions)
                    launchEditActivity(transactions, "Bankkart")
                } else {
                    withContext(Dispatchers.Main) {
                        showError("İşlem bulunamadı")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showError(e.message ?: "Error processing XLSX file")
                }
            }
        }
    }

    private suspend fun createBankkartAccountIfNeeded(transactions: List<Transaction>) {
        try {
            val accountDao = DatabaseProvider.getAccountDao(this)
            val accounts = accountDao.getAllAccounts().first()
            
            // Check if Bankkart account already exists
            if (accounts.none { it.bankName == "Bankkart" }) {
                // Get the latest balance from transactions
                val latestBalance = transactions.lastOrNull()?.balance?.toDoubleOrNull() ?: 0.0
                
                // Create new Bankkart account
                val account = AccountEntity(
                    accountId = UUID.randomUUID().toString(),
                    accountName = "Ziraat Hesap",
                    bankName = "Ziraat",
                    balance = latestBalance,
                    accountType = "Vadesiz",
                    isActive = true
                )
                
                accountDao.insertAccount(account)
                log("ACCOUNT", "Bankkart account created with balance: $latestBalance")
            }
        } catch (e: Exception) {
            log("ACCOUNT_ERROR", "Error creating Bankkart account: ${e.message}")
            throw e
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
                    category = if (transaction.bankName != "Manuel Giriş") {
                        // Bank transactions should be categorized as BANK
                        TransactionCategory.BANK.displayName
                    } else {
                        // Manual transactions use category detection from description
                        TransactionCategory.fromDescription(transaction.description).displayName
                    }
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
            try {
                val transactions = DatabaseProvider.getTransactionDao().getAllTransactions()
                withContext(Dispatchers.Main) {
                    // Update adapter with new data in a single operation
                    transactionAdapter.submitNewData(transactions)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showError("Error refreshing transactions: ${e.message}")
                }
            }
        }
    }

    private fun clearDatabase() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Clear both transactions and accounts
                DatabaseProvider.getTransactionDao().deleteAllTransactions()
                DatabaseProvider.getAccountDao(this@MainActivity).deleteAllAccounts()
                
                // Recreate the default account
                createDefaultAccountIfNeeded()
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Veritabanı temizlendi!", Toast.LENGTH_SHORT).show()
                    refreshRecyclerView()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showError("Veritabanı temizlenirken hata oluştu: ${e.message}")
                }
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

    /*private fun launchAnalytics() {
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
    }*/

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
        fun onBudgetGoalsSelected()
        fun onAccountsSelected()
    }

    private fun setupNavigationViews() {
        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            handleNavigationItemSelected(menuItem)
            true
        }
        navigationRailView?.setOnItemSelectedListener { menuItem ->
            handleNavigationItemSelected(menuItem)
            true
        }
    }

    private fun handleNavigationItemSelected(menuItem: MenuItem) {
        when (menuItem.itemId) {
            R.id.menu_home -> {
                // Already on home
            }
            R.id.menu_analytics -> {
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val transactions = DatabaseProvider.getTransactionDao().getAllTransactions()
                        val analytics = TransactionAnalytics().analyzeTransactions(transactions)
                        
                        withContext(Dispatchers.Main) {
                            val intent = Intent(this@MainActivity, AnalyticsActivity::class.java)
                            intent.putExtra("analytics", analytics as Parcelable)
                            startActivity(intent)
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            showError("Analytics error: ${e.message}")
                        }
                    }
                }
            }
            R.id.menu_budget -> {
                val intent = Intent(this, BudgetGoalsActivity::class.java)
                startActivity(intent)
            }
            R.id.menu_accounts -> {
                val intent = Intent(this, AccountsActivity::class.java)
                startActivity(intent)
            }
            R.id.menu_settings -> {
                showSettingsDialog()
            }
        }
    }

    private fun showSettingsDialog() {
        val options = arrayOf(
            "Veritabanını Temizle",
            "PDF Yükle",
            "Excel Yükle",
            "Dışa Aktar",
            "Yedekle/Geri Yükle",
            "Otomatik Yedekleme",
            "Gece Modu",
            "İptal"
        )

        AlertDialog.Builder(this)
            .setTitle("Ayarlar")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> clearDatabase()
                    1 -> {
                        // PDF Yükle
                        val pdfIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                            type = "application/pdf"
                        }
                        startActivityForResult(pdfIntent, pdfRequestCode)
                    }
                    2 -> {
                        // Excel Yükle
                        val xlsxIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                        }
                        startActivityForResult(xlsxIntent, xlsxRequestCode)
                    }
                    3 -> showExportDialog()
                    4 -> showBackupRestoreDialog()
                    5 -> showAutoBackupDialog()
                    6 -> toggleDarkMode()
                    // 7 -> İptal
                }
            }
            .show()
    }

    private fun showAutoBackupDialog() {
        val autoBackupManager = AutoBackupManager(this)
        val options = arrayOf("Otomatik Yedeklemeyi Başlat", "Otomatik Yedeklemeyi Durdur")
        
        AlertDialog.Builder(this)
            .setTitle("Otomatik Yedekleme")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        autoBackupManager.scheduleWeeklyBackup()
                        showSuccess("Otomatik yedekleme başlatıldı")
                    }
                    1 -> {
                        autoBackupManager.cancelAutoBackup()
                        showSuccess("Otomatik yedekleme durduruldu")
                    }
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

    private fun showDatePicker(editText: EditText) {
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

    private fun showBackupRestoreDialog() {
        val options = arrayOf("Yedekle", "Geri Yükle")
        AlertDialog.Builder(this)
            .setTitle("Yedekleme ve Geri Yükleme")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> createBackup()
                    1 -> restoreFromBackup()
                }
            }
            .show()
    }

    private fun createBackup() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, BackupUtils.getBackupFilename())
        }
        startActivityForResult(intent, BACKUP_REQUEST_CODE)
    }

    private fun restoreFromBackup() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
        }
        startActivityForResult(intent, RESTORE_REQUEST_CODE)
    }

    private fun convertToTransactionList(entities: List<TransactionEntity>): List<Transaction> {
        return entities.map { entity ->
            Transaction(
                date = entity.date,
                time = entity.time,
                transactionId = entity.transactionId,
                amount = entity.amount.toString(),
                balance = entity.balance?.toString() ?: "0.0",
                description = entity.description,
                bankName = entity.bankName
            )
        }
    }
}