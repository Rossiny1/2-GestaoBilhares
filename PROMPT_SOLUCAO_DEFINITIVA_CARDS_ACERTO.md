# 🎯 PROMPT: Solução definitiva para cards de troca de pano via ACERTO - Abordagem incremental

**Para:** IA Android Senior (Cascade, Claude, Windsurf, etc.)  
**Projeto:** Gestão de Bilhares  
**Data:** 23/01/2026  
**Protocolo:** [AI_USAGE.md] - GATES obrigatórios aplicam-se

---

## ⚠️ INSTRUÇÕES CRÍTICAS - LEIA PRIMEIRO

Você é um desenvolvedor Android Sênior trabalhando neste projeto. Este prompt segue o protocolo de trabalho com IA do projeto [AI_USAGE.md].

**REGRAS OBRIGATÓRIAS:**
1. ✅ Execute em **2 fases separadas** com validação entre cada uma
2. ✅ Build + teste após cada fase
3. ✅ Se algo falhar 3 vezes, PARE e peça ajuda humana (Gate 4)
4. ❌ NÃO inventar comandos Gradle ou tasks
5. ❌ NÃO tocar em sync Firebase, multi-tenancy, migrations existentes
6. ❌ NÃO adicionar migration de banco de dados

---

## 📋 CONTEXTO DO PROBLEMA

### Bug Original
- **Sintoma:** Trocas de pano feitas na tela **Acerto** não geram cards na tela "Reforma de Mesas"
- **Status atual:** Hotfix V15 implementado (filtro por string em `observacoes`)
- **Problema do hotfix:** Frágil, depende de texto exato "Troca realizada durante acerto"

### Diagnóstico Confirmado
- ✅ `SettlementViewModel` → chama use case com `OrigemTrocaPano.ACERTO` corretamente
- ✅ `RegistrarTrocaPanoUseCase` → grava em `MesaReformada` corretamente
- ❌ `MesasReformadasViewModel` → filtro baseado em string é frágil
- 📊 Logs mostram: `Total HistoricoManutencaoMesa: 0` (entidade subutilizada)

### Objetivo da Solução
1. **Curto prazo (FASE 1):** Tornar o hotfix mais resiliente sem alterar arquitetura
2. **Definitivo (FASE 2):** Usar dados estruturados existentes (`HistoricoManutencaoMesa`) em vez de inferir por texto

---

## 🎯 FASE 1: HOTFIX RESILIENTE (20 minutos)

### Gate 1: Plano de Ação

```text
PLANO DE AÇÃO - FASE 1

Objetivo: Tornar o filtro de "reformas do ACERTO" resiliente a variações de texto

Módulos afetados:
- ui (MesasReformadasViewModel)

Impacto no multi-tenancy: NÃO

Riscos: BAIXO (apenas lógica de filtro, sem alteração de dados)

Passos propostos:
1. Ajustar filtro em MesasReformadasViewModel (1 alteração)
2. Criar teste unitário para lógica de filtro (novo arquivo)
3. Build e validação

Critério de sucesso:
- Cards do Acerto aparecem mesmo se o texto mudar levemente
- Teste unitário passa
- Build sem erros
- Validação manual: cards aparecem
```

### Gate 2: Escopo Definido

```text
ESCOPO DEFINIDO - FASE 1

Arquivos a modificar:
- ui/src/main/java/.../mesas/MesasReformadasViewModel.kt
  Motivo: Melhorar lógica de filtro de reformas do Acerto

Arquivos a criar:
- ui/src/test/java/.../mesas/MesasReformadasViewModelTest.kt (ou similar)
  Motivo: Teste unitário para validar filtro

Arquivos a NÃO tocar:
- SettlementViewModel.kt (já funciona)
- RegistrarTrocaPanoUseCase.kt (já funciona)
- Entidades de data (MesaReformada, HistoricoManutencaoMesa)
- Módulos sync, core, data
- Migrations

Validação necessária:
- Build: ./gradlew assembleDebug --build-cache --parallel
- Testes: ./gradlew testDebugUnitTest
- Validação manual no device
```

### Gate 3: Implementação

#### 1. Ajustar MesasReformadasViewModel

**Localizar o método** `carregarMesasReformadas()` ou similar onde está o filtro atual:

```kotlin
// ❌ ANTES (V15 - frágil)
val reformasAcerto = reformas.filter { 
    it.observacoes?.contains("Troca realizada durante acerto", ignoreCase = true) == true 
}
```

**Substituir por:**

```kotlin
// ✅ DEPOIS (resiliente)
val reformasAcerto = reformas.filter { reforma ->
    reforma.observacoes?.let { obs ->
        // Procura por padrões que indicam origem do Acerto
        val contemAcerto = obs.contains("acerto", ignoreCase = true)
        val contemContexto = obs.contains("durante", ignoreCase = true) || 
                              obs.contains("via acerto", ignoreCase = true) ||
                              obs.contains("realizada", ignoreCase = true)

        contemAcerto && contemContexto
    } == true
}

// Log para diagnóstico
Log.d("DEBUG_CARDS", "🔍 Filtro resiliente:")
Log.d("DEBUG_CARDS", "   - Reformas encontradas: ${reformasAcerto.size}")
reformasAcerto.forEach {
    Log.d("DEBUG_CARDS", "   - Mesa ${it.numeroMesa}: ${it.observacoes}")
}
```

#### 2. Criar teste unitário

**Arquivo:** `ui/src/test/java/.../mesas/ReformaFilterTest.kt` (ou nome adequado)

```kotlin
import org.junit.Test
import org.junit.Assert.*

class ReformaFilterTest {

    @Test
    fun `filtro identifica troca de pano do acerto - texto padrao`() {
        val observacao = "Troca realizada durante acerto"

        val contemAcerto = observacao.contains("acerto", ignoreCase = true)
        val contemContexto = observacao.contains("durante", ignoreCase = true) ||
                              observacao.contains("via acerto", ignoreCase = true) ||
                              observacao.contains("realizada", ignoreCase = true)

        assertTrue(contemAcerto && contemContexto)
    }

    @Test
    fun `filtro identifica troca de pano do acerto - variacao de texto`() {
        val observacoes = listOf(
            "Troca de pano realizada durante acerto",
            "Troca durante acerto - Pano: P16",
            "Pano trocado via acerto",
            "Acerto - troca realizada"
        )

        observacoes.forEach { obs ->
            val contemAcerto = obs.contains("acerto", ignoreCase = true)
            val contemContexto = obs.contains("durante", ignoreCase = true) ||
                                  obs.contains("via acerto", ignoreCase = true) ||
                                  obs.contains("realizada", ignoreCase = true)

            assertTrue("Falhou para: $obs", contemAcerto && contemContexto)
        }
    }

    @Test
    fun `filtro NAO identifica reforma manual`() {
        val observacoes = listOf(
            "Troca de pano via reforma",
            "Reforma completa da mesa",
            "Manutenção preventiva"
        )

        observacoes.forEach { obs ->
            val contemAcerto = obs.contains("acerto", ignoreCase = true)
            val contemContexto = obs.contains("durante", ignoreCase = true) ||
                                  obs.contains("via acerto", ignoreCase = true) ||
                                  obs.contains("realizada", ignoreCase = true)

            assertFalse("Falhou para: $obs", contemAcerto && contemContexto)
        }
    }
}
```

### Gate 4: Validação FASE 1

**Executar:**

```bash
# Build
./gradlew assembleDebug --build-cache --parallel

# Testes unitários
./gradlew testDebugUnitTest

# Instalar no device
./gradlew installDebug
```

**Validação manual:**
1. Fazer um **Acerto** com troca de pano
2. Abrir tela **Reforma de Mesas**
3. Verificar que o card aparece
4. Verificar logs `DEBUG_CARDS` mostram "Reformas do ACERTO encontradas: > 0"

**Critério de sucesso FASE 1:**
- ✅ Build sem erros
- ✅ Testes unitários passam (3/3)
- ✅ Cards do Acerto aparecem na tela
- ✅ Logs confirmam filtro funcionando

---

## 🏗️ FASE 2: SOLUÇÃO DEFINITIVA COM DADOS ESTRUTURADOS (60 minutos)

**⚠️ SÓ EXECUTE FASE 2 APÓS FASE 1 ESTAR VALIDADA E FUNCIONANDO**

### Gate 1: Plano de Ação

```text
PLANO DE AÇÃO - FASE 2

Objetivo: Usar HistoricoManutencaoMesa de forma estruturada para identificar trocas do Acerto

Módulos afetados:
- ui (RegistrarTrocaPanoUseCase, MesasReformadasViewModel)
- Potencialmente: data (se precisar ajustar queries)

Impacto no multi-tenancy: NÃO (respeita rotasPermitidas)

Riscos: MÉDIO (altera fluxo de escrita e leitura, mas sem migration)

Passos propostos:
1. Investigar entidade HistoricoManutencaoMesa (10 min)
2. Ajustar RegistrarTrocaPanoUseCase para inserir em HistoricoManutencaoMesa quando origem=ACERTO (20 min)
3. Ajustar MesasReformadasViewModel para buscar e montar cards de ambas as fontes (20 min)
4. Validação completa (10 min)

Critério de sucesso:
- Acerto insere em HistoricoManutencaoMesa com tipoManutencao=TROCA_PANO
- Cards montados a partir de dados estruturados, não texto
- Nova Reforma continua funcionando
- Logs DEBUG_CARDS mostram fontes separadas
```

### Gate 2: Investigação e Escopo

#### Passo 1: Investigar HistoricoManutencaoMesa

**Execute no código:**

```bash
# Localizar entidade HistoricoManutencaoMesa
find . -name "HistoricoManutencaoMesa.kt" -type f

# Analisar campos da entidade
rg "data class HistoricoManutencaoMesa" --type kt -A 20
```

**Confirme se a entidade tem:**
- ✅ Campo `tipoManutencao` (enum? String?)
- ✅ Campo `responsavel` ou `origem` ou similar
- ✅ Campo `mesaId`, `numeroMesa`, `dataManutencao`

**Se NÃO tiver campos estruturados suficientes:**
- PARE aqui
- Informe ao desenvolvedor humano
- Não invente campos novos sem approval

#### Escopo Definido

```text
ESCOPO DEFINIDO - FASE 2

Arquivos a modificar:
- ui/src/main/java/.../mesas/usecases/RegistrarTrocaPanoUseCase.kt
  Motivo: Inserir em HistoricoManutencaoMesa quando origem=ACERTO

- ui/src/main/java/.../mesas/MesasReformadasViewModel.kt
  Motivo: Buscar HistoricoManutencaoMesa e gerar cards estruturados

- (Se necessário) data/src/main/java/.../dao/HistoricoManutencaoMesaDao.kt
  Motivo: Adicionar query se não existir

Arquivos a NÃO tocar:
- SettlementViewModel.kt (continua chamando use case da mesma forma)
- Entidades de data (sem migration)
- Módulos sync, migrations

Validação necessária:
- Build completo
- Testes unitários
- Validação manual de ambos os fluxos (Nova Reforma + Acerto)
```

### Gate 3: Implementação

#### Passo 2: Ajustar RegistrarTrocaPanoUseCase

**Localizar o método `invoke()`** no use case (linhas 30-134 segundo relatório).

**Adicionar lógica para quando origem=ACERTO:**

```kotlin
@HiltViewModel
class RegistrarTrocaPanoUseCase @Inject constructor(
    private val appRepository: AppRepository
) {
    suspend operator fun invoke(params: TrocaPanoParams) {
        Log.d("DEBUG_CARDS", "════════════════════════════════════════")
        Log.d("DEBUG_CARDS", "🔵 USE CASE INICIADO - RegistrarTrocaPanoUseCase")
        Log.d("DEBUG_CARDS", "   Mesa: ${params.numeroMesa} (ID: ${params.mesaId})")
        Log.d("DEBUG_CARDS", "   Origem: ${params.origem}")

        try {
            val mesa = appRepository.obterMesaPorId(params.mesaId)
            if (mesa == null) {
                Log.e("DEBUG_CARDS", "❌ ERRO: Mesa ${params.mesaId} não encontrada")
                throw IllegalArgumentException("Mesa ${params.mesaId} não encontrada")
            }

            when (params.origem) {
                OrigemTrocaPano.NOVA_REFORMA -> {
                    // ✅ Fluxo atual: insere em MesaReformada
                    Log.d("DEBUG_CARDS", "📋 NOVA_REFORMA: Inserindo em MesaReformada")

                    val mesaReformada = MesaReformada(
                        mesaId = params.mesaId,
                        numeroMesa = params.numeroMesa,
                        tipoMesa = mesa.tipoMesa,
                        tamanhoMesa = mesa.tamanho ?: TamanhoMesa.GRANDE,
                        pintura = false,
                        tabela = false,
                        panos = true,
                        numeroPanos = params.panoNovoId?.toString() ?: "",
                        outros = false,
                        observacoes = params.observacao ?: "Troca de pano via reforma",
                        fotoReforma = null,
                        dataReforma = params.dataManutencao
                    )

                    val idReforma = appRepository.inserirMesaReformada(mesaReformada)
                    Log.d("DEBUG_CARDS", "✅ MesaReformada inserida com ID: $idReforma")
                }

                OrigemTrocaPano.ACERTO -> {
                    // 🆕 NOVO FLUXO: insere em HistoricoManutencaoMesa
                    Log.d("DEBUG_CARDS", "📋 ACERTO: Inserindo em HistoricoManutencaoMesa")

                    val historico = HistoricoManutencaoMesa(
                        mesaId = params.mesaId,
                        numeroMesa = params.numeroMesa,
                        tipoManutencao = TipoManutencao.TROCA_PANO, // ✅ ESTRUTURADO
                        descricao = params.descricao,
                        dataManutencao = params.dataManutencao,
                        responsavel = "Acerto", // ✅ ESTRUTURADO - identifica origem
                        observacoes = params.observacao
                    )

                    val idHistorico = appRepository.inserirHistoricoManutencaoMesaSync(historico)
                    Log.d("DEBUG_CARDS", "✅ HistoricoManutencaoMesa inserido com ID: $idHistorico")
                    Log.d("DEBUG_CARDS", "   - tipoManutencao: ${TipoManutencao.TROCA_PANO}")
                    Log.d("DEBUG_CARDS", "   - responsavel: Acerto")
                }
            }

            // Atualizar pano atual da mesa (comum para ambos os fluxos)
            if (params.panoNovoId != null) {
                Log.d("DEBUG_CARDS", "🔄 Atualizando mesa com novo pano...")
                val mesaAtualizada = mesa.copy(
                    panoAtualId = params.panoNovoId,
                    dataUltimaTrocaPano = params.dataManutencao
                )
                appRepository.atualizarMesa(mesaAtualizada)
                Log.d("DEBUG_CARDS", "✅ Mesa atualizada com novo pano")
            }

            Log.d("DEBUG_CARDS", "🎉 USE CASE CONCLUÍDO COM SUCESSO")
            Log.d("DEBUG_CARDS", "════════════════════════════════════════")

        } catch (e: Exception) {
            Log.e("DEBUG_CARDS", "❌❌❌ ERRO NO USE CASE ❌❌❌")
            Log.e("DEBUG_CARDS", "Mesa: ${params.numeroMesa}")
            Log.e("DEBUG_CARDS", "Origem: ${params.origem}")
            Log.e("DEBUG_CARDS", "Exception: ${e.javaClass.simpleName}")
            Log.e("DEBUG_CARDS", "Message: ${e.message}")
            Log.e("DEBUG_CARDS", "════════════════════════════════════════", e)
            throw e
        }
    }
}
```

#### Passo 3: Ajustar MesasReformadasViewModel

**Localizar o método `carregarMesasReformadas()`** (linhas 75-83 segundo relatório).

**Substituir por:**

```kotlin
private fun carregarMesasReformadas() {
    viewModelScope.launch {
        try {
            Log.d("DEBUG_CARDS", "")
            Log.d("DEBUG_CARDS", "┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓")
            Log.d("DEBUG_CARDS", "┃  CARREGANDO CARDS - Reforma de Mesas  ┃")
            Log.d("DEBUG_CARDS", "┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛")

            // Buscar ambas as fontes
            val mesasReformadas = appRepository.obterTodasMesasReformadas()
            val historicos = appRepository.obterTodosHistoricosManutencaoMesa()
            val mesas = appRepository.obterTodasMesas()

            Log.d("DEBUG_CARDS", "📊 Dados recebidos:")
            Log.d("DEBUG_CARDS", "   - Total MesasReformadas: ${mesasReformadas.size}")
            Log.d("DEBUG_CARDS", "   - Total HistoricoManutencaoMesa: ${historicos.size}")
            Log.d("DEBUG_CARDS", "   - Total Mesas: ${mesas.size}")

            // 1. Filtrar reformas manuais (Nova Reforma)
            val reformasManuais = mesasReformadas.filter { reforma ->
                // Reformas que não são do Acerto (compatibilidade com dados antigos)
                reforma.observacoes?.let { obs ->
                    val contemAcerto = obs.contains("acerto", ignoreCase = true)
                    !contemAcerto // Inverte: pega as que NÃO são do acerto
                } ?: true // Se não tem observação, considera manual
            }

            Log.d("DEBUG_CARDS", "🔍 Reformas MANUAIS (Nova Reforma): ${reformasManuais.size}")

            // 2. Filtrar históricos do ACERTO (novo fluxo estruturado)
            val historicosAcerto = historicos.filter { historico ->
                historico.tipoManutencao == TipoManutencao.TROCA_PANO &&
                historico.responsavel?.equals("Acerto", ignoreCase = true) == true
            }

            Log.d("DEBUG_CARDS", "🔍 Históricos do ACERTO (estruturado): ${historicosAcerto.size}")
            historicosAcerto.forEach {
                Log.d("DEBUG_CARDS", "   - Mesa ${it.numeroMesa}: ${it.descricao}")
            }

            // 3. Fallback: reformas antigas do Acerto (compatibilidade)
            val reformasAcertoLegacy = mesasReformadas.filter { reforma ->
                reforma.observacoes?.let { obs ->
                    val contemAcerto = obs.contains("acerto", ignoreCase = true)
                    val contemContexto = obs.contains("durante", ignoreCase = true) ||
                                          obs.contains("via acerto", ignoreCase = true) ||
                                          obs.contains("realizada", ignoreCase = true)
                    contemAcerto && contemContexto
                } == true
            }

            Log.d("DEBUG_CARDS", "🔍 Reformas do ACERTO (legacy/texto): ${reformasAcertoLegacy.size}")

            // 4. Montar cards
            val cards = mutableListOf<ReformaCard>()

            // Cards de reformas manuais
            reformasManuais.forEach { reforma ->
                val mesa = mesas.find { it.id == reforma.mesaId }
                cards.add(
                    ReformaCard(
                        id = reforma.id,
                        mesaId = reforma.mesaId,
                        numeroMesa = reforma.numeroMesa,
                        descricao = "Reforma manual - Panos: ${reforma.numeroPanos}",
                        data = reforma.dataReforma,
                        origem = "NOVA_REFORMA",
                        observacoes = reforma.observacoes
                    )
                )
            }

            // Cards de históricos do Acerto (estruturado)
            historicosAcerto.forEach { historico ->
                val mesa = mesas.find { it.id == historico.mesaId }
                cards.add(
                    ReformaCard(
                        id = historico.id,
                        mesaId = historico.mesaId,
                        numeroMesa = historico.numeroMesa,
                        descricao = historico.descricao ?: "Troca de pano via Acerto",
                        data = historico.dataManutencao,
                        origem = "ACERTO",
                        observacoes = historico.observacoes
                    )
                )
            }

            // Cards de reformas do Acerto legacy (fallback)
            reformasAcertoLegacy.forEach { reforma ->
                val mesa = mesas.find { it.id == reforma.mesaId }
                cards.add(
                    ReformaCard(
                        id = reforma.id,
                        mesaId = reforma.mesaId,
                        numeroMesa = reforma.numeroMesa,
                        descricao = "Troca via Acerto (legacy) - Panos: ${reforma.numeroPanos}",
                        data = reforma.dataReforma,
                        origem = "ACERTO_LEGACY",
                        observacoes = reforma.observacoes
                    )
                )
            }

            // Ordenar por data (mais recente primeiro)
            cards.sortByDescending { it.data }

            Log.d("DEBUG_CARDS", "")
            Log.d("DEBUG_CARDS", "📊 Resumo final:")
            Log.d("DEBUG_CARDS", "   - Cards de Nova Reforma: ${reformasManuais.size}")
            Log.d("DEBUG_CARDS", "   - Cards de Acerto (estruturado): ${historicosAcerto.size}")
            Log.d("DEBUG_CARDS", "   - Cards de Acerto (legacy): ${reformasAcertoLegacy.size}")
            Log.d("DEBUG_CARDS", "   - Total de cards gerados: ${cards.size}")
            Log.d("DEBUG_CARDS", "┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛")

            // Emitir para UI
            _cards.value = cards

        } catch (e: Exception) {
            Log.e("DEBUG_CARDS", "❌ Erro ao carregar cards", e)
            _cards.value = emptyList()
        }
    }
}

// Data class para o card (se não existir)
data class ReformaCard(
    val id: Long,
    val mesaId: Long,
    val numeroMesa: Int,
    val descricao: String,
    val data: Long,
    val origem: String, // "NOVA_REFORMA", "ACERTO", "ACERTO_LEGACY"
    val observacoes: String?
)
```

### Gate 4: Validação FASE 2

**Executar:**

```bash
# Build completo
./gradlew clean assembleDebug --build-cache --parallel

# Testes
./gradlew testDebugUnitTest

# Instalar
./gradlew installDebug
```

**Validação manual completa:**

1. **Teste 1: Nova Reforma**
   - Fazer Nova Reforma com troca de pano
   - Abrir Reforma de Mesas
   - ✅ Card aparece com origem "NOVA_REFORMA"
   - ✅ Logs mostram inserção em `MesaReformada`

2. **Teste 2: Acerto (novo)**
   - Fazer Acerto com troca de pano
   - Abrir Reforma de Mesas
   - ✅ Card aparece com origem "ACERTO"
   - ✅ Logs mostram inserção em `HistoricoManutencaoMesa`
   - ✅ Logs mostram `tipoManutencao=TROCA_PANO` e `responsavel=Acerto`

3. **Teste 3: Dados antigos (legacy)**
   - Se houver reformas antigas do Acerto no banco
   - ✅ Cards aparecem com origem "ACERTO_LEGACY"

**Critério de sucesso FASE 2:**
- ✅ Build sem erros
- ✅ Ambos os fluxos funcionam (Nova Reforma + Acerto)
- ✅ Cards são gerados de fontes estruturadas (`tipoManutencao`, `responsavel`)
- ✅ Fallback legacy continua funcionando para dados antigos
- ✅ Logs DEBUG_CARDS mostram origem de cada card claramente
- ✅ Nenhuma regressão em multi-tenancy ou sync

---

## 📊 RELATÓRIO FINAL (você deve gerar)

Após concluir ambas as fases, gere um relatório markdown com:

```markdown
# RELATÓRIO: Solução definitiva - Cards de troca de pano via ACERTO

## Status: ✅ CONCLUÍDO / ⚠️ PARCIAL / ❌ BLOQUEADO

## FASE 1: Hotfix Resiliente
- Status: [CONCLUÍDO/FALHOU]
- Arquivos modificados: [lista]
- Testes: [X/Y passaram]
- Validação manual: [OK/FALHOU]
- Observações: [texto]

## FASE 2: Solução Estruturada
- Status: [CONCLUÍDO/FALHOU]
- Arquivos modificados: [lista]
- Mudanças na arquitetura: [descrição]
- Testes: [X/Y passaram]
- Validação manual: [OK/FALHOU]
- Observações: [texto]

## Resultados
- Cards de Nova Reforma: [funcionando SIM/NÃO]
- Cards de Acerto (estruturado): [funcionando SIM/NÃO]
- Cards de Acerto (legacy): [funcionando SIM/NÃO]
- Total de cards gerados em teste: [número]

## Logs de validação
[Cole aqui os logs DEBUG_CARDS mais importantes]

## Próximos passos (se houver)
[Lista de itens pendentes ou melhorias futuras]
```

---

## ⚠️ TROUBLESHOOTING

### Se houver erro de compilação:
1. Verificar se `TipoManutencao.TROCA_PANO` existe
2. Verificar se `HistoricoManutencaoMesa` tem campo `responsavel`
3. Se não existirem, PARE e informe ao desenvolvedor humano

### Se build passar mas app crashar:
1. Capture logs com: `adb logcat DEBUG_CARDS:D AndroidRuntime:E *:S`
2. Identifique stack trace
3. Se for `JobCancellationException`, verifique escopo de coroutine
4. PARE após 3 tentativas (Gate 4)

### Se cards não aparecerem:
1. Verifique logs `DEBUG_CARDS` para confirmar:
   - Use case foi chamado
   - Inserção foi bem-sucedida
   - Query retornou dados
   - Filtros estão corretos
2. Execute query manual no banco via ADB
3. PARE e peça ajuda após 3 tentativas

---

## 🎯 CHECKLIST FINAL

Antes de considerar concluído:

**FASE 1:**
- [ ] Filtro resiliente implementado
- [ ] Testes unitários criados e passando
- [ ] Build sem erros
- [ ] Cards do Acerto aparecem na UI
- [ ] Logs DEBUG_CARDS confirmam funcionamento

**FASE 2:**
- [ ] RegistrarTrocaPanoUseCase ajustado (when/ACERTO)
- [ ] MesasReformadasViewModel busca HistoricoManutencaoMesa
- [ ] Cards montados com dados estruturados
- [ ] Fallback legacy funciona
- [ ] Nova Reforma continua funcionando (sem regressão)
- [ ] Logs DEBUG_CARDS mostram ambas as fontes
- [ ] Build e testes passando
- [ ] Validação manual completa

**QUALIDADE:**
- [ ] Código segue padrões do projeto (MVVM, Hilt)
- [ ] Multi-tenancy por rota respeitado
- [ ] Logs informativos adicionados
- [ ] Sem TODOs/FIXMEs novos sem justificativa

**ENTREGA:**
- [ ] Relatório final gerado
- [ ] Código commitado (se aplicável)
- [ ] Documentação atualizada (se aplicável)

---

**Boa sorte! Siga os Gates e não invente soluções. Se bloquear, PARE e peça ajuda humana.**
