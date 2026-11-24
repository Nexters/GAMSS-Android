package com.gamss.android.feature.store

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import org.orbitmvi.orbit.compose.collectAsState

@Composable
fun StoreScreen(
    viewModel: StoreViewModel = hiltViewModel()
) {
    val state by viewModel.collectAsState()


}