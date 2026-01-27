# 🎯 DIAGNÓSTICO CIRÚRGICO - DESCOBRIR ONDE panoNovoId DEVE SER PREENCHIDO

Você é uma IA sênior Android/Kotlin. Siga AI_GUIDE.md (Gates 1-4) e PROJECT.md rigorosamente.

## CONTEXTO CRÍTICO
**Log prova:**
- `[SETTLEMENT] Mesa 1 atualizada com pano 2 com sucesso` ← Pano EXISTE
- `DEBUG_FIX: Mesas com panoNovoId: 0` ← Campo VAZIO no DTO

**Causa raiz confirmada:** O campo `panoNovoId` foi adicionado ao data class `MesaAcerto`, mas NÃO está sendo preenchido quando o objeto é construído/mapeado.

## GATE 1 - PLANO DE DIAGNÓSTICO
**Objetivo:** Adicionar logs precisos para identificar ONDE o `panoNovoId` deveria ser copiado mas não está.
**Módulos:** ui (SettlementViewModel e/ou SettlementFragment)
**Impacto multi-tenancy:** NÃO
**Risco:** ZERO (apenas logs temporários)
**Passos:**
1. Localizar TODOS os pontos onde `MesaAcerto` é construída
2. Adicionar log ANTES e DEPOIS da construção mostrando se `panoNovoId` existe na fonte
3. Validar com captura de log

## GATE 2 - LOCALIZAR PONTOS DE CONSTRUÇÃO

Execute estes comandos para descobrir onde `MesaAcerto` é criada:

```cmd
rg "MesaAcerto\(" --type kt -A 5 -B 5
rg "data class MesaAcerto" --type kt -A 10
rg "trocarPanoNaMesa|atualizarMesaPano" --type kt -A 10 -B 5
rg "panoTrocado\s*=" --type kt -A 5 -B 5
```

**Confirme os paths reais** e liste todos os arquivos que constroem ou modificam `MesaAcerto`.

## GATE 3 - IMPLEMENTAR LOGS DE RASTREAMENTO

### 3.1 No ponto ONDE A MESA É ATUALIZADA COM O PANO

Localize a linha que loga:
```
[SETTLEMENT] Mesa 1 atualizada com pano 2 com sucesso
```

**IMEDIATAMENTE APÓS** esse log, adicione:

```kotlin
// ════════════════════════════════════════════════════════════════
// LOG DIAGNÓSTICO: Rastrear panoId da mesa ANTES de virar DTO
// ════════════════════════════════════════════════════════════════
Log.w("DEBUG_POPUP", "╔═══════════════════════════════════════════════════╗")
Log.w("DEBUG_POPUP", "║  RASTREAMENTO PANO - APÓS ATUALIZAR MESA          ║")
Log.w("DEBUG_POPUP", "╚═══════════════════════════════════════════════════╝")
Log.w("DEBUG_POPUP", "🔍 Mesa ID: ${mesa.id}")
Log.w("DEBUG_POPUP", "🔍 Mesa Número: ${mesa.numero}")
Log.w("DEBUG_POPUP", "🔍 mesa.panoAtualId: ${mesa.panoAtualId}")
Log.w("DEBUG_POPUP", "🔍 panoId recém atribuído: ${panoId}") // Se existir variável local
Log.w("DEBUG_POPUP", "🔍 Tipo do objeto mesa: ${mesa.javaClass.simpleName}")
```

### 3.2 No ponto ONDE MesaAcerto É CONSTRUÍDA (para o DTO)

Localize TODAS as ocorrências de `MesaAcerto(` no código.

**ANTES de cada construção**, adicione:

```kotlin
// ════════════════════════════════════════════════════════════════
// LOG DIAGNÓSTICO: Rastrear dados ANTES de construir MesaAcerto DTO
// ════════════════════════════════════════════════════════════════
Log.w("DEBUG_POPUP", "╔═══════════════════════════════════════════════════╗")
Log.w("DEBUG_POPUP", "║  CONSTRUINDO MesaAcerto DTO                       ║")
Log.w("DEBUG_POPUP", "╚═══════════════════════════════════════════════════╝")
Log.w("DEBUG_POPUP", "📦 Fonte dos dados: ${mesaOrigem.javaClass.simpleName}") // mesaOrigem = variável de onde vem os dados
Log.w("DEBUG_POPUP", "📦 mesaOrigem.id: ${mesaOrigem.id}")
Log.w("DEBUG_POPUP", "📦 mesaOrigem.numero: ${mesaOrigem.numero}")
Log.w("DEBUG_POPUP", "📦 mesaOrigem tem panoAtualId? ${mesaOrigem.panoAtualId}")
Log.w("DEBUG_POPUP", "📦 mesaOrigem tem panoNovoId? ${mesaOrigem.panoNovoId}") // Se existir
```

**DEPOIS de cada construção**, adicione:

```kotlin
// ════════════════════════════════════════════════════════════════
// LOG DIAGNÓSTICO: Validar MesaAcerto APÓS construção
// ════════════════════════════════════════════════════════════════
Log.w("DEBUG_POPUP", "╔═══════════════════════════════════════════════════╗")
Log.w("DEBUG_POPUP", "║  MesaAcerto CONSTRUÍDA                            ║")
Log.w("DEBUG_POPUP", "╚═══════════════════════════════════════════════════╝")
Log.w("DEBUG_POPUP", "✅ mesaAcerto.id: ${mesaAcerto.id}")
Log.w("DEBUG_POPUP", "✅ mesaAcerto.numero: ${mesaAcerto.numero}")
Log.w("DEBUG_POPUP", "✅ mesaAcerto.panoNovoId: ${mesaAcerto.panoNovoId}")
Log.w("DEBUG_POPUP", "⚠️  SE NULL: Campo existe mas não foi preenchido na construção!")
Log.w("DEBUG_POPUP", "╚═══════════════════════════════════════════════════╝")
```

### 3.3 No ponto ONDE dadosAcerto É MONTADO (antes de salvar)

Localize onde o objeto `dadosAcerto` (ou similar) é criado com a lista de mesas.

**IMEDIATAMENTE APÓS** a construção, adicione:

```kotlin
// ════════════════════════════════════════════════════════════════
// LOG DIAGNÓSTICO: Validar dadosAcerto completo antes de salvar
// ════════════════════════════════════════════════════════════════
Log.w("DEBUG_POPUP", "╔═══════════════════════════════════════════════════╗")
Log.w("DEBUG_POPUP", "║  dadosAcerto FINAL (antes de salvar)              ║")
Log.w("DEBUG_POPUP", "╚═══════════════════════════════════════════════════╝")
Log.w("DEBUG_POPUP", "🎯 dadosAcerto.panoTrocado: ${dadosAcerto.panoTrocado}")
Log.w("DEBUG_POPUP", "🎯 dadosAcerto.numeroPano: '${dadosAcerto.numeroPano}'")
Log.w("DEBUG_POPUP", "🎯 dadosAcerto.mesas.size: ${dadosAcerto.mesas.size}")
dadosAcerto.mesas.forEachIndexed { idx, mesa ->
    Log.w("DEBUG_POPUP", "🎯   Mesa [$idx]: id=${mesa.id}, numero=${mesa.numero}, panoNovoId=${mesa.panoNovoId}")
}
Log.w("DEBUG_POPUP", "╚═══════════════════════════════════════════════════╝")
```

## GATE 4 - VALIDAÇÃO

### Build
```cmd
.\gradlew.bat :app:assembleDebug --build-cache
```
Esperado: SUCCESS

### Instalação
```cmd
.\gradlew.bat installDebug
```

### Captura de logs (use script atualizado)
```cmd
.\scripts\capturar-logs-diagnostico-popup.ps1
```

## RELATÓRIO ESPERADO

Após executar o fluxo de teste, os logs devem revelar:

**Cenário esperado:**
```
DEBUG_POPUP: RASTREAMENTO PANO - APÓS ATUALIZAR MESA
DEBUG_POPUP: 🔍 mesa.panoAtualId: 2          ← EXISTE aqui
DEBUG_POPUP: ─────────────────────────────────
DEBUG_POPUP: CONSTRUINDO MesaAcerto DTO
DEBUG_POPUP: 📦 mesaOrigem.panoAtualId: 2    ← Ainda existe na fonte
DEBUG_POPUP: ─────────────────────────────────
DEBUG_POPUP: MesaAcerto CONSTRUÍDA
DEBUG_POPUP: ✅ mesaAcerto.panoNovoId: null  ← PERDEU AQUI! (Motivo: não foi copiado)
```

**Com base nesse log, você saberá EXATAMENTE:**
- Qual variável tem o `panoId` correto
- Onde ele se perde (na construção do DTO)
- Qual linha precisa ser modificada para copiar o campo

## PRÓXIMO PASSO

Após capturar o log `DEBUG_POPUP`, gere um relatório com:

### 📋 Relatório - Rastreamento panoNovoId
**Arquivo rastreado:** [path completo]
**Ponto onde pano existe:** [linha do código]
**Ponto onde se perde:** [linha do código]
**Campo fonte:** [nome da variável]
**Campo destino:** [nome da variável]
**Correção necessária:** [adicionar `panoNovoId = mesa.panoAtualId` na linha X]

---

**EXECUTE AGORA. Logs vão revelar exatamente onde copiar o campo.**
