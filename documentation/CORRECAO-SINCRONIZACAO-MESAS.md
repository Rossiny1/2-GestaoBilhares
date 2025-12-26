# 🔧 Correção: Mesa Desaparecendo Após Sincronização

## 📋 Problema Identificado

**Sintoma:** Após inserir uma mesa e gerar um aditivo, tudo funciona corretamente. Porém, após sincronizar, a mesa desaparece do cliente e retorna para o depósito.

**Causa Raiz:** Durante a sincronização (pull), o código estava sobrescrevendo o `clienteId` local da mesa com o valor do Firestore. Se o Firestore ainda não tivesse o `clienteId` atualizado (por exemplo, se a sincronização acontecesse antes do push ser concluído, ou se houvesse uma condição de corrida), a mesa era desvinculada do cliente.

## 🔍 Análise Técnica

### Código Problemático

No método `tryPullMesasIncremental()`, o código estava:

1. ❌ **Não definindo `mesaLocal`**: A variável `mesaLocal` era usada mas nunca definida
2. ❌ **Não preservando `clienteId` local**: A mesa do Firestore era inserida diretamente sem verificar se o `clienteId` local deveria ser preservado

```kotlin
// ❌ CÓDIGO ANTIGO (PROBLEMÁTICO)
val mesaFirestore = gson.fromJson(mesaJson, Mesa::class.java)?.copy(id = mesaId)
// ... código truncado ...
val localTimestamp = mesaLocal?.dataUltimaLeitura?.time  // ❌ mesaLocal não estava definido!
// ... código truncado que inseriria mesaFirestore diretamente
```

### Fluxo do Problema

1. ✅ Usuário adiciona mesa ao cliente via aditivo
2. ✅ Mesa é atualizada localmente com `clienteId` do contrato
3. ✅ Mesa é enviada para o Firestore (push)
4. ⚠️ **PROBLEMA**: Se a sincronização (pull) acontecer antes do push ser concluído, ou se o Firestore ainda tiver o valor antigo (null), a mesa é sobrescrita
5. ❌ Mesa perde o `clienteId` e volta para o depósito

## ✅ Solução Implementada

### Correção Aplicada

Adicionada lógica para preservar o `clienteId` local quando ele existir e o Firestore não tiver:

```kotlin
// ✅ CÓDIGO CORRIGIDO
val mesaLocal = mesasCache[mesaId]  // ✅ Definir mesaLocal do cache

// ✅ Preservar clienteId local se existir e o Firestore não tiver
val clienteIdParaSalvar = when {
    // Se a mesa local tem clienteId e o Firestore não tem (null ou 0), preservar local
    mesaLocal?.clienteId != null && mesaLocal.clienteId > 0L && 
    (mesaFirestore.clienteId == null || mesaFirestore.clienteId <= 0L) -> {
        Timber.tag(TAG).d("🛡️ Preservando clienteId local para mesa ${mesaId}: ${mesaLocal.clienteId}")
        mesaLocal.clienteId
    }
    // Se ambos têm clienteId, usar o do Firestore (servidor é fonte da verdade após push)
    mesaFirestore.clienteId != null && mesaFirestore.clienteId > 0L -> {
        mesaFirestore.clienteId
    }
    // Se local tem e Firestore tem null/0, preservar local
    mesaLocal?.clienteId != null && mesaLocal.clienteId > 0L -> {
        Timber.tag(TAG).d("🛡️ Preservando clienteId local para mesa ${mesaId}: ${mesaLocal.clienteId}")
        mesaLocal.clienteId
    }
    // Caso padrão: usar o do Firestore (pode ser null)
    else -> mesaFirestore.clienteId
}

val mesaParaSalvar = mesaFirestore.copy(clienteId = clienteIdParaSalvar)
```

### Lógica de Preservação

A correção segue o mesmo padrão usado para preservar `dataReforma` em `mesaReformada`:

1. **Prioridade 1**: Se a mesa local tem `clienteId` e o Firestore não tem → **Preservar local**
2. **Prioridade 2**: Se ambos têm `clienteId` → **Usar do Firestore** (servidor é fonte da verdade após push bem-sucedido)
3. **Prioridade 3**: Se local tem e Firestore tem null/0 → **Preservar local**
4. **Fallback**: Usar o do Firestore (pode ser null para mesas no depósito)

## 📝 Arquivos Modificados

- `sync/src/main/java/com/example/gestaobilhares/sync/SyncRepository.kt`
  - Método: `tryPullMesasIncremental()`
  - Linhas: ~2652-2706

## 🧪 Como Testar

1. **Cenário 1: Aditivo + Sincronização Imediata**
   - Adicionar uma mesa a um cliente via aditivo
   - Sincronizar imediatamente após
   - ✅ **Esperado**: Mesa permanece vinculada ao cliente

2. **Cenário 2: Aditivo + Sincronização Tardia**
   - Adicionar uma mesa a um cliente via aditivo
   - Aguardar alguns segundos
   - Sincronizar
   - ✅ **Esperado**: Mesa permanece vinculada ao cliente

3. **Cenário 3: Mesa no Depósito**
   - Verificar mesas no depósito (sem clienteId)
   - Sincronizar
   - ✅ **Esperado**: Mesas permanecem no depósito

4. **Cenário 4: Retirada de Mesa**
   - Retirar uma mesa de um cliente (via aditivo de retirada)
   - Sincronizar
   - ✅ **Esperado**: Mesa volta para o depósito (clienteId = null)

## ⚠️ Observações Importantes

1. **Não relacionado aos erros do Crashlytics**: Este problema não está relacionado aos erros listados no relatório do Crashlytics. É um problema de lógica de sincronização.

2. **Preservação Inteligente**: A correção preserva o `clienteId` local apenas quando o Firestore não tem esse valor. Se o Firestore tiver um `clienteId` válido, ele será usado (servidor é fonte da verdade após push bem-sucedido).

3. **Compatibilidade**: A correção é compatível com o comportamento existente para mesas no depósito (clienteId = null).

## 🔗 Relacionado

- **Padrão Similar**: `mesaReformada` preserva `dataReforma` local da mesma forma
- **Documentação**: Ver `documentation/RELATORIO-CRASHLYTICS.md` para outros problemas identificados

---

**Data da Correção:** 23 de Dezembro de 2025  
**Status:** ✅ Implementado e pronto para teste

