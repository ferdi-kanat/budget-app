package com.example.budget.ui

import android.icu.util.Calendar
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.budget.DatabaseProvider
import com.example.budget.R
import com.example.budget.adapter.BudgetGoalAdapter
import com.example.budget.data.dao.BudgetProgress
import com.example.budget.databinding.ActivityBudgetGoalsBinding
import com.example.budget.viewmodel.BudgetViewModel
import com.example.budget.viewmodel.BudgetViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.google.android.material.snackbar.Snackbar

class BudgetGoalsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBudgetGoalsBinding
    private lateinit var adapter: BudgetGoalAdapter

    // Context parametresi ile güncellendi.
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
        observeViewModel()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.budget_goals)
    }

    private fun setupRecyclerView() {
        adapter = BudgetGoalAdapter()
        binding.recyclerViewBudgetGoals.apply {
            layoutManager = LinearLayoutManager(this@BudgetGoalsActivity)
            adapter = this@BudgetGoalsActivity.adapter
        }

        // Düzenleme için tıklama işleyicisi
        // Düzenleme için tıklama işleyicisi
        adapter.setOnItemClickListener { budgetProgress ->
            // BudgetProgress'i BudgetGoalEntity'ye çevirmen gerekiyor
            lifecycleScope.launch {
                try {
                    // Normalde id ile çekmen gerekir, ancak burada kategoriden buluyoruz
                    val monthYear = getCurrentMonthYear()
                    val goals = DatabaseProvider.getBudgetGoalDao(this@BudgetGoalsActivity).getBudgetGoalsForMonth(monthYear)
                    val goalToEdit = goals.find { it.category == budgetProgress.category }

                    goalToEdit?.let { goal ->
                        val editSheet = BudgetGoalBottomSheet.newInstance(
                            goal.id,
                            goal.category,
                            goal.targetAmount
                        )
                        editSheet.show(supportFragmentManager, BudgetGoalBottomSheet.TAG)
                    }
                } catch (e: Exception) {
                    Snackbar.make(binding.root, "Hedef bulunamadı: ${e.message}", Snackbar.LENGTH_SHORT).show()
                }
            }
        }

        // Silme için uzun tıklama işleyicisi
        adapter.setOnItemLongClickListener { budgetProgress ->
            showDeleteConfirmationDialog(budgetProgress)
        }
    }
    private fun showDeleteConfirmationDialog(budgetProgress: BudgetProgress) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_budget_goal))
            .setMessage(getString(R.string.delete_budget_goal_confirmation, budgetProgress.category))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                lifecycleScope.launch {
                    try {
                        val monthYear = getCurrentMonthYear()
                        val goals = DatabaseProvider.getBudgetGoalDao(this@BudgetGoalsActivity).getBudgetGoalsForMonth(monthYear)
                        val goalToDelete = goals.find { goal -> goal.category == budgetProgress.category }

                        goalToDelete?.let { goal ->
                            viewModel.deleteBudgetGoal(goal)
                            Snackbar.make(binding.root, getString(R.string.budget_goal_deleted), Snackbar.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Snackbar.make(binding.root, "Silme işlemi başarısız: ${e.message}", Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    private fun getCurrentMonthYear(): String {
        val calendar = Calendar.getInstance()
        return "${calendar.get(Calendar.YEAR)}-${String.format("%02d", calendar.get(Calendar.MONTH) + 1)}"
    }
    private fun setupFab() {
        binding.fabAddBudget.setOnClickListener {
            BudgetGoalBottomSheet().show(supportFragmentManager, BudgetGoalBottomSheet.TAG)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.budgetProgress.collectLatest { progress ->
                adapter.submitList(progress)
                binding.emptyStateLayout.root.visibility =
                    if (progress.isEmpty()) View.VISIBLE else View.GONE
            }
        }
        
        lifecycleScope.launch {
            viewModel.error.collectLatest { errorMessage ->
                errorMessage?.let {
                    Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.updateBudgetProgress() // updateBudgetProgress artık public.
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}