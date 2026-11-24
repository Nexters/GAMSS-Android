package com.gamss.android.feature.store

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

@HiltViewModel
class StoreViewModel @Inject constructor() : ViewModel(), ContainerHost<StoreState, StoreSideEffect> {
    override val container = container<StoreState, StoreSideEffect>(StoreState())

    fun navigateToSetting() = intent {
        postSideEffect(StoreSideEffect.NavigateToSetting)
    }
}
