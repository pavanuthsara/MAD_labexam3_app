package com.example.mad_labexam3_app.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.mad_labexam3_app.models.ExpenseCategory
import com.example.mad_labexam3_app.models.Transaction
import com.example.mad_labexam3_app.models.TransactionType
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class TransactionManager(private val context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        "expense_tracker_prefs",
        Context.MODE_PRIVATE
    )

    fun getSharedPreferences(): SharedPreferences {
        return sharedPreferences
    }

    fun saveTransaction(transaction: Transaction) {
        val transactions = getAllTransactions().toMutableList()
        transactions.add(transaction)
        saveTransactions(transactions)
    }

    fun deleteTransaction(transactionId: Long) {
        val transactions = getAllTransactions().toMutableList()
        transactions.removeAll { it.id == transactionId }
        saveTransactions(transactions)
    }

    fun updateTransaction(updatedTransaction: Transaction) {
        val transactions = getAllTransactions().toMutableList()
        val index = transactions.indexOfFirst { it.id == updatedTransaction.id }
        if (index != -1) {
            transactions[index] = updatedTransaction
            saveTransactions(transactions)
        }
    }

    fun getAllTransactions(): List<Transaction> {
        val transactionsJson = sharedPreferences.getString("transactions", "[]")
        return parseTransactionsFromJson(JSONArray(transactionsJson))
    }

    fun getAllTransactionsJson(): JSONArray {
        val transactions = getAllTransactions()
        val jsonArray = JSONArray()
        
        for (transaction in transactions) {
            val jsonObject = JSONObject().apply {
                put("title", transaction.title)
                put("amount", transaction.amount)
                put("date", transaction.date.time)
                put("type", transaction.type.name)
                put("category", transaction.category?.name)
            }
            jsonArray.put(jsonObject)
        }
        
        return jsonArray
    }

    fun restoreTransactionsFromJson(jsonArray: JSONArray) {
        val transactions = parseTransactionsFromJson(jsonArray)
        saveTransactions(transactions)
    }

    private fun parseTransactionsFromJson(jsonArray: JSONArray): List<Transaction> {
        val transactions = mutableListOf<Transaction>()
        
        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            val transaction = Transaction(
                title = jsonObject.getString("title"),
                amount = jsonObject.getDouble("amount"),
                date = Date(jsonObject.getLong("date")),
                type = TransactionType.valueOf(jsonObject.getString("type")),
                category = if (!jsonObject.isNull("category")) 
                    ExpenseCategory.valueOf(jsonObject.getString("category")) 
                else null
            )
            transactions.add(transaction)
        }
        
        return transactions
    }

    private fun saveTransactions(transactions: List<Transaction>) {
        val jsonArray = JSONArray()
        transactions.forEach { transaction ->
            val jsonObject = JSONObject().apply {
                put("title", transaction.title)
                put("amount", transaction.amount)
                put("date", transaction.date.time)
                put("type", transaction.type.name)
                put("category", transaction.category?.name)
            }
            jsonArray.put(jsonObject)
        }
        
        sharedPreferences.edit()
            .putString("transactions", jsonArray.toString())
            .apply()
    }

    fun getCurrency(): String {
        return sharedPreferences.getString("currency", "LKR") ?: "LKR"
    }

    fun setCurrency(currency: String) {
        sharedPreferences.edit()
            .putString("currency", currency)
            .apply()
    }

    fun getBudget(): Double {
        return sharedPreferences.getFloat("budget", 0f).toDouble()
    }

    fun setBudget(amount: Double) {
        sharedPreferences.edit()
            .putFloat("budget", amount.toFloat())
            .apply()
    }
} 