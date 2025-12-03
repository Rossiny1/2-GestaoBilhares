# Análise: Problemas de Sincronização - Ciclo e Clientes

## Data: 2025-01-XX

## Commit de Referência: cc27db6

---

## 🔍 PROBLEMAS IDENTIFICADOS

### 1. **Ciclo não refletindo corretamente entre telas**

**Sintoma:** O ciclo exibido no card da tela "Rotas" não corresponde ao ciclo exibido na tela "Clientes da Rota".

**Causa Raiz Identificada:**

- O `RotaResumo` é calculado usando `obterCicloAtualRota()` que busca o ciclo em andamento ou o último finalizado
- Quando um novo ciclo é iniciado em `ClientListViewModel.iniciarRota()`, a entidade `Rota` é atualizada com `cicloAcertoAtual`, mas o `RotaResumo` pode não estar reagindo imediatamente
- O `combine()` em `RotaRepository.getRotasResumoComAtualizacaoTempoReal()` observa mudanças em `rotas`, `ciclos` e `clientes`, mas pode haver um delay na propagação

**Localização do Problema:**

- `data/src/main/java/com/example/gestaobilhares/data/repository/domain/RotaRepository.kt:44-75`
- `ui/src/main/java/com/example/gestaobilhares/ui/clients/ClientListViewModel.kt:452-519`
- `data/src/main/java/com/example/gestaobilhares/data/repository/AppRepository.kt:358-381`

### 2. **Clientes desaparecendo após sincronização**

**Sintoma:** Após sincronizar, os clientes de uma rota sincronizada desaparecem do app.

**Causa Raiz Identificada:**

- A lógica de preservação de clientes locais existe em `pullClientesComplete()` (linhas 2298-2321), mas **NÃO existe em `tryPullClientesIncremental()`**
- O método incremental processa apenas documentos do Firestore e não verifica se há clientes locais que devem ser preservados
- Quando uma rota é sincronizada pela primeira vez, o `accessibleRouteIdsCache` pode não estar atualizado no momento do pull de clientes, causando filtragem incorreta

**Localização do Problema:**

- `sync/src/main/java/com/example/gestaobilhares/sync/SyncRepository.kt:2172-2242` (método incremental não preserva clientes locais)
- `sync/src/main/java/com/example/gestaobilhares/sync/SyncRepository.kt:2248-2340` (método completo tem preservação, mas pode não estar funcionando corretamente)

### 3. **Dados não sendo espelho (inconsistência temporária)**

**Sintoma:** Às vezes os dados não refletem mudanças imediatamente.

**Causa Raiz Identificada:**

- O `obterCicloAtualRota()` usa `runBlocking`, o que pode causar bloqueios e atrasos
- O `combine()` pode não estar disparando corretamente quando apenas a entidade `Rota` é atualizada (sem mudança em `ciclos`)
- A atualização da rota em `iniciarRota()` pode não estar disparando o Flow corretamente

**Localização do Problema:**

- `data/src/main/java/com/example/gestaobilhares/data/repository/AppRepository.kt:358-381` (uso de `runBlocking`)
- `data/src/main/java/com/example/gestaobilhares/data/repository/domain/RotaRepository.kt:44-75` (combine pode não estar reagindo)

---

## 🔧 CORREÇÕES PROPOSTAS

### Correção 1: Adicionar preservação de clientes locais no método incremental

**Arquivo:** `sync/src/main/java/com/example/gestaobilhares/sync/SyncRepository.kt`

**Mudança:** Adicionar a mesma lógica de preservação de clientes locais que existe no método completo ao final do método `tryPullClientesIncremental()`, logo antes de salvar a metadata.

```kotlin
// Após processar todos os documentos, antes de salvar metadata
// ✅ CORREÇÃO CRÍTICA: Verificar se há clientes locais que não estão no Firestore mas pertencem a rotas acessíveis
val clientesFirestoreIds = documents.mapNotNull { doc ->
    doc.id.toLongOrNull()
        ?: (doc.data?.get("roomId") as? Number)?.toLong()
        ?: (doc.data?.get("id") as? Number)?.toLong()
}.toSet()

val clientesLocaisPreservados = todosClientes.filter { clienteLocal ->
    clienteLocal.id !in clientesFirestoreIds && shouldSyncRouteData(clienteLocal.rotaId, allowUnknown = false)
}

if (clientesLocaisPreservados.isNotEmpty()) {
    Log.d(TAG, "   ✅ [INCREMENTAL] Preservando ${clientesLocaisPreservados.size} clientes locais que não estão no Firestore mas pertencem a rotas acessíveis")
    clientesLocaisPreservados.forEach { cliente ->
        try {
            val clienteExistente = appRepository.obterClientePorId(cliente.id)
            if (clienteExistente == null) {
                Log.w(TAG, "   ⚠️ [INCREMENTAL] Cliente ${cliente.nome} (ID=${cliente.id}) foi removido - re-inserindo")
                appRepository.inserirCliente(cliente)
            }
        } catch (e: Exception) {
            Log.e(TAG, "   ❌ [INCREMENTAL] Erro ao verificar/preservar cliente ${cliente.nome}: ${e.message}")
        }
    }
}
```

### Correção 2: Garantir que o ciclo seja atualizado na rota durante sincronização

**Arquivo:** `sync/src/main/java/com/example/gestaobilhares/sync/SyncRepository.kt`

**Mudança:** Melhorar a lógica de preservação do ciclo na rota durante `processRotaDocument()`. A lógica atual preserva o ciclo local se for maior, mas não garante que o ciclo em andamento seja sempre preservado.

```kotlin
// Na função processRotaDocument(), após linha 2752
val cicloLocalEmAndamento = appRepository.buscarCicloAtivo(roomId)
val cicloLocalMaior = localRota.cicloAcertoAtual > (cicloAcertoAtualFirestore ?: 0)

// ✅ CORREÇÃO: SEMPRE preservar ciclo local se houver ciclo em andamento
// OU se o ciclo local for maior que o do servidor
val rotaFinal = if (cicloLocalEmAndamento != null || cicloLocalMaior) {
    // Preservar ciclo local
    rotaFirestore.copy(
        cicloAcertoAtual = localRota.cicloAcertoAtual,
        anoCiclo = localRota.anoCiclo,
        dataInicioCiclo = localRota.dataInicioCiclo,
        dataFimCiclo = localRota.dataFimCiclo,
        statusAtual = localRota.statusAtual,
        dataAtualizacao = maxOf(localRota.dataAtualizacao, serverTimestamp)
    ).also {
        Log.d(TAG, "🔄 Rota sincronizada PRESERVANDO ciclo local: ${it.nome} (Ciclo local: ${localRota.cicloAcertoAtual}, Em andamento: ${cicloLocalEmAndamento != null})")
    }
} else {
    rotaFirestore
}
```

### Correção 3: Remover runBlocking e tornar obterCicloAtualRota reativo

**Arquivo:** `data/src/main/java/com/example/gestaobilhares/data/repository/AppRepository.kt`

**Mudança:** Converter `obterCicloAtualRota()` para usar Flow em vez de `runBlocking`, ou pelo menos garantir que seja chamado de forma assíncrona.

**Problema:** O `runBlocking` pode causar bloqueios e atrasos na atualização do `RotaResumo`.

**Solução Alternativa (mais simples):** Manter `runBlocking` mas garantir que o `combine()` em `RotaRepository` seja disparado corretamente quando a rota é atualizada. Isso já está sendo feito, mas podemos melhorar.

### Correção 4: Garantir atualização imediata do RotaResumo ao iniciar ciclo

**Arquivo:** `ui/src/main/java/com/example/gestaobilhares/ui/clients/ClientListViewModel.kt`

**Mudança:** Após atualizar a rota com o novo ciclo, forçar uma atualização do Flow de rotas. Isso já está sendo feito (linhas 504-513), mas podemos melhorar garantindo que o `dataAtualizacao` seja sempre atualizado.

**Verificação:** O código atual já atualiza `dataAtualizacao` e `cicloAcertoAtual`. O problema pode ser que o `combine()` não está reagindo porque a entidade `Rota` não está sendo "tocada" de forma que dispare o Flow.

**Solução:** Garantir que após inserir o ciclo, a rota seja atualizada de forma que o Flow seja disparado. Isso já está sendo feito, mas podemos adicionar um pequeno delay ou garantir que a atualização seja feita de forma assíncrona.

---

## 📋 RESUMO DAS MUDANÇAS

### Prioridade ALTA (Crítico)

1. ✅ **Adicionar preservação de clientes locais no método incremental** - Isso resolve o problema de clientes desaparecendo
2. ✅ **Melhorar lógica de preservação de ciclo durante sincronização** - Garante que ciclos em andamento sejam sempre preservados

### Prioridade MÉDIA (Importante)

3. ⚠️ **Garantir atualização imediata do RotaResumo** - Melhora a consistência entre telas
4. ⚠️ **Otimizar obterCicloAtualRota para evitar runBlocking** - Melhora performance e reatividade

---

## 🧪 TESTES NECESSÁRIOS

Após implementar as correções:

1. **Teste de Sincronização de Clientes:**
   - Criar clientes localmente em uma rota
   - Sincronizar
   - Verificar se os clientes permanecem visíveis

2. **Teste de Consistência de Ciclo:**
   - Iniciar um novo ciclo na tela "Clientes da Rota"
   - Verificar se o card na tela "Rotas" atualiza imediatamente
   - Sincronizar e verificar se o ciclo permanece correto

3. **Teste de Sincronização Incremental:**
   - Fazer uma sincronização completa
   - Criar clientes localmente
   - Fazer uma sincronização incremental
   - Verificar se os clientes locais são preservados

---

## ✅ IMPLEMENTAÇÃO

**Status:** ✅ **CORREÇÕES IMPLEMENTADAS**

### Correções Aplicadas

1. ✅ **Preservação de clientes locais no método incremental**
   - Arquivo: `sync/src/main/java/com/example/gestaobilhares/sync/SyncRepository.kt`
   - Linhas: 2227-2250 (após processar documentos, antes de salvar metadata)
   - Implementado: Lógica completa de preservação de clientes locais que não estão no Firestore mas pertencem a rotas acessíveis

2. ✅ **Melhoria na preservação de ciclo durante sincronização**
   - Arquivo: `sync/src/main/java/com/example/gestaobilhares/sync/SyncRepository.kt`
   - Linhas: 2749-2770
   - Implementado: Lógica melhorada que SEMPRE preserva ciclo local se houver ciclo em andamento OU se o ciclo local for maior, com logs detalhados

3. ✅ **Garantir atualização imediata do RotaResumo ao iniciar ciclo**
   - Arquivo: `ui/src/main/java/com/example/gestaobilhares/ui/clients/ClientListViewModel.kt`
   - Linhas: 500-519
   - Implementado: Atualização completa da rota incluindo ciclo, ano, datas e status para garantir que o Flow seja disparado corretamente

**Próximos Passos:**

1. ✅ Correções implementadas
2. ⏳ Testes e validação pelo usuário
3. ⏳ Verificar se problemas foram resolvidos
