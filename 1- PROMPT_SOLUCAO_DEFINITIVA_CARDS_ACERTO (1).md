# 🎯 SOLUÇÃO DEFINITIVA - BUG CARDS ACERTO

Você é uma IA sênior Android/Kotlin. Siga AI_GUIDE.md (Gates 1-4) e PROJECT.md rigorosamente.

## CONTEXTO
Bug: Cards de troca de pano do ACERTO não aparecem em "Reforma de Mesas".
Causa confirmada: `dadosAcerto.numeroPano` é null, então o if nunca executa.
Evidência no log: "Pano trocado: true" + "Número do pano: 'null'" + "Mesa 2 atualizada com pano 1".

## GATE 1 - PLANO
**Objetivo:** Mudar lógica de validação para usar FONTE DA VERDADE (mesa.panoNovoId) em vez de string UI (numeroPano).
**Módulos:** ui (SettlementViewModel)
**Impacto multi-tenancy:** NÃO
**Risco:** BAIXO (não mexe em UI/Binding)
**Passos:**
1. Localizar função que salva acerto no SettlementViewModel
2. Substituir condição `if (panoTrocado && isNaoVazia(numeroPano))` por filtro em `mesas.panoNovoId`
3. Adicionar logs de diagnóstico
4. Build + validação

**Critérios de sucesso:**
- Build OK sem warnings novos
- Log mostra "Mesas com pano novo: 1"
- Log mostra "✅ Registro concluído"
- Tela "Reforma de Mesas" exibe Total HistoricoManutencaoMesa > 0

## GATE 2 - ESCOPO
**Arquivo a modificar (confirme path real com rg):**
```cmd
rg "Salvando acerto|registrarTrocaPanoNoHistorico" --type kt -A 5 -B 5
```

Path esperado: `ui/src/main/java/com/example/gestaobilhares/ui/settlement/SettlementViewModel.kt`

**Linhas aproximadas:** Dentro da função que salva acerto, após inserir AcertoMesa, ANTES de emitir `_resultadoSalvamento`.

## GATE 3 - IMPLEMENTAÇÃO

Localize no `SettlementViewModel.kt` o trecho após salvar mesas do acerto, ANTES de `_resultadoSalvamento.value = ...`.

**Substitua** qualquer código que verifica `panoTrocado` pelo código abaixo:

```kotlin
// ════════════════════════════════════════════════════════════════
// SOLUÇÃO DEFINITIVA: Usar dados estruturados (panoNovoId), não string UI
// ════════════════════════════════════════════════════════════════

// 1. FONTE DA VERDADE: Se mesa tem panoNovoId, houve troca
val mesasComPanoNovo = dadosAcerto.mesas.filter { 
    it.panoNovoId != null && it.panoNovoId > 0 
}

Log.d("DEBUG_FIX", "═══════════════════════════════════════")
Log.d("DEBUG_FIX", "🔍 DIAGNÓSTICO TROCA DE PANO:")
Log.d("DEBUG_FIX", "   Flag panoTrocado (UI): ${dadosAcerto.panoTrocado}")
Log.d("DEBUG_FIX", "   String numeroPano: '${dadosAcerto.numeroPano}'")
Log.d("DEBUG_FIX", "   Mesas com panoNovoId: ${mesasComPanoNovo.size}")
mesasComPanoNovo.forEachIndexed { idx, mesa ->
    Log.d("DEBUG_FIX", "   [$idx] Mesa ${mesa.numero} → panoNovoId=${mesa.panoNovoId}")
}
Log.d("DEBUG_FIX", "═══════════════════════════════════════")

// 2. DECISÃO: Se houver mesas com pano novo, registrar no histórico
if (mesasComPanoNovo.isNotEmpty()) {
    Log.d("DEBUG_FIX", "✅ Detectada troca de pano baseada em dados estruturados")

    try {
        // 3. EXECUÇÃO SEQUENCIAL (impede cancelamento por lifecycle)
        // Chamada DIRETA, SEM viewModelScope.launch
        registrarTrocaPanoNoHistorico(
            mesas = mesasComPanoNovo.map { mesa ->
                MesaDTO(
                    id = mesa.id,
                    numero = mesa.numero,
                    panoNovoId = mesa.panoNovoId!!,
                    // Fallback multinível: campo da mesa > campo global > ID
                    descricao = mesa.numeroPano?.takeIf { it.isNotBlank() }
                        ?: dadosAcerto.numeroPano?.takeIf { it.isNotBlank() }
                        ?: "Pano ID ${mesa.panoNovoId}"
                )
            },
            observacao = dadosAcerto.numeroPano ?: ""
        )

        Log.d("DEBUG_FIX", "✅ registrarTrocaPanoNoHistorico CONCLUÍDO")

    } catch (e: Exception) {
        Log.e("DEBUG_FIX", "❌ ERRO ao registrar troca de pano: ${e.message}", e)
        // NÃO relançar exceção (não bloquear salvamento financeiro)
    }
} else {
    Log.d("DEBUG_FIX", "ℹ️ Nenhuma mesa com panoNovoId detectada (sem troca)")
}

// 4. Emitir resultado APÓS garantir persistência (ou após try/catch se houver erro)
```

**IMPORTANTE:**
- Coloque esse código EXATAMENTE antes de `_resultadoSalvamento.value = ResultadoSalvamento.Sucesso(acertoId)`
- NÃO use `viewModelScope.launch { }` em volta
- Mantenha os logs DEBUG_FIX (são críticos para prova)

## GATE 4 - VALIDAÇÃO

**Build:**
```cmd
.\gradlew.bat :app:assembleDebug --build-cache
```
Esperado: SUCCESS sem warnings novos

**Testes:**
```cmd
.\gradlew.bat testDebugUnitTest
```
Esperado: Passar ou se falhar 1 teste, explicar o motivo

**Instalação e teste manual:**
```cmd
.\gradlew.bat installDebug
adb logcat -s DEBUG_FIX:D DEBUG_CARDS:D BaseViewModel:D -v time
```

**Fluxo de teste:**
1. Abrir app → Acerto
2. Selecionar cliente → Adicionar mesa
3. MARCAR "Trocar Pano" → Selecionar pano na lista
4. Salvar acerto
5. Abrir "Reforma de Mesas"

**Logs que DEVEM aparecer:**
```
DEBUG_FIX: 🔍 DIAGNÓSTICO TROCA DE PANO:
DEBUG_FIX:    Mesas com panoNovoId: 1
DEBUG_FIX:    [0] Mesa 333 → panoNovoId=1
DEBUG_FIX: ✅ Detectada troca de pano baseada em dados estruturados
DEBUG_FIX: ✅ registrarTrocaPanoNoHistorico CONCLUÍDO
DEBUG_CARDS: 📋 ACERTO: Inserindo em HistoricoManutencaoMesa
DEBUG_CARDS: ✅ HistoricoManutencaoMesa inserido com ID: 1
DEBUG_CARDS: Total HistoricoManutencaoMesa: 1
DEBUG_CARDS: 🔍 Históricos do ACERTO (estruturado): 1
```

**Resultado na tela:**
- Cards de troca de pano do ACERTO devem aparecer
- Total > 0

## RELATÓRIO FINAL

Após implementar e validar, gere relatório markdown com:

### ✅ Relatório - Solução Definitiva Cards Acerto
**Data:** [data]
**Causa raiz:** Validação baseada em `numeroPano` (string UI null) em vez de `panoNovoId` (dado estruturado)
**Solução aplicada:** Mudança de fonte da verdade - filtro em `mesas.panoNovoId`
**Arquivo alterado:** [path completo]
**Linhas modificadas:** [aproximadamente X linhas]
**Build:** SUCCESS em Xm Ys
**Testes:** [resultado]
**Logs de prova:** [colar trecho DEBUG_FIX + DEBUG_CARDS]
**Validação manual:** [resultado - cards aparecem? sim/não]
**Próximos passos:** [se ainda falhar, qual evidência adicional coletar]

---

**EXECUTE AGORA. ZERO suposições. 100% baseado em evidência do log.**
