package com.example.gestaobilhares.core.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Utilitário para verificar conectividade de rede
 * Monitora mudanças na conectividade em tempo real
 */
class NetworkUtils(private val context: Context) {
    
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    private val _isNetworkAvailable = MutableStateFlow(false)
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable
    
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isNetworkAvailable.value = true
        }
        
        override fun onLost(network: Network) {
            _isNetworkAvailable.value = false
        }
        
        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val isValidated = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            _isNetworkAvailable.value = hasInternet && isValidated
        }
    }
    
    init {
        android.util.Log.d("NetworkUtils", "NetworkUtils inicializando...")
        registerNetworkCallback()
        checkInitialConnectivity()
        android.util.Log.d("NetworkUtils", "NetworkUtils inicializado - isConnected = ${_isNetworkAvailable.value}")
    }
    
    /**
     * Registra callback para monitorar mudanças na rede
     */
    private fun registerNetworkCallback() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }
    
    /**
     * Verifica conectividade inicial
     */
    private fun checkInitialConnectivity() {
        val network = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
        
        val hasInternet = networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ?: false
        val isValidated = networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) ?: false
        
        _isNetworkAvailable.value = hasInternet && isValidated
    }
    
    /**
     * Verifica se há conectividade ativa
     * Verifica em tempo real, não apenas o StateFlow
     */
    fun isConnected(): Boolean {
        // Verificar em tempo real para garantir precisão
        val network = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
        
        val hasInternet = networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ?: false
        val isValidated = networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) ?: false
        
        val isConnected = hasInternet && isValidated
        
        // Atualizar StateFlow se mudou
        if (_isNetworkAvailable.value != isConnected) {
            _isNetworkAvailable.value = isConnected
        }
        
        return isConnected
    }
    
    /**
     * Verifica se há conectividade WiFi
     */
    fun isWifiConnected(): Boolean {
        val network = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
        
        return networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    }
    
    /**
     * Verifica se há conectividade móvel
     */
    fun isMobileConnected(): Boolean {
        val network = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
        
        return networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
    }
    
    /**
     * Libera recursos
     */
    fun cleanup() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            // Ignorar erro se callback não estiver registrado
        }
    }
}
