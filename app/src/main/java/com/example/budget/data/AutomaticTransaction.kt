package com.example.budget.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import java.util.*

@Entity(tableName = "automatic_transactions")
data class AutomaticTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "amount")
    val amount: Double,
    
    @ColumnInfo(name = "description")
    val description: String,
    
    @ColumnInfo(name = "payment_date")
    val paymentDate: Long,
    
    @ColumnInfo(name = "account_id")
    val accountId: Long,
    
    @ColumnInfo(name = "repeat_period")
    val repeatPeriod: RepeatPeriod,
    
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,
    
    @ColumnInfo(name = "last_processed_date")
    val lastProcessedDate: Long? = null
)

enum class RepeatPeriod {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY;
    
    companion object {
        fun fromString(value: String): RepeatPeriod {
            return try {
                valueOf(value)
            } catch (e: IllegalArgumentException) {
                // Default to MONTHLY if the string doesn't match any enum value
                MONTHLY
            }
        }
    }
} 