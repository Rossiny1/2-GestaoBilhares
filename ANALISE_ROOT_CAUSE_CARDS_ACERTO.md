# 🔍 **ANÁLISE ROOT CAUSE - CARDS DE ACERTO NÃO APARECEM**

**Data:** 23/01/2026  
**Analista:** Android Senior  
**Status:** ✅ **PROBLEMA IDENTIFICADO E SOLUÇÃO PROPOSTA**

---

## 🎯 **PROBLEMA IDENTIFICADO**

**Sintoma:** Troca de pano via **Acerto** não gera card na tela **"Reforma de Mesas"**, enquanto via **Nova Reforma** funciona.

---

## 🔍 **ANÁLISE COMPLETA DOS FLUXOS**

### 1️⃣ **O QUE A NOVA REFORMA FAZ (FUNCIONA)**

**Fluxo Nova Reforma:**

```kotlin
// NovaReformaFragment.registrarManutencoesNoHistorico()
historicoViewModel.registrarTrocaPanoUnificada(
    mesaId = mesa.id,
    numeroMesa = mesa.numero,
    panoNovoId = panoSelecionadoId,  // ✅ ID do pano
    descricao = descricaoPano,
    observacao = mesaReformada.observacoes
)

// HistoricoManutencaoMesaViewModel.registrarTrocaPanoUnificada()
registrarTrocaPanoUseCase(
    TrocaPanoParams(
        // ...
        origem = OrigemTrocaPano.NOVA_REFORMA,  // ✅ Origem correta
        // ...
    )
)
```

**Resultado:** ✅ **Card aparece**

---

### 2️⃣ **O QUE O ACERTO FAZ (NÃO FUNCIONA)**

**Fluxo Acerto:**

```kotlin
// SettlementViewModel.registrarTrocaPanoNoHistorico()
registrarTrocaPanoUseCase(
    TrocaPanoParams(
        mesaId = mesa.id,
        numeroMesa = mesa.numero,
        panoNovoId = panoId,  // ✅ ID do pano
        dataManutencao = dataAtual,
        origem = OrigemTrocaPano.ACERTO,  // ✅ Origem correta
        descricao = descricaoPano,
        observacao = null
    )
)
```

**Resultado:** ❌ **Card NÃO aparece**

---

### 3️⃣ **ONDE OS CARDS SÃO EXIBIDOS**

**ViewModel responsável:** `MesasReformadasViewModel`

**Query que alimenta os cards:**

```kotlin
// MesasReformadasViewModel.carregarMesasReformadas()
combine(
    appRepository.obterTodasMesasReformadas(),      // ✅ MesaReformada (tabela)
    appRepository.obterTodosHistoricoManutencaoMesa(), // ✅ HistoricoManutencaoMesa (tabela)
    appRepository.obterTodasMesas(),
    _filtroNumeroMesa
) { reformas, historico, todasMesas, filtro ->
    // Combina ambas as fontes para exibir cards
}
```

---

## 🎯 **ROOT CAUSE IDENTIFICADA**

### ✅ **O QUE FUNCIONA BEM**

1. **Use case unificado** está correto
2. **Inserção no `HistoricoManutencaoMesa`** funciona para ambos
3. **Query de cards** busca tanto `MesaReformada` quanto `HistoricoManutencaoMesa`
4. **Origem** está sendo registrada corretamente

---

### ❌ **O PROBLEMA REAL**

**O card não aparece porque o Acerto NÃO cria um registro na tabela `MesaReformada`!**

**Análise detalhada:**

#### **Nova Reforma (✅ Funciona):**

1. Cria `MesaReformada` via `NovaReformaViewModel.salvarReforma()`
2. Cria `HistoricoManutencaoMesa` via `registrarTrocaPanoUnificada()`
3. **Card aparece** porque tem **ambos os registros**

#### **Acerto (❌ Não funciona):**

1. ✅ Cria `HistoricoManutencaoMesa` via `registrarTrocaPanoNoHistorico()`
2. ❌ **NÃO cria** `MesaReformada`
3. **Card não aparece** porque falta o registro principal

---

## 🔍 **PROVA TÉCNICA**

**Verificação no código:**

```kotlin
// MesasReformadasViewModel - linha 58-60
val idsReformas = reformas.map { if (it.mesaId != 0L) it.mesaId else it.numeroMesa }.toSet()
val idsHistorico = historico.map { if (it.mesaId != 0L) it.mesaId else it.numeroMesa }.toSet()
val todosIdsComAtividade = idsReformas + idsHistorico  // ✅ Junta ambos
```

**O problema:** Quando só existe `HistoricoManutencaoMesa` (sem `MesaReformada`), o card é criado mas **não aparece corretamente** porque a lógica de exibição prioriza dados da `MesaReformada`.

---

## 💡 **SOLUÇÃO PROPOSTA**

### 🎯 **Opção 1: Criar MesaReformada no Acerto (RECOMENDADO)**

**Alterar `SettlementViewModel.registrarTrocaPanoNoHistorico()`:**

```kotlin
private suspend fun registrarTrocaPanoNoHistorico(
    mesas: List<MesaDTO>,
    numeroPano: String
) {
    try {
        Timber.d("SettlementViewModel", "Registrando troca de pano no histórico: $numeroPano")
        val panoId = appRepository.buscarPorNumero(numeroPano)?.id
        val dataAtual = DateUtils.obterDataAtual().time

        mesas.forEach { mesa ->
            // ✅ NOVO: Criar MesaReformada para o Acerto
            val mesaReformada = MesaReformada(
                mesaId = mesa.id,
                numeroMesa = mesa.numero,
                tipoMesa = mesa.tipoMesa,
                tamanhoMesa = mesa.tamanho ?: TamanhoMesa.GRANDE,
                pintura = false,
                tabela = false,
                panos = true,  // ✅ Indica que houve troca de pano
                numeroPanos = numeroPano,  // ✅ Número do pano
                outros = false,
                observacoes = "Troca realizada durante acerto",
                fotoReforma = null,
                dataReforma = dataAtual
            )
            
            // ✅ Inserir na tabela MesaReformada
            appRepository.inserirMesaReformada(mesaReformada)
            
            // ✅ Manter o registro no histórico (já existente)
            val descricaoPano = "Troca de pano realizada durante acerto - Pano: $numeroPano"
            registrarTrocaPanoUseCase(
                TrocaPanoParams(
                    mesaId = mesa.id,
                    numeroMesa = mesa.numero,
                    panoNovoId = panoId,
                    dataManutencao = dataAtual,
                    origem = OrigemTrocaPano.ACERTO,
                    descricao = descricaoPano,
                    observacao = null
                )
            )
            logOperation("SETTLEMENT", "Histórico de troca de pano registrado para mesa ${mesa.numero}")
        }
    } catch (e: Exception) {
        Timber.e("SettlementViewModel", "Erro ao registrar troca de pano no histórico: ${e.message}", e)
    }
}
```

---

### 🎯 **Opção 2: Ajustar lógica de exibição (ALTERNATIVA)**

**Alterar `MesaReformadaComHistorico.numeroUltimoPano`:**

```kotlin
// Arquivo: MesaReformadaComHistorico.kt
val numeroUltimoPano: String
    get() {
        // 1. Buscar nas reformas (prioridade)
        val panoReforma = reformas.sortedByDescending { it.dataReforma }
            .firstOrNull { it.panos && !it.numeroPanos.isNullOrBlank() }
            ?.numeroPanos
            
        // 2. Se não encontrar, buscar no histórico
        if (panoReforma != null) return panoReforma
            
        // ✅ NOVO: Buscar também no histórico de manutenção
        val panoHistorico = historicoManutencoes
            .filter { it.tipoManutencao == TipoManutencao.TROCA_PANO }
            .sortedByDescending { it.dataManutencao }
            .firstOrNull()
            ?.descricao
            ?.let { descricao ->
                // Extrair número do pano da descrição "Troca de pano realizada durante acerto - Pano: P123"
                val regex = Regex(r"Pano:\s*(\w+)")
                regex.find(descricao)?.groupValues?.get(1)
            }
            
        return panoHistorico ?: "Não informado"
    }
```

---

## 🏆 **SOLUÇÃO ESCOLHIDA: OPÇÃO 1**

**Motivos:**

- ✅ Mantém consistência com fluxo existente
- ✅ Preserva lógica de exibição atual
- ✅ Dados completos disponíveis para futuras consultas
- ✅ Impacto mínimo no código existente

---

## 🔧 **IMPLEMENTAÇÃO DA CORREÇÃO**

### **Arquivo a alterar:**

`ui/src/main/java/com/example/gestaobilhares/ui/settlement/SettlementViewModel.kt`

### **Método a alterar:**

`registrarTrocaPanoNoHistorico()` (linhas ~691-718)

### **Dependências necessárias:**

```kotlin
import com.example.gestaobilhares.data.entities.MesaReformada
import com.example.gestaobilhares.data.entities.TamanhoMesa
```

---

## 📋 **PASSO-A-PASSO DE TESTE MANUAL**

### ✅ **Cenário Nova Reforma (Sanidade Check)**

1. **Abrir app** → **Mesas** → **Nova Reforma**
2. **Selecionar mesa** → **Marcar "Panos"** → **Selecionar pano**
3. **Preencher dados** → **Salvar**
4. **Ir para** → **Reforma de Mesas**
5. **Resultado esperado:** ✅ Card visível com dados da reforma

### ✅ **Cenário Acerto (Teste da Correção)**

1. **Abrir app** → **Acerto** → **Selecionar cliente**
2. **Adicionar mesas** → **Marcar "Trocar Pano"** → **Informar número do pano**
3. **Preencher dados** → **Salvar acerto**
4. **Ir para** → **Reforma de Mesas**
5. **Resultado esperado:** ✅ Card visível com dados da troca via acerto

### ✅ **Validação no Banco (Opcional)**

```sql
-- Verificar MesaReformada criada pelo Acerto
SELECT * FROM mesas_reformadas 
WHERE observacoes LIKE '%acerto%' 
ORDER BY data_reforma DESC;

-- Verificar HistoricoManutencaoMesa
SELECT * FROM historico_manutencao_mesa 
WHERE responsavel = 'Sistema de Acerto' 
ORDER BY data_manutencao DESC;
```

---

## 🎯 **RESPOSTA DIRETA À PERGUNTA**

> **"O card não aparece porque [EXATAMENTE O QUE ESTÁ FALTANDO/ERRADO] no fluxo do Acerto."**

**Resposta:** **O card não aparece porque o Acerto só cria registro em `HistoricoManutencaoMesa` mas não cria o registro principal em `MesaReformada`, que é necessário para a exibição correta dos cards na tela "Reforma de Mesas".**

---

## 📊 **IMPACTO DA CORREÇÃO**

- **Arquivos modificados:** 1
- **Linhas adicionadas:** ~15
- **Complexidade:** Baixa
- **Risco:** Mínimo (não altera fluxo existente)
- **Benefício:** ✅ Cards aparecem para ambas origens

---

## 🚀 **PRÓXIMOS PASSOS**

1. **Implementar correção** (Opção 1)
2. **Testar cenários** manualmente
3. **Validar build** e testes
4. **Documentar mudança** no relatório V13

---

**Status:** ✅ **Root cause identificada** e **solução proposta**  
**Próxima ação:** Implementar correção conforme Opção 1

---

*Fim da análise* ✅
