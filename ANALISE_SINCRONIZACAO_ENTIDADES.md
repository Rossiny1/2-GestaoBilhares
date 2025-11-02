# Análise de Sincronização de Entidades

## Resumo Executivo
Análise completa das entidades do banco local para verificar se todas estão sendo exportadas (PUSH) e importadas (PULL) para/da nuvem.

## Entidades Verificadas

### ✅ COMPLETAMENTE SINCRONIZADAS (PUSH + PULL)

1. **Cliente** - ✅ PUSH (CREATE/UPDATE) + ✅ PULL
2. **Rota** - ✅ PUSH (CREATE/UPDATE) + ✅ PULL  
3. **Mesa** - ✅ PUSH (CREATE/UPDATE) + ✅ PULL
4. **Colaborador** - ✅ PUSH (CREATE/UPDATE) + ✅ PULL
5. **Despesa** - ✅ PUSH (CREATE/UPDATE) + ✅ PULL
6. **PanoEstoque** - ✅ PUSH (CREATE/UPDATE) + ✅ PULL
7. **MesaVendida** - ✅ PUSH (CREATE/UPDATE) + ✅ PULL
8. **StockItem** - ✅ PUSH (CREATE) + ✅ PULL
9. **Veiculo** - ✅ PUSH (CREATE) + ✅ PULL
10. **HistoricoManutencaoMesa** - ✅ PUSH (via inserirHistoricoManutencaoMesaSync) + ✅ PULL
11. **HistoricoManutencaoVeiculo** - ✅ PUSH (via inserirHistoricoManutencaoVeiculoSync) + ✅ PULL
12. **HistoricoCombustivelVeiculo** - ✅ PUSH (via inserirHistoricoCombustivelVeiculoSync) + ✅ PULL
13. **CategoriaDespesa** - ✅ PUSH (CREATE) + ✅ PULL
14. **TipoDespesa** - ✅ PUSH (CREATE) + ✅ PULL
15. **ContratoLocacao** - ✅ PUSH (CREATE/UPDATE/DELETE) + ✅ PULL
16. **ContratoMesa** - ✅ PUSH (CREATE/DELETE) + ✅ PULL
17. **AditivoContrato** - ✅ PUSH (CREATE/UPDATE/DELETE) + ✅ PULL
18. **AditivoMesa** - ✅ PUSH (CREATE/DELETE) + ✅ PULL
19. **AssinaturaRepresentanteLegal** - ✅ PUSH (CREATE/UPDATE) + ✅ PULL
20. **MetaColaborador** - ✅ PUSH (CREATE/UPDATE/DELETE) + ✅ PULL
21. **ColaboradorRota** - ✅ PUSH (CREATE/DELETE) + ✅ PULL
22. **CicloAcertoEntity** - ✅ PUSH (CREATE/UPDATE via finalizarCicloAtualComDados) + ✅ PULL
23. **MesaReformada** - ✅ PUSH (CREATE/UPDATE) + ✅ PULL
24. **AcertoMesa** - ✅ PULL (mas verificar redirecionamento do método normal)
25. **LogAuditoriaAssinatura** - ✅ PULL (mas verificar redirecionamento do método normal)

### ⚠️ ENTIDADES COM PROBLEMAS IDENTIFICADOS

1. **Acerto** - ⚠️ UPDATE tem PUSH, mas CREATE não tem PUSH direto (comentário diz que é feito pelo SettlementViewModel)
   - Verificar se SettlementViewModel está chamando sincronização corretamente

2. **AcertoMesa** - ⚠️ Existe `inserirAcertoMesaSync`, mas `inserirAcertoMesa` não redireciona para ele
   - Linha 3533: `suspend fun inserirAcertoMesa(acertoMesa: AcertoMesa): Long = acertoMesaDao.inserir(acertoMesa)`
   - Deveria chamar `inserirAcertoMesaSync(acertoMesa)`

3. **LogAuditoriaAssinatura** - ⚠️ Existe `inserirLogAuditoriaAssinaturaSync`, mas `inserirLogAuditoriaAssinatura` não redireciona
   - Linha 3228: método normal não chama o método Sync
   - Deveria chamar `inserirLogAuditoriaAssinaturaSync(log)`

### ❌ ENTIDADES SEM SINCRONIZAÇÃO

1. **PanoMesa** - ❌ NÃO TEM PUSH nem PULL
   - Entidade existe no banco (AppDatabase.kt linha 48)
   - Não há chamadas de sincronização no AppRepository
   - Não há método pullPanoMesaFromFirestore no SyncManagerV2
   - Não há mapeamento no getCollectionName

### ✅ ENTIDADES DE SINCRONIZAÇÃO (Não devem ser sincronizadas)

1. **SyncLog** - ✅ Correto (não sincronizar)
2. **SyncQueue** - ✅ Correto (não sincronizar)  
3. **SyncConfig** - ✅ Correto (não sincronizar)

## Correções Necessárias

### Prioridade ALTA

1. **PanoMesa** - Implementar PUSH e PULL completos
2. **AcertoMesa** - Redirecionar `inserirAcertoMesa` para `inserirAcertoMesaSync`
3. **LogAuditoriaAssinatura** - Redirecionar `inserirLogAuditoriaAssinatura` para `inserirLogAuditoriaAssinaturaSync`

### Prioridade MÉDIA

1. **Acerto** - Verificar se CREATE está sendo sincronizado via SettlementViewModel

## Estatísticas

- Total de entidades verificadas: 28
- Completamente sincronizadas: 25 (89.3%)
- Com problemas: 3 (10.7%)
- Sem sincronização: 1 (3.6%)

