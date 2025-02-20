package com.example.budget

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.budget.database.DatabaseProvider
import com.example.budget.models.Transaction
import com.example.budget.parsers.ExcelParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class MainActivity : AppCompatActivity() {

    lateinit var textViewResult: TextView
    private val pdfRequestCode = 1
    private val xlsxRequestCode = 2
    private val editRequestCode = 100
    lateinit var textViewDate: TextView
    lateinit var recyclerView: RecyclerView
    lateinit var transactionAdapter: TransactionAdapter
    val supportedBanks = mapOf(
        "VakıfBank" to "Dönem Borcunuz",
        "Bankkart" to "Dönem Borcu TL"
    )

    private lateinit var transactionManager: TransactionManager
    private lateinit var fileProcessor: FileProcessor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        transactionManager = TransactionManager(this)
        fileProcessor = FileProcessor(this)

        checkPermissions()

        val buttonLoadPDF: Button = findViewById(R.id.buttonLoadPDF)
        val buttonLoadXLSX: Button = findViewById(R.id.buttonLoadXLSX)
        val buttonClearDatabase: Button = findViewById(R.id.buttonClearDatabase)
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
            startActivityForResult(intent, xlsxRequestCode)
        }
        buttonClearDatabase.setOnClickListener {
            clearDatabase()
        }

        recyclerView = findViewById(R.id.recyclerViewTransactions)
        recyclerView.layoutManager = LinearLayoutManager(this)

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
                pdfRequestCode -> fileProcessor.processPdfText(data.data ?: return)
                xlsxRequestCode -> fileProcessor.processExcelFile(data.data ?: return)
                editRequestCode -> {
                    val updatedTransactions = data.getParcelableArrayListExtra<Transaction>("updatedTransactions")
                    if (updatedTransactions != null) {
                        lifecycleScope.launch(Dispatchers.IO) {
                            transactionManager.saveTransactionsToDatabase(updatedTransactions, "VakıfBank")
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
            transactionManager.saveTransaction(description, amount)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun refreshRecyclerView() {
        lifecycleScope.launch(Dispatchers.IO) {
            val updatedTransactions = DatabaseProvider.getTransactionDao().getAllTransactions()
            val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
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
}
