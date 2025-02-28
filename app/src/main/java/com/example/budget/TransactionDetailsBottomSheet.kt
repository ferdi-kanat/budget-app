package com.example.budget

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import android.graphics.Color
import android.os.Parcelable

class TransactionDetailsBottomSheet : BottomSheetDialogFragment() {

    companion object {
        private const val ARG_TRANSACTION = "transaction"

        fun newInstance(transaction: TransactionEntity): TransactionDetailsBottomSheet {
            return TransactionDetailsBottomSheet().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_TRANSACTION, transaction as Parcelable)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_transaction_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Use the new getParcelable API for Android 13+ compatibility
        val transaction = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable(ARG_TRANSACTION, TransactionEntity::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelable(ARG_TRANSACTION) as? TransactionEntity
        }
        
        transaction?.let { displayTransactionDetails(it, view) }
    }

    private fun displayTransactionDetails(transaction: TransactionEntity, view: View) {
        view.findViewById<TextView>(R.id.textViewTransactionDate).text = transaction.date
        view.findViewById<TextView>(R.id.textViewTransactionDescription).text = transaction.description

        view.findViewById<TextView>(R.id.textViewTransactionAmount).apply {
            text = getString(R.string.amount_format, transaction.amount)
            setTextColor(if (transaction.amount >= 0)
                Color.parseColor("#4CAF50") else Color.parseColor("#F44336"))
        }

        view.findViewById<TextView>(R.id.textViewTransactionBank).text = transaction.bankName
        view.findViewById<TextView>(R.id.textViewTransactionId).text =
            getString(R.string.transaction_id_format, transaction.transactionId)

        // String olarak saklanan kategori adını kullan
        val categoryName = transaction.category
        // displayName olmadan direkt category string'i kullanılır
        view.findViewById<Chip>(R.id.chipCategory).apply {
            text = categoryName
            contentDescription = getString(
                R.string.transaction_category_with_name,
                categoryName
            )
        }
    }
} 