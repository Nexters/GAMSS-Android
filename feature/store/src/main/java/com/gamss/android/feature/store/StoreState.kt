package com.gamss.android.feature.store

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.gamss.android.core.billing.PaymentState
import com.gamss.android.domain.payment.StoreProduct
import com.gamss.android.domain.payment.TokenProduct

data class StoreState(
    val tokenProducts: List<StoreTokenProduct> = emptyList(),
    val isLoading: Boolean = false,
    val isPreparing: Boolean = false,
    val payment: PaymentState = PaymentState(),
    val error: String? = null,
)

data class StoreTokenProduct(
    val product: TokenProduct,
    val billingProduct: StoreProduct?,
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
    val iconSize: DpSize,
)

// 아이콘별 원본 비율이 달라 크기를 통일하면 Fit 스케일링으로 작게 쪼그라든다. Figma 기준 박스 크기를 그대로 쓴다.
// 서버 상품을 tokenAmount 기준 정렬했을 때의 순서(30/50/100만)에 맞춘 비주얼 템플릿.
internal val TokenProductVisuals = listOf(
    TokenProductVisual(
        labelRes = R.string.store_token_label_30,
        iconRes = R.drawable.img_store_coin_small,
        iconSize = DpSize(42.dp, 42.dp),
    ),
    TokenProductVisual(
        labelRes = R.string.store_token_label_50,
        iconRes = R.drawable.img_store_coin_medium,
        iconSize = DpSize(80.dp, 42.dp),
    ),
    TokenProductVisual(
        labelRes = R.string.store_token_label_100,
        iconRes = R.drawable.img_store_coin_large,
        iconSize = DpSize(62.dp, 62.dp),
    ),
)

internal data class TokenProductVisual(
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
    val iconSize: DpSize,
)
