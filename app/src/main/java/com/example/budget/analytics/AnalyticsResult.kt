package com.example.budget.analytics

import android.os.Parcel
import android.os.Parcelable
import java.io.Serializable

data class AnalyticsResult(
    val totalIncome: Double,
    val totalExpense: Double,
    val balance: Double,
    val monthlyBreakdown: Map<String, Double>,
    val categoryBreakdown: Map<String, Double>,
    val bankBreakdown: Map<String, Double>
) : Parcelable, Serializable {
    
    constructor(parcel: Parcel) : this(
        totalIncome = parcel.readDouble(),
        totalExpense = parcel.readDouble(),
        balance = parcel.readDouble(),
        monthlyBreakdown = readStringDoubleMap(parcel),
        categoryBreakdown = readStringDoubleMap(parcel),
        bankBreakdown = readStringDoubleMap(parcel)
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeDouble(totalIncome)
        parcel.writeDouble(totalExpense)
        parcel.writeDouble(balance)
        writeStringDoubleMap(parcel, monthlyBreakdown)
        writeStringDoubleMap(parcel, categoryBreakdown)
        writeStringDoubleMap(parcel, bankBreakdown)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<AnalyticsResult> {
        override fun createFromParcel(parcel: Parcel): AnalyticsResult {
            return AnalyticsResult(parcel)
        }

        override fun newArray(size: Int): Array<AnalyticsResult?> {
            return arrayOfNulls(size)
        }

        private fun readStringDoubleMap(parcel: Parcel): Map<String, Double> {
            val size = parcel.readInt()
            val map = mutableMapOf<String, Double>()
            repeat(size) {
                val key = parcel.readString() ?: ""
                val value = parcel.readDouble()
                map[key] = value
            }
            return map
        }

        private fun writeStringDoubleMap(parcel: Parcel, map: Map<String, Double>) {
            parcel.writeInt(map.size)
            map.forEach { (key, value) ->
                parcel.writeString(key)
                parcel.writeDouble(value)
            }
        }
    }
} 