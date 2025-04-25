package com.example.mad_labexam3_app

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.mad_labexam3_app.databinding.ActivityMainBinding
import com.example.mad_labexam3_app.models.ExpenseCategory
import com.example.mad_labexam3_app.models.Transaction
import com.example.mad_labexam3_app.models.TransactionType
import com.example.mad_labexam3_app.utils.TransactionManager
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var transactionManager: TransactionManager
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        transactionManager = TransactionManager(this)
        createNotificationChannel()
        setupClickListeners()
        setupPieChart()
        updateSummary()
    }

    private fun setupPieChart() {
        binding.pieChart.apply {
            description.isEnabled = false
            isDrawHoleEnabled = true
            setHoleColor(Color.WHITE)
            setTransparentCircleColor(Color.WHITE)
            setTransparentCircleAlpha(110)
            holeRadius = 58f
            transparentCircleRadius = 61f
            setDrawCenterText(true)
            rotationAngle = 0f
            isRotationEnabled = true
            isHighlightPerTapEnabled = true
            setUsePercentValues(true)
            legend.isEnabled = true
        }
    }

    private fun updatePieChart() {
        val transactions = transactionManager.getAllTransactions()
        val expensesByCategory = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .mapValues { it.value.sumOf { transaction -> transaction.amount } }

        val entries = ArrayList<PieEntry>()
        val colors = ArrayList<Int>()

        expensesByCategory.forEach { (category, amount) ->
            if (category != null && amount > 0) {
                entries.add(PieEntry(amount.toFloat(), category.name))
                colors.add(getCategoryColor(category))
            }
        }

        if (entries.isEmpty()) {
            binding.pieChart.setNoDataText("No expense data available")
            binding.pieChart.invalidate()
            return
        }

        val dataSet = PieDataSet(entries, "Expenses by Category")
        dataSet.colors = colors
        dataSet.sliceSpace = 3f
        dataSet.selectionShift = 5f

        val data = PieData(dataSet)
        data.setValueFormatter(PercentFormatter(binding.pieChart))
        data.setValueTextSize(11f)
        data.setValueTextColor(Color.WHITE)

        binding.pieChart.data = data
        binding.pieChart.highlightValues(null)
        binding.pieChart.invalidate()
    }

    private fun getCategoryColor(category: ExpenseCategory): Int {
        return when (category) {
            ExpenseCategory.BILL -> Color.rgb(255, 123, 123)
            ExpenseCategory.EDUCATION -> Color.rgb(123, 123, 255)
            ExpenseCategory.TRANSPORTATION -> Color.rgb(123, 255, 123)
            ExpenseCategory.FOOD -> Color.rgb(255, 187, 123)
        }
    }

    private fun setupClickListeners() {
        binding.addIncomeButton.setOnClickListener { showAddTransactionDialog(TransactionType.INCOME) }
        binding.addExpenseButton.setOnClickListener { showAddTransactionDialog(TransactionType.EXPENSE) }
        binding.viewTransactionsButton.setOnClickListener {
            startActivity(Intent(this, TransactionsActivity::class.java))
        }
        binding.settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun showAddTransactionDialog(type: TransactionType) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_transaction, null)
        val titleEdit = dialogView.findViewById<EditText>(R.id.titleEdit)
        val amountEdit = dialogView.findViewById<EditText>(R.id.amountEdit)
        val dateEdit = dialogView.findViewById<EditText>(R.id.dateEdit)
        val categorySpinner = dialogView.findViewById<Spinner>(R.id.categorySpinner)

        if (type == TransactionType.EXPENSE) {
            categorySpinner.adapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                ExpenseCategory.values()
            )
        } else {
            categorySpinner.visibility = android.view.View.GONE
        }

        var selectedDate = Date()
        dateEdit.setText(dateFormat.format(selectedDate))
        dateEdit.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    calendar.set(year, month, day)
                    selectedDate = calendar.time
                    dateEdit.setText(dateFormat.format(selectedDate))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        AlertDialog.Builder(this)
            .setTitle(if (type == TransactionType.INCOME) "Add Income" else "Add Expense")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val title = titleEdit.text.toString()
                val amount = amountEdit.text.toString().toDoubleOrNull() ?: 0.0
                val category = if (type == TransactionType.EXPENSE)
                    ExpenseCategory.values()[categorySpinner.selectedItemPosition]
                else null

                val transaction = Transaction(
                    title = title,
                    amount = amount,
                    date = selectedDate,
                    type = type,
                    category = category
                )

                transactionManager.saveTransaction(transaction)
                updateSummary()
                checkBudget()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateSummary() {
        val transactions = transactionManager.getAllTransactions()
        val currency = transactionManager.getCurrency()
        
        val totalIncome = transactions
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amount }
        
        val totalExpenses = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }
        
        val balance = totalIncome - totalExpenses

        binding.balanceText.text = "Balance: $currency %.2f".format(balance)
        binding.incomeText.text = "Income: $currency %.2f".format(totalIncome)
        binding.expenseText.text = "Expenses: $currency %.2f".format(totalExpenses)

        updatePieChart()
        checkBudget()
    }

    private fun checkBudget() {
        val budget = transactionManager.getBudget()
        if (budget <= 0) return

        val totalExpenses = transactionManager.getAllTransactions()
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }

        if (totalExpenses > budget) {
            showBudgetWarningNotification()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "budget_warning",
                "Budget Warnings",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showBudgetWarningNotification() {
        // Compute dynamic content
        val budget = transactionManager.getBudget()
        val totalExpenses = transactionManager.getAllTransactions()
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }
        val currency = transactionManager.getCurrency()
        val contentText = "You have spent $currency %.2f, exceeding your budget of $currency %.2f".format(
            totalExpenses, budget
        )

        // Intent to open app
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else PendingIntent.FLAG_UPDATE_CURRENT
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, pendingFlags)

        // Build notification
        val builder = NotificationCompat.Builder(this, "budget_warning")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Budget Warning")
            .setContentText(contentText)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        // Show notification
        NotificationManagerCompat.from(this).notify(1, builder.build())
    }

    override fun onResume() {
        super.onResume()
        updateSummary()
    }
}