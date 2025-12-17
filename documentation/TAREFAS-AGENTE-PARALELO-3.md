# TAREFAS PARA AGENTE PARALELO - CORREÇÃO FINAL DE ERROS

## CONTEXTO

O agente principal já corrigiu:

- ✅ AppRepository: type mismatch, conflicting overloads, Equipment
- ✅ CicloAcertoRepository: DespesaRepository → DespesaDao
- ✅ Fragments: DespesaRepository → DespesaDao

## ERROS RESTANTES PARA CORRIGIR

### LOTE 1: ✅ CONCLUÍDO pelo agente principal

**Arquivo:** `app/src/main/java/com/example/gestaobilhares/data/repository/CicloAcertoRepository.kt`

**Status:** Ambiguidade de tipos corrigida - tipos explícitos adicionados

### LOTE 2: Corrigir conflicting overloads em CycleExpensesViewModel

**Arquivo:** `app/src/main/java/com/example/gestaobilhares/ui/cycles/CycleExpensesViewModel.kt`

**Problema:**

- **Linha 178**: `Overload resolution ambiguity` para `appRepository.buscarCicloPorId(cicloId)`
- Causa: AppRepository tem método duplicado `buscarCicloPorId` (agente principal já removeu um, mas pode ter ficado outro)

**Ação:**

- Verificar se há dois métodos `buscarCicloPorId` no AppRepository
- Se sim, usar o método correto ou qualificar a chamada
- Ou usar `appRepository.buscarCicloPorId(cicloId)` com tipo explícito

### LOTE 3: Verificar e corrigir erros de tipo restantes

**Arquivos para verificar:**

1. **ClientListViewModel.kt** (linha 1144)
   - `Cannot infer a type for this parameter`
   - Adicionar tipos explícitos nos parâmetros do lambda

2. **MesasReformadasViewModel.kt** (se ainda houver erros)
   - Verificar se todos os tipos estão explícitos

3. **EquipmentsViewModel.kt** (se ainda houver erros)
   - Verificar se código comentado está correto

### LOTE 4: Verificar imports e dependências

**Ação:**

- Verificar se todos os imports necessários estão presentes
- Verificar se não há imports não utilizados causando problemas
- Garantir que `DespesaDao` está importado onde necessário

## ORDEM DE EXECUÇÃO

1. **LOTE 1** (crítico - impede build)
2. **LOTE 2** (crítico - impede build)
3. **LOTE 3** (importante)
4. **LOTE 4** (verificação final)

## NOTAS IMPORTANTES

- **NÃO modificar** AppRepository.kt ou CicloAcertoRepository.kt além do necessário
- Focar em correções de tipo e ambiguidade
- Testar mentalmente se as correções fazem sentido
- Manter compatibilidade com tipos existentes

## EXEMPLO DE CORREÇÃO

**ANTES (com ambiguidade):**

```kotlin
val lucroLiquido = valorTotalAcertado - valorTotalDespesas
```

**DEPOIS (tipo explícito):**

```kotlin
val lucroLiquido: Double = valorTotalAcertado.toDouble() - valorTotalDespesas.toDouble()
```

**OU se já forem Double:**

```kotlin
val lucroLiquido = (valorTotalAcertado - valorTotalDespesas).toDouble()
```

## STATUS

- [x] LOTE 1: Problemas de tipo no CicloAcertoRepository ✅ CONCLUÍDO
- [ ] LOTE 2: Conflicting overloads em CycleExpensesViewModel
- [ ] LOTE 3: Erros de tipo restantes
- [ ] LOTE 4: Verificação de imports
