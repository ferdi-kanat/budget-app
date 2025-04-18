package com.example.budget.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey
    val accountId: String,
    val accountName: String,
    val bankName: String,
    val balance: Double,
    val accountType: String, // Checking, Savings, Credit Card, etc.
    val currency: String = "TRY", // Default to Turkish Lira
    val isActive: Boolean = true
) 