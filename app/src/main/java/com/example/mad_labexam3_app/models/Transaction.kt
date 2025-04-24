package com.example.mad_labexam3_app.models

import java.util.Date

enum class TransactionType {
    EXPENSE,
    INCOME
}

enum class ExpenseCategory {
    BILL,
    EDUCATION,
    TRANSPORTATION,
    FOOD
}

data class Transaction(
    var id: Long = System.currentTimeMillis(),
    var title: String,
    var amount: Double,
    var date: Date,
    var type: TransactionType,
    var category: ExpenseCategory? = null
) 