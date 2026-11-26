package com.gamss.android.feature.store

import androidx.annotation.DrawableRes
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.gamss.android.core.billing.PaymentState
import com.gamss.android.domain.payment.StoreProduct
import com.gamss.android.domain.payment.TokenProduct

/**
 * [loadFailure]·[isEmpty]는 상품 목록 영역 대신 보여줄 화면을 정한다. [error]는 응답 코드 등이 담긴 진단용
 * 문구라 디버그 화면에서만 쓰고, 배포 화면의 사용자 안내는 리소스와 Toast로 한다.
 */
data class StoreState(
    val tokenProducts: List<StoreTokenProduct> = emptyList(),
    val isLoading: Boolean = false,
    val isPreparing: Boolean = false,
    val payment: PaymentState = PaymentState(),
    val loadFailure: StoreLoadFailure? = null,
    val isEmpty: Boolean = false,
    val error: String? = null,
)

/** 서버 상품 조회 실패 종류. 네트워크 오류는 공용 네트워크 오류 화면을, 그 외에는 상점 전용 안내를 보여준다. */
enum class StoreLoadFailure { NETWORK, UNKNOWN }

/**
 * [labelAmountInTenThousands]는 카드 라벨("30만 토큰")에 쓰는 만 단위 수량으로, 서버가 내려준 실제 지급량
 * [TokenProduct.tokenAmount]에서 계산한다.
 */
data class StoreTokenProduct(
    val product: TokenProduct,
    val billingProduct: StoreProduct?,
    val labelAmountInTenThousands: Long,
    @DrawableRes val iconRes: Int,
    val iconSize: DpSize,
)

internal fun TokenProduct.labelAmountInTenThousands(): Long = tokenAmount / TOKEN_LABEL_UNIT

private const val TOKEN_LABEL_UNIT = 10_000L

// 아이콘은 금액·수량에 영향이 없는 장식이라, tokenAmount 오름차순 순서대로 작은/중간/큰 동전을 쓴다.
// 상품이 더 많으면 마지막 아이콘을 반복한다.
// 아이콘별 원본 비율이 달라 크기를 통일하면 Fit 스케일링으로 작게 쪼그라든다. Figma 기준 박스 크기를 그대로 쓴다.
internal val TokenProductIcons = listOf(
    TokenProductIcon(iconRes = R.drawable.img_store_coin_small, iconSize = DpSize(42.dp, 42.dp)),
    TokenProductIcon(iconRes = R.drawable.img_store_coin_medium, iconSize = DpSize(80.dp, 42.dp)),
    TokenProductIcon(iconRes = R.drawable.img_store_coin_large, iconSize = DpSize(62.dp, 62.dp)),
)

internal data class TokenProductIcon(
    @DrawableRes val iconRes: Int,
    val iconSize: DpSize,
)
