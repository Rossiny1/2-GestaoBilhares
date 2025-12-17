# TAREFAS PARA AGENTE PARALELO: IMPLEMENTAÇÃO DE HANDLERS DE SINCRONIZAÇÃO

## 🎯 OBJETIVO

Implementar os handlers de pull/push específicos por entidade no `SyncRepository`, seguindo a arquitetura híbrida modular estabelecida.

## 📋 CONTEXTO

- **SyncRepository**: Estrutura base criada em `app/src/main/java/com/example/gestaobilhares/data/repository/domain/SyncRepository.kt`
- **AppRepository**: Facade centralizado que será usado pelos handlers
- **Firebase Firestore**: Configurado e pronto para uso
- **Arquitetura**: Offline-first, sincronização bidirecional (Pull/Push)

## ✅ TAREFAS

### **1. Implementar Pull Handlers (Servidor → Local)**

Implementar métodos privados no `SyncRepository` para sincronizar dados do Firestore para o Room:

#### **1.1 Pull Clientes**
```kotlin
private suspend fun pullClientes(): Result<Int> {
    // 1. Buscar clientes do Firestore (collection "clientes")
    // 2. Para cada cliente:
    //    - Verificar se existe localmente (por ID ou CPF/CNPJ)
    //    - Se não existe: inserir no Room via appRepository
    //    - Se existe: comparar timestamps (última modificação)
    //      - Se servidor mais recente: atualizar no Room
    //      - Se local mais recente: manter local (conflito será resolvido no push)
    // 3. Retornar número de clientes sincronizados
}
```

#### **1.2 Pull Acertos**
```kotlin
private suspend fun pullAcertos(): Result<Int> {
    // Similar ao pullClientes, mas para acertos
    // Importante: Sincronizar também AcertoMesa relacionados
}
```

#### **1.3 Pull Mesas**
```kotlin
private suspend fun pullMesas(): Result<Int> {
    // Similar ao pullClientes, mas para mesas
}
```

#### **1.4 Pull Rotas**
```kotlin
private suspend fun pullRotas(): Result<Int> {
    // Similar ao pullClientes, mas para rotas
}
```

#### **1.5 Pull Despesas**
```kotlin
private suspend fun pullDespesas(): Result<Int> {
    // Similar ao pullClientes, mas para despesas
}
```

#### **1.6 Pull Ciclos**
```kotlin
private suspend fun pullCiclos(): Result<Int> {
    // Similar ao pullClientes, mas para ciclos
}
```

#### **1.7 Pull Colaboradores**
```kotlin
private suspend fun pullColaboradores(): Result<Int> {
    // Similar ao pullClientes, mas para colaboradores
}
```

#### **1.8 Pull Contratos**
```kotlin
private suspend fun pullContratos(): Result<Int> {
    // Similar ao pullClientes, mas para contratos
    // Importante: Sincronizar também Aditivos e Assinaturas relacionados
}
```

### **2. Implementar Push Handlers (Local → Servidor)**

Implementar métodos privados no `SyncRepository` para enviar dados do Room para o Firestore:

#### **2.1 Push Clientes**
```kotlin
private suspend fun pushClientes(): Result<Int> {
    // 1. Buscar clientes locais modificados (campo syncTimestamp ou similar)
    // 2. Para cada cliente modificado:
    //    - Serializar para JSON
    //    - Enviar para Firestore (collection "clientes")
    //    - Atualizar syncTimestamp local
    // 3. Retornar número de clientes sincronizados
}
```

#### **2.2 Push Acertos**
```kotlin
private suspend fun pushAcertos(): Result<Int> {
    // Similar ao pushClientes, mas para acertos
    // Importante: Enviar também AcertoMesa relacionados
}
```

#### **2.3 Push Mesas**
```kotlin
private suspend fun pushMesas(): Result<Int> {
    // Similar ao pushClientes, mas para mesas
}
```

#### **2.4 Push Rotas**
```kotlin
private suspend fun pushRotas(): Result<Int> {
    // Similar ao pushClientes, mas para rotas
}
```

#### **2.5 Push Despesas**
```kotlin
private suspend fun pushDespesas(): Result<Int> {
    // Similar ao pushClientes, mas para despesas
}
```

#### **2.6 Push Ciclos**
```kotlin
private suspend fun pushCiclos(): Result<Int> {
    // Similar ao pushClientes, mas para ciclos
}
```

#### **2.7 Push Colaboradores**
```kotlin
private suspend fun pushColaboradores(): Result<Int> {
    // Similar ao pushClientes, mas para colaboradores
}
```

#### **2.8 Push Contratos**
```kotlin
private suspend fun pushContratos(): Result<Int> {
    // Similar ao pushClientes, mas para contratos
    // Importante: Enviar também Aditivos e Assinaturas relacionados
}
```

### **3. Atualizar Métodos Principais**

Atualizar os métodos `syncPull()` e `syncPush()` no `SyncRepository` para chamar os handlers:

```kotlin
suspend fun syncPull(): Result<Unit> {
    // Chamar todos os pull handlers em sequência
    // Atualizar _syncStatus com progresso
}

suspend fun syncPush(): Result<Unit> {
    // Chamar todos os push handlers em sequência
    // Atualizar _syncStatus com progresso
}
```

## 🔧 PADRÕES E CONVENÇÕES

### **Estrutura de Dados no Firestore**

- **Collection**: Nome da entidade em minúsculas (ex: "clientes", "acertos")
- **Document ID**: ID da entidade (Long convertido para String)
- **Campos**: Mesmos campos da entidade Room, com timestamps adicionais:
  - `lastModified`: Timestamp da última modificação
  - `syncTimestamp`: Timestamp da última sincronização
  - `createdAt`: Timestamp de criação

### **Resolução de Conflitos**

- **Estratégia**: Última escrita vence (Last Write Wins)
- **Comparação**: Usar `lastModified` para determinar qual versão é mais recente
- **Pull**: Se servidor mais recente → atualizar local
- **Push**: Se local mais recente → atualizar servidor

### **Tratamento de Erros**

- Usar `Result<T>` para retornar sucesso/falha
- Logar erros com `Log.e(TAG, ...)`
- Continuar sincronização mesmo se um handler falhar
- Atualizar `_syncStatus` com erros específicos

### **Performance**

- Processar em lotes (batch operations)
- Usar transações do Firestore quando necessário
- Limitar número de documentos por batch (500 máximo)
- Usar coroutines para operações assíncronas

## 📝 EXEMPLO DE IMPLEMENTAÇÃO

```kotlin
private suspend fun pullClientes(): Result<Int> {
    return try {
        val clientesSnapshot = firestore.collection(COLLECTION_CLIENTES)
            .get()
            .await()
        
        var syncCount = 0
        clientesSnapshot.documents.forEach { doc ->
            try {
                val clienteFirestore = doc.toObject(Cliente::class.java)
                    ?: return@forEach
                
                val clienteLocal = appRepository.obterClientePorId(clienteFirestore.id).first()
                
                when {
                    clienteLocal == null -> {
                        // Novo cliente: inserir
                        appRepository.inserirCliente(clienteFirestore)
                        syncCount++
                    }
                    clienteFirestore.lastModified > clienteLocal.lastModified -> {
                        // Servidor mais recente: atualizar
                        appRepository.atualizarCliente(clienteFirestore)
                        syncCount++
                    }
                    // Se local mais recente, manter local (conflito resolvido no push)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao sincronizar cliente ${doc.id}: ${e.message}", e)
            }
        }
        
        Result.success(syncCount)
    } catch (e: Exception) {
        Log.e(TAG, "Erro no pull de clientes: ${e.message}", e)
        Result.failure(e)
    }
}
```

## ⚠️ OBSERVAÇÕES IMPORTANTES

1. **Offline-first**: Handlers devem funcionar mesmo se Firestore estiver offline (usar cache)
2. **Idempotência**: Operações devem ser idempotentes (pode ser executada múltiplas vezes sem efeitos colaterais)
3. **Atomicidade**: Usar transações quando necessário para garantir consistência
4. **Logs**: Adicionar logs detalhados para debugging
5. **Testes**: Testar cada handler individualmente antes de integrar

## 🎯 ENTREGÁVEIS

1. ✅ Métodos `pull*()` implementados para todas as entidades
2. ✅ Métodos `push*()` implementados para todas as entidades
3. ✅ Métodos `syncPull()` e `syncPush()` atualizados
4. ✅ Tratamento de erros robusto
5. ✅ Logs detalhados
6. ✅ Build passando sem erros

## 📚 REFERÊNCIAS

- Arquitetura: `.cursor/rules/2-ARQUITETURA-TECNICA.md`
- Status: `.cursor/rules/1-STATUS-ATUAL-PROJETO.md`
- Firebase Firestore: https://firebase.google.com/docs/firestore
- Room Database: https://developer.android.com/training/data-storage/room

