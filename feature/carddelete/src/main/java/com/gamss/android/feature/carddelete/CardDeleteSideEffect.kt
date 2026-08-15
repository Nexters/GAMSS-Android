package com.gamss.android.feature.carddelete

sealed interface CardDeleteSideEffect {
    data object ShredSuccess : CardDeleteSideEffect
    data object ShredFailure : CardDeleteSideEffect
}
