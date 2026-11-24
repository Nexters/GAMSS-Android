package com.gamss.android.feature.store

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

data class StoreState(
    val isLoading: Boolean = false,
    val tokenProducts: List<StoreTokenProduct> = defaultTokenProducts,
)

data class StoreTokenProduct(
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
    val iconSize: DpSize,
)

// 아이콘별 원본 비율이 달라 크기를 통일하면 Fit 스케일링으로 작게 쪼그라든다. Figma 기준 박스 크기를 그대로 쓴다.
private val defaultTokenProducts = listOf(
    StoreTokenProduct(
        labelRes = R.string.store_token_label_30,
        iconRes = R.drawable.img_store_coin_small,
        iconSize = DpSize(42.dp, 42.dp),
    ),
    StoreTokenProduct(
        labelRes = R.string.store_token_label_50,
        iconRes = R.drawable.img_store_coin_medium,
        iconSize = DpSize(80.dp, 42.dp),
    ),
    StoreTokenProduct(
        labelRes = R.string.store_token_label_100,
        iconRes = R.drawable.img_store_coin_large,
        iconSize = DpSize(62.dp, 62.dp),
    ),
)
