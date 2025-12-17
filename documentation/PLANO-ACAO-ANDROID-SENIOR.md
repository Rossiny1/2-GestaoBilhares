# 📋 PLANO DE AÇÃO - DESENVOLVEDOR ANDROID SÊNIOR

## 🎯 OBJETIVO
Completar a modularização E centralização do projeto, garantindo:
- ✅ Build passando
- ✅ Arquitetura limpa e centralizada
- ✅ Código pronto para testes manuais

---

## 📊 SITUAÇÃO ATUAL

### ✅ O QUE ESTÁ PRONTO
- **Modularização estrutural**: Módulos `core`, `data`, `sync`, `ui` criados
- **AppRepository**: Existe no código atual (mas não estava no commit `85f46f6`)
- **RepositoryFactory**: Existe e está funcionando
- **Layouts críticos**: Todos presentes

### ❌ O QUE ESTÁ FALTANDO
- **19 arquivos** ainda usam repositories individuais:
  - `CategoriaDespesaRepository`
  - `TipoDespesaRepository`
  - `ClienteRepository`
  - `AcertoRepository`
  - `CicloAcertoRepository`
  - `AcertoMesaRepository`
  - `DespesaRepository`
  - `VeiculoRepository`
  - `HistoricoManutencaoVeiculoRepository`
  - `HistoricoCombustivelVeiculoRepository`

---

## 🚀 PLANO DE EXECUÇÃO

### **FASE 1: DIAGNÓSTICO (PRIORIDADE ALTA)**
1. ✅ Verificar se `AppRepository` atual tem TODOS os métodos necessários
2. ✅ Testar build atual e listar TODOS os erros
3. ✅ Identificar quais métodos faltam no `AppRepository`

**Comando:**
```powershell
.\gradlew assembleDebug 2>&1 | Select-String "error:" | Select-Object -First 30
```

---

### **FASE 2: COMPLETAR AppRepository (PRIORIDADE ALTA)**
1. ✅ Adicionar métodos faltantes no `AppRepository`
2. ✅ Garantir que TODOS os DAOs estão no construtor
3. ✅ Verificar métodos de CategoriaDespesa e TipoDespesa (já migrados em ExpenseTypesFragment)

**Arquivos a verificar:**
- `app/src/main/java/com/example/gestaobilhares/data/repository/AppRepository.kt`

---

### **FASE 3: MIGRAÇÃO SISTEMÁTICA (PRIORIDADE ALTA)**
Migrar os 19 arquivos identificados, um por um:

#### **Grupo 1: Expenses (3 arquivos)**
- ✅ `ExpenseTypesFragment.kt` - JÁ MIGRADO
- ✅ `ExpenseCategoriesFragment.kt` - JÁ MIGRADO
- ⏳ `ExpenseRegisterFragment.kt`
- ⏳ `GlobalExpensesFragment.kt`
- ⏳ `GlobalExpensesViewModel.kt`

#### **Grupo 2: Settlement (4 arquivos)**
- ⏳ `SettlementFragment.kt`
- ⏳ `SettlementViewModel.kt`
- ⏳ `SettlementDetailFragment.kt`
- ⏳ `SettlementDetailViewModel.kt`

#### **Grupo 3: Cycles (6 arquivos)**
- ⏳ `CycleManagementFragment.kt`
- ⏳ `CycleManagementViewModel.kt`
- ⏳ `CycleExpensesFragment.kt`
- ⏳ `CycleExpensesViewModel.kt`
- ⏳ `CycleClientsFragment.kt`
- ⏳ `CycleClientsViewModel.kt`
- ⏳ `CycleReceiptsFragment.kt`

#### **Grupo 4: Clients (4 arquivos)**
- ⏳ `ClientRegisterFragment.kt`
- ⏳ `ClientRegisterViewModel.kt`
- ⏳ `CycleHistoryFragment.kt`
- ⏳ `CycleHistoryViewModel.kt`

#### **Grupo 5: Vehicles (2 arquivos)**
- ⏳ `VehicleDetailFragment.kt` - JÁ COMENTADO (TODO)
- ⏳ `VehicleDetailViewModel.kt` - JÁ COMENTADO (TODO)
- ⏳ `VehiclesViewModel.kt` - JÁ COMENTADO (TODO)

#### **Grupo 6: Metas (1 arquivo)**
- ⏳ `MetasViewModel.kt`

**Padrão de migração:**
```kotlin
// ❌ ANTES
val categoriaRepository = CategoriaDespesaRepository(...)
categoriaRepository.buscarAtivas()

// ✅ DEPOIS
val appRepository = RepositoryFactory.getAppRepository(requireContext())
appRepository.buscarCategoriasAtivas()
```

---

### **FASE 4: LIMPEZA (PRIORIDADE MÉDIA)**
1. ⏳ Remover imports de repositories individuais
2. ⏳ Verificar se repositories individuais ainda são necessários
3. ⏳ Marcar como `@Deprecated` se ainda forem usados em algum lugar

---

### **FASE 5: TESTES E VALIDAÇÃO (PRIORIDADE ALTA)**
1. ⏳ Build deve passar sem erros
2. ⏳ Verificar warnings (não devem bloquear)
3. ⏳ Testar funcionalidades críticas manualmente

---

### **FASE 6: COMMIT FINAL (PRIORIDADE ALTA)**
1. ⏳ Commit com mensagem clara:
   ```
   feat: Completa modularização e centralização
   
   - Modularização estrutural completa (core, data, sync, ui)
   - Centralização completa: todos os fragments usam AppRepository
   - Removidos repositories individuais
   - Build passando
   ```
2. ⏳ Criar tag: `backup-modularizacao-completa-YYYYMMDD`

---

## 🎯 PRIORIDADES

### **URGENTE (Fazer AGORA)**
1. ✅ Verificar build atual
2. ✅ Completar métodos faltantes no AppRepository
3. ✅ Migrar os 19 arquivos restantes

### **IMPORTANTE (Fazer HOJE)**
4. ✅ Testar build final
5. ✅ Fazer commit do estado correto

### **NICE TO HAVE (Pode esperar)**
6. ⏳ Remover repositories individuais completamente
7. ⏳ Documentar arquitetura final

---

## 📝 CHECKLIST DE VALIDAÇÃO

Antes de considerar completo, verificar:

- [ ] Build passa sem erros
- [ ] Todos os 19 arquivos migrados para AppRepository
- [ ] Nenhum import de repository individual nos fragments
- [ ] AppRepository tem todos os métodos necessários
- [ ] RepositoryFactory está sendo usado corretamente
- [ ] Layouts críticos presentes
- [ ] Módulos configurados corretamente no settings.gradle.kts
- [ ] Commit feito com mensagem descritiva
- [ ] Tag criada para backup

---

## 🚨 RISCOS E MITIGAÇÕES

### **Risco 1: Build quebrar durante migração**
- **Mitigação**: Fazer migração incremental, testando após cada grupo

### **Risco 2: Métodos faltantes no AppRepository**
- **Mitigação**: Verificar todos os usos antes de remover repositories individuais

### **Risco 3: Perder funcionalidades**
- **Mitigação**: Manter repositories individuais como @Deprecated até confirmar que tudo funciona

---

## 💡 RECOMENDAÇÕES FINAIS

1. **Fazer uma migração por vez**: Não tentar migrar tudo de uma vez
2. **Testar após cada grupo**: Garantir que build passa após cada grupo migrado
3. **Manter backups**: Criar tags Git após cada fase importante
4. **Documentar mudanças**: Comentar no código o que foi migrado e quando

---

**Última atualização**: 2025-11-12
**Status**: Em andamento
**Próximo passo**: Verificar build atual e identificar erros

