# ANÁLISE COMPARATIVA: ENTIDADES DE SINCRONIZAÇÃO

## 📊 RESUMO EXECUTIVO

**Status**: ❌ **18 ENTIDADES FALTANDO** de sincronização

Comparação entre `SyncManagerV2` (commit `7feb452b` - antes da modularização) e `SyncRepository` atual.

---

## ✅ ENTIDADES IMPLEMENTADAS (8)

1. ✅ **Rota** - `pullRotas()` / `pushRotas()`
2. ✅ **Cliente** - `pullClientes()` / `pushClientes()`
3. ✅ **Mesa** - `pullMesas()` / `pushMesas()`
4. ✅ **Colaborador** - `pullColaboradores()` / `pushColaboradores()`
5. ✅ **Ciclo** - `pullCiclos()` / `pushCiclos()`
6. ✅ **Acerto** - `pullAcertos()` / `pushAcertos()`
7. ✅ **AcertoMesa** - `pullAcertoMesas()` / `pushAcertoMesas()` (dentro de Acertos)
8. ✅ **Despesa** - `pullDespesas()` / `pushDespesas()`
9. ✅ **ContratoLocacao** - `pullContratos()` / `pushContratos()`
10. ✅ **AditivoContrato** - `pullAditivosContrato()` / `pushAditivosContrato()`

---

## ❌ ENTIDADES FALTANDO (18)

### **1. PanoEstoque** ✅
- **Antigo**: `pullPanoEstoqueFromFirestore()`
- **Status**: ❌ Não implementado
- **Prioridade**: MÉDIA
- **Métodos AppRepository**: ✅ `obterTodosPanosEstoque()`, `inserirPanoEstoque()`, `buscarPorNumero()`, `obterPanoPorId()`

### **2. MesaVendida** ✅
- **Antigo**: `pullMesaVendidaFromFirestore()`
- **Status**: ❌ Não implementado
- **Prioridade**: MÉDIA
- **Métodos AppRepository**: ✅ `obterTodasMesasVendidas()`, `inserirMesaVendida()`, `buscarMesaVendidaPorId()`

### **3. StockItem** ✅
- **Antigo**: `pullStockItemsFromFirestore()`
- **Status**: ❌ Não implementado
- **Prioridade**: MÉDIA
- **Métodos AppRepository**: ✅ `obterTodosStockItems()`, `inserirStockItem()`

### **4. Veiculo** ⚠️
- **Antigo**: `pullVeiculosFromFirestore()`
- **Status**: ❌ Não implementado
- **Prioridade**: MÉDIA
- **Métodos AppRepository**: ⚠️ **PRECISA VERIFICAR** - Entidade existe no banco, mas métodos podem não estar implementados

### **5. HistoricoManutencaoMesa** ✅
- **Antigo**: `pullHistoricoManutencaoMesaFromFirestore()`
- **Status**: ❌ Não implementado
- **Prioridade**: BAIXA
- **Métodos AppRepository**: ✅ `obterTodosHistoricoManutencaoMesa()`, `inserirHistoricoManutencaoMesa()`

### **6. HistoricoManutencaoVeiculo** ⚠️
- **Antigo**: `pullHistoricoManutencaoVeiculoFromFirestore()`
- **Status**: ❌ Não implementado
- **Prioridade**: BAIXA
- **Métodos AppRepository**: ⚠️ `inserirHistoricoManutencao()` existe, mas precisa verificar métodos de listagem

### **7. HistoricoCombustivelVeiculo** ⚠️
- **Antigo**: `pullHistoricoCombustivelVeiculoFromFirestore()`
- **Status**: ❌ Não implementado
- **Prioridade**: BAIXA
- **Métodos AppRepository**: ⚠️ `inserirHistoricoCombustivel()` existe, mas precisa verificar métodos de listagem

### **8. CategoriaDespesa** ✅
- **Antigo**: `pullCategoriasDespesaFromFirestore()`
- **Status**: ❌ Não implementado
- **Prioridade**: ALTA (necessário para Despesas)
- **Métodos AppRepository**: ✅ `buscarCategoriasAtivas()`, `buscarCategoriaPorNome()`, `buscarCategoriaPorId()`, `criarCategoria()`, `atualizarCategoria()`, `deletarCategoria()`

### **9. TipoDespesa** ✅
- **Antigo**: `pullTiposDespesaFromFirestore()`
- **Status**: ❌ Não implementado
- **Prioridade**: ALTA (necessário para Despesas)
- **Métodos AppRepository**: ✅ `buscarTipoPorNome()`, `buscarTipoPorId()`, `buscarTiposPorCategoria()`, `buscarTiposAtivosComCategoria()`, `criarTipo()`, `atualizarTipo()`, `deletarTipo()`

### **10. Meta (MetaColaborador)** ✅
- **Antigo**: `pullMetasFromFirestore()`
- **Status**: ❌ Não implementado
- **Prioridade**: MÉDIA
- **Métodos AppRepository**: ✅ `obterMetasPorColaborador()`, `obterMetaAtual()`, `inserirMeta()`, `atualizarMeta()`, `deletarMeta()`, `buscarMetasPorColaboradorECiclo()`, `buscarMetasPorRotaECiclo()`

### **11. ColaboradorRota** ✅
- **Antigo**: `pullColaboradoresRotasFromFirestore()`
- **Status**: ❌ Não implementado
- **Prioridade**: MÉDIA (vinculação Colaborador-Rota)
- **Métodos AppRepository**: ✅ `inserirColaboradorRota()`, `deletarColaboradorRota()`, `vincularColaboradorRota()`

### **12. AditivoMesa** ✅
- **Antigo**: `pullAditivoMesasFromFirestore()`
- **Status**: ❌ Não implementado
- **Prioridade**: MÉDIA (vinculação Aditivo-Mesa)
- **Métodos AppRepository**: ✅ `inserirAditivoMesas()`, `excluirAditivoMesa()`, `buscarMesasPorAditivo()`

### **13. ContratoMesa** ✅
- **Antigo**: `pullContratoMesasFromFirestore()`
- **Status**: ❌ Não implementado
- **Prioridade**: MÉDIA (vinculação Contrato-Mesa)
- **Métodos AppRepository**: ✅ `inserirContratoMesa()`, `inserirContratoMesas()`, `excluirContratoMesa()`

### **14. AssinaturaRepresentanteLegal** ✅
- **Antigo**: `pullAssinaturasRepresentanteLegalFromFirestore()`
- **Status**: ❌ Não implementado
- **Prioridade**: ALTA (funcionalidade jurídica crítica)
- **Métodos AppRepository**: ✅ `obterAssinaturaRepresentanteLegalAtiva()`, `obterTodasAssinaturasRepresentanteLegal()`, `obterAssinaturaRepresentanteLegalPorId()`, `atualizarAssinaturaRepresentanteLegal()`, `desativarAssinaturaRepresentanteLegal()`

### **15. LogAuditoriaAssinatura** ✅
- **Antigo**: `pullLogsAuditoriaAssinaturaFromFirestore()`
- **Status**: ❌ Não implementado
- **Prioridade**: ALTA (auditoria jurídica)
- **Métodos AppRepository**: ✅ `obterTodosLogsAuditoria()`, `obterTodosLogsAuditoriaFlow()`

### **16. MesaReformada** ⚠️
- **Antigo**: `pullMesaReformadaFromFirestore()`
- **Status**: ❌ Não implementado
- **Prioridade**: MÉDIA
- **Métodos AppRepository**: ⚠️ `inserirMesaReformada()` existe, mas precisa verificar métodos de listagem

### **17. PanoMesa** ⚠️
- **Antigo**: `pullPanoMesaFromFirestore()`
- **Status**: ❌ Não implementado
- **Prioridade**: MÉDIA (vinculação Pano-Mesa)
- **Métodos AppRepository**: ⚠️ **PRECISA VERIFICAR** - Entidade existe no banco, mas métodos podem não estar implementados

### **18. Equipment** (REMOVIDO)
- **Antigo**: `pullEquipmentFromFirestore()`
- **Status**: ✅ **INTENCIONALMENTE REMOVIDO** (entidade foi deletada)
- **Prioridade**: N/A

---

## 🔄 ORDEM DE SINCRONIZAÇÃO (ANTIGA)

O código antigo seguia uma ordem específica para respeitar dependências:

1. **Rota** (dependência dos clientes)
2. **Cliente** (depende de rotas)
3. **Mesa** (depende de clientes)
4. **Acerto** (depende de clientes)
5. **Ciclo** (depende de acertos)
6. **Colaborador**
7. **Despesa**
8. **PanoEstoque**
9. **MesaVendida**
10. **StockItem**
11. **Veiculo**
12. **HistoricoManutencaoMesa**
13. **HistoricoManutencaoVeiculo**
14. **HistoricoCombustivelVeiculo**
15. **CategoriaDespesa**
16. **TipoDespesa**
17. **ContratoLocacao**
18. **Meta** (depende de colaboradores/rotas)
19. **ColaboradorRota** (depende de colaboradores e rotas)
20. **AditivoContrato** (depende de contratos)
21. **AditivoMesa** (depende de aditivos)
22. **ContratoMesa** (depende de contratos)
23. **AssinaturaRepresentanteLegal**
24. **LogAuditoriaAssinatura**
25. **AcertoMesa** (depende de acertos)
26. **MesaReformada**
27. **PanoMesa**

---

## 🎯 PRÓXIMOS PASSOS

1. **Verificar métodos AppRepository** para cada entidade faltante
2. **Implementar pull/push handlers** para as 18 entidades faltantes
3. **Atualizar `syncPull()` e `syncPush()`** para incluir todas as entidades na ordem correta
4. **Testar sincronização completa** após implementação

---

## 📝 NOTAS

- O código antigo usava **handlers especializados** para Rota, Cliente, Mesa, Acerto e Ciclo
- O código atual usa **métodos diretos** no `SyncRepository`
- A ordem de sincronização é **crítica** para evitar erros de dependência
- Algumas entidades podem não ter métodos no `AppRepository` ainda (precisam ser verificadas)

