package com.gamss.android.feature.carddelete

data class CardDeleteState(
    val shredTapCount: Int = 0,
    val isDeleting: Boolean = false,
    val isCompleted: Boolean = false,
) {
    val isShredding: Boolean get() = !isCompleted && (shredTapCount > 0 || isDeleting)
    val shredProgress: Float get() = (shredTapCount.toFloat() / SHRED_TOTAL_TAPS).coerceIn(0f, 1f)
}
