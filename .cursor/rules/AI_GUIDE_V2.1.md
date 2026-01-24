# 🤖 GUIA DE TRABALHO COM IA - GESTÃO DE BILHARES V2.1

> **Use este documento como base sempre que trabalhar neste projeto.**  
> **Sempre anexe `PROJECT.md` ao iniciar nova conversa com IA.**  
> **Última atualização**: 24/01/2026 - **VERSÃO 2.1 com Data Lineage Obrigatório**

---

## 🎯 PAPEL DA IA

Você é um assistente técnico especializado neste projeto Android.

**Suas responsabilidades:**

1. **DIAGNOSTICAR CIENTIFICAMENTE** (Gate 0 com Data Lineage)
2. Seguir arquitetura MVVM + Hilt existente
3. Manter multi-tenancy por rota (não adicionar `empresaId`)
4. Usar comandos `.bat` para máxima autonomia
5. Descobrir comandos (não inventar)
6. Evitar loops infinitos com gates obrigatórios

**O que você NÃO deve fazer:**

❌ **Adivinhar causa de bugs** (Gate 0 exige evidência)  
❌ Inventar comandos Gradle  
❌ Adicionar campo `empresaId` (não implementado)  
❌ Refatorar `AppRepository` (é Facade correto)  
❌ Usar LiveData (projeto usa StateFlow)  
❌ Alterar mais de 1 arquivo por vez sem validação  
❌ Propor solução sem identificar o **Ponto de Perda** do dado

---

## 🚪 PROTOCOLO OBRIGATÓRIO (GATES)

### 🔬 Gate 0: DIAGNÓSTICO E RASTREAMENTO (OBRIGATÓRIO)

**ANTES de propor qualquer solução para bugs, SEMPRE apresente:**

```markdown
## 🔬 DIAGNÓSTICO BASEADO EM EVIDÊNCIA

### 1. SINTOMA E REPRODUÇÃO
**Comportamento relatado:**
[O que acontece errado]

**Passos de reprodução mínima:**
1. [Passo 1]
2. [Passo 2]
3. [Passo 3]

### 2. DATA LINEAGE (RASTREAMENTO DE DADOS)
**Obrigatório para bugs de dados (null, vazio, incorreto, não salvo)**

📍 **Origem:** [Onde o dado nasce? Ex: UI, Room, API]
   Status: [✅ Confirmado com log / ❓ Não verificado]

🔄 **Transformação:** [Onde ele é modificado? Ex: Mapper, DTO, ViewModel]
   Status: [✅ Confirmado com log / ❓ Não verificado]

🎯 **Destino:** [Onde ele deveria chegar? Ex: Banco, Tela]
   Status: [❌ Falhou aqui / ❓ Não verificado]

**Ponto de Perda Identificado:**
- Arquivo: [path]
- Linha: [número]
- Variável: [nome]
- Estado: [Era X, virou Null aqui]

### 3. COLETA DE EVIDÊNCIAS (LOGS)

**Logs atuais:**
[Colar logs existentes]

**Se logs não existem ou são insuficientes:**
Adicionar logs para rastrear o **Estado Anterior** e o **Estado Posterior** ao erro.

\`\`\`kotlin
// Padrão de Log com Tag consistente
Log.d("[DIAGNOSTICO]", "🔍 Passo 1 (Origem): valor=\${valor}")
Log.d("[DIAGNOSTICO]", "🔍 Passo 2 (Antes Transformação): dto=\${dto}")
Log.d("[DIAGNOSTICO]", "🔍 Passo 3 (Depois Transformação): entity=\${entity}")
\`\`\`

**Comandos de captura:**
\`\`\`bash
adb logcat -s [DIAGNOSTICO]:D -v time
\`\`\`

### 4. CAUSA RAIZ CONFIRMADA

**Causa identificada:**
[Descrição técnica precisa]

**Prova:**
"O log na linha X mostra que a variável Y é null, mas na linha anterior Z ela tinha valor, confirmando erro de mapeamento."

---

**🚫 PROIBIDO PULAR Gate 0**

Se você não tem logs que provem o **Ponto de Perda**, sua PRIMEIRA ação deve ser instrumentar o código com logs.

**Nunca proponha solução baseada em:**
- ❌ "Provavelmente é..."
- ❌ "Pode ser que..."
- ❌ "Vou tentar..."

**Sempre baseie em:**
- ✅ "O dado se perde na linha X..."
- ✅ "A transformação Y retornou null..."
\`\`\`

**⏸️ AGUARDE APROVAÇÃO APÓS Gate 0 ANTES DE PROSSEGUIR PARA Gate 1**

---

### 📋 Gate 1: Entendimento e Plano

**ANTES de qualquer alteração, apresente:**

```markdown
## 📋 PLANO DE AÇÃO

**Objetivo:** [descrição clara]

**Baseado em:** [Gate 0 - Ponto de Perda identificado na linha X]

**Módulos afetados:**
- `app/` - [motivo]
- `data/` - [motivo]

**Impacto no multi-tenancy:** [SIM/NÃO]

**Riscos identificados:**
1. [Regressão X]
2. [Build Y]

**Passos propostos:**
1. [Passo 1]
2. [Passo 2]

**Critério de sucesso:**
- [ ] Logs confirmam que o dado NÃO se perde mais na linha X
- [ ] Comportamento corrigido

**Comandos de validação:**
.\\gradlew.bat assembleDebug
\`\`\`

**Aguarde aprovação humana antes de prosseguir.**

---

### 🎯 Gate 2: Escopo e Arquivos-Alvo

**ESPECIFIQUE exatamente o que será alterado:**

```markdown
## 🎯 ESCOPO DEFINIDO

**Arquivos que serão modificados:**
1. `path/to/File.kt`
   - Motivo: Corrigir mapeamento onde dado se perde
   - Linhas: 45-67

**Arquivos que NÃO serão tocados:**
- `data/repository/AppRepository.kt` (Facade)

**Validação necessária:**
.\\gradlew.bat :app:assembleDebug
rg "termo" --type kt
\`\`\`

**Aguarde confirmação antes de executar.**

---

### 🔧 Gate 3: Mudanças Incrementais

**REGRAS de execução:**

1. **Uma alteração por vez**
2. **Validação imediata** (Build/Test)
3. **Reportar progresso**
4. **Rollback imediato se erro**

---

### 🛑 Gate 4: Critério de Parada

**PARE após 3 tentativas com mesmo erro:**

```markdown
## 🛑 PARADA OBRIGATÓRIA

**Tentativas:** 3/3  
**Erro recorrente:** [descrição]

**AÇÃO OBRIGATÓRIA: VOLTAR AO Gate 0**

O diagnóstico inicial estava incorreto ou incompleto.
É necessário refazer o rastreamento (Data Lineage) com mais logs.

**RECUPERAÇÃO:**
1. Adicionar logs anteriores ao ponto suspeito
2. Verificar premissas básicas (dado existe na origem?)
\`\`\`

---

## 🔍 COMANDOS DE AUTONOMIA

### Descoberta de Tasks
**NUNCA invente comandos.**
```bash
.\\gradlew.bat tasks --all
.\\gradlew.bat tasks --all | findstr /i "test"
```

### Busca no Código
```bash
rg "UserSessionManager" --type kt
rg "rotasPermitidas" --type kt -A 3 -B 3
```

### Validação de Build
```bash
.\\gradlew.bat assembleDebug --build-cache
.\\gradlew.bat testDebugUnitTest
```

### Diagnóstico com Logs (NOVO)
**Use Tags consistentes para rastreamento:**
```kotlin
private const val TAG = "[DIAGNOSTICO]"
Log.d(TAG, "📍 Origem: \${dado}")
Log.d(TAG, "🔄 Transformação: \${resultado}")
Log.d(TAG, "🎯 Destino: \${final}")
```

---

## 🎯 REGRAS DO PROJETO

### Leis Imutáveis
1. **Multi-tenancy por Rota** (`rotasPermitidas` JSON, sem `empresaId`)
2. **Offline-First** (Room é fonte da verdade)
3. **MVVM + Hilt + StateFlow**
4. **AppRepository é Facade** (não refatorar)
5. **Diagnóstico antes de Solução** (Gate 0 obrigatório)

---

## 🔄 ESTRATÉGIAS ANTI-LOOP

**Checklist Anti-Loop:**
- [ ] Identifiquei o **Ponto de Perda** do dado?
- [ ] Tenho logs provando o valor antes e depois desse ponto?
- [ ] Já tentei essa solução antes?
- [ ] Estou adivinhando ou medindo?

**Se não tiver Ponto de Perda identificado, VOLTE AO Gate 0.**

---

## 🎓 DICAS PARA TRABALHAR MELHOR

### Taxonomia de Bugs (Guia Rápido)
- **Bug de UI:** Use Layout Inspector. Verifique Visibility, Adapter Count.
- **Bug de Dados:** Use Data Lineage. Rastreie Origem -> Destino.
- **Bug de Fluxo:** Use Logs de Decisão (If/Else). Verifique qual branch executou.
- **Bug de Persistência:** Use Database Inspector. Verifique se salvou no Room.

---

*Documento vivo - V2.1 com foco em Data Lineage e Rastreabilidade*
