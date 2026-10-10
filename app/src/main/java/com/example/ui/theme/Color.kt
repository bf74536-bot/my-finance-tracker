package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Brand Accents
val FinanceGreenPrimary = Color(0xFF10B981)
val FinanceGreenDark = Color(0xFF059669)
val FinanceGreenLight = Color(0xFF34D399)

val FinanceIncome = Color(0xFF10B981)
val FinanceExpense = Color(0xFFEF4444)
val FinanceWarning = Color(0xFFF59E0B)
val FinanceInfo = Color(0xFF3B82F6)

// Dark Theme Palette
val DarkBackground = Color(0xFF0F172A)
val DarkSurface = Color(0xFF1E293B)
val DarkSurfaceVariant = Color(0xFF334155)
val DarkOnBackground = Color(0xFFF8FAFC)
val DarkOnSurface = Color(0xFFE2E8F0)
val DarkOutline = Color(0xFF475569)

// Light Theme Palette
val LightBackground = Color(0xFFF8FAFC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF1F5F9)
val LightOnBackground = Color(0xFF0F172A)
val LightOnSurface = Color(0xFF1E293B)
val LightOutline = Color(0xFFCBD5E1)

// Category Colors
val CategoryColors = mapOf(
    "Food" to Color(0xFFF97316),
    "Shopping" to Color(0xFFEC4899),
    "Travel" to Color(0xFF06B6D4),
    "Bills" to Color(0xFFEAB308),
    "Education" to Color(0xFF8B5CF6),
    "Entertainment" to Color(0xFFA855F7),
    "Health" to Color(0xFF14B8A6),
    "Salary" to Color(0xFF10B981),
    "Freelance" to Color(0xFF3B82F6),
    "Other" to Color(0xFF64748B)
)

fun getCategoryColor(category: String): Color {
    return CategoryColors[category] ?: Color(0xFF64748B)
}
