package com.example.gestaobilhares.ui.expenses.model

data class ExpenseType(
    val id: Long,
    val name: String,
    val categoryId: Long,
    val categoryName: String = ""
)
