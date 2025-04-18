package com.example.budget.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.budget.R
import com.example.budget.data.AccountEntity
import java.text.NumberFormat
import java.util.Locale

class AccountAdapter : ListAdapter<AccountEntity, AccountAdapter.AccountViewHolder>(AccountDiffCallback()) {

    private var onItemClickListener: ((AccountEntity) -> Unit)? = null
    private var onItemLongClickListener: ((AccountEntity) -> Unit)? = null

    fun setOnItemClickListener(listener: (AccountEntity) -> Unit) {
        onItemClickListener = listener
    }

    fun setOnItemLongClickListener(listener: (AccountEntity) -> Unit) {
        onItemLongClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_account, parent, false)
        return AccountViewHolder(view)
    }

    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
        val account = getItem(position)
        holder.bind(account)
        
        holder.itemView.setOnClickListener {
            onItemClickListener?.invoke(account)
        }
        
        holder.itemView.setOnLongClickListener {
            onItemLongClickListener?.invoke(account)
            true
        }
    }

    class AccountViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageViewBank: ImageView = itemView.findViewById(R.id.imageViewBank)
        private val textViewAccountName: TextView = itemView.findViewById(R.id.textViewAccountName)
        private val textViewBankName: TextView = itemView.findViewById(R.id.textViewBankName)
        private val textViewAccountType: TextView = itemView.findViewById(R.id.textViewAccountType)
        private val textViewBalance: TextView = itemView.findViewById(R.id.textViewBalance)

        fun bind(account: AccountEntity) {
            textViewAccountName.text = account.accountName
            textViewBankName.text = account.bankName
            textViewAccountType.text = account.accountType
            
            val formatter = NumberFormat.getCurrencyInstance(Locale("tr", "TR"))
            textViewBalance.text = formatter.format(account.balance)
            
            // Set text color based on balance
            val context = itemView.context
            textViewBalance.setTextColor(
                if (account.balance >= 0) context.getColor(R.color.positive_amount)
                else context.getColor(R.color.negative_amount)
            )
        }
    }

    private class AccountDiffCallback : DiffUtil.ItemCallback<AccountEntity>() {
        override fun areItemsTheSame(oldItem: AccountEntity, newItem: AccountEntity): Boolean {
            return oldItem.accountId == newItem.accountId
        }

        override fun areContentsTheSame(oldItem: AccountEntity, newItem: AccountEntity): Boolean {
            return oldItem == newItem
        }
    }
} 