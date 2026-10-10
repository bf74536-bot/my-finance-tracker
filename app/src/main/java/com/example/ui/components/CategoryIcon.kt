package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.LaptopMac
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.getCategoryColor

fun getCategoryIconVector(category: String): ImageVector {
    return when (category.lowercase()) {
        "food" -> Icons.Default.Restaurant
        "shopping" -> Icons.Default.ShoppingCart
        "travel" -> Icons.Default.Flight
        "bills" -> Icons.Default.Receipt
        "education" -> Icons.Default.School
        "entertainment" -> Icons.Default.Movie
        "health" -> Icons.Default.LocalHospital
        "salary" -> Icons.Default.AccountBalance
        "freelance" -> Icons.Default.LaptopMac
        else -> Icons.Default.Category
    }
}

@Composable
fun CategoryIconBadge(
    category: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    shape: RoundedCornerShape = RoundedCornerShape(12.dp)
) {
    val catColor = getCategoryColor(category)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(catColor.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = getCategoryIconVector(category),
            contentDescription = category,
            tint = catColor,
            modifier = Modifier.size(iconSize)
        )
    }
}
