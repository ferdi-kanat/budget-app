package com.example.budget.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.budget.DatabaseProvider
import com.example.budget.MainActivity
import com.example.budget.R
import com.example.budget.adapter.AccountAdapter
import com.example.budget.analytics.AnalyticsActivity
import com.example.budget.data.AccountEntity
import com.example.budget.databinding.ActivityAccountsBinding
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*
class AccountsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAccountsBinding
    private lateinit var adapter: AccountAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccountsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        showLoading()
        
        try {
            setupToolbar()
            setupRecyclerView()
            setupNavigation()
            setupFab()
            observeAccounts()
        } catch (e: Exception) {
            showError("Error initializing accounts: ${e.message}")
            hideLoading()
        }
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.recyclerViewAccounts.visibility = View.GONE
    }

    private fun hideLoading() {
        binding.progressBar.visibility = View.GONE
        binding.recyclerViewAccounts.visibility = View.VISIBLE
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.accounts)
    }

    private fun setupRecyclerView() {
        adapter = AccountAdapter()
        binding.recyclerViewAccounts.apply {
            layoutManager = LinearLayoutManager(this@AccountsActivity)
            adapter = this@AccountsActivity.adapter
            setHasFixedSize(true)
        }

        adapter.setOnItemClickListener { account ->
            showEditAccountDialog(account)
        }

        adapter.setOnItemLongClickListener { account ->
            showDeleteAccountDialog(account)
        }
    }

    private fun setupNavigation() {
        val isLargeScreen = resources.configuration.screenWidthDp >= 600

        if (isLargeScreen) {
            binding.bottomNavigationView.visibility = View.GONE
            binding.navigationRail.apply {
                visibility = View.VISIBLE
                setOnItemSelectedListener(createNavigationHandler())
                selectedItemId = R.id.menu_accounts
            }
        } else {
            binding.navigationRail.visibility = View.GONE
            binding.bottomNavigationView.apply {
                visibility = View.VISIBLE
                setOnItemSelectedListener(createNavigationHandler())
                selectedItemId = R.id.menu_accounts
            }
        }
    }

    private fun createNavigationHandler() = NavigationBarView.OnItemSelectedListener { item ->
        when (item.itemId) {
            R.id.menu_home -> {
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
                finish()
                true
            }
            R.id.menu_analytics -> {
                val intent = Intent(this, AnalyticsActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                startActivity(intent)
                true
            }
            R.id.menu_budget -> {
                val intent = Intent(this, BudgetGoalsActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                startActivity(intent)
                true
            }
            R.id.menu_accounts -> true // Already in Accounts
            R.id.menu_settings -> {
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("openSettings", true)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
                finish()
                true
            }
            else -> false
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        val isLargeScreen = resources.configuration.screenWidthDp >= 600
        if (isLargeScreen) {
            binding.navigationRail.selectedItemId = R.id.menu_accounts
        } else {
            binding.bottomNavigationView.selectedItemId = R.id.menu_accounts
        }
    }

    private fun setupFab() {
        binding.fabAddAccount.setOnClickListener {
            showAddAccountDialog()
        }
    }

    private fun observeAccounts() {
        lifecycleScope.launch {
            try {
                DatabaseProvider.getAccountDao(this@AccountsActivity).getAllActiveAccounts()
                    .collectLatest { accounts ->
                        try {
                            adapter.submitList(accounts)
                            updateTotalBalance(accounts)
                            binding.emptyStateLayout.visibility = 
                                if (accounts.isEmpty()) View.VISIBLE else View.GONE
                            binding.recyclerViewAccounts.visibility = View.VISIBLE
                            hideLoading()
                        } catch (e: Exception) {
                            showError("Error updating accounts list: ${e.message}")
                            hideLoading()
                        }
                    }
            } catch (e: Exception) {
                showError("Error loading accounts: ${e.message}")
                hideLoading()
            }
        }
    }

    private fun updateTotalBalance(accounts: List<AccountEntity>) {
        try {
            val totalBalance = accounts.sumOf { it.balance }
            val formatter = NumberFormat.getCurrencyInstance(Locale("tr", "TR"))
            binding.textViewTotalBalance.text = formatter.format(totalBalance)
        } catch (e: Exception) {
            showError("Error calculating total balance: ${e.message}")
        }
    }

    private fun showAddAccountDialog() {
        try {
            val dialogView = layoutInflater.inflate(R.layout.dialog_account, null)
            val dialog = AlertDialog.Builder(this)
                .setTitle(getString(R.string.add_account))
                .setView(dialogView)
                .create()

            setupAccountDialog(dialogView, null) { account ->
                lifecycleScope.launch {
                    try {
                        showLoading()
                        DatabaseProvider.getAccountDao(this@AccountsActivity)
                            .insertAccount(account)
                        dialog.dismiss()
                        showSuccess("Account added successfully")
                    } catch (e: Exception) {
                        showError("Error adding account: ${e.message}")
                    } finally {
                        hideLoading()
                    }
                }
            }

            dialog.show()
        } catch (e: Exception) {
            showError("Error showing add account dialog: ${e.message}")
        }
    }

    private fun showEditAccountDialog(account: AccountEntity) {
        try {
            val dialogView = layoutInflater.inflate(R.layout.dialog_account, null)
            val dialog = AlertDialog.Builder(this)
                .setTitle(getString(R.string.edit_account))
                .setView(dialogView)
                .create()

            setupAccountDialog(dialogView, account) { updatedAccount ->
                lifecycleScope.launch {
                    try {
                        showLoading()
                        DatabaseProvider.getAccountDao(this@AccountsActivity)
                            .updateAccount(updatedAccount)
                        dialog.dismiss()
                        showSuccess("Account updated successfully")
                    } catch (e: Exception) {
                        showError("Error updating account: ${e.message}")
                    } finally {
                        hideLoading()
                    }
                }
            }

            dialog.show()
        } catch (e: Exception) {
            showError("Error showing edit account dialog: ${e.message}")
        }
    }

    private fun setupAccountDialog(
        dialogView: View,
        existingAccount: AccountEntity?,
        onSave: (AccountEntity) -> Unit
    ) {
        val editTextAccountName = dialogView.findViewById<EditText>(R.id.editTextAccountName)
        val editTextBankName = dialogView.findViewById<EditText>(R.id.editTextBankName)
        val spinnerAccountType = dialogView.findViewById<AutoCompleteTextView>(R.id.spinnerAccountType)
        val editTextBalance = dialogView.findViewById<EditText>(R.id.editTextBalance)
        val buttonSave = dialogView.findViewById<Button>(R.id.buttonSave)

        // Set up account type spinner
        val accountTypes = arrayOf(
            getString(R.string.checking),
            getString(R.string.savings),
            getString(R.string.credit_card),
            getString(R.string.investment)
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, accountTypes)
        spinnerAccountType.setAdapter(adapter)

        // Pre-fill fields if editing
        existingAccount?.let {
            editTextAccountName.setText(it.accountName)
            editTextBankName.setText(it.bankName)
            spinnerAccountType.setText(it.accountType, false)
            editTextBalance.setText(it.balance.toString())
        }

        buttonSave.setOnClickListener {
            val accountName = editTextAccountName.text.toString()
            val bankName = editTextBankName.text.toString()
            val accountType = spinnerAccountType.text.toString()
            val balance = editTextBalance.text.toString().toDoubleOrNull()

            when {
                accountName.isBlank() -> {
                    editTextAccountName.error = "Account name is required"
                    return@setOnClickListener
                }
                bankName.isBlank() -> {
                    editTextBankName.error = "Bank name is required"
                    return@setOnClickListener
                }
                accountType.isBlank() -> {
                    spinnerAccountType.error = "Account type is required"
                    return@setOnClickListener
                }
                balance == null -> {
                    editTextBalance.error = "Valid balance is required"
                    return@setOnClickListener
                }
            }

            val account = balance?.let { it1 ->
                AccountEntity(
                    accountId = existingAccount?.accountId ?: UUID.randomUUID().toString(),
                    accountName = accountName,
                    bankName = bankName,
                    accountType = accountType,
                    balance = it1,
                    isActive = true
                )
            }

            if (account != null) {
                onSave(account)
            }
        }
    }

    private fun showDeleteAccountDialog(account: AccountEntity) {
        try {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.delete_account))
                .setMessage(getString(R.string.delete_account_confirmation, account.accountName))
                .setPositiveButton(getString(R.string.delete)) { _, _ ->
                    lifecycleScope.launch {
                        try {
                            showLoading()
                            DatabaseProvider.getAccountDao(this@AccountsActivity)
                                .deleteAccount(account)
                            showSuccess(getString(R.string.account_deleted))
                        } catch (e: Exception) {
                            showError("Error deleting account: ${e.message}")
                        } finally {
                            hideLoading()
                        }
                    }
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        } catch (e: Exception) {
            showError("Error showing delete account dialog: ${e.message}")
        }
    }

    private fun showSuccess(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
} 