package com.example.budget.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import com.example.budget.TransactionEntity
import com.example.budget.TransactionCategory
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
) {
    fun toTransaction(): TransactionEntity {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = paymentDate
        }
        
        val date = String.format(
            "%02d.%02d.%04d",
            calendar.get(Calendar.DAY_OF_MONTH),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.YEAR)
        )
        
        val time = String.format(
            "%02d:%02d",
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE)
        )
        
        return TransactionEntity(
            transactionId = UUID.randomUUID().toString(),
            date = date,
            time = time,
            description = description,
            amount = amount,
            balance = null, // Will be updated by the account
            bankName = "Otomatik İşlem",
            category = TransactionCategory.fromDescription(description).displayName
        )
    }
}

enum class RepeatPeriod {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY,
    ONE_TIME;
    
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