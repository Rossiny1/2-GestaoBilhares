# 📊 RELATÓRIO DE ANÁLISE DO BANCO DE DADOS
## Análise baseada em melhores práticas Android Room 2025

**Data:** 2025  
**Versão do Banco:** 44  
**Total de Entidades:** 30+  
**Total de DAOs:** 28

---

## ✅ PONTOS POSITIVOS (O que já está bem implementado)

### 1. **Estrutura Geral**
- ✅ Uso correto do Room Database
- ✅ TypeConverters implementados corretamente
- ✅ Foreign Keys configuradas com CASCADE/SET_NULL apropriados
- ✅ Padrão Singleton para instância do banco
- ✅ Migrations bem estruturadas

### 2. **Índices Existentes**
- ✅ Índices em Foreign Keys (rota_id, cliente_id, etc.)
- ✅ Índices compostos em algumas entidades (Acerto, Despesa)
- ✅ Índices para ORDER BY (nome, data_acerto)
- ✅ Índices únicos onde necessário (numeroContrato)

### 3. **Queries**
- ✅ Uso de Flow para observação reativa
- ✅ Queries parametrizadas (prevenção de SQL injection)
- ✅ Uso de @RewriteQueriesToDropUnusedColumns em algumas queries

---

## ⚠️ OPORTUNIDADES DE MELHORIA

### 🔴 PRIORIDADE ALTA (Impacto significativo na performance)

#### 1. **Índices Faltantes em Queries Frequentes**

**Problema:** Algumas queries usam ORDER BY e WHERE sem índices adequados.

**Recomendações:**

##### 1.1. Mesa Entity
```kotlin
// ❌ FALTA: Índice para número (usado em busca - MesaDao.kt:75,81,95)
@Index(value = ["numero"])

// ❌ FALTA: Índice composto para queries por cliente e ativa (MesaDao.kt:23,69)
@Index(value = ["cliente_id", "ativa"])

// ❌ FALTA: Índice para tipo_mesa (se usado em filtros)
@Index(value = ["tipo_mesa"])
```

**Queries Afetadas:**
- `MesaDao.kt:75` - `WHERE cliente_id IS NULL AND numero = :numero`
- `MesaDao.kt:81` - `WHERE numero = :numero`
- `MesaDao.kt:23,69` - `WHERE cliente_id = :clienteId AND ativa = 1 ORDER BY numero ASC`

**Impacto:** Queries de busca por número e filtros por cliente+ativa podem ser lentas sem índices.

##### 1.2. AcertoMesa Entity
```kotlin
// ✅ JÁ TEM: Índice para acerto_id (linha 30)
// ✅ JÁ TEM: Índice composto [acerto_id, mesa_id] (linha 32)

// ❌ FALTA: Índice para data_criacao (usado em ORDER BY - AcertoMesaDao.kt:21,22,45)
@Index(value = ["data_criacao"])

// ❌ FALTA: Índice composto para queries por mesa e data
@Index(value = ["mesa_id", "data_criacao"])
```

**Queries Afetadas:**
- `AcertoMesaDao.kt:21` - `WHERE mesa_id = :mesaId ORDER BY data_criacao DESC`
- `AcertoMesaDao.kt:22` - `WHERE mesa_id = :mesaId ORDER BY data_criacao DESC LIMIT 1`
- `AcertoMesaDao.kt:45` - `WHERE mesa_id = :mesaId AND fichas_jogadas > 0 ORDER BY data_criacao DESC`

**Impacto:** Queries ordenadas por data_criacao podem ser lentas sem índice.

##### 1.3. Equipment Entity
```kotlin
// ❌ FALTA: Índice para name (usado em ORDER BY - EquipmentDao.kt:22,28,31)
@Index(value = ["name"])

// ❌ FALTA: Índice para location (usado em filtros - EquipmentDao.kt:31)
@Index(value = ["location"])
```

**Queries Afetadas:**
- `EquipmentDao.kt:22` - `ORDER BY name ASC`
- `EquipmentDao.kt:28` - `WHERE name LIKE '%' || :search || '%' ORDER BY name ASC`
- `EquipmentDao.kt:31` - `WHERE location = :location ORDER BY name ASC`

**Impacto:** Ordenação por nome e busca por localização podem ser lentas sem índices.

##### 1.4. CicloAcertoEntity
```kotlin
// ✅ JÁ TEM: Índice composto [rota_id, ano, numero_ciclo] (linha 29)

// ❌ FALTA: Índice para status (usado em filtros - CicloAcertoDao.kt:64,82,88,94)
@Index(value = ["status"])

// ❌ FALTA: Índice composto para queries por rota e status
@Index(value = ["rota_id", "status"])
```

**Queries Afetadas:**
- `CicloAcertoDao.kt:64` - `WHERE status = 'FINALIZADO' ORDER BY ano DESC, numero_ciclo DESC`
- `CicloAcertoDao.kt:82` - `WHERE rota_id = :rotaId AND status = 'EM_ANDAMENTO'`
- `CicloAcertoDao.kt:88,94` - `WHERE rota_id = :rotaId AND status = 'EM_ANDAMENTO' LIMIT 1`

**Impacto:** Queries filtradas por status podem ser lentas sem índice.

##### 1.5. Despesa Entity
```kotlin
// ✅ JÁ TEM: Índices básicos (rotaId, cicloId, etc.)
// ❌ FALTA: Índice para dataHora (usado em ORDER BY - DespesaDao.kt:53,66,79,81,93,130,148,154)
@Index(value = ["dataHora"])

// ❌ FALTA: Índice composto para queries por rota e data
@Index(value = ["rotaId", "dataHora"])

// ❌ FALTA: Índice composto para queries por origem e data
@Index(value = ["origemLancamento", "dataHora"])
```

**Queries Afetadas:**
- `DespesaDao.kt:53` - `WHERE rotaId = :rotaId AND origemLancamento = 'ROTA' ORDER BY dataHora DESC`
- `DespesaDao.kt:66,79,81,93` - `ORDER BY d.dataHora DESC`
- `DespesaDao.kt:130` - `WHERE rotaId = :rotaId AND origemLancamento = 'ROTA' AND dataHora BETWEEN ... ORDER BY dataHora DESC`

**Impacto:** Listagem de despesas ordenadas por data pode ser lenta sem índice.

#### 2. **Queries com LIKE sem Índices**

**Problema:** Queries com `LIKE '%texto%'` não podem usar índices eficientemente.

**Localização:**
- `EquipmentDao.kt:28` - `WHERE name LIKE '%' || :search || '%'`
- `SyncConfigDao.kt:60` - `WHERE key LIKE :pattern`

**Recomendação:**
- Para busca de texto, considerar usar FTS (Full-Text Search) do SQLite
- Ou limitar busca apenas ao início: `LIKE 'texto%'` (pode usar índice)

**Impacto:** Buscas de texto podem ser muito lentas com muitos registros.

#### 3. **Queries com strftime() sem Índices**

**Problema:** Funções de data no WHERE não podem usar índices.

**Localização:**
- `HistoricoCombustivelVeiculoDao.kt:17,30,34,38` - `strftime('%Y', data_abastecimento)`
- `HistoricoManutencaoVeiculoDao.kt:17,30` - `strftime('%Y', data_manutencao)`

**Recomendação:**
- Criar colunas calculadas ou índices funcionais (SQLite 3.38+)
- Ou usar range queries: `WHERE data_abastecimento >= ? AND data_abastecimento < ?`

**Impacto:** Queries por ano podem ser lentas.

#### 4. **Falta de Transações em Operações Múltiplas**

**Problema:** Algumas operações que modificam múltiplas tabelas não estão em transações.

**Recomendação:**
- Usar `@Transaction` em métodos que fazem múltiplas operações
- Garantir atomicidade em operações críticas

**Impacto:** Risco de inconsistência de dados em caso de falha.

---

### 🟡 PRIORIDADE MÉDIA (Melhorias incrementais)

#### 5. **Índices Compostos Otimizados**

**Recomendação:** Revisar índices compostos para garantir ordem correta das colunas.

**Regra:** Colunas mais seletivas primeiro, menos seletivas depois.

**Exemplo:**
```kotlin
// ✅ BOM: cliente_id é mais seletivo que data_acerto
@Index(value = ["cliente_id", "data_acerto"])

// ❌ REVISAR: Verificar se a ordem está correta em outros índices
```

#### 6. **Queries com Subqueries Complexas**

**Problema:** Algumas queries têm subqueries que podem ser otimizadas.

**Localização:**
- `ClienteDao.kt:20-37` - Query com subquery para debito_atual
- `AcertoDao.kt:36-48` - Query com subquery para últimos acertos

**Recomendação:**
- Considerar usar VIEWs ou materializar dados calculados
- Ou usar JOINs em vez de subqueries quando possível

**Impacto:** Queries podem ser mais lentas do que necessário.

#### 7. **Falta de Índices em Campos de Busca**

**Problema:** Campos usados em WHERE mas sem índices.

**Exemplos:**
- `Mesa.numero` - usado em busca mas sem índice
- `Equipment.location` - usado em filtro mas sem índice
- `CicloAcertoEntity.status` - usado em filtro mas sem índice

---

### 🟢 PRIORIDADE BAIXA (Otimizações finas)

#### 8. **Normalização de Dados**

**Status:** Banco já está bem normalizado.

**Observação:** Alguns campos JSON (metodosPagamentoJson) são aceitáveis para flexibilidade.

#### 9. **Tamanho do Banco**

**Recomendação:** Implementar limpeza periódica de logs antigos (SyncLog, SyncQueue).

**Status:** Já existe limpeza em alguns DAOs, mas pode ser expandida.

#### 10. **Uso de @Embedded e @Relation**

**Status:** Não há uso de @Embedded ou @Relation.

**Recomendação:** Considerar para reduzir queries aninhadas em alguns casos.

---

## 📋 RESUMO DE RECOMENDAÇÕES POR PRIORIDADE

### 🔴 PRIORIDADE ALTA (Implementar primeiro)

1. **Adicionar índices faltantes:**
   - **Mesa:** `numero`, `[cliente_id, ativa]`, `tipo_mesa`
   - **AcertoMesa:** `data_criacao`, `[mesa_id, data_criacao]` (acerto_id já tem)
   - **Equipment:** `name`, `location`
   - **CicloAcertoEntity:** `status`, `[rota_id, status]` (composto rota/ano/número já tem)
   - **Despesa:** `dataHora`, `[rotaId, dataHora]`, `[origemLancamento, dataHora]`

2. **Otimizar queries com strftime():**
   - Substituir por range queries ou colunas calculadas

3. **Adicionar @Transaction em operações múltiplas:**
   - Revisar métodos que fazem múltiplas inserções/atualizações

### 🟡 PRIORIDADE MÉDIA (Implementar depois)

4. **Otimizar queries com LIKE:**
   - Considerar FTS ou limitar busca ao início

5. **Revisar índices compostos:**
   - Garantir ordem correta das colunas

6. **Otimizar subqueries:**
   - Considerar JOINs ou VIEWs

### 🟢 PRIORIDADE BAIXA (Otimizações finas)

7. **Implementar limpeza automática de logs antigos**

8. **Considerar uso de @Embedded/@Relation em casos específicos**

---

## 📊 ESTIMATIVA DE IMPACTO

### Performance Esperada:
- **Queries de busca:** 50-80% mais rápidas com índices adequados
- **Queries com ORDER BY:** 30-60% mais rápidas
- **Queries com JOINs:** 40-70% mais rápidas com índices compostos

### Risco de Implementação:
- **BAIXO:** Adicionar índices não quebra funcionalidade existente
- **MÉDIO:** Otimizar queries requer testes cuidadosos
- **BAIXO:** Adicionar @Transaction melhora consistência

---

## 🎯 PLANO DE IMPLEMENTAÇÃO SUGERIDO

### FASE 1: Índices Essenciais (Baixo Risco)
- Adicionar índices simples faltantes
- Criar migration para novos índices
- Testar performance

### FASE 2: Otimização de Queries (Médio Risco)
- Substituir strftime() por range queries
- Otimizar queries com LIKE
- Adicionar @Transaction onde necessário

### FASE 3: Otimizações Avançadas (Baixo Risco)
- Revisar índices compostos
- Implementar limpeza automática
- Considerar VIEWs para queries complexas

---

## ⚠️ AVISOS IMPORTANTES

1. **NÃO REMOVER ÍNDICES EXISTENTES** sem análise cuidadosa
2. **TESTAR CADA MUDANÇA** em ambiente de desenvolvimento
3. **FAZER BACKUP** antes de aplicar migrations
4. **MONITORAR PERFORMANCE** após cada mudança

---

## 📝 NOTAS TÉCNICAS

- SQLite suporta até 64 índices por tabela (estamos bem abaixo)
- Índices aumentam ligeiramente o tamanho do banco (~5-10%)
- Índices melhoram SELECT mas podem tornar INSERT/UPDATE mais lentos (impacto mínimo)
- Room cria índices automaticamente para Foreign Keys, mas índices compostos devem ser explícitos

---

---

## 📊 ESTATÍSTICAS DO BANCO

### Estrutura Atual:
- **Total de Entidades:** 30+
- **Total de DAOs:** 28
- **Total de Índices Existentes:** ~40
- **Total de Foreign Keys:** ~25
- **Versão do Banco:** 44

### Índices Faltantes Identificados:
- **Prioridade Alta:** 12 índices
- **Prioridade Média:** 3 índices
- **Total:** 15 índices recomendados

### Queries a Otimizar:
- **Queries com strftime():** 5 queries
- **Queries com LIKE:** 2 queries
- **Queries sem @Transaction:** ~10 métodos

---

## 🎯 RESUMO EXECUTIVO

### Situação Atual:
O banco de dados está **bem estruturado** e segue a maioria das melhores práticas. A arquitetura é sólida, com Foreign Keys, TypeConverters e Migrations bem implementadas.

### Principais Oportunidades:
1. **Falta de índices** em campos usados frequentemente em ORDER BY e WHERE
2. **Queries com funções de data** (strftime) que não podem usar índices
3. **Falta de transações** em algumas operações múltiplas

### Impacto Esperado:
- **Performance:** Melhoria de 30-80% em queries frequentes
- **Risco:** BAIXO (adicionar índices não quebra funcionalidade)
- **Esforço:** MÉDIO (requer migrations e testes)

### Recomendação:
Implementar as melhorias em **3 fases**, começando pelos índices essenciais (Fase 1), que têm maior impacto e menor risco.

---

**Próximo Passo:** Aguardar aprovação para implementar as melhorias por fase.

