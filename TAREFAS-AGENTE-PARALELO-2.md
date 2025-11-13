# TAREFAS PARA AGENTE PARALELO - CORREÇÃO DE ERROS DE BUILD

## CONTEXTO
O build está falhando com vários erros. Este agente deve trabalhar em paralelo corrigindo os erros mais diretos e simples, enquanto o agente principal corrige os erros mais complexos no AppRepository e CicloAcertoRepository.

## REGRAS IMPORTANTES
1. **SEMPRE usar AppRepository** em vez de repositories individuais
2. **NÃO modificar** AppRepository.kt ou CicloAcertoRepository.kt (agente principal está cuidando)
3. **NÃO criar** novos arquivos sem necessidade
4. **Comentar** código que depende de classes/arquivos que não existem mais
5. **Usar RepositoryFactory.getAppRepository(context)** para obter AppRepository

## TAREFAS PRIORITÁRIAS

### LOTE 1: Substituir DespesaRepository por AppRepository nos Fragments
**Arquivos afetados:**
- `app/src/main/java/com/example/gestaobilhares/ui/clients/CycleHistoryFragment.kt` (linha 42)
- `app/src/main/java/com/example/gestaobilhares/ui/cycles/CycleClientsFragment.kt` (linha 61)
- `app/src/main/java/com/example/gestaobilhares/ui/cycles/CycleManagementFragment.kt` (linha 76)
- `app/src/main/java/com/example/gestaobilhares/ui/cycles/CycleReceiptsFragment.kt` (linha 85)
- `app/src/main/java/com/example/gestaobilhares/ui/settlement/SettlementDetailFragment.kt` (linha 485)

**Ação:** Remover `DespesaRepository(database.despesaDao())` e usar `appRepository` diretamente onde necessário. Se for para passar ao CicloAcertoRepository, manter como está (será corrigido depois).

### LOTE 2: Comentar/Corrigir referências a classes removidas
**Arquivos afetados:**

1. **EquipmentsViewModel.kt** (linhas 5, 34, 36, 62)
   - `Equipment` e `EquipmentEntity` não existem mais
   - Comentar métodos que usam essas classes ou retornar `flowOf(emptyList())`

2. **MesasReformadasFragment.kt e MesasReformadasViewModel.kt**
   - `MesaReformadaRepository` não existe mais
   - Substituir por `appRepository` usando métodos como `obterTodasMesasReformadas()`, `inserirMesaReformada()`, etc.

3. **VehicleDetailFragment.kt** (linhas 13-15, 148, 155, 158)
   - `VeiculoRepository`, `HistoricoManutencaoVeiculoRepository`, `HistoricoCombustivelVeiculoRepository` não existem
   - `maintenanceAdapter` e `fuelAdapter` não existem
   - Comentar código relacionado a veículos (já estava comentado antes)

4. **ClientListViewModel.kt** (linhas 14, 53, 1144)
   - `PaginationManager` não existe
   - Comentar ou remover referências

5. **CycleReceiptsFragment.kt** (linhas 42, 90)
   - `CycleReceiptsViewModel` não existe
   - Verificar se deve ser criado ou se o fragment deve usar outro ViewModel

### LOTE 3: Corrigir problemas de tipo/inferência
**Arquivos afetados:**

1. **CicloAcertoRepository.kt** (linhas 137, 143, 148, 210, 211)
   - Erro: `Unresolved reference: valor` na linha 137
   - Erros: `Overload resolution ambiguity` com operador `-`
   - Erro: `Cannot infer a type for this parameter` na linha 210
   - **NÃO MODIFICAR** - agente principal está cuidando

2. **CycleReceiptsFragment.kt** (linhas 118, 126, 132, 134)
   - `Cannot infer a type for this parameter`
   - `Unresolved reference: it`
   - Adicionar tipos explícitos nos lambdas

3. **MesasReformadasViewModel.kt** (linhas 62, 66, 68, 71, 82, 85, 92, 95)
   - `Cannot infer a type` e `Unresolved reference: it`
   - Adicionar tipos explícitos

4. **ClientListViewModel.kt** (linha 1144)
   - `Cannot infer a type for this parameter`
   - Adicionar tipos explícitos

5. **EquipmentsViewModel.kt** (linhas 34, 36)
   - `Cannot infer a type for this parameter`
   - Adicionar tipos explícitos

### LOTE 4: Corrigir SignatureCaptureFragment e ViewModel
**Arquivos afetados:**

1. **SignatureCaptureFragment.kt** (linhas 198, 270-273)
   - `Unresolved reference: totalPoints`, `averagePressure`, `averageVelocity`, `duration`
   - Usar acesso via map: `statistics["totalPoints"] as? Int ?: 0`

2. **SignatureCaptureViewModel.kt** (linhas 99-106)
   - `Cannot find a parameter with this name: locatarioAssinaturaHash`, etc.
   - Verificar quais campos existem na entidade `ContratoLocacao` e ajustar

### LOTE 5: Corrigir ClientDetailFragment
**Arquivos afetados:**

1. **ClientDetailFragment.kt** (linhas 45-49, 54, 110, 234, 371, 405, 409, 419)
   - `Unresolved reference: dialogs` - verificar imports
   - `Unresolved reference: ConfirmarRetiradaMesaDialogFragment`, etc.
   - `'onDialogPositiveClick' overrides nothing` - remover `override` ou implementar interface correta
   - Verificar se os dialogs existem ou devem ser comentados

### LOTE 6: Layouts faltando (ViewBinding)
**Arquivos afetados:**

1. **ColaboradorAdapter.kt e ColaboradorManagementFragment.kt**
   - `ItemColaboradorBinding` e `FragmentColaboradorManagementBinding` não existem
   - Verificar se os layouts XML existem:
     - `item_colaborador.xml`
     - `fragment_colaborador_management.xml`
   - Se não existirem, comentar código que usa ViewBinding ou criar placeholders

## ORDEM DE EXECUÇÃO
1. **LOTE 1** (mais crítico - impede build)
2. **LOTE 2** (classes removidas)
3. **LOTE 3** (tipos/inferência)
4. **LOTE 4** (SignatureCapture)
5. **LOTE 5** (ClientDetailFragment)
6. **LOTE 6** (Layouts)

## NOTAS IMPORTANTES
- Se encontrar `DespesaRepository` sendo passado ao `CicloAcertoRepository`, **NÃO REMOVER** ainda - será corrigido depois
- Sempre verificar se métodos existem no `AppRepository` antes de usar
- Se uma classe não existe, comentar o código e adicionar `// TODO: Implementar quando [classe] estiver disponível`
- Manter compatibilidade com a arquitetura híbrida modular

## EXEMPLO DE CORREÇÃO

**ANTES:**
```kotlin
val despesaRepository = DespesaRepository(database.despesaDao())
val despesas = despesaRepository.buscarPorCicloId(cicloId)
```

**DEPOIS:**
```kotlin
val appRepository = RepositoryFactory.getAppRepository(requireContext())
val despesas = appRepository.buscarDespesasPorCicloId(cicloId)
```

**OU se for para passar ao CicloAcertoRepository (temporário):**
```kotlin
// TODO: CicloAcertoRepository será migrado para usar AppRepository diretamente
val despesaRepository = DespesaRepository(database.despesaDao())
```

## STATUS
- [ ] LOTE 1: Substituir DespesaRepository
- [ ] LOTE 2: Classes removidas
- [ ] LOTE 3: Tipos/inferência
- [ ] LOTE 4: SignatureCapture
- [ ] LOTE 5: ClientDetailFragment
- [ ] LOTE 6: Layouts

