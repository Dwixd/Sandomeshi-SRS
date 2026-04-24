package com.example.deckmaking

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    object Discover : BottomNavItem("discover", "Jelajah", Icons.Default.Search)
    object Create : BottomNavItem("create", "Buat", Icons.Default.AddCircle)
    object Settings : BottomNavItem("settings", "Pengaturan", Icons.Default.Settings)
}
