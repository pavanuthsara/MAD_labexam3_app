package com.example.mad_labexam3_app

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mad_labexam3_app.databinding.ActivitySettingsBinding
import com.example.mad_labexam3_app.utils.TransactionManager
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var transactionManager: TransactionManager

    private val currencies = arrayOf("LKR", "USD", "INR", "EUR", "AUD", "DNR")
    private val BACKUP_FILE_NAME = "expense_tracker_backup.json"

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
        binding.backupButton.setOnClickListener {
            if (backupData()) {
                Toast.makeText(this, "Backup created successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to create backup", Toast.LENGTH_SHORT).show()
            }
        }

        // Setup restore button
        binding.restoreButton.setOnClickListener {
            if (restoreData()) {
                Toast.makeText(this, "Data restored successfully", Toast.LENGTH_SHORT).show()
                // Refresh the UI with restored data
                val restoredCurrency = transactionManager.getCurrency()
                binding.currencySpinner.setSelection(currencies.indexOf(restoredCurrency))
                binding.budgetEdit.setText(transactionManager.getBudget().toString())
            } else {
                Toast.makeText(this, "Failed to restore data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun backupData(): Boolean {
        try {
            val sharedPrefs = transactionManager.getSharedPreferences()
            val backupJson = JSONObject()

            // Backup currency
            backupJson.put("currency", sharedPrefs.getString("currency", "LKR"))
            
            // Backup budget
            backupJson.put("budget", sharedPrefs.getFloat("budget", 0f))
            
            // Backup transactions
            backupJson.put("transactions", transactionManager.getAllTransactionsJson())

            // Write to internal storage
            val file = File(filesDir, BACKUP_FILE_NAME)
            FileOutputStream(file).use { fos ->
                fos.write(backupJson.toString().toByteArray())
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun restoreData(): Boolean {
        try {
            val file = File(filesDir, BACKUP_FILE_NAME)
            if (!file.exists()) {
                return false
            }

            val jsonString = FileInputStream(file).bufferedReader().use { it.readText() }
            val backupJson = JSONObject(jsonString)

            // Get SharedPreferences editor
            val editor = transactionManager.getSharedPreferences().edit()

            // Restore currency
            editor.putString("currency", backupJson.getString("currency"))
            
            // Restore budget
            editor.putFloat("budget", backupJson.getDouble("budget").toFloat())
            
            // Commit changes
            editor.apply()

            // Restore transactions
            transactionManager.restoreTransactionsFromJson(backupJson.getJSONArray("transactions"))

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
} 