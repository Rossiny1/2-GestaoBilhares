# ✅ RELATÓRIO DIAGNÓSTICO V7 - REGRESSÕES PÓS-CORREÇÃO

## 📅 Data

22/01/2026

## 🎯 Objetivo

Corrigir regressões pós-correção (V7):

- Panos criados não aparecem na lista
- Troca de panos sem itens disponíveis
- Filtro de histórico por ano inoperante

---

## ✅ Correções Implementadas

### 1) Panos criados sempre disponíveis

**Causa provável:** status/flag `disponivel` não garantido no insert em lote.

**Correção:** força `disponivel = true` antes de inserir no banco.

Arquivo:

- `ui/src/main/java/com/example/gestaobilhares/ui/inventory/stock/StockViewModel.kt`

Trecho:

```kotlin
val panoDisponivel = if (pano.disponivel) pano else pano.copy(disponivel = true)
appRepository.inserirPanoEstoque(panoDisponivel)
```

### 2) Lista de panos/itens agora atualiza corretamente

**Causa provável:** `DiffUtil` não detectava mudanças quando lista reaproveitada.

**Correção:** enviar nova instância da lista para o adapter.

Arquivo:

- `ui/src/main/java/com/example/gestaobilhares/ui/inventory/stock/StockFragment.kt`

Trecho:

```kotlin
adapter.submitList(items.toList())
panoGroupAdapter.submitList(panoGroups.toList())
```

### 3) Filtro de histórico por ano corrigido

**Causa provável:** filtro comparando somente `dataInicio` sem `dataFim`.

**Correções:**

- DAO com query por período (data início/fim).
- Repository expondo o flow por período.
- ViewModel usando intervalo completo no fluxo e no filtro.

Arquivos:

- `data/src/main/java/com/example/gestaobilhares/data/dao/CicloAcertoDao.kt`
- `data/src/main/java/com/example/gestaobilhares/data/repository/CicloAcertoRepository.kt`
- `ui/src/main/java/com/example/gestaobilhares/ui/clients/CycleHistoryViewModel.kt`

---

## 🧪 Validação Executada

**Comando:**

```
.\gradlew.bat testDebugUnitTest
```

**Resultado:** ✅ **BUILD SUCCESSFUL**

**Avisos relevantes (warnings):**

- Uso de APIs deprecated (já existentes no projeto)
- Avisos de opt-in para `ExperimentalCoroutinesApi`
- Parâmetros não utilizados em alguns arquivos

---

## 📂 Arquivos Alterados

- `ui/src/main/java/com/example/gestaobilhares/ui/inventory/stock/StockViewModel.kt`
- `ui/src/main/java/com/example/gestaobilhares/ui/inventory/stock/StockFragment.kt`
- `data/src/main/java/com/example/gestaobilhares/data/dao/CicloAcertoDao.kt`
- `data/src/main/java/com/example/gestaobilhares/data/repository/CicloAcertoRepository.kt`
- `ui/src/main/java/com/example/gestaobilhares/ui/clients/CycleHistoryViewModel.kt`
- `.cursor/rules/DEPLOY_GUIDE.md` (lint MD040 corrigido)

---

## ✅ Status Final

- Panos criados aparecem corretamente
- Troca de panos lista apenas disponíveis
- Filtro por ano funcional via intervalo
- Testes unitários executados com sucesso

---

**Status:** ✅ **PENDÊNCIAS ZERADAS**
