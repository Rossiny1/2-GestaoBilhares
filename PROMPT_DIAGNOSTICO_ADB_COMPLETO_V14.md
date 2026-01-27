# 🔍 PROMPT: Diagnóstico ADB Completo - Cards Acerto não aparecem

## 🎯 Objetivo

Criar um diagnóstico DEFINITIVO usando logs ADB e queries SQL para identificar EXATAMENTE por que os cards de Acerto não aparecem, mesmo após implementação do V13.

---

## 📋 Contexto Crítico

**Status atual:**
- ✅ Build compila sem erros
- ✅ Testes unitários passam
- ✅ App instala no dispositivo
- ❌ **Cards de Acerto AINDA NÃO aparecem na tela "Reforma de Mesas"**
- ✅ Cards de Nova Reforma continuam funcionando

**Hipóteses a investigar:**
1. Use case não está sendo chamado pelo Acerto
2. Use case está falhando silenciosamente (exception engolida)
3. `MesaReformada` não está sendo inserido no banco
4. `MesaReformada` está sendo inserido com campos incorretos
5. Query da tela "Reforma de Mesas" não está pegando registros do Acerto
6. Multi-tenancy está filtrando registros do Acerto

---

## 🔧 FASE 1: Adicionar Logs Estratégicos

### 1️⃣ Instrumentar RegistrarTrocaPanoUseCase

**Arquivo:** `ui/src/main/java/com/example/gestaobilhares/ui/mesas/usecases/RegistrarTrocaPanoUseCase.kt`

**Adicionar logs em TODOS os pontos críticos:**

```kotlin
class RegistrarTrocaPanoUseCase @Inject constructor(
    private val appRepository: AppRepository
) {
    suspend operator fun invoke(params: TrocaPanoParams) {
        Log.d("DEBUG_CARDS", "════════════════════════════════════════")
        Log.d("DEBUG_CARDS", "🔵 USE CASE INICIADO")
        Log.d("DEBUG_CARDS", "   Mesa: ${params.numeroMesa} (ID: ${params.mesaId})")
        Log.d("DEBUG_CARDS", "   Origem: ${params.origem}")
        Log.d("DEBUG_CARDS", "   Pano ID: ${params.panoNovoId}")
        Log.d("DEBUG_CARDS", "   Descrição: ${params.descricao}")

        try {
            // 1. Buscar mesa
            Log.d("DEBUG_CARDS", "🔍 Buscando mesa ${params.mesaId}...")
            val mesa = appRepository.obterMesaPorId(params.mesaId)

            if (mesa == null) {
                Log.e("DEBUG_CARDS", "❌ ERRO: Mesa ${params.mesaId} não encontrada!")
                throw IllegalArgumentException("Mesa ${params.mesaId} não encontrada")
            }

            Log.d("DEBUG_CARDS", "✅ Mesa encontrada: ${mesa.numero} (Tipo: ${mesa.tipoMesa})")

            // 2. Criar MesaReformada
            val numeroPanoExtraido = extrairNumeroPano(params.descricao)
            Log.d("DEBUG_CARDS", "🔍 Número pano extraído: $numeroPanoExtraido")

            val mesaReformada = MesaReformada(
                mesaId = params.mesaId,
                numeroMesa = params.numeroMesa,
                tipoMesa = mesa.tipoMesa,
                tamanhoMesa = mesa.tamanho ?: TamanhoMesa.GRANDE,
                pintura = false,
                tabela = false,
                panos = true,
                numeroPanos = numeroPanoExtraido ?: params.panoNovoId?.toString() ?: "",
                outros = false,
                observacoes = when (params.origem) {
                    OrigemTrocaPano.NOVA_REFORMA -> params.observacao ?: "Troca de pano via reforma"
                    OrigemTrocaPano.ACERTO -> "Troca realizada durante acerto"
                },
                fotoReforma = null,
                dataReforma = params.dataManutencao
            )

            Log.d("DEBUG_CARDS", "📝 MesaReformada criada:")
            Log.d("DEBUG_CARDS", "   - mesaId: ${mesaReformada.mesaId}")
            Log.d("DEBUG_CARDS", "   - numeroMesa: ${mesaReformada.numeroMesa}")
            Log.d("DEBUG_CARDS", "   - panos: ${mesaReformada.panos}")
            Log.d("DEBUG_CARDS", "   - numeroPanos: ${mesaReformada.numeroPanos}")
            Log.d("DEBUG_CARDS", "   - observacoes: ${mesaReformada.observacoes}")
            Log.d("DEBUG_CARDS", "   - dataReforma: ${mesaReformada.dataReforma}")

            // 3. Inserir MesaReformada
            Log.d("DEBUG_CARDS", "💾 Inserindo MesaReformada no banco...")
            val idReforma = appRepository.inserirMesaReformada(mesaReformada)
            Log.d("DEBUG_CARDS", "✅ MesaReformada inserida com ID: $idReforma")

            // 4. Criar e inserir HistoricoManutencaoMesa
            val historico = HistoricoManutencaoMesa(
                mesaId = params.mesaId,
                numeroMesa = params.numeroMesa,
                tipoManutencao = TipoManutencao.TROCA_PANO,
                descricao = params.descricao,
                dataManutencao = params.dataManutencao,
                responsavel = when (params.origem) {
                    OrigemTrocaPano.NOVA_REFORMA -> "Reforma de mesa"
                    OrigemTrocaPano.ACERTO -> "Sistema de Acerto"
                },
                observacoes = params.observacao
            )

            Log.d("DEBUG_CARDS", "💾 Inserindo HistoricoManutencaoMesa...")
            val idHistorico = appRepository.inserirHistoricoManutencaoMesaSync(historico)
            Log.d("DEBUG_CARDS", "✅ HistoricoManutencaoMesa inserido com ID: $idHistorico")

            // 5. Atualizar pano atual da mesa
            if (params.panoNovoId != null) {
                Log.d("DEBUG_CARDS", "🔄 Atualizando pano atual da mesa...")
                val mesaAtualizada = mesa.copy(
                    panoAtualId = params.panoNovoId,
                    dataUltimaTrocaPano = params.dataManutencao
                )
                appRepository.atualizarMesa(mesaAtualizada)
                Log.d("DEBUG_CARDS", "✅ Mesa atualizada com novo pano")
            }

            Log.d("DEBUG_CARDS", "🎉 USE CASE CONCLUÍDO COM SUCESSO!")
            Log.d("DEBUG_CARDS", "════════════════════════════════════════")

        } catch (e: Exception) {
            Log.e("DEBUG_CARDS", "❌❌❌ ERRO NO USE CASE ❌❌❌")
            Log.e("DEBUG_CARDS", "Mesa: ${params.numeroMesa}")
            Log.e("DEBUG_CARDS", "Origem: ${params.origem}")
            Log.e("DEBUG_CARDS", "Exception: ${e.javaClass.simpleName}")
            Log.e("DEBUG_CARDS", "Message: ${e.message}")
            Log.e("DEBUG_CARDS", "StackTrace:", e)
            Log.e("DEBUG_CARDS", "════════════════════════════════════════")
            throw e
        }
    }

    private fun extrairNumeroPano(descricao: String?): String? {
        if (descricao == null) return null
        return Regex("""Pano:\\s*(\\w+)""").find(descricao)?.groupValues?.get(1)
    }
}
```

---

### 2️⃣ Instrumentar SettlementViewModel

**Arquivo:** `ui/src/main/java/com/example/gestaobilhares/ui/settlement/SettlementViewModel.kt`

**Adicionar logs no método de registro:**

```kotlin
private suspend fun registrarTrocaPanoNoHistorico(
    mesas: List<MesaDTO>,
    numeroPano: String
) {
    Log.d("DEBUG_CARDS", "")
    Log.d("DEBUG_CARDS", "╔════════════════════════════════════════╗")
    Log.d("DEBUG_CARDS", "║   ACERTO - Registrando Troca de Pano  ║")
    Log.d("DEBUG_CARDS", "╚════════════════════════════════════════╝")
    Log.d("DEBUG_CARDS", "📋 Total mesas: ${mesas.size}")
    Log.d("DEBUG_CARDS", "📋 Pano: $numeroPano")

    try {
        val panoId = appRepository.buscarPorNumero(numeroPano)?.id
        Log.d("DEBUG_CARDS", "🔍 Pano ID encontrado: $panoId")

        val dataAtual = DateUtils.obterDataAtual().time
        Log.d("DEBUG_CARDS", "📅 Data atual: $dataAtual")

        mesas.forEachIndexed { index, mesa ->
            Log.d("DEBUG_CARDS", "")
            Log.d("DEBUG_CARDS", "─────────────────────────────────────────")
            Log.d("DEBUG_CARDS", "🔹 Mesa ${index + 1}/${mesas.size}")
            Log.d("DEBUG_CARDS", "   ID: ${mesa.id}")
            Log.d("DEBUG_CARDS", "   Número: ${mesa.numero}")

            val descricaoPano = "Troca de pano realizada durante acerto - Pano: $numeroPano"

            Log.d("DEBUG_CARDS", "🚀 Chamando registrarTrocaPanoUseCase...")

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

            Log.d("DEBUG_CARDS", "✅ Use case executado para mesa ${mesa.numero}")
            logOperation("SETTLEMENT", "Troca de pano registrada para mesa ${mesa.numero}")
        }

        Log.d("DEBUG_CARDS", "")
        Log.d("DEBUG_CARDS", "╔════════════════════════════════════════╗")
        Log.d("DEBUG_CARDS", "║   ACERTO - Concluído com Sucesso       ║")
        Log.d("DEBUG_CARDS", "╚════════════════════════════════════════╝")

    } catch (e: Exception) {
        Log.e("DEBUG_CARDS", "")
        Log.e("DEBUG_CARDS", "╔════════════════════════════════════════╗")
        Log.e("DEBUG_CARDS", "║   ACERTO - ERRO FATAL                  ║")
        Log.e("DEBUG_CARDS", "╚════════════════════════════════════════╝")
        Log.e("DEBUG_CARDS", "Exception: ${e.javaClass.simpleName}")
        Log.e("DEBUG_CARDS", "Message: ${e.message}")
        Log.e("DEBUG_CARDS", "StackTrace:", e)
        Timber.e("SettlementViewModel", "Erro ao registrar troca de pano: ${e.message}", e)
    }
}
```

---

### 3️⃣ Instrumentar MesasReformadasViewModel

**Arquivo:** `ui/src/main/java/com/example/gestaobilhares/ui/mesas/MesasReformadasViewModel.kt`

**Adicionar logs na query que alimenta os cards:**

```kotlin
private fun carregarMesasReformadas() {
    viewModelScope.launch {
        try {
            Log.d("DEBUG_CARDS", "")
            Log.d("DEBUG_CARDS", "┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓")
            Log.d("DEBUG_CARDS", "┃  CARREGANDO CARDS - Reforma de Mesas  ┃")
            Log.d("DEBUG_CARDS", "┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛")

            combine(
                appRepository.obterTodasMesasReformadas(),
                appRepository.obterTodosHistoricoManutencaoMesa(),
                appRepository.obterTodasMesas(),
                _filtroNumeroMesa
            ) { reformas, historico, todasMesas, filtro ->

                Log.d("DEBUG_CARDS", "📊 Dados recebidos:")
                Log.d("DEBUG_CARDS", "   - Total MesasReformadas: ${reformas.size}")
                Log.d("DEBUG_CARDS", "   - Total HistoricoManutencaoMesa: ${historico.size}")
                Log.d("DEBUG_CARDS", "   - Total Mesas: ${todasMesas.size}")

                // Log detalhado das reformas
                Log.d("DEBUG_CARDS", "")
                Log.d("DEBUG_CARDS", "📋 MesasReformadas (primeiras 5):")
                reformas.take(5).forEach { reforma ->
                    Log.d("DEBUG_CARDS", "   - ID: ${reforma.id}")
                    Log.d("DEBUG_CARDS", "     Mesa: ${reforma.numeroMesa} (ID: ${reforma.mesaId})")
                    Log.d("DEBUG_CARDS", "     Panos: ${reforma.panos} | Número: ${reforma.numeroPanos}")
                    Log.d("DEBUG_CARDS", "     Observações: ${reforma.observacoes}")
                    Log.d("DEBUG_CARDS", "     Data: ${reforma.dataReforma}")
                }

                // Log de reformas do Acerto especificamente
                val reformasAcerto = reformas.filter { 
                    it.observacoes?.contains("acerto", ignoreCase = true) == true 
                }
                Log.d("DEBUG_CARDS", "")
                Log.d("DEBUG_CARDS", "🔍 Reformas do ACERTO encontradas: ${reformasAcerto.size}")
                reformasAcerto.forEach { reforma ->
                    Log.d("DEBUG_CARDS", "   ⭐ Mesa: ${reforma.numeroMesa} | Pano: ${reforma.numeroPanos}")
                }

                // Log de histórico do Acerto
                val historicoAcerto = historico.filter { 
                    it.responsavel == "Sistema de Acerto" 
                }
                Log.d("DEBUG_CARDS", "")
                Log.d("DEBUG_CARDS", "🔍 Históricos do ACERTO encontrados: ${historicoAcerto.size}")
                historicoAcerto.forEach { hist ->
                    Log.d("DEBUG_CARDS", "   ⭐ Mesa: ${hist.numeroMesa} | Desc: ${hist.descricao}")
                }

                // Continuar processamento normal...
                val idsReformas = reformas.map { 
                    if (it.mesaId != 0L) it.mesaId else it.numeroMesa 
                }.toSet()

                val idsHistorico = historico.map { 
                    if (it.mesaId != 0L) it.mesaId else it.numeroMesa 
                }.toSet()

                val todosIdsComAtividade = idsReformas + idsHistorico

                Log.d("DEBUG_CARDS", "")
                Log.d("DEBUG_CARDS", "📊 IDs com atividade:")
                Log.d("DEBUG_CARDS", "   - IDs de Reformas: ${idsReformas.size}")
                Log.d("DEBUG_CARDS", "   - IDs de Histórico: ${idsHistorico.size}")
                Log.d("DEBUG_CARDS", "   - Total únicos: ${todosIdsComAtividade.size}")

                // ... resto do código original de processamento

            }.collect { mesasComHistorico ->
                Log.d("DEBUG_CARDS", "")
                Log.d("DEBUG_CARDS", "✅ Cards gerados: ${mesasComHistorico.size}")
                Log.d("DEBUG_CARDS", "┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛")

                _mesasReformadas.value = mesasComHistorico
            }

        } catch (e: Exception) {
            Log.e("DEBUG_CARDS", "❌ Erro ao carregar cards:", e)
        }
    }
}
```

---

## 🔄 FASE 2: Rebuild e Reinstalar

**Comandos Windows-safe:**

```bash
# 1. Limpar projeto
.\gradlew.bat clean

# 2. Build com logs
.\gradlew.bat assembleDebug --build-cache --parallel

# 3. Instalar no dispositivo
.\gradlew.bat installDebug

# 4. Verificar instalação
adb devices
```

---

## 🧪 FASE 3: Executar Testes Controlados

### Teste 1: Nova Reforma (Baseline - deve funcionar)

**Passos:**

1. Abrir app no dispositivo
2. Ir em: Mesas → Nova Reforma
3. Selecionar mesa "M01"
4. Marcar "Panos" + escolher pano "P100"
5. Salvar reforma
6. Voltar e ir em "Reforma de Mesas"

**Capturar logs em paralelo:**

```bash
# Terminal 1: Capturar logs de DEBUG_CARDS
adb logcat -c && adb logcat -s DEBUG_CARDS:D

# Terminal 2: Capturar erros gerais
adb logcat *:E
```

---

### Teste 2: Acerto (Problema - deve falhar)

**Passos:**

1. Ir em: Acerto
2. Selecionar cliente
3. Adicionar mesa "M02"
4. Marcar "Trocar Pano" + informar "P200"
5. Salvar acerto
6. Voltar e ir em "Reforma de Mesas"

**Capturar logs em paralelo:**

```bash
# Terminal 1: Capturar logs de DEBUG_CARDS
adb logcat -c && adb logcat -s DEBUG_CARDS:D

# Terminal 2: Capturar erros gerais
adb logcat *:E
```

---

## 📊 FASE 4: Queries SQL Diretas no Banco

### Query 1: Verificar MesasReformadas

```bash
adb shell "run-as com.example.gestaobilhares sqlite3 /data/data/com.example.gestaobilhares/databases/gestaobilhares.db \"SELECT id, mesa_id, numero_mesa, panos, numero_panos, observacoes, data_reforma FROM mesas_reformadas ORDER BY data_reforma DESC LIMIT 10;\""
```

**Expectativa:**
- Deve mostrar registros de Nova Reforma E Acerto
- Observações do Acerto devem conter "acerto"

---

### Query 2: Verificar HistoricoManutencaoMesa

```bash
adb shell "run-as com.example.gestaobilhares sqlite3 /data/data/com.example.gestaobilhares/databases/gestaobilhares.db \"SELECT id, mesa_id, numero_mesa, responsavel, descricao, data_manutencao FROM historico_manutencao_mesa WHERE responsavel = 'Sistema de Acerto' ORDER BY data_manutencao DESC LIMIT 10;\""
```

**Expectativa:**
- Deve mostrar registros com responsável "Sistema de Acerto"

---

### Query 3: Verificar Mesas atualizadas

```bash
adb shell "run-as com.example.gestaobilhares sqlite3 /data/data/com.example.gestaobilhares/databases/gestaobilhares.db \"SELECT id, numero, pano_atual_id, data_ultima_troca_pano FROM mesas WHERE data_ultima_troca_pano > 0 ORDER BY data_ultima_troca_pano DESC LIMIT 10;\""
```

**Expectativa:**
- Deve mostrar mesas com pano_atual_id atualizado

---

### Query 4: Contar registros por origem

```bash
adb shell "run-as com.example.gestaobilhares sqlite3 /data/data/com.example.gestaobilhares/databases/gestaobilhares.db \"SELECT observacoes, COUNT(*) as total FROM mesas_reformadas GROUP BY observacoes;\""
```

**Expectativa:**
- Linha com "Troca realizada durante acerto" deve existir

---

## 📋 FASE 5: Análise de Resultados

### Checklist de Diagnóstico

Use este checklist para identificar o problema:

**[ ] 1. Use case é chamado pelo Acerto?**
- Procurar nos logs: "ACERTO - Registrando Troca de Pano"
- Se NÃO aparece: Problema está antes do use case (SettlementViewModel)

**[ ] 2. Use case recebe parâmetros corretos?**
- Procurar nos logs: "USE CASE INICIADO" + parâmetros
- Verificar: mesaId, origem=ACERTO, panoNovoId

**[ ] 3. Mesa é encontrada no banco?**
- Procurar nos logs: "Mesa encontrada"
- Se NÃO: Problema de ID incorreto ou mesa não existe

**[ ] 4. MesaReformada é criada?**
- Procurar nos logs: "MesaReformada criada" + detalhes
- Verificar: panos=true, numeroPanos preenchido

**[ ] 5. MesaReformada é inserida com sucesso?**
- Procurar nos logs: "MesaReformada inserida com ID"
- Se ID = -1 ou erro: Problema de constraint/foreign key

**[ ] 6. HistoricoManutencaoMesa é inserido?**
- Procurar nos logs: "HistoricoManutencaoMesa inserido com ID"

**[ ] 7. Exception acontece e é engolida?**
- Procurar nos logs: "ERRO NO USE CASE"
- Analisar stacktrace completo

**[ ] 8. Registros existem no banco?**
- Executar Query 1 e Query 2
- Comparar quantidade de registros

**[ ] 9. Query dos cards busca registros do Acerto?**
- Procurar nos logs: "Reformas do ACERTO encontradas"
- Se = 0: Registros não existem OU filtro está errado

**[ ] 10. Cards são gerados mas não aparecem?**
- Procurar nos logs: "Cards gerados"
- Se quantidade correta mas UI não mostra: Problema de binding

---

## 🎯 FASE 6: Solução Baseada no Diagnóstico

### Cenário A: Use case não é chamado

**Problema:** Logs do Acerto não aparecem

**Solução:** Verificar se `registrarTrocaPanoNoHistorico` está sendo chamado no fluxo de salvamento do acerto

---

### Cenário B: Exception silenciosa

**Problema:** Logs mostram "ERRO NO USE CASE"

**Solução:** Analisar stacktrace e corrigir:
- Foreign key constraint
- Campos nulos obrigatórios
- Tipo de dados incorreto

---

### Cenário C: Registros não inseridos

**Problema:** Queries SQL não retornam registros do Acerto

**Solução:** Verificar:
- Transaction não commitada
- Multi-tenancy filtrando registros
- DAO usando métodos síncronos vs assíncronos

---

### Cenário D: Registros existem mas cards não aparecem

**Problema:** Query 1 mostra registros, mas logs da tela mostram 0

**Solução:** Verificar:
- Flow não está emitindo novos valores
- Cache antigo sendo usado
- Filtro de tenant/usuário na query

---

## 📦 Entrega Esperada

Ao final do diagnóstico, você terá:

1. **Logs completos** do fluxo Acerto (salvos em arquivo .txt)
2. **Resultados das queries SQL** (screenshot ou texto)
3. **Checklist preenchido** identificando exatamente onde o problema ocorre
4. **Solução proposta** baseada no cenário diagnosticado

---

## 🔄 Comandos de Coleta Rápida

**Script único para coletar tudo:**

```bash
# Limpar logs antigos
adb logcat -c

# Executar teste no app (Acerto com troca de pano)
# ... aguardar execução ...

# Capturar logs
adb logcat -d -s DEBUG_CARDS:D > logs_acerto_debug.txt

# Capturar erros
adb logcat -d *:E > logs_acerto_errors.txt

# Executar queries
adb shell "run-as com.example.gestaobilhares sqlite3 /data/data/com.example.gestaobilhares/databases/gestaobilhares.db \"SELECT * FROM mesas_reformadas WHERE observacoes LIKE '%acerto%' ORDER BY data_reforma DESC;\"" > query_reformas_acerto.txt

adb shell "run-as com.example.gestaobilhares sqlite3 /data/data/com.example.gestaobilhares/databases/gestaobilhares.db \"SELECT * FROM historico_manutencao_mesa WHERE responsavel = 'Sistema de Acerto' ORDER BY data_manutencao DESC;\"" > query_historico_acerto.txt
```

---

## ✅ Critérios de Sucesso

Após implementar logs e executar diagnóstico:

1. **Logs claramente mostram** onde o fluxo para
2. **Queries SQL provam** se dados estão no banco ou não
3. **Problema identificado** sem ambiguidade
4. **Solução aplicada** com base em evidências concretas
5. **Cards aparecem** para ambas origens

---

**Versão:** V14 - Diagnóstico ADB Completo  
**Status:** Pronto para execução  
**Objetivo:** Identificar root cause DEFINITIVO e resolver de uma vez
