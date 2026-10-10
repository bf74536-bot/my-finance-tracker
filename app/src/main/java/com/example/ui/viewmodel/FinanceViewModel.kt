package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.ALL_CATEGORIES
import com.example.data.model.AppThemeMode
import com.example.data.model.BudgetEntity
import com.example.data.model.CurrencyOption
import com.example.data.model.TransactionEntity
import com.example.data.repository.FinanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class DateFilterOption(val label: String) {
    ALL("All Time"),
    THIS_MONTH("This Month"),
    LAST_30_DAYS("Last 30 Days"),
    THIS_YEAR("This Year")
}

data class MonthlyTrendItem(
    val monthLabel: String,
    val yearMonth: String,
    val income: Double,
    val expense: Double,
    val savings: Double
)

data class CategorySpendItem(
    val category: String,
    val amount: Double,
    val percentage: Float,
    val count: Int
)

data class FinanceOverview(
    val totalBalance: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val totalSavings: Double = 0.0,
    val currentMonthSpending: Double = 0.0,
    val currentMonthIncome: Double = 0.0,
    val totalMonthlyBudget: Double = 0.0,
    val currentMonthBudgetRemaining: Double = 0.0,
    val currentMonthBudgetRatio: Float = 0f,
    val isBudgetApproaching: Boolean = false,
    val isBudgetExceeded: Boolean = false
)

class FinanceViewModel(
    private val repository: FinanceRepository
) : ViewModel() {

    val currency: StateFlow<CurrencyOption> = repository.currency
    val themeMode: StateFlow<AppThemeMode> = repository.themeMode

    val allTransactions: StateFlow<List<TransactionEntity>> = repository.transactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBudgets: StateFlow<List<BudgetEntity>> = repository.budgets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            if (!repository.hasCleanedInitialData()) {
                repository.clearAllData()
                repository.markInitialDataCleaned()
            }
        }
    }

    // Search and filter state
    val searchQuery = MutableStateFlow("")
    val selectedTypeFilter = MutableStateFlow("ALL") // "ALL", "INCOME", "EXPENSE"
    val selectedCategoryFilter = MutableStateFlow("ALL")
    val selectedDateFilter = MutableStateFlow(DateFilterOption.ALL)

    // Filtered transactions
    val filteredTransactions: StateFlow<List<TransactionEntity>> = combine(
        allTransactions,
        searchQuery,
        selectedTypeFilter,
        selectedCategoryFilter,
        selectedDateFilter
    ) { txs, query, type, category, dateFilter ->
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfThisMonth = cal.timeInMillis

        cal.set(Calendar.DAY_OF_YEAR, 1)
        val startOfThisYear = cal.timeInMillis

        val thirtyDaysAgo = now - (30L * 24 * 60 * 60 * 1000)

        txs.filter { tx ->
            // Search query
            val matchesQuery = query.isBlank() ||
                tx.description.contains(query, ignoreCase = true) ||
                tx.category.contains(query, ignoreCase = true) ||
                tx.paymentMethod.contains(query, ignoreCase = true)

            // Type filter
            val matchesType = when (type) {
                "INCOME" -> tx.type.equals("INCOME", ignoreCase = true)
                "EXPENSE" -> tx.type.equals("EXPENSE", ignoreCase = true)
                else -> true
            }

            // Category filter
            val matchesCategory = (category == "ALL") || tx.category.equals(category, ignoreCase = true)

            // Date filter
            val matchesDate = when (dateFilter) {
                DateFilterOption.ALL -> true
                DateFilterOption.THIS_MONTH -> tx.dateMillis >= startOfThisMonth
                DateFilterOption.LAST_30_DAYS -> tx.dateMillis >= thirtyDaysAgo
                DateFilterOption.THIS_YEAR -> tx.dateMillis >= startOfThisYear
            }

            matchesQuery && matchesType && matchesCategory && matchesDate
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Overview statistics
    val overview: StateFlow<FinanceOverview> = combine(
        allTransactions,
        allBudgets
    ) { txs, budgets ->
        var income = 0.0
        var expense = 0.0
        var curMonthExpense = 0.0
        var curMonthIncome = 0.0

        val cal = Calendar.getInstance()
        val curMonth = cal.get(Calendar.MONTH)
        val curYear = cal.get(Calendar.YEAR)

        val txCal = Calendar.getInstance()

        txs.forEach { tx ->
            txCal.timeInMillis = tx.dateMillis
            val isCurrentMonth = txCal.get(Calendar.MONTH) == curMonth && txCal.get(Calendar.YEAR) == curYear

            if (tx.type.equals("INCOME", ignoreCase = true)) {
                income += tx.amount
                if (isCurrentMonth) curMonthIncome += tx.amount
            } else {
                expense += tx.amount
                if (isCurrentMonth) curMonthExpense += tx.amount
            }
        }

        val totalBalance = income - expense
        val totalSavings = if (totalBalance > 0) totalBalance else 0.0

        val totalBudget = budgets.find { it.category == "TOTAL" }?.monthlyLimit ?: 0.0
        val remainingBudget = if (totalBudget > 0) (totalBudget - curMonthExpense) else 0.0
        val budgetRatio = if (totalBudget > 0) (curMonthExpense / totalBudget).toFloat() else 0f

        val isApproaching = totalBudget > 0 && budgetRatio >= 0.8f && budgetRatio < 1.0f
        val isExceeded = totalBudget > 0 && budgetRatio >= 1.0f

        FinanceOverview(
            totalBalance = totalBalance,
            totalIncome = income,
            totalExpenses = expense,
            totalSavings = totalSavings,
            currentMonthSpending = curMonthExpense,
            currentMonthIncome = curMonthIncome,
            totalMonthlyBudget = totalBudget,
            currentMonthBudgetRemaining = remainingBudget,
            currentMonthBudgetRatio = budgetRatio,
            isBudgetApproaching = isApproaching,
            isBudgetExceeded = isExceeded
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FinanceOverview())

    // Category breakdown for current month
    val categoryExpensesCurrentMonth: StateFlow<List<CategorySpendItem>> = allTransactions.combine(allBudgets) { txs, _ ->
        val cal = Calendar.getInstance()
        val curMonth = cal.get(Calendar.MONTH)
        val curYear = cal.get(Calendar.YEAR)
        val txCal = Calendar.getInstance()

        val map = mutableMapOf<String, Double>()
        val countMap = mutableMapOf<String, Int>()
        var totalCurrentMonthExp = 0.0

        txs.filter { it.type.equals("EXPENSE", ignoreCase = true) }.forEach { tx ->
            txCal.timeInMillis = tx.dateMillis
            if (txCal.get(Calendar.MONTH) == curMonth && txCal.get(Calendar.YEAR) == curYear) {
                map[tx.category] = (map[tx.category] ?: 0.0) + tx.amount
                countMap[tx.category] = (countMap[tx.category] ?: 0) + 1
                totalCurrentMonthExp += tx.amount
            }
        }

        map.map { (cat, amt) ->
            val pct = if (totalCurrentMonthExp > 0) (amt / totalCurrentMonthExp).toFloat() else 0f
            CategorySpendItem(
                category = cat,
                amount = amt,
                percentage = pct,
                count = countMap[cat] ?: 0
            )
        }.sortedByDescending { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Monthly trends for past 6 months
    val monthlyTrends: StateFlow<List<MonthlyTrendItem>> = allTransactions.combine(currency) { txs, _ ->
        val result = mutableListOf<MonthlyTrendItem>()
        val cal = Calendar.getInstance()
        val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
        val yearMonthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())

        for (i in 5 downTo 0) {
            val monthCal = Calendar.getInstance()
            monthCal.add(Calendar.MONTH, -i)
            val targetMonth = monthCal.get(Calendar.MONTH)
            val targetYear = monthCal.get(Calendar.YEAR)
            val monthLabel = monthFormat.format(monthCal.time)
            val yearMonth = yearMonthFormat.format(monthCal.time)

            var monthInc = 0.0
            var monthExp = 0.0

            val txCal = Calendar.getInstance()
            txs.forEach { tx ->
                txCal.timeInMillis = tx.dateMillis
                if (txCal.get(Calendar.MONTH) == targetMonth && txCal.get(Calendar.YEAR) == targetYear) {
                    if (tx.type.equals("INCOME", ignoreCase = true)) {
                        monthInc += tx.amount
                    } else {
                        monthExp += tx.amount
                    }
                }
            }

            result.add(
                MonthlyTrendItem(
                    monthLabel = monthLabel,
                    yearMonth = yearMonth,
                    income = monthInc,
                    expense = monthExp,
                    savings = (monthInc - monthExp).coerceAtLeast(0.0)
                )
            )
        }
        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Category budget tracking items
    data class CategoryBudgetStatus(
        val category: String,
        val budgetAmount: Double,
        val spentAmount: Double,
        val remainingAmount: Double,
        val progressRatio: Float,
        val isApproaching: Boolean,
        val isExceeded: Boolean
    )

    val categoryBudgetStatuses: StateFlow<List<CategoryBudgetStatus>> = combine(
        allTransactions,
        allBudgets
    ) { txs, budgets ->
        val cal = Calendar.getInstance()
        val curMonth = cal.get(Calendar.MONTH)
        val curYear = cal.get(Calendar.YEAR)
        val txCal = Calendar.getInstance()

        val spentMap = mutableMapOf<String, Double>()
        txs.filter { it.type.equals("EXPENSE", ignoreCase = true) }.forEach { tx ->
            txCal.timeInMillis = tx.dateMillis
            if (txCal.get(Calendar.MONTH) == curMonth && txCal.get(Calendar.YEAR) == curYear) {
                spentMap[tx.category] = (spentMap[tx.category] ?: 0.0) + tx.amount
            }
        }

        // All configured category budgets (excluding "TOTAL")
        budgets.filter { it.category != "TOTAL" }.map { b ->
            val spent = spentMap[b.category] ?: 0.0
            val remaining = b.monthlyLimit - spent
            val ratio = if (b.monthlyLimit > 0) (spent / b.monthlyLimit).toFloat() else 0f
            CategoryBudgetStatus(
                category = b.category,
                budgetAmount = b.monthlyLimit,
                spentAmount = spent,
                remainingAmount = remaining,
                progressRatio = ratio,
                isApproaching = ratio >= 0.8f && ratio < 1.0f,
                isExceeded = ratio >= 1.0f
            )
        }.sortedByDescending { it.progressRatio }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addTransaction(
        type: String,
        amount: Double,
        category: String,
        dateMillis: Long,
        description: String,
        paymentMethod: String
    ) {
        viewModelScope.launch {
            repository.addTransaction(
                TransactionEntity(
                    id = 0,
                    type = type,
                    amount = amount,
                    category = category,
                    dateMillis = dateMillis,
                    description = description,
                    paymentMethod = paymentMethod
                )
            )
        }
    }

    fun updateTransaction(tx: TransactionEntity) {
        viewModelScope.launch {
            repository.updateTransaction(tx)
        }
    }

    fun deleteTransaction(tx: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(tx)
        }
    }

    fun setBudget(category: String, amount: Double) {
        viewModelScope.launch {
            repository.setBudget(category, amount)
        }
    }

    fun deleteBudget(category: String) {
        viewModelScope.launch {
            repository.deleteBudget(category)
        }
    }

    fun setCurrency(currency: CurrencyOption) {
        repository.setCurrency(currency)
    }

    fun setThemeMode(mode: AppThemeMode) {
        repository.setThemeMode(mode)
    }

    fun exportTransactionsCsv(): String {
        return repository.exportTransactionsToCsv(allTransactions.value)
    }

    fun importTransactionsCsv(csv: String, onResult: (Int) -> Unit) {
        viewModelScope.launch {
            val count = repository.importTransactionsFromCsv(csv)
            onResult(count)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
        }
    }

    fun loadSampleData() {
        viewModelScope.launch {
            repository.populateSampleData()
        }
    }
}

class FinanceViewModelFactory(
    private val repository: FinanceRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FinanceViewModel::class.java)) {
            return FinanceViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
