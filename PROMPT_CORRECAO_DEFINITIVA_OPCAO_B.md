# 🎯 CORREÇÃO DEFINITIVA - BUG CARDS ACERTO (OPÇÃO B)

Você é uma IA sênior Android/Kotlin. Siga AI_GUIDE.md (Gates 1-4) e PROJECT.md rigorosamente.

## CONTEXTO - CAUSA RAIZ CONFIRMADA
**Log prova que:**
- Mesa atualizada no Room: `mesa.panoAtualId: 1` ✅
- MesaDTO usado na construção: `MesaDTO não tem este campo` ❌
- MesaAcerto criado: `panoNovoId: null` ❌

**Problema:** MesaAcerto é construído a partir de MesaDTO, mas MesaDTO não contém o `panoAtualId`.

**Solução escolhida:** Buscar o `panoAtualId` diretamente do Room no momento da construção do MesaAcerto.

---

## GATE 1 - PLANO
**Objetivo:** Preencher `panoNovoId` no MesaAcerto buscando dados do Room
**Módulos:** ui (SettlementFragment ou SettlementViewModel)
**Impacto multi-tenancy:** NÃO
**Risco:** BAIXO (mudança cirúrgica e isolada)
**Passos:**
1. Localizar onde MesaAcerto é construído (linha com log DEBUG_POPUP "CONSTRUINDO MesaAcerto DTO")
2. Buscar a Mesa do Room usando o ID
3. Atribuir mesa.panoAtualId ao campo panoNovoId do MesaAcerto
4. Build + validação

**Critérios de sucesso:**
- Build OK sem warnings novos
- Log DEBUG_POPUP mostra `mesaAcerto.panoNovoId: 1` (não null)
- Log DEBUG_FIX mostra `Mesas com panoNovoId: 1`
- Tela "Reforma de Mesas" exibe cards

---

## GATE 2 - ESCOPO

### Localizar arquivo e função
O log mostra que a construção acontece no ViewModel. Execute:

```cmd
rg "CONSTRUINDO MesaAcerto DTO" --type kt -A 10 -B 10
```

Path esperado: `ui/src/main/java/com/example/gestaobilhares/ui/settlement/SettlementFragment.kt` 
OU `ui/src/main/java/com/example/gestaobilhares/ui/settlement/SettlementViewModel.kt`

### Ponto exato de modificação
Localize o trecho que tem os logs:
```kotlin
Log.w("DEBUG_POPUP", "╔═══════════════════════════════════════════════════╗")
Log.w("DEBUG_POPUP", "║  CONSTRUINDO MesaAcerto DTO                       ║")
Log.w("DEBUG_POPUP", "╚═══════════════════════════════════════════════════╝")
// ... logs de diagnóstico ...

// AQUI: Construção do MesaAcerto (código existente)
val mesaAcerto = MesaAcerto(
    id = mesaOriginal.id,
    numero = mesaOriginal.numero,
    // ... outros campos ...
    panoNovoId = null  // ← ESTE É O PROBLEMA
)
```

---

## GATE 3 - IMPLEMENTAÇÃO

### 3.1 Localizar o AppRepository
Primeiro, confirme que o repositório está disponível no escopo. Procure por:
- `appRepository` (injetado via construtor/DI)
- `repository` 
- Ou qualquer variável que dê acesso ao Room

### 3.2 Modificar a construção do MesaAcerto

**Substitua** a construção existente por:

```kotlin
// ════════════════════════════════════════════════════════════════
// CORREÇÃO: Buscar panoAtualId do Room para preencher panoNovoId
// ════════════════════════════════════════════════════════════════

// 1. Buscar a Mesa atualizada do Room (fonte da verdade)
val mesaRoom = appRepository.getMesaById(mesaOriginal.id)

// 2. Extrair panoAtualId se existir
val panoIdParaRegistro = mesaRoom?.panoAtualId

Log.w("DEBUG_POPUP", "╔═══════════════════════════════════════════════════╗")
Log.w("DEBUG_POPUP", "║  BUSCANDO PANO DO ROOM                            ║")
Log.w("DEBUG_POPUP", "╚═══════════════════════════════════════════════════╝")
Log.w("DEBUG_POPUP", "🔍 Mesa ID buscada no Room: ${mesaOriginal.id}")
Log.w("DEBUG_POPUP", "🔍 mesaRoom encontrada? ${mesaRoom != null}")
Log.w("DEBUG_POPUP", "🔍 mesaRoom.panoAtualId: ${mesaRoom?.panoAtualId}")
Log.w("DEBUG_POPUP", "✅ panoIdParaRegistro que será usado: $panoIdParaRegistro")

// 3. Construir MesaAcerto COM o panoNovoId preenchido
val mesaAcerto = MesaAcerto(
    id = mesaOriginal.id,
    numero = mesaOriginal.numero,
    relogioInicial = mesaState.relogioInicial,
    relogioFinal = mesaState.relogioFinal,
    valorFixo = mesaState.valorFixo,
    subtotal = mesaState.subtotal,
    comDefeito = mesaState.comDefeito,
    relogioReiniciou = mesaState.relogioReiniciou,
    observacao = mesaState.observacao,
    fotoUri = mesaState.fotoUri,
    // ✅ CORREÇÃO: Preencher com o panoAtualId do Room
    panoNovoId = panoIdParaRegistro  // null se não houver troca, ID se houver
)

Log.w("DEBUG_POPUP", "╔═══════════════════════════════════════════════════╗")
Log.w("DEBUG_POPUP", "║  MesaAcerto CONSTRUÍDA (CORRIGIDA)                ║")
Log.w("DEBUG_POPUP", "╚═══════════════════════════════════════════════════╝")
Log.w("DEBUG_POPUP", "✅ mesaAcerto.id: ${mesaAcerto.id}")
Log.w("DEBUG_POPUP", "✅ mesaAcerto.numero: ${mesaAcerto.numero}")
Log.w("DEBUG_POPUP", "✅ mesaAcerto.panoNovoId: ${mesaAcerto.panoNovoId}")
if (mesaAcerto.panoNovoId != null) {
    Log.w("DEBUG_POPUP", "🎉 SUCESSO: panoNovoId PREENCHIDO!")
} else {
    Log.w("DEBUG_POPUP", "ℹ️  NULL: Mesa não teve troca de pano")
}
Log.w("DEBUG_POPUP", "╚═══════════════════════════════════════════════════╝")
```

### 3.3 Notas importantes
- **NÃO remova** os logs DEBUG_POPUP existentes, apenas adicione os novos
- Se `appRepository` não estiver disponível no escopo, injete via construtor/DI
- O método `getMesaById(id: Long)` deve retornar `Mesa?` (nullable)

---

## GATE 4 - VALIDAÇÃO

### Build
```cmd
.\gradlew.bat :app:assembleDebug --build-cache
```
Esperado: SUCCESS sem warnings novos

### Testes
```cmd
.\gradlew.bat testDebugUnitTest
```
Esperado: Passar ou mesmo número de falhas anteriores

### Instalação e teste manual
```cmd
.\gradlew.bat installDebug
adb logcat -s DEBUG_POPUP:W DEBUG_FIX:D DEBUG_CARDS:D BaseViewModel:D -v time
```

**Fluxo de teste:**
1. Abrir app → Acerto
2. Selecionar cliente → Adicionar mesa
3. MARCAR "Trocar Pano" → Selecionar pano na lista
4. Salvar acerto
5. Abrir "Reforma de Mesas"

### Logs que DEVEM aparecer
```
DEBUG_POPUP: BUSCANDO PANO DO ROOM
DEBUG_POPUP: 🔍 mesaRoom.panoAtualId: 1          ← Encontrou!
DEBUG_POPUP: ✅ panoIdParaRegistro que será usado: 1
DEBUG_POPUP: ─────────────────────────────────────────
DEBUG_POPUP: MesaAcerto CONSTRUÍDA (CORRIGIDA)
DEBUG_POPUP: ✅ mesaAcerto.panoNovoId: 1         ← PREENCHIDO!
DEBUG_POPUP: 🎉 SUCESSO: panoNovoId PREENCHIDO!
DEBUG_POPUP: ─────────────────────────────────────────
DEBUG_POPUP: dadosAcerto FINAL
DEBUG_POPUP: 🎯 Mesa [0]: panoNovoId=1           ← Propagado!
DEBUG_FIX: Mesas com panoNovoId: 1               ← Filtro encontra!
DEBUG_FIX: ✅ Detectada troca de pano
DEBUG_CARDS: Total HistoricoManutencaoMesa: 1    ← Registrado!
```

### Resultado na tela
- ✅ Cards de troca de pano do ACERTO aparecem
- ✅ Total HistoricoManutencaoMesa > 0

---

## RELATÓRIO FINAL

Após implementar e validar, gere relatório markdown com:

### ✅ Relatório - Correção Definitiva (Opção B)
**Data:** [data]
**Causa raiz:** MesaDTO não contém panoAtualId, então MesaAcerto é construído sem panoNovoId
**Solução aplicada:** Buscar mesa do Room no momento da construção e usar panoAtualId
**Arquivo alterado:** [path completo]
**Linhas modificadas:** [aproximadamente X linhas]
**Build:** [SUCCESS ou FAILED]
**Testes:** [resultado]
**Logs de prova:** [colar DEBUG_POPUP mostrando panoNovoId preenchido]
**Validação manual:** [cards aparecem? sim/não]
**Status:** [RESOLVIDO ou precisa ajustes]

---

**EXECUTE AGORA. Esta é a correção cirúrgica baseada em evidência concreta do log.**
