package com.gamss.android.feature.setting.main

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

@HiltViewModel
class SettingViewModel @Inject constructor() :
    ViewModel(),
    ContainerHost<SettingState, SettingSideEffect> {

    override val container = container<SettingState, SettingSideEffect>(SettingState())
}
