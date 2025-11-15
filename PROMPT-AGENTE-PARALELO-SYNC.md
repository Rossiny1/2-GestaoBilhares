# PROMPT PARA AGENTE PARALELO - IMPLEMENTAÇÃO DE SINCRONIZAÇÃO

## 🎯 OBJETIVO

Implementar métodos de sincronização (pull/push) para **entidades faltantes** no `SyncRepository.kt`, seguindo o padrão já estabelecido no projeto.

---

## 📋 CONTEXTO

Estamos completando a sincronização offline-first que foi parcialmente implementada. O `SyncRepository.kt` já tem 10 entidades implementadas, mas faltam **18 entidades** que existiam no código antigo (`SyncManagerV2` do commit `7feb452b`).

**Arquivo principal**: `app/src/main/java/com/example/gestaobilhares/data/repository/domain/SyncRepository.kt`

---

## ✅ PADRÃO A SEGUIR

### 1. **Estrutura de Pull Method**
```kotlin
private suspend fun pull[NomeEntidade](): Result<Int> {
    return try {
        Log.d(TAG, "🔵 Iniciando pull de [nome_entidade]...")
        val collectionPath = getCollectionPath(COLLECTION_[NOME])
        val snapshot = firestore.collection(collectionPath).get().await()
        Log.d(TAG, "📥 Total de [nome_entidade] no Firestore: ${snapshot.size()}")

        var syncCount = 0
        var skipCount = 0
        var errorCount = 0

        snapshot.documents.forEach { doc ->
            try {
                val data = doc.data ?: emptyMap()
                Log.d(TAG, "📄 Processando [nome_entidade]: ID=${doc.id}")

                // ✅ CONVERSÃO MANUAL (não usar apenas Gson)
                // - Converter Timestamp para Date/Long
                // - Tratar campos opcionais
                // - Suportar camelCase e snake_case
                // - Incluir roomId e id do documento

                val [entidade] = [NomeEntidade](
                    id = (data["roomId"] as? Long) ?: (data["id"] as? Long) ?: doc.id.toLongOrNull() ?: 0L,
                    // ... outros campos com conversão manual
                )

                // ✅ Usar OnConflictStrategy.REPLACE
                appRepository.inserir[NomeEntidade]([entidade])
                syncCount++
                Log.d(TAG, "✅ [NomeEntidade] sincronizado: ID=${[entidade].id}")
            } catch (e: Exception) {
                errorCount++
                Log.e(TAG, "❌ Erro ao processar [nome_entidade] ${doc.id}: ${e.message}", e)
            }
        }

        Log.d(TAG, "✅ Pull de [nome_entidade] concluído: $syncCount sincronizados, $skipCount ignorados, $errorCount erros")
        Result.success(syncCount)
    } catch (e: Exception) {
        Log.e(TAG, "❌ Erro no pull de [nome_entidade]: ${e.message}", e)
        Result.failure(e)
    }
}
```

### 2. **Estrutura de Push Method**
```kotlin
private suspend fun push[NomeEntidade](): Result<Int> {
    return try {
        Log.d(TAG, "🔵 Iniciando push de [nome_entidade]...")
        val [entidades]Locais = appRepository.obterTodos[NomeEntidade]().first()
        Log.d(TAG, "📥 Total de [nome_entidade] locais encontradas: ${[entidades]Locais.size}")

        var syncCount = 0
        var errorCount = 0

        [entidades]Locais.forEach { [entidade] ->
            try {
                Log.d(TAG, "📄 Processando [nome_entidade]: ID=${[entidade].id}")

                val [entidade]Map = entityToMap([entidade])
                Log.d(TAG, "   Mapa criado com ${[entidade]Map.size} campos")

                // ✅ CRÍTICO: Adicionar roomId para compatibilidade com pull
                [entidade]Map["roomId"] = [entidade].id
                [entidade]Map["id"] = [entidade].id

                // Adicionar metadados de sincronização
                [entidade]Map["lastModified"] = FieldValue.serverTimestamp()
                [entidade]Map["syncTimestamp"] = FieldValue.serverTimestamp()

                val documentId = [entidade].id.toString()
                val collectionPath = getCollectionPath(COLLECTION_[NOME])
                Log.d(TAG, "   Enviando para Firestore: collection=$collectionPath, document=$documentId")

                firestore.collection(collectionPath)
                    .document(documentId)
                    .set([entidade]Map)
                    .await()

                syncCount++
                Log.d(TAG, "✅ [NomeEntidade] enviado com sucesso: ID=${[entidade].id}")
            } catch (e: Exception) {
                errorCount++
                Log.e(TAG, "❌ Erro ao enviar [nome_entidade] ${[entidade].id}: ${e.message}", e)
            }
        }

        Log.d(TAG, "✅ Push de [nome_entidade] concluído: $syncCount enviadas, $errorCount erros")
        Result.success(syncCount)
    } catch (e: Exception) {
        Log.e(TAG, "❌ Erro no push de [nome_entidade]: ${e.message}", e)
        Result.failure(e)
    }
}
```

### 3. **Constantes de Coleção**
Adicionar no `companion object`:
```kotlin
private const val COLLECTION_[NOME] = "[nome_colecao]"
```

### 4. **Atualizar syncPull() e syncPush()**
Adicionar chamadas na ordem correta de dependências (ver `ANALISE-ENTIDADES-SYNC.md`).

---

## 🎯 SUAS TAREFAS (AGENTE PARALELO)

### **ENTIDADES PARA VOCÊ IMPLEMENTAR:**

#### **Prioridade MÉDIA (5 entidades):**
1. **PanoEstoque**
   - Collection: `"panos_estoque"` ou `"pano_estoque"`
   - Métodos AppRepository: `obterTodosPanosEstoque()`, `inserirPanoEstoque()`
   - Verificar estrutura da entidade `PanoEstoque`

2. **MesaVendida**
   - Collection: `"mesas_vendidas"` ou `"mesa_vendida"`
   - Métodos AppRepository: `obterTodasMesasVendidas()`, `inserirMesaVendida()`
   - Verificar estrutura da entidade `MesaVendida`

3. **StockItem**
   - Collection: `"stock_items"` ou `"stock_item"`
   - Métodos AppRepository: `obterTodosStockItems()`, `inserirStockItem()`
   - Verificar estrutura da entidade `StockItem`

4. **MesaReformada**
   - Collection: `"mesas_reformadas"` ou `"mesa_reformada"`
   - Métodos AppRepository: `inserirMesaReformada()` (verificar se tem método de listagem)
   - Verificar estrutura da entidade `MesaReformada`

5. **PanoMesa**
   - Collection: `"pano_mesas"` ou `"pano_mesa"`
   - Métodos AppRepository: **PRECISA VERIFICAR** - pode não ter métodos ainda
   - Verificar estrutura da entidade `PanoMesa`

#### **Prioridade BAIXA (3 entidades):**
6. **HistoricoManutencaoMesa**
   - Collection: `"historico_manutencao_mesa"` ou similar
   - Métodos AppRepository: `obterTodosHistoricoManutencaoMesa()`, `inserirHistoricoManutencaoMesa()`
   - Verificar estrutura da entidade `HistoricoManutencaoMesa`

7. **HistoricoManutencaoVeiculo**
   - Collection: `"historico_manutencao_veiculo"` ou similar
   - Métodos AppRepository: `inserirHistoricoManutencao()` (verificar se tem método de listagem)
   - Verificar estrutura da entidade `HistoricoManutencaoVeiculo`

8. **HistoricoCombustivelVeiculo**
   - Collection: `"historico_combustivel_veiculo"` ou similar
   - Métodos AppRepository: `inserirHistoricoCombustivel()` (verificar se tem método de listagem)
   - Verificar estrutura da entidade `HistoricoCombustivelVeiculo`

---

## ⚠️ REGRAS CRÍTICAS

1. **NUNCA** use apenas `gson.fromJson()` - sempre faça conversão manual campo por campo
2. **SEMPRE** inclua `roomId` e `id` no push (para compatibilidade com pull)
3. **SEMPRE** use `OnConflictStrategy.REPLACE` nos DAOs
4. **SEMPRE** use `getCollectionPath()` para construir caminhos Firestore
5. **SEMPRE** adicione logs detalhados (🔵, 📥, 📄, ✅, ❌)
6. **SEMPRE** trate `Timestamp` do Firestore convertendo para `Date` ou `Long`
7. **SEMPRE** suporte campos opcionais com fallbacks
8. **SEMPRE** verifique se os métodos do AppRepository existem antes de usar

---

## 📝 CHECKLIST ANTES DE FINALIZAR

- [ ] Métodos `pull[NomeEntidade]()` implementados
- [ ] Métodos `push[NomeEntidade]()` implementados
- [ ] Constantes `COLLECTION_[NOME]` adicionadas
- [ ] Chamadas adicionadas em `syncPull()` na ordem correta
- [ ] Chamadas adicionadas em `syncPush()` na ordem correta
- [ ] Logs detalhados adicionados
- [ ] Conversão manual de dados implementada (não apenas Gson)
- [ ] `roomId` e `id` incluídos no push
- [ ] Tratamento de erros implementado
- [ ] Verificado se métodos AppRepository existem

---

## 🔍 COMO VERIFICAR ESTRUTURAS

1. Buscar entidade: `grep -r "class PanoEstoque\|data class PanoEstoque" app/src/main/java/`
2. Buscar DAO: `grep -r "interface PanoEstoqueDao\|abstract class PanoEstoqueDao" app/src/main/java/`
3. Buscar métodos AppRepository: `grep -r "PanoEstoque\|panoEstoque" app/src/main/java/com/example/gestaobilhares/data/repository/AppRepository.kt`

---

## 🎯 ORDEM DE IMPLEMENTAÇÃO SUGERIDA

1. PanoEstoque (mais simples)
2. MesaVendida
3. StockItem
4. MesaReformada
5. HistoricoManutencaoMesa
6. PanoMesa (pode precisar verificar métodos)
7. HistoricoManutencaoVeiculo (pode precisar verificar métodos)
8. HistoricoCombustivelVeiculo (pode precisar verificar métodos)

---

## 📚 REFERÊNCIAS

- **Arquivo principal**: `app/src/main/java/com/example/gestaobilhares/data/repository/domain/SyncRepository.kt`
- **Análise completa**: `ANALISE-ENTIDADES-SYNC.md`
- **Exemplos de implementação**: Ver métodos `pullClientes()`, `pushRotas()`, etc. no SyncRepository.kt

---

## ✅ ENTREGÁVEIS

Ao finalizar, você deve ter:
1. ✅ 8 métodos `pull` implementados
2. ✅ 8 métodos `push` implementados
3. ✅ 8 constantes de coleção adicionadas
4. ✅ `syncPull()` atualizado com todas as chamadas
5. ✅ `syncPush()` atualizado com todas as chamadas
6. ✅ Código seguindo exatamente o padrão estabelecido

---

**IMPORTANTE**: Trabalhe de forma harmônica com o outro agente. Se você ver que ele já implementou algo, não duplique. Foque nas suas 8 entidades e siga o padrão estabelecido.

