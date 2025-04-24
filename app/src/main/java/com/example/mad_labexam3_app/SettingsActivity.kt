package com.example.mad_labexam3_app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.mad_labexam3_app.databinding.ActivitySettingsBinding
import com.example.mad_labexam3_app.utils.TransactionManager
import com.example.mad_labexam3_app.models.Transaction
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var transactionManager: TransactionManager

    private val currencies = arrayOf("LKR", "USD", "INR", "EUR", "AUD", "DNR")
    private val PICK_JSON_FILE = 2

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

        // Setup restore button
        binding.button7.setOnClickListener {
            showBackupFilesList()
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

    private fun showBackupFilesList() {
        val documentsDir = File(getExternalFilesDir(null), "Documents")
        val backupFiles = documentsDir.listFiles { file -> 
            file.name.endsWith(".json") && file.name.startsWith("expense_tracker_backup_")
        }?.sortedByDescending { it.lastModified() }

        if (backupFiles.isNullOrEmpty()) {
            Toast.makeText(this, "No backup files found", Toast.LENGTH_SHORT).show()
            return
        }

        // Create list of backup file names with dates
        val fileItems = backupFiles.map { file ->
            val date = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                .format(Date(file.lastModified()))
            "Backup from $date"
        }.toTypedArray()

        // Show dialog with backup files
        AlertDialog.Builder(this)
            .setTitle("Select Backup to Restore")
            .setItems(fileItems) { _, position ->
                restoreFromFile(backupFiles[position])
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun restoreFromFile(file: File) {
        try {
            val jsonString = file.readText()
            val jsonObject = Gson().fromJson(jsonString, JsonObject::class.java)
            
            // Restore currency
            val currency = jsonObject.get("currency")?.asString
            if (currency != null) {
                transactionManager.setCurrency(currency)
            }
            
            // Restore budget
            val budget = jsonObject.get("budget")?.asDouble
            if (budget != null) {
                transactionManager.setBudget(budget)
            }
            
            // Restore transactions
            val transactionsJson = jsonObject.get("transactions")?.asJsonArray
            if (transactionsJson != null) {
                val type = object : TypeToken<List<Transaction>>() {}.type
                val transactions: List<Transaction> = Gson().fromJson(transactionsJson, type)
                
                // Clear existing transactions and save restored ones
                transactions.forEach { transaction ->
                    transactionManager.saveTransaction(transaction)
                }
            }
            
            // Update UI
            val restoredCurrency = transactionManager.getCurrency()
            binding.currencySpinner.setSelection(currencies.indexOf(restoredCurrency))
            binding.budgetEdit.setText(transactionManager.getBudget().toString())
            
            Toast.makeText(this, "Data restored successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Restore failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
} 