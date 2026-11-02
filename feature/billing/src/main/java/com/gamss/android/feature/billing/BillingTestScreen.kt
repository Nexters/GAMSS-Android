package com.gamss.android.feature.billing

import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.gamss.android.core.designsystem.theme.GamssTheme
import com.gamss.android.core.designsystem.topnavigation.GamssTopNavigation
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
fun BillingTestScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BillingViewModel = hiltViewModel(),
) {
    val state by viewModel.collectAsState()
    val context = LocalContext.current
    val activity = LocalActivity.current
    val defaultErrorMessage = stringResource(R.string.billing_test_error_default)

    LaunchedEffect(Unit) { viewModel.loadProducts() }

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is BillingSideEffect.LaunchPurchase ->
                activity?.let { viewModel.launchPurchase(it, sideEffect) }

            is BillingSideEffect.ShowError ->
                Toast.makeText(context, sideEffect.message ?: defaultErrorMessage, Toast.LENGTH_SHORT).show()
        }
    }

    if (state.isPendingNoticeVisible) {
        AlertDialog(
            onDismissRequest = viewModel::dismissPendingNotice,
            confirmButton = {
                Button(onClick = viewModel::dismissPendingNotice) {
                    Text(stringResource(R.string.billing_test_confirm))
                }
            },
            title = { Text(stringResource(R.string.billing_test_pending_title)) },
            text = { Text(stringResource(R.string.billing_test_pending_message)) },
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        GamssTopNavigation(
            title = stringResource(R.string.billing_test_title),
            showLeftIcon = true,
            onLeftIconClick = onBackClick,
        )

        if (state.isLoadingProducts) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator(modifier = Modifier.padding(top = GamssTheme.spacing.spacing400))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(GamssTheme.spacing.spacing300),
                verticalArrangement = Arrangement.spacedBy(GamssTheme.spacing.spacing200),
            ) {
                state.products.forEach { product ->
                    item {
                        BillingProductCard(
                            product = product,
                            isPurchaseInProgress = state.isPurchaseInProgress,
                            onOptionClick = { option -> viewModel.onPurchaseClick(product, option) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BillingProductCard(
    product: BillingProduct,
    isPurchaseInProgress: Boolean,
    onOptionClick: (BillingPurchaseOption) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(GamssTheme.spacing.spacing300)) {
            Text(text = product.productId, style = GamssTheme.typography.body2Medium)
            Text(text = product.benefitDescription, style = GamssTheme.typography.body3Medium)

            if (!product.isPurchasable) {
                Text(
                    text = stringResource(R.string.billing_test_not_purchasable),
                    style = GamssTheme.typography.caption3,
                    color = GamssTheme.colors.gray500,
                )
            }

            product.options.forEach { option ->
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = GamssTheme.spacing.spacing100),
                    enabled = product.isPurchasable && !isPurchaseInProgress,
                    onClick = { onOptionClick(option) },
                ) {
                    // 가격은 항상 Play 가 내려준 문자열 그대로 보여준다 — 하드코딩하지 않는다.
                    Text(text = option.formattedPrice)
                }
            }
        }
    }
}
