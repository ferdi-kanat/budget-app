package com.example.budget

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String, // Her iki bankada da var
    val time: String?, // Ziraat'te eksik, nullable
    val transactionId: String?, // Her iki bankada farklı sütun adıyla var
    val amount: Double, // Parasal veriler
    val balance: Double?, // Bakiye bazı durumlarda eksik olabilir
    val description: String, // İşlem açıklaması
    val bankName: String, // Banka adı (Ziraat veya VakıfBank)
    val category: String = "DİĞER"  // Yeni alan
)
