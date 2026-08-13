package com.gamss.android.feature.setting.serviceterms

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

@HiltViewModel
class ServiceTermsViewModel @Inject constructor() :
    ViewModel(),
    ContainerHost<ServiceTermsState, ServiceTermsSideEffect> {

    override val container = container<ServiceTermsState, ServiceTermsSideEffect>(ServiceTermsState())
}
