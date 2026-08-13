package com.gamss.android.feature.setting.privacypolicy

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

@HiltViewModel
class PrivacyPolicyViewModel @Inject constructor() :
    ViewModel(),
    ContainerHost<PrivacyPolicyState, PrivacyPolicySideEffect> {

    override val container = container<PrivacyPolicyState, PrivacyPolicySideEffect>(PrivacyPolicyState())
}
