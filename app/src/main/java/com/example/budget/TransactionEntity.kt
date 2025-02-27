package com.example.budget

import android.graphics.Color
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

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
    val category: TransactionCategory = TransactionCategory.OTHER
)

enum class TransactionCategory(val color: Int, val displayName: String) {
    FOOD(Color.parseColor("#FF6B6B"), "Yemek"),
    SHOPPING(Color.parseColor("#4ECDC4"), "Alışveriş"),
    TRANSPORTATION(Color.parseColor("#45B7D1"), "Ulaşım"),
    BILLS(Color.parseColor("#96CEB4"), "Faturalar"),
    ENTERTAINMENT(Color.parseColor("#FFBE0B"), "Eğlence"),
    HEALTH(Color.parseColor("#FF006E"), "Sağlık"),
    EDUCATION(Color.parseColor("#8338EC"), "Eğitim"),
    INCOME(Color.parseColor("#06D6A0"), "Gelir"),
    OTHER(Color.parseColor("#A0A0A0"), "Diğer");

    companion object {
        fun fromDescription(description: String): TransactionCategory {
            return when {
                description.contains(Regex("market|alisveris|mağaza", RegexOption.IGNORE_CASE)) -> SHOPPING
                description.contains(Regex("yemek|cafe|restaurant|restoran", RegexOption.IGNORE_CASE)) -> FOOD
                description.contains(Regex("taksi|metro|otobus|otobüs|uber", RegexOption.IGNORE_CASE)) -> TRANSPORTATION
                description.contains(Regex("fatura|elektrik|su|dogalgaz|doğalgaz|internet", RegexOption.IGNORE_CASE)) -> BILLS
                description.contains(Regex("sinema|tiyatro|konser", RegexOption.IGNORE_CASE)) -> ENTERTAINMENT
                description.contains(Regex("hastane|eczane|doktor", RegexOption.IGNORE_CASE)) -> HEALTH
                description.contains(Regex("kurs|kitap|okul", RegexOption.IGNORE_CASE)) -> EDUCATION
                description.contains(Regex("maaş|gelir|kira geliri", RegexOption.IGNORE_CASE)) -> INCOME
                else -> OTHER
            }
        }
    }
}
