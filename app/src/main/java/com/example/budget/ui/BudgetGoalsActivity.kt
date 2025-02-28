package com.example.budget.ui

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.budget.DatabaseProvider
import com.example.budget.R
import com.example.budget.adapter.BudgetGoalAdapter
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