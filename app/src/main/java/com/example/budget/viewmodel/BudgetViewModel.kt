package com.example.budget.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budget.data.entity.BudgetGoalEntity
import com.example.budget.data.BudgetProgress
import com.example.budget.data.dao.BudgetGoalDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BudgetViewModel(private val budgetGoalDao: BudgetGoalDao) : ViewModel() {
    private val _budgetProgress = MutableLiveData<List<BudgetProgress>>()
    val budgetProgress: LiveData<List<BudgetProgress>> = _budgetProgress

    private val _budgetGoals = MutableLiveData<List<BudgetGoalEntity>>()
    val budgetGoals: LiveData<List<BudgetGoalEntity>> = _budgetGoals
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun saveBudgetGoal(budgetGoal: BudgetGoalEntity) {
        viewModelScope.launch {
            try {
                budgetGoalDao.insertBudgetGoal(budgetGoal)
                loadBudgetGoals(budgetGoal.monthYear)
            } catch (e: Exception) {
                _error.value = "Bütçe hedefi kaydedilirken hata oluştu: ${e.message}"
            }
        }
    }

    fun loadBudgetGoals(monthYear: String) {
        viewModelScope.launch {
            try {
                _budgetGoals.value = budgetGoalDao.getBudgetGoalsForMonth(monthYear)
            } catch (e: Exception) {
                _error.value = "Bütçe hedefleri yüklenirken hata oluştu: ${e.message}"
            }
        }
    }

    fun loadBudgetProgress(monthYear: String) {
        viewModelScope.launch {
            try {
                _budgetProgress.value = budgetGoalDao.getBudgetProgress(monthYear).first()
            } catch (e: Exception) {
                _error.value = "Bütçe ilerlemesi yüklenirken hata oluştu: ${e.message}"
            }
        }
    }

    fun updateBudgetGoal(budgetGoal: BudgetGoalEntity) {
        viewModelScope.launch {
            try {
                budgetGoalDao.updateBudgetGoal(budgetGoal)
                loadBudgetGoals(budgetGoal.monthYear)
            } catch (e: Exception) {
                _error.value = "Bütçe hedefi güncellenirken hata oluştu: ${e.message}"
            }
        }
    }
    
    fun updateBudgetProgress() {
        val currentMonthYear = getCurrentMonthYear()
        loadBudgetProgress(currentMonthYear)
    }
    
    private fun getCurrentMonthYear(): String {
        val calendar = java.util.Calendar.getInstance()
        return "${calendar.get(java.util.Calendar.YEAR)}-${String.format("%02d", calendar.get(java.util.Calendar.MONTH) + 1)}"
    }
}