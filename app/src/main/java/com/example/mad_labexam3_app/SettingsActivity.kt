package com.example.mad_labexam3_app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.mad_labexam3_app.databinding.ActivitySettingsBinding
import com.example.mad_labexam3_app.models.Transaction
import com.example.mad_labexam3_app.utils.TransactionManager
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var transactionManager: TransactionManager
    private val PERMISSION_REQUEST_CODE = 123
    private val FILE_PICKER_REQUEST_CODE = 456
    private val currencies = arrayOf("LKR", "USD", "INR", "EUR", "AUD", "DNR")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        transactionManager = TransactionManager(this)
        setupViews()

        val btnBackup: Button = findViewById(R.id.button6)
        val btnRestore: Button = findViewById(R.id.button7)

        btnBackup.setOnClickListener {
            if (checkStoragePermission()) {
                performBackup()
            } else {
                requestStoragePermission()
            }
        }

        btnRestore.setOnClickListener {
            if (checkStoragePermission()) {
                openFilePicker()
            } else {
                requestStoragePermission()
            }
        }
    }

    private fun setupViews() {
        // Setup currency spinner
        val spinnerAdapter = ArrayAdapter(
            this,
            R.layout.spinner_item,
            currencies
        )
        spinnerAdapter.setDropDownViewResource(R.layout.spinner_item)
        binding.currencySpinner.adapter = spinnerAdapter

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
    }

    private fun checkStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ),
            PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                performBackup()
            } else {
                Toast.makeText(
                    this,
                    "Storage permission is required for backup",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun performBackup() {
        try {
            // Create backup data object
            val backupData = mapOf(
                "transactions" to transactionManager.getAllTransactions(),
                "currency" to transactionManager.getCurrency(),
                "budget" to transactionManager.getBudget(),
                "timestamp" to System.currentTimeMillis()
            )

            // Create Gson instance with pretty printing
            val gson = GsonBuilder().setPrettyPrinting().create()
            val jsonData = gson.toJson(backupData)

            // Get Documents directory
            val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            if (!documentsDir.exists()) {
                documentsDir.mkdirs()
            }

            // Create backup file with timestamp
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(Date())
            val backupFile = File(documentsDir, "expense_tracker_backup_$timestamp.json")

            // Write data to file
            FileWriter(backupFile).use { writer ->
                writer.write(jsonData)
            }

            Toast.makeText(
                this,
                "Backup saved to Documents: ${backupFile.name}",
                Toast.LENGTH_LONG
            ).show()

        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Backup failed: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
            e.printStackTrace()
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "application/json"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(intent, FILE_PICKER_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILE_PICKER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                try {
                    performRestore(uri)
                } catch (e: Exception) {
                    Toast.makeText(
                        this,
                        "Failed to restore: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    e.printStackTrace()
                }
            }
        }
    }

    private fun performRestore(uri: Uri) {
        try {
            // Read the JSON content from the selected file
            val inputStream = contentResolver.openInputStream(uri)
            val jsonContent = inputStream?.bufferedReader().use { it?.readText() }
            
            if (jsonContent == null) {
                Toast.makeText(this, "Error: Could not read file", Toast.LENGTH_SHORT).show()
                return
            }

            // Parse the JSON content
            val gson = GsonBuilder().create()
            val type = object : TypeToken<Map<String, Any>>() {}.type
            val backupData = gson.fromJson<Map<String, Any>>(jsonContent, type)

            // Restore transactions
            val transactionsJson = gson.toJson(backupData["transactions"])
            val transactionsType = object : TypeToken<List<Transaction>>() {}.type
            val restoredTransactions: List<Transaction> = gson.fromJson(transactionsJson, transactionsType)
            
            // Restore currency
            val currency = backupData["currency"] as? String
            
            // Restore budget
            val budget = (backupData["budget"] as? Double) ?: 0.0

            // Apply the restored data
            if (currency != null) {
                transactionManager.setCurrency(currency)
            }
            transactionManager.setBudget(budget)
            
            // Get existing transactions
            val existingTransactions = transactionManager.getAllTransactions()
            val existingIds = existingTransactions.map { it.id }
            
            // Filter out transactions that already exist
            val newTransactions = restoredTransactions.filter { transaction ->
                !existingIds.contains(transaction.id)
            }
            
            // Add only new transactions
            var restoredCount = 0
            newTransactions.forEach { transaction ->
                transactionManager.saveTransaction(transaction)
                restoredCount++
            }

            Toast.makeText(
                this,
                "Restored $restoredCount new transactions successfully",
                Toast.LENGTH_SHORT
            ).show()

            // Refresh the activity to show restored data
            recreate()

        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Restore failed: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
            e.printStackTrace()
        }
    }
} 