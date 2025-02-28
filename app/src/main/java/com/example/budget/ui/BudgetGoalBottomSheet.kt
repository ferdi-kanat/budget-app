package com.example.budget.ui

import com.example.budget.data.entity.BudgetGoalEntity
import android.icu.util.Calendar
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.activityViewModels
import com.example.budget.R
import com.example.budget.TransactionCategory
import com.example.budget.databinding.BottomSheetBudgetGoalBinding
import com.example.budget.utils.CurrencyTextWatcher
import com.example.budget.viewmodel.BudgetViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class BudgetGoalBottomSheet : BottomSheetDialogFragment() {
    private var _binding: BottomSheetBudgetGoalBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BudgetViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetBudgetGoalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCategoryDropdown()
        setupAmountInput()
        setupSaveButton()
    }

    private fun setupCategoryDropdown() {
        // TransactionCategory enum'undan kategori listesi oluştur
        val categories = TransactionCategory.values().map { it.displayName }
        val adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, categories)
        binding.categoryDropdown.setAdapter(adapter)
    }

    private fun setupAmountInput() {
        binding.amountInput.addTextChangedListener(CurrencyTextWatcher())
    }

    private fun setupSaveButton() {
        binding.saveButton.setOnClickListener {
            val categoryName = binding.categoryDropdown.text.toString()
            val amount = binding.amountInput.text.toString().toDoubleOrNull() ?: return@setOnClickListener

            viewModel.saveBudgetGoal(
                BudgetGoalEntity(
                    category = categoryName,
                    monthYear = getCurrentMonthYear(),
                    targetAmount = amount
                )
            )
            dismiss()
        }
    }

    private fun getCurrentMonthYear(): String {
        val calendar = Calendar.getInstance()
        return "${calendar.get(Calendar.YEAR)}-${String.format("%02d", calendar.get(Calendar.MONTH) + 1)}"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "BudgetGoalBottomSheet"
    }
}