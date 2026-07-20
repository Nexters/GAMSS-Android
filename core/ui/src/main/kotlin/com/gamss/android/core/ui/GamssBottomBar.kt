package com.gamss.android.core.ui

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun <T> GamssBottomBar(
    items: List<GamssBottomBarItem<T>>,
    selectedValue: T,
    onItemClick: (T) -> Unit,
) {
    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                selected = item.value == selectedValue,
                onClick = { onItemClick(item.value) },
                icon = { Icon(imageVector = item.icon, contentDescription = item.label) },
                label = { Text(text = item.label) },
            )
        }
    }
}
