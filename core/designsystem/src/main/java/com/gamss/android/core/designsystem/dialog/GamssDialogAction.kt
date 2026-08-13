package com.gamss.android.core.designsystem.dialog

import com.gamss.android.core.designsystem.button.GamssButtonVariant

data class GamssDialogAction(
    val label: String,
    val onClick: () -> Unit,
    val variant: GamssButtonVariant = GamssButtonVariant.Primary,
    val enabled: Boolean = true,
)
