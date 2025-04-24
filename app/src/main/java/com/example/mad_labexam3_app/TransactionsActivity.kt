package com.example.mad_labexam3_app

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mad_labexam3_app.databinding.ActivityTransactionsBinding
import com.example.mad_labexam3_app.databinding.ItemTransactionBinding
import com.example.mad_labexam3_app.models.ExpenseCategory
import com.example.mad_labexam3_app.models.Transaction
import com.example.mad_labexam3_app.models.TransactionType
import com.example.mad_labexam3_app.utils.TransactionManager
import java.text.SimpleDateFormat
import java.util.*

class TransactionsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTransactionsBinding
    private lateinit var transactionManager: TransactionManager
    private lateinit var adapter: TransactionAdapter
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        transactionManager = TransactionManager(this)
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        adapter = TransactionAdapter(
            transactions = transactionManager.getAllTransactions().sortedByDescending { it.date },
            currency = transactionManager.getCurrency(),
            onEditClick = { transaction -> showEditDialog(transaction) },
            onDeleteClick = { transaction ->
                AlertDialog.Builder(this)
                    .setTitle("Delete Transaction")
                    .setMessage("Are you sure you want to delete this transaction?")
                    .setPositiveButton("Delete") { _, _ ->
                        transactionManager.deleteTransaction(transaction.id)
                        updateTransactions()
                        Toast.makeText(this, "Transaction deleted", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )

        binding.transactionsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@TransactionsActivity)
            adapter = this@TransactionsActivity.adapter
        }
    }

    private fun showEditDialog(transaction: Transaction) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_transaction, null)
        val titleEdit = dialogView.findViewById<EditText>(R.id.titleEdit)
        val amountEdit = dialogView.findViewById<EditText>(R.id.amountEdit)
        val dateEdit = dialogView.findViewById<EditText>(R.id.dateEdit)
        val categorySpinner = dialogView.findViewById<Spinner>(R.id.categorySpinner)

        titleEdit.setText(transaction.title)
        amountEdit.setText(transaction.amount.toString())
        dateEdit.setText(dateFormat.format(transaction.date))

        var selectedDate = transaction.date

        if (transaction.type == TransactionType.EXPENSE) {
            categorySpinner.adapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                ExpenseCategory.values()
            )
            categorySpinner.setSelection(transaction.category?.ordinal ?: 0)
        } else {
            categorySpinner.visibility = android.view.View.GONE
        }

        dateEdit.setOnClickListener {
            val calendar = Calendar.getInstance()
            calendar.time = selectedDate
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    calendar.set(year, month, day)
                    if (calendar.time.after(Date())) {
                        Toast.makeText(this, "Future dates are not allowed", Toast.LENGTH_SHORT).show()
                        return@DatePickerDialog
                    }
                    selectedDate = calendar.time
                    dateEdit.setText(dateFormat.format(selectedDate))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Edit Transaction")
            .setView(dialogView)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                if (titleEdit.text.toString().trim().isEmpty()) {
                    titleEdit.error = "Title is required"
                    return@setOnClickListener
                }

                val amountStr = amountEdit.text.toString()
                val amount = amountStr.toDoubleOrNull()
                when {
                    amountStr.isEmpty() -> {
                        amountEdit.error = "Amount is required"
                        return@setOnClickListener
                    }
                    amount == null -> {
                        amountEdit.error = "Invalid amount format"
                        return@setOnClickListener
                    }
                    amount <= 0 -> {
                        amountEdit.error = "Amount must be greater than 0"
                        return@setOnClickListener
                    }
                }

                val updatedTransaction = transaction.copy(
                    title = titleEdit.text.toString().trim(),
                    amount = amount,
                    date = selectedDate,
                    category = if (transaction.type == TransactionType.EXPENSE)
                        ExpenseCategory.values()[categorySpinner.selectedItemPosition]
                    else null
                )

                transactionManager.updateTransaction(updatedTransaction)
                updateTransactions()
                Toast.makeText(this, "Transaction updated", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun updateTransactions() {
        adapter.updateTransactions(
            transactionManager.getAllTransactions().sortedByDescending { it.date }
        )
    }
}

class TransactionAdapter(
    private var transactions: List<Transaction>,
    private val currency: String,
    private val onEditClick: (Transaction) -> Unit,
    private val onDeleteClick: (Transaction) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    class ViewHolder(val binding: ItemTransactionBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTransactionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val transaction = transactions[position]
        holder.binding.apply {
            titleText.text = transaction.title
            dateText.text = dateFormat.format(transaction.date)
            amountText.apply {
                text = "$currency %.2f".format(transaction.amount)
                setTextColor(
                    root.context.getColor(
                        if (transaction.type == TransactionType.INCOME)
                            android.R.color.holo_green_dark
                        else
                            android.R.color.holo_red_dark
                    )
                )
            }
            categoryText.apply {
                text = transaction.category?.name ?: transaction.type.name
                setTextColor(
                    root.context.getColor(
                        if (transaction.type == TransactionType.INCOME)
                            android.R.color.holo_green_dark
                        else
                            android.R.color.holo_red_dark
                    )
                )
            }

            editButton.setOnClickListener { onEditClick(transaction) }
            deleteButton.setOnClickListener { onDeleteClick(transaction) }
        }
    }

    override fun getItemCount() = transactions.size

    fun updateTransactions(newTransactions: List<Transaction>) {
        transactions = newTransactions
        notifyDataSetChanged()
    }
} 