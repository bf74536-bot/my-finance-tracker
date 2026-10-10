package com.example.ui.components

import com.example.data.model.CurrencyOption
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FinanceFormatters {
    fun formatAmount(amount: Double, currency: CurrencyOption, showDecimals: Boolean = true): String {
        val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = if (showDecimals) 2 else 0
            maximumFractionDigits = 2
        }
        return "${currency.symbol}${formatter.format(amount)}"
    }

    fun formatDate(millis: Long): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    fun formatShortDate(millis: Long): String {
        val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    fun formatIsoDate(millis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date(millis))
    }
}
