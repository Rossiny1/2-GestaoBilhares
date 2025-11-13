# TAREFAS PARA AGENTE PARALELO: IMPLEMENTAÇÃO DE FILA DE SINCRONIZAÇÃO OFFLINE-FIRST

## 🎯 OBJETIVO

Implementar sistema completo de fila de sincronização offline-first para garantir que operações sejam enfileiradas quando o dispositivo estiver offline e processadas automaticamente quando voltar online.

## 📋 CONTEXTO

- **SyncRepository**: Estrutura base criada e handlers de pull/push implementados
- **AppRepository**: Facade centralizado disponível
- **Room Database**: Configurado e funcionando
- **WorkManager**: Configurado para sincronização periódica
- **Arquitetura**: Offline-first, operações devem ser enfileiradas quando offline

## ✅ TAREFAS

### **1. Criar Entidade e DAO para Fila de Sincronização**

#### **1.1 Criar Entidade SyncOperationEntity**
```kotlin
// app/src/main/java/com/example/gestaobilhares/data/entities/SyncOperationEntity.kt

@Entity(tableName = "sync_operations")
data class SyncOperationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "operation_type")
    val operationType: String, // CREATE, UPDATE, DELETE
    
    @ColumnInfo(name = "entity_type")
    val entityType: String, // Cliente, Acerto, Mesa, etc.
    
    @ColumnInfo(name = "entity_id")
    val entityId: String,
    
    @ColumnInfo(name = "entity_data")
    val entityData: String, // JSON serializado
    
    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "retry_count")
    val retryCount: Int = 0,
    
    @ColumnInfo(name = "max_retries")
    val maxRetries: Int = 3,
    
    @ColumnInfo(name = "status")
    val status: SyncOperationStatus = SyncOperationStatus.PENDING
)

enum class SyncOperationStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED
}
```

#### **1.2 Criar DAO SyncOperationDao**
```kotlin
// app/src/main/java/com/example/gestaobilhares/data/dao/SyncOperationDao.kt

@Dao
interface SyncOperationDao {
    @Query("SELECT * FROM sync_operations WHERE status = 'PENDING' ORDER BY timestamp ASC")
    fun obterOperacoesPendentes(): Flow<List<SyncOperationEntity>>
    
    @Query("SELECT * FROM sync_operations WHERE status = 'PENDING' ORDER BY timestamp ASC LIMIT :limit")
    suspend fun obterOperacoesPendentesLimitadas(limit: Int): List<SyncOperationEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(operation: SyncOperationEntity): Long
    
    @Update
    suspend fun atualizar(operation: SyncOperationEntity)
    
    @Delete
    suspend fun deletar(operation: SyncOperationEntity)
    
    @Query("DELETE FROM sync_operations WHERE status = 'COMPLETED' AND timestamp < :beforeTimestamp")
    suspend fun limparOperacoesCompletadas(beforeTimestamp: Long)
    
    @Query("SELECT COUNT(*) FROM sync_operations WHERE status = 'PENDING'")
    suspend fun contarOperacoesPendentes(): Int
    
    @Query("SELECT COUNT(*) FROM sync_operations WHERE status = 'FAILED'")
    suspend fun contarOperacoesFalhadas(): Int
}
```

#### **1.3 Adicionar ao AppDatabase**
```kotlin
// Adicionar ao AppDatabase.kt:
@Database(
    entities = [
        // ... entidades existentes ...
        SyncOperationEntity::class
    ],
    version = X // Incrementar versão
)
abstract class AppDatabase : RoomDatabase() {
    // ... DAOs existentes ...
    abstract fun syncOperationDao(): SyncOperationDao
}
```

### **2. Implementar Métodos no AppRepository**

Adicionar métodos no `AppRepository.kt` para gerenciar fila:

```kotlin
// ==================== FILA DE SINCRONIZAÇÃO ====================

suspend fun inserirOperacaoSync(operation: SyncOperationEntity): Long {
    return syncOperationDao.inserir(operation)
}

fun obterOperacoesSyncPendentes(): Flow<List<SyncOperationEntity>> {
    return syncOperationDao.obterOperacoesPendentes()
}

suspend fun obterOperacoesSyncPendentesLimitadas(limit: Int): List<SyncOperationEntity> {
    return syncOperationDao.obterOperacoesPendentesLimitadas(limit)
}

suspend fun atualizarOperacaoSync(operation: SyncOperationEntity) {
    syncOperationDao.atualizar(operation)
}

suspend fun deletarOperacaoSync(operation: SyncOperationEntity) {
    syncOperationDao.deletar(operation)
}

suspend fun contarOperacoesSyncPendentes(): Int {
    return syncOperationDao.contarOperacoesPendentes()
}

suspend fun contarOperacoesSyncFalhadas(): Int {
    return syncOperationDao.contarOperacoesFalhadas()
}

suspend fun limparOperacoesSyncCompletadas(dias: Int = 7) {
    val beforeTimestamp = System.currentTimeMillis() - (dias * 24 * 60 * 60 * 1000L)
    syncOperationDao.limparOperacoesCompletadas(beforeTimestamp)
}
```

### **3. Implementar Fila no SyncRepository**

#### **3.1 Atualizar enqueueOperation()**
```kotlin
suspend fun enqueueOperation(operation: SyncOperation) {
    try {
        val entity = SyncOperationEntity(
            operationType = operation.type.name,
            entityType = operation.entityType,
            entityId = operation.entityId,
            entityData = operation.data,
            timestamp = operation.timestamp,
            retryCount = operation.retryCount,
            status = SyncOperationStatus.PENDING
        )
        
        appRepository.inserirOperacaoSync(entity)
        Log.d(TAG, "Operação enfileirada: ${operation.type} - ${operation.entityId}")
        
        // Atualizar status
        _syncStatus.value = _syncStatus.value.copy(
            pendingOperations = _syncStatus.value.pendingOperations + 1
        )
    } catch (e: Exception) {
        Log.e(TAG, "Erro ao enfileirar operação: ${e.message}", e)
    }
}
```

#### **3.2 Implementar processSyncQueue()**
```kotlin
suspend fun processSyncQueue(): Result<Unit> {
    return try {
        if (!networkUtils.isConnected()) {
            Log.w(TAG, "Fila não processada: dispositivo offline")
            return Result.failure(Exception("Dispositivo offline"))
        }
        
        Log.d(TAG, "Processando fila de sincronização...")
        
        // Processar em lotes de 10 operações
        val batchSize = 10
        var processedCount = 0
        var failedCount = 0
        
        while (true) {
            val operations = appRepository.obterOperacoesSyncPendentesLimitadas(batchSize)
            
            if (operations.isEmpty()) {
                break
            }
            
            operations.forEach { operation ->
                try {
                    // Marcar como processando
                    val processing = operation.copy(status = SyncOperationStatus.PROCESSING)
                    appRepository.atualizarOperacaoSync(processing)
                    
                    // Processar operação
                    val result = processOperation(operation)
                    
                    if (result.isSuccess) {
                        // Marcar como completada
                        val completed = operation.copy(status = SyncOperationStatus.COMPLETED)
                        appRepository.atualizarOperacaoSync(completed)
                        processedCount++
                    } else {
                        // Incrementar retry count
                        val retryCount = operation.retryCount + 1
                        val status = if (retryCount >= operation.maxRetries) {
                            SyncOperationStatus.FAILED
                        } else {
                            SyncOperationStatus.PENDING
                        }
                        
                        val updated = operation.copy(
                            retryCount = retryCount,
                            status = status
                        )
                        appRepository.atualizarOperacaoSync(updated)
                        
                        if (status == SyncOperationStatus.FAILED) {
                            failedCount++
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao processar operação ${operation.id}: ${e.message}", e)
                    failedCount++
                }
            }
        }
        
        // Atualizar status
        _syncStatus.value = _syncStatus.value.copy(
            pendingOperations = appRepository.contarOperacoesSyncPendentes(),
            failedOperations = appRepository.contarOperacoesSyncFalhadas()
        )
        
        Log.d(TAG, "Fila processada: $processedCount processadas, $failedCount falhadas")
        Result.success(Unit)
        
    } catch (e: Exception) {
        Log.e(TAG, "Erro ao processar fila: ${e.message}", e)
        Result.failure(e)
    }
}

private suspend fun processOperation(operation: SyncOperationEntity): Result<Unit> {
    return try {
        when (operation.operationType) {
            "CREATE" -> processCreateOperation(operation)
            "UPDATE" -> processUpdateOperation(operation)
            "DELETE" -> processDeleteOperation(operation)
            else -> Result.failure(Exception("Tipo de operação desconhecido: ${operation.operationType}"))
        }
    } catch (e: Exception) {
        Log.e(TAG, "Erro ao processar operação: ${e.message}", e)
        Result.failure(e)
    }
}

private suspend fun processCreateOperation(operation: SyncOperationEntity): Result<Unit> {
    // Implementar lógica de criação baseada no entityType
    // Usar entityToMap e enviar para Firestore
    return Result.success(Unit)
}

private suspend fun processUpdateOperation(operation: SyncOperationEntity): Result<Unit> {
    // Implementar lógica de atualização
    return Result.success(Unit)
}

private suspend fun processDeleteOperation(operation: SyncOperationEntity): Result<Unit> {
    // Implementar lógica de deleção
    return Result.success(Unit)
}
```

#### **3.3 Integrar com syncPush()**
```kotlin
suspend fun syncPush(): Result<Unit> {
    return try {
        if (!networkUtils.isConnected()) {
            Log.w(TAG, "Sincronização Push cancelada: dispositivo offline - operações enfileiradas")
            return Result.failure(Exception("Dispositivo offline - operações enfileiradas"))
        }
        
        // Processar fila primeiro
        processSyncQueue()
        
        // Depois executar push normal
        // ... código existente ...
    } catch (e: Exception) {
        // ...
    }
}
```

### **4. Adicionar Limpeza Automática**

Adicionar método para limpar operações antigas completadas:

```kotlin
suspend fun limparOperacoesAntigas() {
    try {
        appRepository.limparOperacoesSyncCompletadas(dias = 7)
        Log.d(TAG, "Operações antigas limpas")
    } catch (e: Exception) {
        Log.e(TAG, "Erro ao limpar operações antigas: ${e.message}", e)
    }
}
```

### **5. Integrar com WorkManager**

Atualizar `SyncWorker.kt` para processar fila:

```kotlin
override suspend fun doWork(): Result {
    return try {
        val syncRepository = RepositoryFactory.getSyncRepository(applicationContext)
        
        // Processar fila primeiro
        syncRepository.processSyncQueue()
        
        // Depois sincronização normal
        val result = syncRepository.syncBidirectional()
        
        // Limpar operações antigas
        syncRepository.limparOperacoesAntigas()
        
        if (result.isSuccess) {
            Result.success()
        } else {
            Result.retry()
        }
    } catch (e: Exception) {
        Result.retry()
    }
}
```

## 🔧 PADRÕES E CONVENÇÕES

### **Estrutura de Dados**
- **JSON Serializado**: Usar Gson para serializar/deserializar entidades
- **Timestamps**: Usar `System.currentTimeMillis()` para timestamps locais
- **Retry Logic**: Máximo de 3 tentativas por padrão

### **Tratamento de Erros**
- Usar `Result<T>` para retornar sucesso/falha
- Logar erros detalhadamente
- Marcar operações como FAILED após max retries

### **Performance**
- Processar em lotes (10 operações por vez)
- Limpar operações completadas após 7 dias
- Atualizar status apenas quando necessário

## ⚠️ OBSERVAÇÕES IMPORTANTES

1. **Migração de Banco**: Incrementar versão do banco ao adicionar nova entidade
2. **Idempotência**: Operações devem ser idempotentes
3. **Atomicidade**: Usar transações quando necessário
4. **Logs**: Adicionar logs detalhados para debugging
5. **Testes**: Testar fila offline/online

## 🎯 ENTREGÁVEIS

1. ✅ Entidade `SyncOperationEntity` criada
2. ✅ DAO `SyncOperationDao` criado
3. ✅ Métodos adicionados ao `AppDatabase`
4. ✅ Métodos adicionados ao `AppRepository`
5. ✅ `enqueueOperation()` implementado
6. ✅ `processSyncQueue()` implementado
7. ✅ `processOperation()` e métodos auxiliares implementados
8. ✅ Integração com `syncPush()` e `SyncWorker`
9. ✅ Limpeza automática implementada
10. ✅ Build passando sem erros

## 📚 REFERÊNCIAS

- Arquitetura: `.cursor/rules/2-ARQUITETURA-TECNICA.md`
- Status: `.cursor/rules/1-STATUS-ATUAL-PROJETO.md`
- Room Database: https://developer.android.com/training/data-storage/room
- WorkManager: https://developer.android.com/topic/libraries/architecture/workmanager

