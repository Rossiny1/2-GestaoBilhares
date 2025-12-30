# 🤖 Guia de Inicialização de Agentes - Performance Máxima

> **Propósito**: Garantir que TODOS os agentes iniciem com máxima performance  
> **Última Atualização**: Janeiro 2026  
> **Versão**: 1.0  
> **Status**: 🔴 **OBRIGATÓRIO**

---

## 🎯 OBJETIVO

Este guia garante que **TODOS os agentes** iniciem com o ambiente configurado para **MÁXIMA PERFORMANCE**, otimizando:
- ⚡ Velocidade de resposta da IA
- 🔨 Tempo de builds
- 💻 Eficiência geral do desenvolvimento

---

## 🚨 REGRA FUNDAMENTAL

**ANTES de iniciar QUALQUER tarefa, o agente DEVE:**

1. ✅ Ler `.cursor/rules/0-PERFORMANCE-MAXIMA-OBRIGATORIO.md`
2. ✅ Executar `./scripts/verify-performance.sh`
3. ✅ Corrigir qualquer problema encontrado
4. ✅ Confirmar que o ambiente está otimizado

---

## 📋 FLUXO DE INICIALIZAÇÃO OBRIGATÓRIO

### Passo 1: Leitura Obrigatória
```bash
# O agente DEVE ler este arquivo primeiro:
.cursor/rules/0-PERFORMANCE-MAXIMA-OBRIGATORIO.md
```

**Conteúdo do arquivo inclui:**
- ✅ Checklist obrigatório de inicialização
- ✅ Configurações de performance obrigatórias
- ✅ Comandos de build otimizados
- ✅ Proibições absolutas
- ✅ Verificação rápida de performance

### Passo 2: Verificação de Performance
```bash
# Executar script de verificação
./scripts/verify-performance.sh
```

**O script verifica:**
- ✅ Gradle workers (deve ser 4)
- ✅ Build cache habilitado
- ✅ Compilação paralela
- ✅ Kotlin incremental
- ✅ Cursor settings otimizados
- ✅ Firebase CLI disponível
- ✅ Daemons Gradle

### Passo 3: Correção de Problemas
Se o script encontrar erros:
1. **Parar** qualquer trabalho
2. **Corrigir** os problemas identificados
3. **Re-executar** o script de verificação
4. **Continuar** apenas quando tudo estiver OK

### Passo 4: Atualizar Status (se necessário)
```bash
# Se for trabalhar, atualizar agent-status.json
# Verificar se outro agente está trabalhando
cat .cursor/agent-status.json
```

### Passo 5: Iniciar Trabalho
Apenas após completar todos os passos anteriores.

---

## ⚡ CONFIGURAÇÕES OBRIGATÓRIAS

### Gradle Properties
```properties
# ✅ OBRIGATÓRIO
org.gradle.workers.max=4
org.gradle.caching=true
org.gradle.configuration-cache=true
org.gradle.parallel=true
kotlin.incremental=true
ksp.incremental=true
```

### Cursor Settings
```json
{
  "cursor.general.autoAcceptDelay": 500,
  "files.autoSaveDelay": 500,
  "editor.formatOnSave": true,
  "files.watcherExclude": {
    "**/build/**": true,
    "**/.gradle/**": true
  }
}
```

---

## 🎯 COMANDOS DE BUILD OTIMIZADOS

**SEMPRE use:**
```bash
./gradlew assembleDebug --parallel --build-cache
./gradlew testDebugUnitTest --parallel --build-cache
```

**NUNCA use (sem necessidade):**
```bash
./gradlew clean  # Apenas se necessário
./gradlew --no-daemon  # Apenas se necessário
```

---

## 🚫 PROIBIÇÕES ABSOLUTAS

1. ❌ **NÃO** pular a leitura do arquivo obrigatório
2. ❌ **NÃO** pular a verificação de performance
3. ❌ **NÃO** trabalhar com erros não corrigidos
4. ❌ **NÃO** modificar configurações sem justificativa
5. ❌ **NÃO** ignorar conflitos com outros agentes

---

## 📊 VERIFICAÇÃO RÁPIDA

### Script Automático (Recomendado)
```bash
./scripts/verify-performance.sh
```

### Verificação Manual
```bash
# 1. Gradle workers
grep "workers.max" gradle.properties | grep -q "4" && echo "✅" || echo "❌"

# 2. Cache
grep -q "org.gradle.caching=true" gradle.properties && echo "✅" || echo "❌"

# 3. Auto-save
grep -q '"files.autoSaveDelay": 500' .cursor/settings.json && echo "✅" || echo "❌"

# 4. Firebase CLI
firebase --version > /dev/null 2>&1 && echo "✅" || echo "⚠️"
```

---

## 🔄 ATUALIZAÇÃO DO ARQUIVO OBRIGATÓRIO

O arquivo `.cursor/rules/0-PERFORMANCE-MAXIMA-OBRIGATORIO.md` é atualizado quando:
- Novas otimizações são implementadas
- Configurações mudam
- Novos problemas são identificados

**Sempre consulte a versão mais recente.**

---

## 📚 ARQUIVOS RELACIONADOS

1. **`.cursor/rules/0-PERFORMANCE-MAXIMA-OBRIGATORIO.md`** - Leitura obrigatória
2. **`scripts/verify-performance.sh`** - Script de verificação
3. **`scripts/setup-optimization.sh`** - Script de otimização completa
4. **`documentation/OTIMIZACAO-AMBIENTE-IA.md`** - Guia completo
5. **`.cursor/agent-status.json`** - Coordenação de agentes

---

## ✅ CHECKLIST FINAL

Antes de iniciar qualquer trabalho, confirme:

- [ ] Li `.cursor/rules/0-PERFORMANCE-MAXIMA-OBRIGATORIO.md`
- [ ] Executei `./scripts/verify-performance.sh`
- [ ] Corrigi todos os erros encontrados
- [ ] Verifiquei `.cursor/agent-status.json` (se necessário)
- [ ] Ambiente está otimizado para máxima performance

**Apenas após completar TODOS os itens, inicie o trabalho.**

---

## 🎯 RESULTADO ESPERADO

Após seguir este guia, o agente terá:
- ⚡ Ambiente configurado para máxima performance
- 🔨 Builds otimizados e rápidos
- 💻 Resposta da IA mais eficiente
- ✅ Confiança de que está trabalhando no melhor ambiente possível

---

## ⚠️ LEMBRETE FINAL

**A performance do ambiente impacta diretamente:**
- Tempo de resposta da IA
- Velocidade de builds
- Eficiência geral do desenvolvimento

**NÃO PULE AS VERIFICAÇÕES.**

---

**Última atualização**: Janeiro 2026  
**Próxima revisão**: Quando houver mudanças significativas
