package com.example.budget
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.Button
import android.widget.Toast

@Suppress("DEPRECATION")
class EditTransactionsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: EditableTransactionAdapter
    private lateinit var buttonSave: Button
    private var transactions: MutableList<MainActivity.Transaction> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_transactions)

        recyclerView = findViewById(R.id.recyclerViewEditableTransactions)
        buttonSave = findViewById(R.id.buttonSaveTransactions)

        transactions = intent.getParcelableArrayListExtra("transactions") ?: mutableListOf()
        adapter = EditableTransactionAdapter(transactions)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        buttonSave.setOnClickListener {
            if (transactions.isNotEmpty()) {
                val resultIntent = Intent()
                resultIntent.putParcelableArrayListExtra("updatedTransactions", ArrayList(transactions))
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            } else {
                Toast.makeText(this, "No transactions to save", Toast.LENGTH_SHORT).show()
            }
        }
    }
}