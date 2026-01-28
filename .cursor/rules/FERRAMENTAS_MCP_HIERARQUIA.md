# 🛠️ FERRAMENTAS MCP E HIERARQUIA DE USO

> **Documento complementar ao AI_GUIDE_FINAL.md**  
> **Integração de Model Context Protocol no fluxo de trabalho**  
> **Criado em:** 27/01/2026  
> **Baseado em:** Melhores práticas de documentação de software e MCP

---

## 📋 PRIORIDADE ESTABELECIDA (2026)

> **Regra fundamental:** Documentação primeiro, comandos como exceção

### 🥇 Prioridade 1: Documentação .cursor/rules (SEMPRE)

**Quando usar:** Para TODAS as atividades de desenvolvimento

**Arquivos principais:**
- **`AI_GUIDE_FINAL.md`** - Protocolo completo de trabalho (Gates, Static/Dynamic Analysis)
- **`PROJECT_CONTEXT_FULL.md`** - Contexto completo do projeto (500+ linhas, 34 ViewModels, 27 DAOs)
- **`GUIA_DIAGNOSTICO_SENIOR_FINAL.md`** - Metodologia científica de debugging

**Benefícios:**
- Conhecimento específico do projeto
- Protocolos validados em casos reais
- Diagnóstico 10x mais rápido

---

### 🥈 Prioridade 2: Comandos (APENAS QUANDO NECESSÁRIO)

**Quando usar:** Apenas quando documentação não tiver informação específica

**Comandos permitidos:**
- `rg` para busca no código (após consultar documentação)
- `gradlew` para builds (máximo 2 por problema, ver Gate 4)
- `adb` para logs (apenas Dynamic Analysis)

**Regras:**
- Sempre consultar documentação primeiro
- Máximo 2 builds por problema
- Usar apenas para validação final

---

### 🥉 Prioridade 3: Ferramentas MCP (COMPLEMENTAR)

**Quando usar:** Para operações específicas após diagnóstico

**Ferramentas disponíveis:**

#### 🔄 Filesystem MCP
- **Uso:** Operações de arquivo (leitura, edição, busca)
- **NÃO usar para:** Diagnóstico de problemas
- **Exemplo:** `mcp1_read_file()`, `mcp1_edit_file()`, `mcp1_search_files()`

#### 🔍 Perplexity MCP
- **Uso:** Pesquisa externa e melhores práticas
- **NÃO usar para:** Contexto do projeto (usar PROJECT_CONTEXT_FULL.md)
- **Exemplo:** Pesquisar "Firebase Firestore Kotlin coroutines best practices 2026"

#### 📁 GitKraken MCP
- **Uso:** Controle de versão e operações Git
- **Exemplo:** `mcp0_git_status()`, `mcp0_git_log_or_diff()`

---

## 🎯 FLUXO DE TRABALHO OTIMIZADO

```
1. PROBLEMA IDENTIFICADO
   ↓
2. CONSULTAR DOCUMENTAÇÃO (.cursor/rules)
   ↓
3. APLICAR PROTOCOLO (Static/Dynamic Analysis)
   ↓
4. USAR COMANDOS (apenas se necessário)
   ↓
5. FERRAMENTAS MCP (para operações específicas)
```

### Exemplo prático:

**Problema:** Campo não sendo salvo

1. **Documentação:** `AI_GUIDE_FINAL.md` → Static Analysis
2. **Contexto:** `PROJECT_CONTEXT_FULL.md` → DAOs mapeados
3. **Diagnóstico:** `GUIA_DIAGNOSTICO_SENIOR.md` → Receita pronta
4. **Comando:** `rg "campoEspecifico"` (apenas se necessário)
5. **MCP:** `mcp1_edit_file()` (para aplicar correção)

---

## ⚡ EFICIÊNCIA COMPROVADA

| Operação | ❌ Sem Documentação | ✅ Com Documentação |
|----------|-------------------|-------------------|
| Encontrar ViewModel | `find . -name "*ViewModel.kt"` (2 min) | Lista completa (34 itens) (10 seg) |
| Buscar DAO específico | `rg "Dao"` (1 min) | Mapeado por funcionalidade (5 seg) |
| Entender arquitetura | Análise manual (15 min) | Stack documentada (30 seg) |
| Diagnosticar bug | Tentativa-erro (2 horas) | Protocolo científico (15 min) |

---

## 🛑 REGRAS OBRIGATÓRIAS

### ✅ SEMPRE FAÇA:
- Começar com documentação .cursor/rules
- Consultar AI_GUIDE_FINAL.md para protocolo
- Usar PROJECT_CONTEXT_FULL.md para contexto
- Aplicar GUIA_DIAGNOSTICO_SENIOR.md para debugging

### ⚠️ USE COMANDOS APENAS QUANDO:
- Documentação não tiver informação específica
- Precisar de busca textual exata
- Necessitar de build/logs para validação

### 🚫 NUNCA FAÇA:
- Pular documentação e ir direto para comandos
- Usar MCP filesystem para diagnóstico (é operacional)
- Usar MCP perplexity para contexto do projeto
- Fazer mais de 2 builds por problema

---

## 🔧 INTEGRAÇÃO COM PROTOCOLO EXISTENTE

### Gate 0 - Diagnóstico:
- **Static Analysis:** Usar documentação .cursor/rules
- **Dynamic Analysis:** Documentação + logs (se necessário)

### Gate 1 - Plano de Correção:
- Baseado em diagnóstico da documentação
- Usar MCP apenas para edição de arquivos

### Gate 2 - Escopo:
- Consultar PROJECT_CONTEXT_FULL.md para impacto
- Usar comandos apenas para validação

### Gate 3 - Execução:
- MCP filesystem para edições precisas
- Build mínimo para validação

### Gate 4 - Parada:
- Se 3 tentativas ou 2 builds sem sucesso
- Voltar para documentação .cursor/rules

---

## 🎓 BENEFÍCIOS IMPLEMENTADOS

### ⚡ Velocidade:
- **10x mais rápido** encontrar informações
- **Zero tempo perdido** em tentativa-erro
- **Diagnóstico preciso** em 10 minutos

### 🎯 Precisão:
- **Conhecimento específico** do projeto
- **Protocolos validados** em casos reais
- **Receitas testadas** para cada tipo de bug

### 🚀 Produtividade:
- **Máximo 2 builds** por problema
- **Sem loops infinitos**
- **Progresso mensurável** e consistente

---

## 📊 MÉTRICAS DE SUCESSO

### Antes (Sem hierarquia):
- ❌ 6 tentativas em média
- ❌ 10+ builds por problema
- ❌ 2 horas por bug
- ❌ Alta frustração

### Depois (Com hierarquia):
- ✅ Diagnóstico preciso
- ✅ 1-2 builds por problema
- ✅ 15-30 minutos por bug
- ✅ Baixa frustração

---

## 🔄 MANUTENÇÃO CONTÍNUA

### Revisões mensais:
- [ ] Atualizar PROJECT_CONTEXT_FULL.md com novas estruturas
- [ ] Validar protocolos em AI_GUIDE_FINAL.md
- [ ] Adicionar novas receitas ao GUIA_DIAGNOSTICO_SENIOR.md
- [ ] Testar integração MCP

### Controle de versão:
- Mudanças documentadas com data
- Versão atual: 1.0 (27/01/2026)
- Próxima revisão: 27/02/2026

---

## 🏆 REGRA DE OURO

> **"Documentação .cursor/rules primeiro, comandos como exceção, MCP como suporte operacional"**

Esta hierarquia garante eficiência máxima, debugging científico e desenvolvimento sustentável.

---

*Documento vivo - Baseado em melhores práticas de documentação e Model Context Protocol*