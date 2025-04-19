package com.example.budget.ui

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.budget.DatabaseProvider
import com.example.budget.R
import com.example.budget.adapters.AutomaticTransactionAdapter
import com.example.budget.data.AutomaticTransaction
import com.example.budget.data.dao.AutomaticTransactionDao
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.appcompat.app.AlertDialog
import com.example.budget.databinding.ActivityAutomaticTransactionsBinding

class AutomaticTransactionsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAutomaticTransactionsBinding
    private val viewModel: AutomaticTransactionsViewModel by viewModels()
    private lateinit var adapter: AutomaticTransactionAdapter
    private lateinit var automaticTransactionDao: AutomaticTransactionDao
    private lateinit var addTransactionFab: FloatingActionButton
    private lateinit var emptyStateView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAutomaticTransactionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        automaticTransactionDao = DatabaseProvider.getAutomaticTransactionDao()
        setupToolbar()
        setupRecyclerView()
        setupEmptyState()
        setupFab()
        observeTransactions()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.automatic_transactions)
    }

    private fun setupRecyclerView() {
        adapter = AutomaticTransactionAdapter(
            onDeleteClick = { transaction -> showDeleteConfirmationDialog(transaction) },
            onActiveStateChanged = { transaction, isActive ->
                viewModel.updateTransactionActiveState(transaction, isActive)
            }
        )
        
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@AutomaticTransactionsActivity)
            adapter = this@AutomaticTransactionsActivity.adapter
        }
    }

    private fun setupEmptyState() {
        emptyStateView = layoutInflater.inflate(R.layout.layout_empty_automatic_transactions, binding.root, false)
        binding.root.addView(emptyStateView)
        emptyStateView.visibility = View.GONE
    }

    private fun setupFab() {
        addTransactionFab = binding.fab
        addTransactionFab.setOnClickListener {
            startActivity(Intent(this, InstructionsActivity::class.java))
        }
    }

    private fun observeTransactions() {
        lifecycleScope.launch {
            viewModel.transactions.collectLatest { transactions ->
                adapter.submitList(transactions)
                updateEmptyState(transactions.isEmpty())
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        emptyStateView.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun showDeleteConfirmationDialog(transaction: AutomaticTransaction) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_confirmation)
            .setMessage(R.string.delete_automatic_transaction_confirmation)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteTransaction(transaction)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
} 