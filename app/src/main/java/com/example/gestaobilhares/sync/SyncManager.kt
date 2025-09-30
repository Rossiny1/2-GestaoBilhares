package com.example.gestaobilhares.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.gestaobilhares.data.entities.*
import com.example.gestaobilhares.data.repository.AppRepository
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

class SyncManager(
    private val context: Context,
    private val repository: AppRepository
) {
    
    private val syncScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val isSyncing = AtomicBoolean(false)
    private val pendingOperations = ConcurrentLinkedQueue<SyncOperation>()
    
    private val _syncStatus = MutableLiveData<SyncStatus>()
    val syncStatus: LiveData<SyncStatus> = _syncStatus
    
    private val _pendingOperationsCount = MutableLiveData<Int>()
    val pendingOperationsCount: LiveData<Int> = _pendingOperationsCount

    init {
        startPeriodicSync()
        loadPendingOperations()
    }

    fun addPendingOperation(operation: SyncOperation) {
        pendingOperations.offer(operation)
        updatePendingCount()
        savePendingOperations()
        
        if (isOnline()) {
            syncScope.launch {
                processPendingOperations()
            }
        }
    }

    fun syncNow() {
        if (isSyncing.get()) return
        
        syncScope.launch {
            try {
                isSyncing.set(true)
                _syncStatus.postValue(SyncStatus.SYNCING)
                
                if (isOnline()) {
                    // Sincronizar dados pendentes
                    processPendingOperations()
                    
                    // Sincronizar dados do servidor
                    syncFromServer()
                    
                    _syncStatus.postValue(SyncStatus.SYNCED)
                } else {
                    _syncStatus.postValue(SyncStatus.OFFLINE)
                }
            } catch (e: Exception) {
                _syncStatus.postValue(SyncStatus.ERROR(e.message ?: "Erro na sincronização"))
            } finally {
                isSyncing.set(false)
            }
        }
    }

    private suspend fun processPendingOperations() {
        while (pendingOperations.isNotEmpty()) {
            val operation = pendingOperations.poll()
            try {
                when (operation) {
                    is SyncOperation.CreateCliente -> {
                        // Enviar para servidor
                        val success = sendToServer(operation)
                        if (!success) {
                            pendingOperations.offer(operation) // Recolocar na fila
                            break
                        }
                    }
                    is SyncOperation.UpdateCliente -> {
                        val success = updateOnServer(operation)
                        if (!success) {
                            pendingOperations.offer(operation)
                            break
                        }
                    }
                    is SyncOperation.CreateAcerto -> {
                        val success = sendAcertoToServer(operation)
                        if (!success) {
                            pendingOperations.offer(operation)
                            break
                        }
                    }
                    is SyncOperation.CreateColaborador -> {
                        val success = sendColaboradorToServer(operation)
                        if (!success) {
                            pendingOperations.offer(operation)
                            break
                        }
                    }
                }
                updatePendingCount()
                savePendingOperations()
            } catch (e: Exception) {
                pendingOperations.offer(operation)
                break
            }
        }
    }

    private suspend fun syncFromServer() {
        try {
            // 🚨 BLOQUEADO: Sincronização do servidor desabilitada para evitar população automática
            android.util.Log.d("SyncManager", "SYNC FROM SERVER BLOQUEADO - Evitando população automática")
            
            // ❌ BLOQUEADO: Sincronizar rotas
            // val rotasServer = fetchRotasFromServer()
            // repository.syncRotas(rotasServer)
            
            // ❌ BLOQUEADO: Sincronizar clientes
            // val clientesServer = fetchClientesFromServer()
            // repository.syncClientes(clientesServer)
            
            // ❌ BLOQUEADO: Sincronizar acertos
            // val acertosServer = fetchAcertosFromServer()
            // repository.syncAcertos(acertosServer)
            
            // ❌ BLOQUEADO: Sincronizar colaboradores
            // val colaboradoresServer = fetchColaboradoresFromServer()
            // repository.syncColaboradores(colaboradoresServer)
            
        } catch (e: Exception) {
            throw Exception("Erro ao sincronizar do servidor: ${e.message}")
        }
    }

    private fun isOnline(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun startPeriodicSync() {
        syncScope.launch {
            while (true) {
                delay(SYNC_INTERVAL)
                if (isOnline() && pendingOperations.isNotEmpty()) {
                    syncNow()
                }
            }
        }
    }

    private fun updatePendingCount() {
        _pendingOperationsCount.postValue(pendingOperations.size)
    }

    private fun savePendingOperations() {
        // Salvar operações pendentes no SharedPreferences ou banco local
        val operations = pendingOperations.toList()
        // TODO: Implementar persistência local
    }

    private fun loadPendingOperations() {
        // Carregar operações pendentes do SharedPreferences ou banco local
        // TODO: Implementar carregamento local
        updatePendingCount()
    }

    // Métodos de comunicação com servidor (mockados por enquanto)
    private suspend fun sendToServer(operation: SyncOperation.CreateCliente): Boolean {
        delay(1000) // Simular delay de rede
        return true // Simular sucesso
    }

    private suspend fun updateOnServer(operation: SyncOperation.UpdateCliente): Boolean {
        delay(1000)
        return true
    }

    private suspend fun sendAcertoToServer(operation: SyncOperation.CreateAcerto): Boolean {
        delay(1000)
        return true
    }

    private suspend fun sendColaboradorToServer(operation: SyncOperation.CreateColaborador): Boolean {
        delay(1000)
        return true
    }

    private suspend fun fetchRotasFromServer(): List<Rota> {
        delay(500)
        return emptyList() // TODO: Implementar chamada real
    }

    private suspend fun fetchClientesFromServer(): List<Cliente> {
        delay(500)
        return emptyList()
    }

    private suspend fun fetchAcertosFromServer(): List<Acerto> {
        delay(500)
        return emptyList()
    }

    private suspend fun fetchColaboradoresFromServer(): List<Colaborador> {
        delay(500)
        return emptyList()
    }

    fun clearPendingOperations() {
        pendingOperations.clear()
        updatePendingCount()
        savePendingOperations()
    }

    fun getPendingOperations(): List<SyncOperation> {
        return pendingOperations.toList()
    }

    fun onDestroy() {
        syncScope.cancel()
    }

    sealed class SyncOperation {
        data class CreateCliente(val cliente: Cliente) : SyncOperation()
        data class UpdateCliente(val cliente: Cliente) : SyncOperation()
        data class CreateAcerto(val acerto: Acerto) : SyncOperation()
        data class CreateColaborador(val colaborador: Colaborador) : SyncOperation()
    }

    sealed class SyncStatus {
        object IDLE : SyncStatus()
        object SYNCING : SyncStatus()
        object SYNCED : SyncStatus()
        object OFFLINE : SyncStatus()
        data class ERROR(val message: String) : SyncStatus()
    }

    companion object {
        private const val SYNC_INTERVAL = 5 * 60 * 1000L // 5 minutos
    }
}
