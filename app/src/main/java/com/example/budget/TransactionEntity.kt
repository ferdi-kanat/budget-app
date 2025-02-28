package com.example.budget

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(
    tableName = "transactions",
    indices = [Index(value = ["transactionId"], unique = true)]
)
data class TransactionEntity(
    @PrimaryKey
    val transactionId: String,
    val date: String,
    val time: String,
    val description: String,
    val amount: Double,
    val balance: Double?,
    val bankName: String,
    val category: String // String olarak saklayalım
) : Parcelable

@Parcelize
enum class TransactionCategory(
    val displayName: String,
    val colorResId: Int
) : Parcelable {
    FOOD("Yemek", R.color.category_food),
    SHOPPING("Alışveriş", R.color.category_shopping),
    BILLS("Faturalar", R.color.category_bills),
    TRANSPORTATION("Ulaşım", R.color.category_transportation),
    HEALTH("Sağlık", R.color.category_health),
    EDUCATION("Eğitim", R.color.category_education),
    ENTERTAINMENT("Eğlence", R.color.category_entertainment),
    SALARY("Maaş", R.color.category_salary),
    OTHER("Diğer", R.color.category_other);

    companion object {
        fun fromDisplayName(displayName: String): TransactionCategory {
            return values().find { it.displayName == displayName } ?: OTHER
        }

        fun fromDescription(description: String): TransactionCategory {
            val lowerDesc = description.lowercase()
            return when {
                lowerDesc.containsAny("market", "restoran", "cafe") -> FOOD
                lowerDesc.containsAny("giyim", "mağaza") -> SHOPPING
                lowerDesc.containsAny("fatura", "elektrik", "su", "doğalgaz") -> BILLS
                lowerDesc.containsAny("taksi", "otobüs", "metro") -> TRANSPORTATION
                lowerDesc.containsAny("hastane", "eczane") -> HEALTH
                lowerDesc.containsAny("okul", "kurs") -> EDUCATION
                lowerDesc.containsAny("sinema", "tiyatro") -> ENTERTAINMENT
                lowerDesc.containsAny("maaş", "ücret") -> SALARY
                else -> OTHER
            }
        }

        private fun String.containsAny(vararg keywords: String): Boolean {
            return keywords.any { this.contains(it) }
        }
    }
}
