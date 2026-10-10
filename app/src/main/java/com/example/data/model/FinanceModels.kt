package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransactionType(val displayName: String) {
    INCOME("Income"),
    EXPENSE("Expense")
}

val ALL_CATEGORIES = listOf(
    "Food",
    "Shopping",
    "Travel",
    "Bills",
    "Education",
    "Entertainment",
    "Health",
    "Salary",
    "Freelance",
    "Other"
)

val DEFAULT_PAYMENT_METHODS = listOf(
    "Cash",
    "Credit Card",
    "Debit Card",
    "Bank Transfer",
    "Digital Wallet",
    "Other"
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String, // "INCOME" or "EXPENSE"
    val amount: Double,
    val category: String,
    val dateMillis: Long,
    val description: String,
    val paymentMethod: String
)

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey
    val category: String, // e.g. "TOTAL" for monthly total or category name like "Food"
    val monthlyLimit: Double
)

enum class CurrencyOption(val symbol: String, val label: String) {
    USD("$", "USD ($)"),
    EUR("€", "EUR (€)"),
    GBP("£", "GBP (£)"),
    INR("₹", "INR (₹)"),
    JPY("¥", "JPY (¥)"),
    CAD("C$", "CAD (C$)"),
    AUD("A$", "AUD (A$)")
}

enum class AppThemeMode(val displayName: String) {
    SYSTEM("System Default"),
    LIGHT("Light Mode"),
    DARK("Dark Mode")
}
