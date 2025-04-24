package com.example.mad_labexam3_app

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mad_labexam3_app.databinding.ActivitySettingsBinding
import com.example.mad_labexam3_app.utils.TransactionManager
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var transactionManager: TransactionManager

    private val currencies = arrayOf("LKR", "USD", "INR", "EUR", "AUD", "DNR")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        transactionManager = TransactionManager(this)
        setupViews()
    }

    private fun setupViews() {
        // Setup currency spinner
        binding.currencySpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            currencies
        )

        // Set current values
        val currentCurrency = transactionManager.getCurrency()
        binding.currencySpinner.setSelection(currencies.indexOf(currentCurrency))
        binding.budgetEdit.setText(transactionManager.getBudget().toString())

        binding.saveButton.setOnClickListener {
            // Save currency
            val selectedCurrency = currencies[binding.currencySpinner.selectedItemPosition]
            transactionManager.setCurrency(selectedCurrency)

            // Save budget
            val budgetAmount = binding.budgetEdit.text.toString().toDoubleOrNull() ?: 0.0
            transactionManager.setBudget(budgetAmount)

            Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
            finish()
        }

        // Setup backup button
        binding.button6.setOnClickListener {
            performBackup()
        }
    }

    private fun performBackup() {
        try {
            // Create backup data
            val backupData = JsonObject().apply {
                addProperty("currency", transactionManager.getCurrency())
                addProperty("budget", transactionManager.getBudget())
                add("transactions", Gson().toJsonTree(transactionManager.getAllTransactions()))
            }

            // Create documents directory if it doesn't exist
            val documentsDir = File(getExternalFilesDir(null), "Documents")
            if (!documentsDir.exists()) {
                documentsDir.mkdirs()
            }

            // Create backup file with timestamp
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val backupFile = File(documentsDir, "expense_tracker_backup_$timestamp.json")
            
            // Write data to file
            backupFile.writeText(backupData.toString())
            
            Toast.makeText(this, "Backup saved to: ${backupFile.path}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Backup failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
} 