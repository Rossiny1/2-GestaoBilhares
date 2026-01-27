# 🎯 PROMPT: Corrigir cards de Acerto não aparecem em Reforma de Mesas

## 📋 Contexto do problema

**Situação atual:**
- Tela **Nova Reforma**: troca de pano → card aparece na "Reforma de Mesas" ✅
- Tela **Acerto**: troca de pano → card NÃO aparece na "Reforma de Mesas" ❌

**Root cause identificado:**
O fluxo de Acerto grava apenas `HistoricoManutencaoMesa`, mas não cria `MesaReformada`. A tela "Reforma de Mesas" precisa de **ambos** os registros para exibir o card corretamente.

**Documentos anexados:**
- `RELATORIO_UNIFICACAO_PANO_EXCHANGE_V12_FINAL.md`: tentativa de unificação via use case
- `ANALISE_ROOT_CAUSE_CARDS_ACERTO.md`: análise técnica do problema

---

## 🎯 Solução a ser implementada

**Centralizar a criação de `MesaReformada` dentro do `RegistrarTrocaPanoUseCase`**, para que qualquer origem (Nova Reforma ou Acerto) automaticamente garanta os dados necessários para os cards.

---

## 🔧 Implementação (passo a passo)

### 1️⃣ Modificar o `RegistrarTrocaPanoUseCase`

**Arquivo:** `ui/src/main/java/com/example/gestaobilhares/ui/mesas/usecases/RegistrarTrocaPanoUseCase.kt`

**Mudanças necessárias:**

```kotlin
class RegistrarTrocaPanoUseCase @Inject constructor(
    private val appRepository: AppRepository
) {
    suspend operator fun invoke(params: TrocaPanoParams) {
        try {
            // 1. Buscar dados da mesa para criar MesaReformada
            val mesa = appRepository.buscarMesaPorId(params.mesaId)
                ?: throw IllegalArgumentException("Mesa ${params.mesaId} não encontrada")

            // 2. Criar/atualizar MesaReformada para garantir que o card apareça
            // (independente da origem: NOVA_REFORMA ou ACERTO)
            val mesaReformada = MesaReformada(
                mesaId = params.mesaId,
                numeroMesa = params.numeroMesa,
                tipoMesa = mesa.tipoMesa,
                tamanhoMesa = mesa.tamanho ?: TamanhoMesa.GRANDE,
                pintura = false,
                tabela = false,
                panos = true,  // Marca que houve troca de pano
                numeroPanos = extrairNumeroPano(params.descricao) ?: params.panoNovoId?.toString() ?: "",
                outros = false,
                observacoes = when (params.origem) {
                    is OrigemTrocaPano.NOVA_REFORMA -> params.observacao ?: "Troca de pano via reforma"
                    is OrigemTrocaPano.ACERTO -> "Troca realizada durante acerto"
                },
                fotoReforma = null,
                dataReforma = params.dataManutencao
            )

            // 3. Inserir/atualizar MesaReformada
            appRepository.inserirMesaReformada(mesaReformada)

            // 4. Registrar no histórico de manutenção (já existente)
            val historico = HistoricoManutencaoMesa(
                mesaId = params.mesaId,
                numeroMesa = params.numeroMesa,
                tipoManutencao = TipoManutencao.TROCA_PANO,
                descricao = params.descricao,
                dataManutencao = params.dataManutencao,
                responsavel = when (params.origem) {
                    is OrigemTrocaPano.NOVA_REFORMA -> "Reforma de mesa"
                    is OrigemTrocaPano.ACERTO -> "Sistema de Acerto"
                },
                observacoes = params.observacao
            )

            appRepository.inserirHistoricoManutencao(historico)

            // 5. Atualizar pano atual da mesa (se fornecido)
            if (params.panoNovoId != null) {
                appRepository.atualizarPanoAtualMesa(params.mesaId, params.panoNovoId)
            }

            Log.d("RegistrarTrocaPanoUseCase", 
                "Troca de pano registrada com sucesso - Mesa: ${params.numeroMesa}, Origem: ${params.origem}")

        } catch (e: Exception) {
            Log.e("RegistrarTrocaPanoUseCase", 
                "Erro ao registrar troca de pano - Mesa: ${params.numeroMesa}", e)
            throw e
        }
    }

    // Helper para extrair número do pano da descrição (ex: "Troca de pano - Pano: P123" -> "P123")
    private fun extrairNumeroPano(descricao: String): String? {
        return Regex("""Pano:\\s*(\\w+)""").find(descricao)?.groupValues?.get(1)
    }
}
```

**Imports necessários:**

```kotlin
import com.example.gestaobilhares.data.entities.MesaReformada
import com.example.gestaobilhares.data.entities.HistoricoManutencaoMesa
import com.example.gestaobilhares.data.entities.TipoManutencao
import com.example.gestaobilhares.data.entities.TamanhoMesa
import android.util.Log
```

---

### 2️⃣ Simplificar o `SettlementViewModel`

**Arquivo:** `ui/src/main/java/com/example/gestaobilhares/ui/settlement/SettlementViewModel.kt`

**Remover qualquer lógica de criar `MesaReformada` diretamente** (se foi adicionada), deixando apenas a chamada do use case:

```kotlin
private suspend fun registrarTrocaPanoNoHistorico(
    mesas: List<MesaDTO>,
    numeroPano: String
) {
    try {
        val panoId = appRepository.buscarPorNumero(numeroPano)?.id
        val dataAtual = DateUtils.obterDataAtual().time

        mesas.forEach { mesa ->
            val descricaoPano = "Troca de pano realizada durante acerto - Pano: $numeroPano"

            // ✅ Use case agora cuida de TUDO (MesaReformada + Histórico)
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

            logOperation("SETTLEMENT", "Troca de pano registrada para mesa ${mesa.numero}")
        }
    } catch (e: Exception) {
        Timber.e("Erro ao registrar troca de pano: ${e.message}", e)
    }
}
```

---

### 3️⃣ Garantir que Nova Reforma não duplique

**Arquivo:** `ui/src/main/java/com/example/gestaobilhares/ui/mesas/NovaReformaFragment.kt`

**Verificar se a Nova Reforma não está criando `MesaReformada` EM DOIS LUGARES** (no próprio fluxo + no use case).

**Cenário ideal:**
- Se a Nova Reforma JÁ cria `MesaReformada` completa no método `salvarReforma()`, então o use case pode apenas atualizar campos relacionados ao pano, OU
- Se o use case sempre cria/atualiza, a Nova Reforma pode remover a criação local e delegar 100% para o use case.

**Escolha a abordagem que fizer mais sentido no seu código atual.**

---

## ✅ Critérios de aceite

Após implementar:

1. **Build e testes passam:**

```bash
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug --build-cache --parallel
.\gradlew.bat installDebug
```

2. **Cenário Nova Reforma (sanity check):**
   - Criar reforma com troca de pano
   - Card deve aparecer normalmente na "Reforma de Mesas" ✅

3. **Cenário Acerto (fix do problema):**
   - Realizar acerto com troca de pano
   - Card deve aparecer na "Reforma de Mesas" ✅
   - Verificar que dados estão corretos (mesa, pano, data, origem)

4. **Validação no banco (opcional):**

```sql
-- Verificar MesaReformada criada pelo use case
SELECT * FROM mesas_reformadas 
WHERE observacoes LIKE '%acerto%' 
ORDER BY data_reforma DESC;

-- Verificar HistoricoManutencaoMesa
SELECT * FROM historico_manutencao_mesa 
WHERE responsavel = 'Sistema de Acerto' 
ORDER BY data_manutencao DESC;
```

---

## 🚫 O que NÃO fazer

- ❌ Não criar `MesaReformada` diretamente no `SettlementViewModel` (isso duplica lógica)
- ❌ Não mexer na lógica de exibição dos cards (UI está correta, o problema é nos dados)
- ❌ Não usar comandos Unix-like (`./gradlew`); sempre usar `.\gradlew.bat` no Windows

---

## 📦 Entrega esperada

- `RegistrarTrocaPanoUseCase.kt` com lógica completa de criar `MesaReformada` + `HistoricoManutencaoMesa`
- `SettlementViewModel.kt` simplificado (só chama use case)
- `NovaReformaFragment.kt` verificado para não duplicar criação de `MesaReformada`
- Build, testes e instalação bem-sucedidos
- Cards de Acerto aparecendo corretamente na tela "Reforma de Mesas"

---

## 🔄 Comandos de validação (Windows-safe)

Após implementar, executar em sequência:

```bash
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug --build-cache --parallel
.\gradlew.bat installDebug
```

Se algum comando falhar, NÃO inventar novos comandos. Aplicar Gate 4 do AI_USAGE.md: parar, analisar erro, listar alternativas e pedir confirmação humana.

---

**Versão:** V13 - Use Case Completo  
**Status:** Pronto para implementação  
**Risco:** Baixo (não mexe em UI, só adiciona persistência)
