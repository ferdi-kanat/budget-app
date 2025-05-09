package com.example.budget.ui

import android.content.Intent
import android.icu.util.Calendar
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.budget.DatabaseProvider
import com.example.budget.MainActivity
import com.example.budget.R
import com.example.budget.adapter.BudgetGoalAdapter
import com.example.budget.analytics.AnalyticsActivity
import com.example.budget.data.BudgetProgress
import com.example.budget.databinding.ActivityBudgetGoalsBinding
import com.example.budget.viewmodel.BudgetViewModel
import com.example.budget.viewmodel.BudgetViewModelFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BudgetGoalsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBudgetGoalsBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: BudgetGoalAdapter
    private val viewModel: BudgetViewModel by viewModels {
        BudgetViewModelFactory(DatabaseProvider.getBudgetGoalDao(this))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBudgetGoalsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupFab()
        setupNavigation()
        observeViewModel()
        loadData()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.budget_goals)
    }

    private fun setupNavigation() {
        val isLargeScreen = resources.configuration.screenWidthDp >= 600

        if (isLargeScreen) {
            binding.bottomNavigationView.visibility = View.GONE
            binding.navigationRail.apply {
                visibility = View.VISIBLE
                setOnItemSelectedListener(createNavigationHandler())
                selectedItemId = R.id.menu_budget
            }
        } else {
            binding.navigationRail.visibility = View.GONE
            binding.bottomNavigationView.apply {
                visibility = View.VISIBLE
                setOnItemSelectedListener(createNavigationHandler())
                selectedItemId = R.id.menu_budget
            }
        }
    }

    private fun createNavigationHandler() = NavigationBarView.OnItemSelectedListener { item ->
        when (item.itemId) {
            R.id.menu_home -> {
                // For home, we want to clear the stack and start fresh
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
            R.id.menu_budget -> true // Already in Budget Goals
            R.id.menu_accounts -> {
                val intent = Intent(this, AccountsActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                startActivity(intent)
                true
            }
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

    private fun setupRecyclerView() {
        recyclerView = binding.recyclerViewBudgetGoals
        adapter = BudgetGoalAdapter(
            onEditClick = {
                showEditDialog()
            },
            onDeleteClick = { budgetProgress ->
                deleteBudgetGoal(budgetProgress)
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun setupFab() {
        binding.fabAddBudget.setOnClickListener {
            showAddDialog()
        }
    }

    private fun observeViewModel() {
        viewModel.budgetProgress.observe(this) { progress ->
            adapter.submitList(progress)
            binding.emptyStateLayout.root.visibility =
                if (progress.isEmpty()) View.VISIBLE else View.GONE
        }
        
        viewModel.error.observe(this) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun loadData() {
        val currentMonthYear = getCurrentMonthYear()
        viewModel.loadBudgetProgress(currentMonthYear)
    }

    private fun showAddDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_budget_goal, null)
        val categoryInput = dialogView.findViewById<TextInputEditText>(R.id.categoryInput)
        val amountInput = dialogView.findViewById<TextInputEditText>(R.id.amountInput)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.add_budget_goal)
            .setView(dialogView)
            .setPositiveButton(R.string.add) { _, _ ->
                val category = categoryInput.text?.toString()?.trim()
                val amount = amountInput.text?.toString()?.toDoubleOrNull()

                if (category.isNullOrEmpty() || amount == null) {
                    Toast.makeText(this, R.string.invalid_input, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                lifecycleScope.launch {
                    val currentMonthYear = getCurrentMonthYear()
                    viewModel.addBudgetGoal(category, amount, currentMonthYear)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showEditDialog() {
        val selectedItem = adapter.currentList.firstOrNull() ?: return
        val dialogView = layoutInflater.inflate(R.layout.dialog_budget_goal, null)
        val categoryInput = dialogView.findViewById<TextInputEditText>(R.id.categoryInput)
        val amountInput = dialogView.findViewById<TextInputEditText>(R.id.amountInput)

        categoryInput.setText(selectedItem.category)
        amountInput.setText(selectedItem.targetAmount.toString())

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.edit_budget_goal)
            .setView(dialogView)
            .setPositiveButton(R.string.save) { _, _ ->
                val category = categoryInput.text?.toString()?.trim()
                val amount = amountInput.text?.toString()?.toDoubleOrNull()

                if (category.isNullOrEmpty() || amount == null) {
                    Toast.makeText(this, R.string.invalid_input, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                lifecycleScope.launch {
                    val currentMonthYear = getCurrentMonthYear()
                    val budgetGoalEntity = DatabaseProvider.getBudgetGoalDao(this@BudgetGoalsActivity)
                        .getBudgetGoalByCategoryAndMonth(selectedItem.category, currentMonthYear)
                        .first()
                    
                    budgetGoalEntity?.let {
                        val updatedEntity = it.copy(
                            category = category,
                            targetAmount = amount
                        )
                        viewModel.updateBudgetGoal(updatedEntity)
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun deleteBudgetGoal(budgetProgress: BudgetProgress) {
        val currentMonthYear = getCurrentMonthYear()
        
        lifecycleScope.launch {
            val budgetGoalEntity = DatabaseProvider.getBudgetGoalDao(this@BudgetGoalsActivity)
                .getBudgetGoalByCategoryAndMonth(budgetProgress.category, currentMonthYear)
                .first()
            
            budgetGoalEntity?.let {
                DatabaseProvider.getBudgetGoalDao(this@BudgetGoalsActivity)
                    .deleteBudgetGoal(it)
                Toast.makeText(this@BudgetGoalsActivity, R.string.budget_goal_deleted, Toast.LENGTH_SHORT).show()
                loadData()
            }
        }
    }

    private fun getCurrentMonthYear(): String {
        val calendar = Calendar.getInstance()
        return "${calendar.get(Calendar.YEAR)}-${String.format("%02d", calendar.get(Calendar.MONTH) + 1)}"
    }

    override fun onResume() {
        super.onResume()
        viewModel.updateBudgetProgress() // updateBudgetProgress artık public.
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Reset selected navigation item
        val isLargeScreen = resources.configuration.screenWidthDp >= 600
        if (isLargeScreen) {
            binding.navigationRail.selectedItemId = R.id.menu_budget
        } else {
            binding.bottomNavigationView.selectedItemId = R.id.menu_budget
        }
    }
}