package com.gamss.android.feature.webview

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * 현재 인터넷 연결 여부를 구독한다.
 *
 * 캐시 모드를 정하는 데만 쓰이므로 검증된 연결까지 따지지 않고 인터넷 가능 여부만 본다.
 */
@Composable
internal fun rememberIsOnline(): Boolean {
    val context = LocalContext.current
    val connectivityManager: ConnectivityManager? = remember(context) {
        context.getSystemService(ConnectivityManager::class.java)
    }
    var isOnline by remember(connectivityManager) {
        mutableStateOf(connectivityManager.hasInternet())
    }

    DisposableEffect(connectivityManager) {
        if (connectivityManager == null) return@DisposableEffect onDispose { }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                isOnline = true
            }

            override fun onLost(network: Network) {
                // 다른 망으로 전환 중일 수 있으므로 끊겼다고 단정하지 않고 다시 조회한다.
                isOnline = connectivityManager.hasInternet()
            }
        }
        connectivityManager.registerDefaultNetworkCallback(callback)
        onDispose { connectivityManager.unregisterNetworkCallback(callback) }
    }

    return isOnline
}

private fun ConnectivityManager?.hasInternet(): Boolean {
    val manager = this ?: return false
    val capabilities = manager.activeNetwork?.let(manager::getNetworkCapabilities)
    return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
}
