package com.gamss.android.feature.chat

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

@HiltViewModel
class ChattingListViewModel @Inject constructor() : ViewModel(),
    ContainerHost<ChattingListState, ChattingListSideEffect> {

    override val container = container<ChattingListState, ChattingListSideEffect>(ChattingListState())
}