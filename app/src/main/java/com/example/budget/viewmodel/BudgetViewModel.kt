package com.example.budget.viewmodel

import com.example.budget.data.entity.BudgetGoalEntity
import android.icu.util.Calendar
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budget.data.dao.BudgetGoalDao
import com.example.budget.data.dao.BudgetProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BudgetViewModel(private val budgetGoalDao: BudgetGoalDao) : ViewModel() {
    private val _budgetProgress = MutableStateFlow<List<BudgetProgress>>(emptyList())
    val budgetProgress: StateFlow<List<BudgetProgress>> = _budgetProgress.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun saveBudgetGoal(goal: BudgetGoalEntity) {
        viewModelScope.launch {
            try {
                budgetGoalDao.insertBudgetGoal(goal)
                updateBudgetProgress()
            } catch (e: Exception) {
                _error.value = "Bütçe hedefi kaydedilemedi: ${e.localizedMessage}"
            }
        }
    }

    fun updateBudgetProgress() {
        viewModelScope.launch {
            try {
                val currentMonth = getCurrentMonthYear()
                _budgetProgress.value = budgetGoalDao.getBudgetProgress(currentMonth)
            } catch (e: Exception) {
                _error.value = "Bütçe bilgileri alınamadı: ${e.localizedMessage}"
            }
        }
    }

    private fun getCurrentMonthYear(): String {
        val calendar = Calendar.getInstance()
        return "${calendar.get(Calendar.YEAR)}-${String.format("%02d", calendar.get(Calendar.MONTH) + 1)}"
    }
}