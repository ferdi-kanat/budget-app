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
import kotlinx.parcelize.Parcelize
import java.text.SimpleDateFormat

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

        // Verileri yüklemek için LifecycleScope başlat
        lifecycleScope.launch(Dispatchers.IO) {
            val transactions = DatabaseProvider.getTransactionDao().getAllTransactions()
            withContext(Dispatchers.Main) {
                transactionAdapter = TransactionAdapter(transactions)
                recyclerView.adapter = transactionAdapter
            }
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
                editRequestCode -> {
                    // Get the updated transactions
                    val updatedTransactions = data.getParcelableArrayListExtra<Transaction>("updatedTransactions")
                    if (updatedTransactions != null) {
                        // Save to database using coroutine
                        lifecycleScope.launch(Dispatchers.IO) {
                            // Save transactions to database
                            saveTransactionsToDatabase(updatedTransactions, "VakıfBank") // or appropriate bank name

                            // Refresh the UI on main thread
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
            .setTitle("Yeni Gelir/Gider Ekle")
            .setView(dialogView)
            .create()

        val editTextDescription = dialogView.findViewById<EditText>(R.id.editTextDescription)
        val editTextAmount = dialogView.findViewById<EditText>(R.id.editTextAmount)
        val buttonSaveTransaction = dialogView.findViewById<Button>(R.id.buttonSaveTransaction)

        buttonSaveTransaction.setOnClickListener {
            val description = editTextDescription.text.toString()
            val amount = editTextAmount.text.toString().toDoubleOrNull() ?: 0.0
            saveTransaction(description, amount)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun saveTransaction(description: String, amount: Double) {
        lifecycleScope.launch(Dispatchers.IO) {
            val transaction = Transaction(
                date = getCurrentDate(),
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
                amount = transaction.amount.toDoubleOrNull() ?: 0.0,
                balance = transaction.balance.toDoubleOrNull(),
                description = transaction.description,
                bankName = "" // Add appropriate bank name if needed
            )
            DatabaseProvider.getTransactionDao().insertTransaction(transactionEntity)
            withContext(Dispatchers.Main) {
                refreshRecyclerView()
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
            launchEditActivity(transactions)
        } catch (e: Exception) {
            Log.e("EXCEL_ERROR", "Excel dosyası okunurken hata oluştu: ${e.message}")
            Toast.makeText(this, "Excel dosyası okunamadı!", Toast.LENGTH_LONG).show()
        }
    }
    //veritabanına kaydetme işlemi
    private suspend fun saveTransactionsToDatabase(transactions: List<Transaction>, bankName: String) {
        transactions.forEach { transaction ->
            val transactionEntity = TransactionEntity(
                date = transaction.date,
                time = if (bankName == "Ziraat Bankası") null else transaction.time, // Ziraat için saat yok
                transactionId = transaction.transactionId, // Ziraat için fiş no burada olacak
                amount = transaction.amount.toDoubleOrNull() ?: 0.0,
                balance = transaction.balance.toDoubleOrNull(),
                description = transaction.description,
                bankName = bankName
            )
            DatabaseProvider.getTransactionDao().insertTransaction(transactionEntity)
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
        var description: String
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
    private fun handleBankTransactions(bankName: String, pdfText: String) {
        val transactions = extractTransactions(pdfText)
        lifecycleScope.launch(Dispatchers.IO) {
            saveTransactionsToDatabase(transactions, bankName)
        }
    }
    private fun launchEditActivity(transactions: List<Transaction>) {
        val intent = Intent(this, EditTransactionsActivity::class.java)
        intent.putExtra("transactions", ArrayList(transactions))
        startActivityForResult(intent, editRequestCode) // 100, özel bir istek kodudur
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
}