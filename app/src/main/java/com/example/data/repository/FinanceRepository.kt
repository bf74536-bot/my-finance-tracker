package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.FinanceDao
import com.example.data.model.AppThemeMode
import com.example.data.model.BudgetEntity
import com.example.data.model.CurrencyOption
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FinanceRepository(
    private val dao: FinanceDao,
    context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("finance_prefs", Context.MODE_PRIVATE)

    private val _currency = MutableStateFlow(
        prefs.getString("currency_code", CurrencyOption.USD.name)?.let {
            try { CurrencyOption.valueOf(it) } catch (e: Exception) { CurrencyOption.USD }
        } ?: CurrencyOption.USD
    )
    val currency: StateFlow<CurrencyOption> = _currency.asStateFlow()

    private val _themeMode = MutableStateFlow(
        prefs.getString("theme_mode", AppThemeMode.SYSTEM.name)?.let {
            try { AppThemeMode.valueOf(it) } catch (e: Exception) { AppThemeMode.SYSTEM }
        } ?: AppThemeMode.SYSTEM
    )
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    fun setCurrency(option: CurrencyOption) {
        prefs.edit().putString("currency_code", option.name).apply()
        _currency.value = option
    }

    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit().putString("theme_mode", mode.name).apply()
        _themeMode.value = mode
    }

    fun hasCleanedInitialData(): Boolean {
        return prefs.getBoolean("fake_data_cleared_v2", false)
    }

    fun markInitialDataCleaned() {
        prefs.edit().putBoolean("fake_data_cleared_v2", true).apply()
    }

    val transactions: Flow<List<TransactionEntity>> = dao.getAllTransactions()
    val budgets: Flow<List<BudgetEntity>> = dao.getAllBudgets()

    suspend fun addTransaction(tx: TransactionEntity): Long = dao.insertTransaction(tx)

    suspend fun updateTransaction(tx: TransactionEntity) = dao.updateTransaction(tx)

    suspend fun deleteTransaction(tx: TransactionEntity) = dao.deleteTransaction(tx)

    suspend fun deleteTransactionById(id: Long) = dao.deleteTransactionById(id)

    suspend fun setBudget(category: String, amount: Double) {
        dao.insertOrUpdateBudget(BudgetEntity(category = category, monthlyLimit = amount))
    }

    suspend fun deleteBudget(category: String) {
        dao.deleteBudgetByCategory(category)
    }

    suspend fun clearAllData() {
        dao.deleteAllTransactions()
        dao.deleteAllBudgets()
    }

    fun exportTransactionsToCsv(transactionsList: List<TransactionEntity>): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sb = StringBuilder()
        sb.append("ID,Type,Amount,Category,Date,Description,PaymentMethod\n")
        transactionsList.forEach { tx ->
            val dateStr = dateFormat.format(Date(tx.dateMillis))
            val cleanDesc = tx.description.replace("\"", "\"\"")
            sb.append("${tx.id},${tx.type},${tx.amount},\"${tx.category}\",\"$dateStr\",\"$cleanDesc\",\"${tx.paymentMethod}\"\n")
        }
        return sb.toString()
    }

    suspend fun importTransactionsFromCsv(csvContent: String): Int {
        val lines = csvContent.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return 0

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        var importedCount = 0
        val toInsert = mutableListOf<TransactionEntity>()

        // Check if first line is header
        val startIndex = if (lines[0].contains("Amount", ignoreCase = true) || lines[0].contains("Type", ignoreCase = true)) 1 else 0

        for (i in startIndex until lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty()) continue

            // Parse CSV row respecting possible quotes
            val tokens = parseCsvLine(line)
            if (tokens.size >= 5) {
                try {
                    // Try matching columns: either ID,Type,Amount... or Type,Amount,Category,Date,Desc,PaymentMethod
                    val row = if (tokens.size >= 7) {
                        listOf(tokens[1], tokens[2], tokens[3], tokens[4], tokens[5], tokens[6])
                    } else if (tokens.size == 6) {
                        listOf(tokens[0], tokens[1], tokens[2], tokens[3], tokens[4], tokens[5])
                    } else {
                        listOf(tokens[0], tokens[1], tokens[2], tokens[3], tokens[4], "Cash")
                    }
                    val typeStr = row[0]
                    val amountStr = row[1]
                    val category = row[2]
                    val dateStr = row[3]
                    val desc = row[4]
                    val payMethod = row[5]

                    val type = if (typeStr.trim().equals("Income", ignoreCase = true)) "INCOME" else "EXPENSE"
                    val amount = amountStr.trim().replace("$", "").replace("€", "").replace("£", "").toDoubleOrNull() ?: 0.0
                    val dateMillis = try {
                        dateFormat.parse(dateStr.trim())?.time ?: System.currentTimeMillis()
                    } catch (e: Exception) {
                        dateStr.trim().toLongOrNull() ?: System.currentTimeMillis()
                    }

                    if (amount > 0.0) {
                        toInsert.add(
                            TransactionEntity(
                                id = 0,
                                type = type,
                                amount = amount,
                                category = category.trim().ifEmpty { "Other" },
                                dateMillis = dateMillis,
                                description = desc.trim(),
                                paymentMethod = payMethod.trim().ifEmpty { "Cash" }
                            )
                        )
                        importedCount++
                    }
                } catch (e: Exception) {
                    // skip corrupted row
                }
            }
        }

        if (toInsert.isNotEmpty()) {
            dao.insertTransactions(toInsert)
        }
        return importedCount
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false

        for (ch in line) {
            when {
                ch == '\"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> {
                    result.add(sb.toString())
                    sb.clear()
                }
                else -> sb.append(ch)
            }
        }
        result.add(sb.toString())
        return result.map { it.trim().trim('\"') }
    }

    suspend fun populateSampleData() {
        val now = System.currentTimeMillis()
        val day = 86_400_000L

        val sampleTransactions = listOf(
            TransactionEntity(0, "INCOME", 4500.0, "Salary", now - (15 * day), "Monthly Corporate Salary", "Bank Transfer"),
            TransactionEntity(0, "INCOME", 850.0, "Freelance", now - (5 * day), "UI/UX App Design project", "Bank Transfer"),
            TransactionEntity(0, "EXPENSE", 120.50, "Food", now - (1 * day), "Weekly groceries at supermarket", "Credit Card"),
            TransactionEntity(0, "EXPENSE", 45.0, "Food", now - (2 * day), "Dinner at Italian Bistro", "Debit Card"),
            TransactionEntity(0, "EXPENSE", 85.0, "Bills", now - (3 * day), "Fiber Internet & Utilities", "Bank Transfer"),
            TransactionEntity(0, "EXPENSE", 150.0, "Shopping", now - (4 * day), "New running shoes & fitness wear", "Credit Card"),
            TransactionEntity(0, "EXPENSE", 65.0, "Travel", now - (6 * day), "Gas station fuel refill", "Debit Card"),
            TransactionEntity(0, "EXPENSE", 30.0, "Entertainment", now - (7 * day), "Cinema tickets & popcorn", "Digital Wallet"),
            TransactionEntity(0, "EXPENSE", 40.0, "Health", now - (10 * day), "Vitamins & pharmacy prescription", "Cash"),
            TransactionEntity(0, "EXPENSE", 200.0, "Education", now - (12 * day), "Mobile Development Masterclass", "Credit Card")
        )
        dao.insertTransactions(sampleTransactions)

        // Sample Budgets
        dao.insertOrUpdateBudget(BudgetEntity("TOTAL", 2500.0))
        dao.insertOrUpdateBudget(BudgetEntity("Food", 600.0))
        dao.insertOrUpdateBudget(BudgetEntity("Shopping", 400.0))
        dao.insertOrUpdateBudget(BudgetEntity("Travel", 250.0))
        dao.insertOrUpdateBudget(BudgetEntity("Bills", 350.0))
        dao.insertOrUpdateBudget(BudgetEntity("Entertainment", 150.0))
        dao.insertOrUpdateBudget(BudgetEntity("Health", 200.0))
        dao.insertOrUpdateBudget(BudgetEntity("Education", 300.0))
    }
}
