package com.example.budget.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budget.DatabaseProvider
import com.example.budget.data.AutomaticTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class AutomaticTransactionsViewModel : ViewModel() {
    private val automaticTransactionDao = DatabaseProvider.getAutomaticTransactionDao()
    
    val transactions: Flow<List<AutomaticTransaction>> = automaticTransactionDao.getAllTransactions()
    
    fun deleteTransaction(transaction: AutomaticTransaction) {
        viewModelScope.launch {
            automaticTransactionDao.deleteTransaction(transaction)
        }
    }
    
    fun updateTransactionActiveState(transaction: AutomaticTransaction, isActive: Boolean) {
        viewModelScope.launch {
            automaticTransactionDao.updateTransaction(transaction.copy(isActive = isActive))
        }
    }
} 