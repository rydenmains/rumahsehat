package com.rumahsehat.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.rumahsehat.ui.theme.Primary

enum class RSTab(val label: String) { BERANDA("Beranda"), INSPEKSI("Inspeksi"), RIWAYAT("Riwayat") }

@Composable
fun RSBottomNavBar(current: RSTab, onSelect: (RSTab) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest) {
        NavigationBarItem(
            selected = current == RSTab.BERANDA,
            onClick = { onSelect(RSTab.BERANDA) },
            icon = { Icon(Icons.Filled.Home, contentDescription = null) },
            label = { Text(RSTab.BERANDA.label) },
            colors = NavigationBarItemDefaults.colors(selectedIconColor = Primary, indicatorColor = Primary.copy(alpha = 0.12f))
        )
        NavigationBarItem(
            selected = current == RSTab.INSPEKSI,
            onClick = { onSelect(RSTab.INSPEKSI) },
            icon = { Icon(Icons.Filled.Assignment, contentDescription = null) },
            label = { Text(RSTab.INSPEKSI.label) },
            colors = NavigationBarItemDefaults.colors(selectedIconColor = Primary, indicatorColor = Primary.copy(alpha = 0.12f))
        )
        NavigationBarItem(
            selected = current == RSTab.RIWAYAT,
            onClick = { onSelect(RSTab.RIWAYAT) },
            icon = { Icon(Icons.Filled.History, contentDescription = null) },
            label = { Text(RSTab.RIWAYAT.label) },
            colors = NavigationBarItemDefaults.colors(selectedIconColor = Primary, indicatorColor = Primary.copy(alpha = 0.12f))
        )
    }
}
