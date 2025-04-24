package com.example.mad_labexam3_app.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.mad_labexam3_app.models.Transaction
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class TransactionManager(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val PREF_NAME = "TransactionPrefs"
        private const val KEY_TRANSACTIONS = "transactions"
        private const val KEY_CURRENCY = "currency"
        private const val KEY_BUDGET = "budget"
        private const val DEFAULT_CURRENCY = "LKR"
    }

    fun saveTransaction(transaction: Transaction) {
        val transactions = getAllTransactions().toMutableList()
        transactions.add(transaction)
        saveAllTransactions(transactions)
    }

    fun updateTransaction(transaction: Transaction) {
        val transactions = getAllTransactions().toMutableList()
        val index = transactions.indexOfFirst { it.id == transaction.id }
        if (index != -1) {
            transactions[index] = transaction
            saveAllTransactions(transactions)
        }
    }

    fun deleteTransaction(transactionId: Long) {
        val transactions = getAllTransactions().toMutableList()
        transactions.removeAll { it.id == transactionId }
        saveAllTransactions(transactions)
    }

    fun getAllTransactions(): List<Transaction> {
        val json = sharedPreferences.getString(KEY_TRANSACTIONS, "[]")
        val type = object : TypeToken<List<Transaction>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    private fun saveAllTransactions(transactions: List<Transaction>) {
        val json = gson.toJson(transactions)
        sharedPreferences.edit().putString(KEY_TRANSACTIONS, json).apply()
    }

    fun setCurrency(currency: String) {
        sharedPreferences.edit().putString(KEY_CURRENCY, currency).apply()
    }

    fun getCurrency(): String {
        return sharedPreferences.getString(KEY_CURRENCY, DEFAULT_CURRENCY) ?: DEFAULT_CURRENCY
    }

    fun setBudget(amount: Double) {
        sharedPreferences.edit().putFloat(KEY_BUDGET, amount.toFloat()).apply()
    }

    fun getBudget(): Double {
        return sharedPreferences.getFloat(KEY_BUDGET, 0f).toDouble()
    }
} 