# 🤖 GUIA DE TRABALHO COM IA - GESTÃO DE BILHARES

> **Use este documento como base sempre que trabalhar neste projeto.**  
> **Sempre anexe `PROJECT.md` ao iniciar nova conversa com IA.**  
> **Última atualização**: 24/01/2026 - **VERSÃO FINAL (Static + Dynamic Analysis)**

---

## ⚡ INÍCIO RÁPIDO - LEIA ISTO PRIMEIRO

### Seu objetivo: Resolver o problema com o MÍNIMO de builds necessário

```
┌─────────────────────────────────────────────────────────┐
│  DECISÃO: Qual método de diagnóstico usar?              │
└─────────────────────────────────────────────────────────┘
         │
         ├─ Usuário disse "REGRESSÃO" ou "funcionava antes"?
         │  └─→ 🅰️ STATIC ANALYSIS (leia código) → Máx 1 build
         │
         ├─ Erro visível lendo o código?
         │  (campo não preenchido, if faltando, mapper errado)
         │  └─→ 🅰️ STATIC ANALYSIS → Máx 1 build
         │
         ├─ Código PARECE correto mas comporta errado?
         │  └─→ 🅱️ DYNAMIC ANALYSIS (logs) → Máx 2 builds
         │
         └─ Não tenho certeza?
            └─→ Comece com 🅰️ STATIC (mais rápido)
```

---

## 📋 MATRIZ DE CLASSIFICAÇÃO

| Tipo de Bug | Sintoma | Método | Builds |
|-------------|---------|--------|--------|
| **Regressão** | "Funcionava antes" | Static | 1 |
| **Campo Null** | NPE, dado sumiu | Static | 1 |
| **Lógica Errada** | If invertido | Static | 1 |
| **Query SQL** | Dados incorretos | Static + DB Inspector | 1-2 |
| **Mistério** | Código OK mas falha | Dynamic (Logs) | 2 |
| **Timing** | Falha intermitente | Dynamic (Logs) | 2 |
| **Lifecycle** | Coroutine cancelada | Dynamic (Logs) | 2 |

---

## 🚪 PROTOCOLO OBRIGATÓRIO (GATES)

### 🔬 Gate 0: DIAGNÓSTICO (ESCOLHA SUA TRILHA)

#### 🅰️ TRILHA ESTÁTICA (PREFERIDA) 🚀

**Quando usar:**
- Regressão (funcionava antes)
- Campo faltando/null
- Lógica visível errada
- Mapper incorreto

**Passos:**
1. **Buscar no código**
   ```bash
   # Encontrar arquivo
   rg "SettlementViewModel" --type kt -l

   # Ver contexto completo
   rg "insertHistorico" --type kt -C 10

   # Arqueologia (se regressão)
   rg "usuarioId" --type kt -C 5
   rg "groupBy" --type kt -C 5
   ```

2. **Identificar erro visualmente**
   ```markdown
   ## 🔬 DIAGNÓSTICO (Static Analysis)

   **Tipo:** Regressão - Campo não preenchido

   **Evidência (Código):**
   - Arquivo: `SettlementViewModel.kt:455`
   - Código atual:
     \`\`\`kotlin
     HistoricoManutencao(
         mesaId = mesa.id,
         panoId = pano.id
         // ❌ FALTA: usuarioId = userSession.currentUser.id
     )
     \`\`\`

   **Causa raiz:** Campo removido acidentalmente na última correção.

   **Solução:** Adicionar campo faltante.
   ```

3. **IR PARA GATE 1** (sem logs, sem build de diagnóstico)

**⚠️ Limite: Máximo 1 build (apenas para validar correção)**

---

#### 🅱️ TRILHA DINÂMICA (LOGS) 🐢

**Quando usar:**
- Comportamento misterioso
- Código parece correto mas falha
- Bugs intermitentes
- Concorrência/Timing

**Passos:**
1. **Adicionar logs estratégicos**
   ```kotlin
   Log.d("[DIAGNOSTICO]", "═══ INICIANDO salvarAcerto ═══")
   Log.d("[DIAGNOSTICO]", "🔍 ANTES: usuarioId=\${usuarioId}")
   val historico = salvarHistorico(dados)
   Log.d("[DIAGNOSTICO]", "🔍 DEPOIS: historicoId=\${historico.id}")
   ```

2. **Compilar UMA VEZ**
   ```bash
   .\gradlew.bat :app:assembleDebug
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

3. **Capturar e analisar**
   ```bash
   adb logcat -s [DIAGNOSTICO]:D -v time > logs.txt
   ```

4. **Apresentar diagnóstico**
   ```markdown
   ## 🔬 DIAGNÓSTICO (Dynamic Analysis)

   **Logs capturados:**
   \`\`\`
   12:34:56 D/[DIAGNOSTICO]: 🔍 ANTES: usuarioId=null
   \`\`\`

   **Interpretação:** UserSessionManager não está injetado.

   **Causa raiz:** Falta @Inject no construtor.
   ```

**⚠️ Limite: Máximo 2 builds (1 diagnóstico + 1 validação)**

---

### 🛑 REGRA ANTI-LOOP (CRÍTICA)

**Se você rodou `gradlew` mais de 2 VEZES:**
1. **PARE IMEDIATAMENTE**
2. Você está em loop (tentativa-erro ou burocracia)
3. **AÇÃO OBRIGATÓRIA:**
   - Volte para Static Analysis
   - Releia o código do zero
   - Procure onde o campo/lógica existia antes
   - Peça ajuda humana com diagnóstico até aqui

---

### 📋 Gate 1: Plano de Correção

```markdown
## 📋 PLANO DE CORREÇÃO

**Baseado em:** [Gate 0 - Static/Dynamic]

**Objetivo:** [descrição cirúrgica]

**Alteração:**
- Arquivo: [path completo]
- Linha: [número]
- Mudança: [código exato]

**Impacto:** [módulos afetados]

**Validação mínima:**
.\gradlew.bat :app:assembleDebug  # Só compilar

**Critério de sucesso:**
- [ ] Compila sem erro
- [ ] [Comportamento específico corrigido]
```

**⏸️ AGUARDE APROVAÇÃO antes de prosseguir**

---

### 🎯 Gate 2: Escopo e Arquivos

```markdown
## 🎯 ESCOPO DEFINIDO

**Arquivos modificados:**
1. [path] - [motivo] - linhas [X-Y]

**Arquivos NÃO tocados:**
- `AppRepository.kt` (Facade)

**Migration?** [SIM/NÃO]

**Validação:**
.\gradlew.bat :app:assembleDebug
rg "[termo]" --type kt
```

---

### 🔧 Gate 3: Execução Incremental

1. **Uma alteração por vez**
2. **Build imediato** (`:app:assembleDebug`)
3. **Reportar progresso**
4. **Rollback se erro** → Voltar ao Gate 0

---

### 🛑 Gate 4: Parada Obrigatória

**PARE após 3 tentativas OU 2 builds:**

```markdown
## 🛑 PARADA OBRIGATÓRIA

**Situação:** 3 tentativas OU 2+ builds sem sucesso

**AÇÃO:**
1. **NÃO adicionar mais logs**
2. **NÃO rodar mais builds**
3. Voltar ao Gate 0 - Static Analysis
4. Buscar código antigo (git blame, histórico)
5. **Se ainda falhar:** Pedir ajuda humana
```

---

## 🔍 COMANDOS ESSENCIAIS

### Busca no Código (RÁPIDO)
```bash
# Encontrar onde variável é usada
rg "usuarioId" --type kt -C 3

# Ver classe completa
rg "data class HistoricoManutencao" --type kt -A 20

# Arqueologia (quem preenchia antes)
rg "groupBy.*mesaId" --type kt
```

### Build Mínimo (RÁPIDO)
```bash
# Compilar apenas módulo alterado
.\gradlew.bat :app:assembleDebug

# Instalar (só se necessário)
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Database Inspector
```
View > Tool Windows > App Inspection > Database Inspector
```

---

## 🎯 REGRAS DO PROJETO

### Leis Imutáveis
1. **Multi-tenancy por Rota** (`rotasPermitidas`)
2. **Offline-First** (Room = fonte verdade)
3. **MVVM + Hilt + StateFlow**
4. **AppRepository é Facade** (não refatorar)

### Proibido
❌ Pular Gate 0  
❌ Adicionar `empresaId`  
❌ Usar LiveData  
❌ Inventar comandos Gradle  
❌ Mais de 2 builds sem solução  

---

## 🎓 EXEMPLOS

### ✅ CORRETO (Static - Regressão)

```markdown
## Tarefa: Usuário não sendo salvo

### Gate 0 (Static)
**Evidência (Código):**
Linha 455 não passa `usuarioId` no construtor.

### Gate 1
**Correção:** Adicionar `usuarioId = userSession.currentUser.id`

**Builds:** 1 (validação)
**Tempo:** 5 minutos
```

### ❌ INCORRETO (Loop)

```markdown
## Tarefa: Usuário não sendo salvo

### Gate 0 (Dynamic)
Adicionando logs... [Build 1]
Capturando... confirma null
Tentando injetar... [Build 2]
Ainda null... [Build 3]
Mudando abordagem... [Build 4]

**❌ PROBLEMA:** 4 builds, 20 minutos, sem diagnóstico real
```

---

## 📞 QUANDO PEDIR AJUDA

- Após Gate 0, múltiplas hipóteses
- Após 2 builds sem solução
- Após 3 tentativas
- Decisão arquitetural
- Múltiplos testes falhando

---

*Documento vivo - Versão Final: Static First + Dynamic quando necessário*
