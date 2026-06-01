package com.sonari.speak2easy.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Event-driven connectivity tracking via ConnectivityManager (the Android analogue of
 * iOS's NWPathMonitor). Exposes [isConnected] as a StateFlow.
 */
class NetworkMonitor(context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val _isConnected = MutableStateFlow(true)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    init {
        // Defensive: never let a system-service failure crash app launch.
        runCatching {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager?.registerNetworkCallback(
                request,
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        _isConnected.value = true
                    }

                    override fun onLost(network: Network) {
                        _isConnected.value = runCatching { connectivityManager.activeNetwork != null }.getOrDefault(false)
                    }
                },
            )
            _isConnected.value = connectivityManager?.activeNetwork != null
        }
    }
}
