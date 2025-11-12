# 📋 Plano de Migração Compose - GestaoBilhares

## ✅ **Status Atual - Telas Já Migradas**

### 🎯 **Telas Principais (100% Funcionais)**

- ✅ **RoutesScreen** - Lista de rotas
- ✅ **ClientListScreen** - Lista de clientes
- ✅ **ClientDetailScreen** - Detalhes do cliente
- ✅ **SettlementScreen** - Tela de acerto
- ✅ **DashboardScreen** - Dashboard principal
- ✅ **ContractGenerationScreen** - Geração de contratos
- ✅ **SignatureCaptureScreen** - Captura de assinatura

### 🎯 **Telas Secundárias (Implementadas)**

- ✅ **VehiclesScreen** - Gestão de veículos
- ✅ **VehicleDetailScreen** - Detalhes do veículo
- ✅ **StockScreen** - Controle de estoque
- ✅ **MetasScreen** - Gestão de metas
- ✅ **ColaboradoresScreen** - Gestão de colaboradores
- ✅ **CiclosScreen** - Gestão de ciclos
- ✅ **ContractManagementScreen** - Gestão de contratos
- ✅ **ClosureReportScreen** - Relatórios de fechamento
- ✅ **ExpenseRegisterScreen** - Cadastro de despesas
- ✅ **MesasDepositoScreen** - Mesas em depósito
- ✅ **NovaReformaScreen** - Nova reforma de mesa
- ✅ **MetaCadastroScreen** - Cadastro de meta

---

## 🚧 **Telas que Precisam ser Migradas (Prioridade)**

### 🔥 **ALTA PRIORIDADE - Funcionalidades Críticas**

#### 1. **LoginScreen**

- **Fragment**: `LoginFragment.kt`
- **Compose**: `LoginScreen.kt` (já existe, mas precisa validação)
- **Status**: ⚠️ Parcialmente implementado
- **Ação**: Validar e corrigir se necessário

#### 2. **ExpenseCategoriesFragment**

- **Fragment**: `ExpenseCategoriesFragment.kt`
- **Compose**: ✅ `ExpenseCategoriesScreen.kt` (IMPLEMENTADO)
- **Prioridade**: 🔥 Alta (gestão de categorias de despesas)
- **Status**: ✅ CONCLUÍDO

#### 3. **ExpenseTypesFragment**

- **Fragment**: `ExpenseTypesFragment.kt`
- **Compose**: ❌ Não existe
- **Prioridade**: 🔥 Alta (gestão de tipos de despesas)
- **Ação**: Criar `ExpenseTypesScreen.kt`

#### 4. **GlobalExpensesFragment**

- **Fragment**: `GlobalExpensesFragment.kt`
- **Compose**: ❌ Não existe
- **Prioridade**: 🔥 Alta (despesas globais)
- **Ação**: Criar `GlobalExpensesScreen.kt`

#### 5. **ExpenseHistoryFragment**

- **Fragment**: `ExpenseHistoryFragment.kt`
- **Compose**: ❌ Não existe
- **Prioridade**: 🔥 Alta (histórico de despesas)
- **Ação**: Criar `ExpenseHistoryScreen.kt`

### 🔶 **MÉDIA PRIORIDADE - Gestão de Mesas**

#### 6. **GerenciarMesasFragment**

- **Fragment**: `GerenciarMesasFragment.kt`
- **Compose**: ❌ Não existe
- **Prioridade**: 🔶 Média (gestão geral de mesas)
- **Ação**: Criar `GerenciarMesasScreen.kt`

#### 7. **CadastroMesaFragment**

- **Fragment**: `CadastroMesaFragment.kt`
- **Compose**: ❌ Não existe
- **Prioridade**: 🔶 Média (cadastro de novas mesas)
- **Ação**: Criar `CadastroMesaScreen.kt`

#### 8. **EditMesaFragment**

- **Fragment**: `EditMesaFragment.kt`
- **Compose**: ❌ Não existe
- **Prioridade**: 🔶 Média (edição de mesas)
- **Ação**: Criar `EditMesaScreen.kt`

#### 9. **RotaMesasFragment**

- **Fragment**: `RotaMesasFragment.kt`
- **Compose**: ❌ Não existe
- **Prioridade**: 🔶 Média (mesas por rota)
- **Ação**: Criar `RotaMesasScreen.kt`

### 🔵 **BAIXA PRIORIDADE - Funcionalidades Específicas**

#### 10. **HistoricoMesasVendidasFragment**

- **Fragment**: `HistoricoMesasVendidasFragment.kt`
- **Compose**: ❌ Não existe
- **Prioridade**: 🔵 Baixa (histórico de vendas)
- **Ação**: Criar `HistoricoMesasVendidasScreen.kt`

#### 11. **HistoricoManutencaoMesaFragment**

- **Fragment**: `HistoricoManutencaoMesaFragment.kt`
- **Compose**: ❌ Não existe
- **Prioridade**: 🔵 Baixa (histórico de manutenção)
- **Ação**: Criar `HistoricoManutencaoMesaScreen.kt`

#### 12. **MesasReformadasFragment**

- **Fragment**: `MesasReformadasFragment.kt`
- **Compose**: ❌ Não existe
- **Prioridade**: 🔵 Baixa (mesas reformadas)
- **Ação**: Criar `MesasReformadasScreen.kt`

#### 13. **ColaboradorRegisterFragment**

- **Fragment**: `ColaboradorRegisterFragment.kt`
- **Compose**: ❌ Não existe
- **Prioridade**: 🔵 Baixa (cadastro de colaboradores)
- **Ação**: Criar `ColaboradorRegisterScreen.kt`

#### 14. **ColaboradorMetasFragment**

- **Fragment**: `ColaboradorMetasFragment.kt`
- **Compose**: ❌ Não existe
- **Prioridade**: 🔵 Baixa (metas por colaborador)
- **Ação**: Criar `ColaboradorMetasScreen.kt`

---

## 🎯 **Estratégia de Implementação**

### **Fase 1: Validação e Correção (1-2 dias)**

1. ✅ Validar `LoginScreen` existente
2. ✅ Testar todas as telas Compose já implementadas
3. ✅ Corrigir problemas encontrados

### **Fase 2: Despesas (3-4 dias)**

1. 🔥 `ExpenseCategoriesScreen` - Gestão de categorias
2. 🔥 `ExpenseTypesScreen` - Gestão de tipos
3. 🔥 `GlobalExpensesScreen` - Despesas globais
4. 🔥 `ExpenseHistoryScreen` - Histórico de despesas

### **Fase 3: Gestão de Mesas (4-5 dias)**

1. 🔶 `GerenciarMesasScreen` - Gestão geral
2. 🔶 `CadastroMesaScreen` - Cadastro de mesas
3. 🔶 `EditMesaScreen` - Edição de mesas
4. 🔶 `RotaMesasScreen` - Mesas por rota

### **Fase 4: Funcionalidades Específicas (3-4 dias)**

1. 🔵 Históricos e relatórios específicos
2. 🔵 Cadastros adicionais
3. 🔵 Funcionalidades de manutenção

---

## 📊 **Métricas de Progresso**

- **Total de Fragments**: 47
- **Telas Compose Implementadas**: 28
- **Telas Restantes**: 19
- **Progresso Atual**: ~60%

### **Por Categoria:**

- **Telas Principais**: 100% ✅
- **Gestão de Despesas**: 40% (2/5) 🔥
- **Gestão de Mesas**: 25% (1/4) 🔶
- **Funcionalidades Específicas**: 0% (0/11) 🔵

---

## 🛠️ **Próximos Passos Imediatos**

1. ✅ **Validar LoginScreen** - Verificar se está funcionando corretamente
2. ✅ **Implementar ExpenseCategoriesScreen** - Primeira tela de alta prioridade
3. **Testar integração** - Garantir que tudo funciona
4. **Continuar com ExpenseTypesScreen** - Segunda tela de alta prioridade

---

## 📝 **Notas Importantes**

- ✅ **Preservar funcionalidades**: Todas as funcionalidades existentes devem ser mantidas
- ✅ **Manter design**: Usar as mesmas cores e layout do tema atual
- ✅ **Testes contínuos**: Validar cada tela após implementação
- ✅ **Migração gradual**: Não quebrar o sistema existente
- ✅ **Documentação**: Atualizar este plano conforme progresso
