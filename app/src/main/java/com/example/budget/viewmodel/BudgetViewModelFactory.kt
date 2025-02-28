package com.example.budget.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.budget.data.dao.BudgetGoalDao

class BudgetViewModelFactory(
    private val budgetGoalDao: BudgetGoalDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BudgetViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BudgetViewModel(budgetGoalDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}