package com.example.mad_labexam3_app

import android.R
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mad_labexam3_app.databinding.ActivitySettingsBinding
import com.example.mad_labexam3_app.utils.TransactionManager
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

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
            R.layout.simple_spinner_dropdown_item,
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
                Toast.makeText(this, "Backup failed", Toast.LENGTH_SHORT).show()
            }
        }
        // Setup restore button
//        binding.restoreButton.setOnClickListener {
//            if (restoreData()) {
//                Toast.makeText(this, "Data restored successfully", Toast.LENGTH_SHORT).show()
//                // Update UI
//                val restoredCurrency = transactionManager.getCurrency()
//                binding.currencySpinner.setSelection(currencies.indexOf(restoredCurrency))
//                binding.budgetEdit.setText(transactionManager.getBudget().toString())
//            } else {
//                Toast.makeText(this, "Restore failed", Toast.LENGTH_SHORT).show()
//            }
//        }
    }

    // Backs up preferences and transactions to a JSON file in internal storage
    private fun backupData(): Boolean {
        return try {
            val prefs = transactionManager.getSharedPreferences()
            val backupJson = JSONObject().apply {
                put("currency", prefs.getString("currency", "LKR"))
                put("budget", prefs.getFloat("budget", 0f))
                put("transactions", JSONArray(prefs.getString("transactions", "[]")))
            }
            val file = File(filesDir, "expense_tracker_backup.json")
            FileOutputStream(file).use { it.write(backupJson.toString().toByteArray()) }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Restores preferences and transactions from the backup JSON file
    private fun restoreData(): Boolean {
        return try {
            val file = File(filesDir, "expense_tracker_backup.json")
            if (!file.exists()) return false
            val jsonString = FileInputStream(file).bufferedReader().use { it.readText() }
            val backupJson = JSONObject(jsonString)
            val prefs = transactionManager.getSharedPreferences().edit().apply {
                putString("currency", backupJson.getString("currency"))
                putFloat("budget", backupJson.getDouble("budget").toFloat())
                putString("transactions", backupJson.getJSONArray("transactions").toString())
            }
            prefs.apply()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
} 