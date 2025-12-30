# ⚡ PERFORMANCE MÁXIMA - LEITURA OBRIGATÓRIA

> **⚠️ ATENÇÃO**: Este arquivo DEVE ser lido PRIMEIRO por TODOS os agentes antes de iniciar qualquer tarefa.  
> **Última Atualização**: Janeiro 2026  
> **Versão**: 1.0  
> **Status**: 🔴 **OBRIGATÓRIO**

---

## 🚨 REGRA FUNDAMENTAL

**TODOS os agentes DEVEM garantir que o ambiente está configurado para MÁXIMA PERFORMANCE antes de iniciar qualquer trabalho.**

---

## ✅ CHECKLIST OBRIGATÓRIO DE INICIALIZAÇÃO

Antes de iniciar QUALQUER tarefa, o agente DEVE:

### 1. Verificar e Otimizar Gradle ⚡
```bash
# Parar daemons órfãos
./gradlew --stop

# Verificar configurações de performance
grep "org.gradle.workers.max" gradle.properties
# Deve retornar: org.gradle.workers.max=4

# Verificar cache habilitado
grep "org.gradle.caching=true" gradle.properties
# Deve retornar: org.gradle.caching=true
```

### 2. Verificar Firebase CLI 🔥
```bash
# Verificar se Firebase CLI está disponível
firebase --version
# Deve retornar versão (ex: 15.1.0 ou superior)

# Se não estiver disponível:
# Windows: Verificar scripts/deploy-*.ps1
# Linux: npm install -g firebase-tools
```

### 3. Verificar Configurações do Cursor 🎯
```bash
# Verificar auto-approve configurado
cat .cursor/config.json | grep -A 5 "autoApprove"

# Verificar auto-save otimizado (500ms)
cat .cursor/settings.json | grep "autoSaveDelay"
# Deve retornar: "files.autoSaveDelay": 500
```

### 4. Verificar Coordenação de Agentes 👥
```bash
# Verificar arquivo de status
cat .cursor/agent-status.json

# Se outro agente estiver trabalhando, verificar conflitos
# Regra: Um arquivo por vez, build tem prioridade
```

### 5. Limpar Cache se Necessário 🧹
```bash
# Apenas se build estiver muito lento ou com erros estranhos
# ./gradlew cleanBuildCache

# NÃO limpar cache desnecessariamente - economiza tempo
```

---

## ⚡ CONFIGURAÇÕES DE PERFORMANCE OBRIGATÓRIAS

### Gradle Properties (gradle.properties)
```properties
# ✅ OBRIGATÓRIO: Workers = número de CPUs (4)
org.gradle.workers.max=4

# ✅ OBRIGATÓRIO: Cache habilitado
org.gradle.caching=true
org.gradle.configuration-cache=true

# ✅ OBRIGATÓRIO: Compilação paralela
org.gradle.parallel=true

# ✅ OBRIGATÓRIO: Kotlin incremental
kotlin.incremental=true
ksp.incremental=true
```

### Cursor Settings (.cursor/settings.json)
```json
{
  "cursor.general.autoAcceptDelay": 500,  // ✅ OBRIGATÓRIO: 500ms
  "files.autoSaveDelay": 500,              // ✅ OBRIGATÓRIO: 500ms
  "editor.formatOnSave": true,            // ✅ OBRIGATÓRIO: Habilitado
  "files.watcherExclude": {               // ✅ OBRIGATÓRIO: Excluir build/
    "**/build/**": true,
    "**/.gradle/**": true
  }
}
```

### Cursor Config (.cursor/config.json)
```json
{
  "cursor.autoApprove": {
    "commands": ["gradlew*", "firebase*", "npm*", "node*", ...],  // ✅ OBRIGATÓRIO
    "filePatterns": ["**/*.kt", "**/*.xml", "*.gradle*", ...]     // ✅ OBRIGATÓRIO
  }
}
```

---

## 🎯 COMANDOS DE BUILD OTIMIZADOS

**SEMPRE use estes comandos para builds:**

```bash
# Build Debug (desenvolvimento)
./gradlew assembleDebug --parallel --build-cache

# Build Release
./gradlew assembleRelease --parallel --build-cache

# Testes
./gradlew testDebugUnitTest --parallel --build-cache

# NUNCA use --no-daemon a menos que seja absolutamente necessário
# NUNCA use clean a menos que seja absolutamente necessário
```

---

## 🚫 PROIBIÇÕES ABSOLUTAS

**NUNCA faça estas ações sem necessidade:**

1. ❌ **NÃO** executar `./gradlew clean` sem necessidade
2. ❌ **NÃO** usar `--no-daemon` sem necessidade
3. ❌ **NÃO** desabilitar cache do Gradle
4. ❌ **NÃO** modificar `gradle.properties` sem justificativa
5. ❌ **NÃO** trabalhar em arquivos já bloqueados por outro agente
6. ❌ **NÃO** ignorar o arquivo `.cursor/agent-status.json`

---

## 📊 VERIFICAÇÃO RÁPIDA DE PERFORMANCE

Execute este comando para verificar se tudo está otimizado:

```bash
# Script de verificação (criar se não existir)
./scripts/verify-performance.sh
```

Ou verifique manualmente:
```bash
# 1. Gradle workers
grep "workers.max" gradle.properties | grep -q "4" && echo "✅ Workers OK" || echo "❌ Workers incorreto"

# 2. Cache habilitado
grep -q "org.gradle.caching=true" gradle.properties && echo "✅ Cache OK" || echo "❌ Cache desabilitado"

# 3. Auto-save otimizado
grep -q '"files.autoSaveDelay": 500' .cursor/settings.json && echo "✅ Auto-save OK" || echo "❌ Auto-save não otimizado"

# 4. Firebase CLI
firebase --version > /dev/null 2>&1 && echo "✅ Firebase CLI OK" || echo "⚠️ Firebase CLI não encontrado"
```

---

## 🔄 FLUXO DE INICIALIZAÇÃO OBRIGATÓRIO

**TODOS os agentes DEVEM seguir esta sequência:**

1. ✅ **Ler este arquivo primeiro** (você está aqui)
2. ✅ **Verificar configurações** (usar checklist acima)
3. ✅ **Corrigir problemas** se encontrados
4. ✅ **Atualizar agent-status.json** se for trabalhar
5. ✅ **Iniciar trabalho** apenas após verificação

---

## 📚 DOCUMENTAÇÃO RELACIONADA

Após ler este arquivo, consulte:
- `documentation/OTIMIZACAO-AMBIENTE-IA.md` - Guia completo de otimizações
- `documentation/OTIMIZACOES-IMPLEMENTADAS.md` - O que já foi implementado
- `.cursor/rules/1-STATUS-GERAL.md` - Status geral do projeto

---

## ⚠️ LEMBRETE FINAL

**Se você não seguiu este checklist, PARE e leia novamente antes de continuar.**

A performance do ambiente impacta diretamente:
- ⏱️ Tempo de resposta da IA
- 🔨 Velocidade de builds
- 💻 Eficiência geral do desenvolvimento

**NÃO PULE ESTAS VERIFICAÇÕES.**

---

**Última atualização**: Janeiro 2026  
**Próxima revisão**: Quando houver mudanças significativas no ambiente
