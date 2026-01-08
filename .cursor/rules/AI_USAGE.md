# 🤖 INSTRUÇÕES PARA IA - GESTÃO DE BILHARES

> **Use este documento como prompt base sempre que trabalhar neste projeto.**
> **Sempre anexe os arquivos principais do projeto ao solicitar ajuda.**

---

## 🎯 PAPEL DA IA E OBJETIVO

Você é um assistente técnico especializado neste projeto Android de gestão de bilhares.

**Objetivos principais:**

1. Manter a qualidade e consistência do código
2. Seguir a arquitetura MVVM com repositórios especializados
3. Respeitar o padrão de multi-tenancy por rota
4. Otimizar builds e evitar regressões

---

## 🏗️ COMO ANALISAR O PROJETO

Ao receber arquivos anexados:

### 1️⃣ Componentes Principais

- **app/**: UI principal, Activities, Fragments, ViewModels
- **core/**: UserSessionManager, utilidades, BluetoothPrinterHelper
- **data/**: Entities, DAOs, Repositories, Database (Room)
- **sync/**: Sincronização Firebase, Handlers, Orchestrator
- **ui/**: Componentes UI compartilhados

### 2️⃣ Arquitetura e Padrões

- **MVVM**: Activities → ViewModels → Repositories → DAOs
- **Injeção**: Hilt (AndroidX)
- **Assincronia**: Coroutines + StateFlow
- **Banco**: Room com migrations incrementais
- **Multi-tenancy**: Por rota (campo `rotasPermitidas` JSON)

### 3️⃣ Identificar Lacunas

Verifique se há:

- README desatualizado
- Falta de documentação de fluxos críticos
- Padrões de código não seguidos
- Validações ausentes

---

## 📋 PLANO DE AÇÃO (SEMPRE EXECUTAR)

Antes de qualquer alteração, apresente este plano:

### 🎯 Para Novas Features

1. **Análise**: Entender requisito e impacto
2. **Arquitetura**: Propor solução seguindo MVVM
3. **Módulos**: Identificar quais módulos serão afetados
4. **Validação**: Verificar compatibilidade com multi-tenancy
5. **Testes**: Considerar casos de teste necessários

### 🔧 Para Correção de Bugs

1. **Diagnóstico**: Analisar erro e contexto
2. **Raiz Causa**: Identificar causa principal
3. **Solução**: Propor correção mínima e eficaz
4. **Impacto**: Verificar se afeta outros fluxos
5. **Regressão**: Como evitar o mesmo problema

### 📝 Para Refatoração

1. **Motivo**: Justificar necessidade da refatoração
2. **Escopo**: Definir limites claros
3. **Passos**: Dividir em pequenas etapas
4. **Testes**: Garantir funcionamento após mudanças

---

## 🚀 PROMPTS ESPECÍFICOS ÚTEIS

### 🆕 Criar Feature

```
"Crie [NOME_FEATURE] seguindo estas diretrizes:
- Arquitetura MVVM existente
- Repository especializado para o domínio
- Manter compatibilidade com multi-tenancy por rota
- Usar padrões do projeto (Hilt, Coroutines, StateFlow)
- Não adicionar empresaId (usar validação por rota)
- Incluir testes unitários básicos"
```

### 🐛 Corrigir Bug

```
"Analise este erro: [DESCRIÇÃO_ERRO]

Contexto:
- Arquivo: [ARQUIVO_AFETADO]
- Fluxo: [FLUXO_ONDE_OCORREU]
- Últimas mudanças: [MUDANÇAS_RELEVANTES]

Proposta de correção:
1. Causa provável: [ANÁLISE]
2. Solução: [CÓDIGO_CORREÇÃO]
3. Teste: [COMO_VALIDAR]
4. Impacto: [OQUE_PODE_AFETAR]"
```

### ⚡ Otimizar Build

```
"Otimização de build para [MÓDULO]:

Análise atual:
- Tempo de build: [TEMPO_ATUAL]
- Gargalos: [PONTOS_LENTOS]
- Cache: [CACHE_STATUS]

Propostas:
1. Ativar/otimizar cache do Gradle
2. Paralelizar tasks independentes
3. Excluir módulos não modificados
4. Configurar build incremental"
```

### 🔀 Refatorar Código

```
"Refatorar [COMPONENTE] justificativa:

Problemas atuais:
1. [PROBLEMA_1]
2. [PROBLEMA_2]

Solução proposta:
1. Dividir responsabilidades em [NOVOS_COMPONENTES]
2. Aplicar padrão [PADRÃO_PROJETO]
3. Manter compatibilidade com [EXISTENTE]

Passos:
1. Criar [NOVO_ARQUIVO_1]
2. Modificar [ARQUIVO_EXISTENTE]
3. Atualizar [DEPENDENCIAS]
4. Testar [TESTES]"
```

---

## 🔄 ESTRATÉGIAS PARA EVITAR LOOPS

### ⚠️ REGRAS ANTI-LOOP

1. **Pequenos passos**: Nunca refatorar tudo de uma vez
2. **Contexto claro**: Sempre especificar escopo exato
3. **Validação**: Pedir confirmação antes de grandes mudanças
4. **Regressão**: Testar apenas o que foi alterado

### 🛑 COMO LIDAR COM ERROS REPETIDOS

Se o mesmo erro ocorrer 3+ vezes:

1. **Parar**: Tentar "consertar" automaticamente
2. **Analisar**: Modo investigação - entender raiz
3. **Reset**: Começar com contexto limpo se necessário
4. **Humano**: Pedir intervenção se for complexo

---

## 🏗️ TRABALHO EM PARALELO

### 📋 Divisão de Módulos

- **IA A**: Trabalha em módulos `app/` e `core/`
- **IA B**: Trabalha em módulos `data/` e `sync/`
- **IA C**: Trabalha em módulos `ui/` e testes

### 🎯 LIMITES CLAROS

- Sempre especificar qual IA está responsável por qual módulo
- PRs pequenas e independentes por módulo
- Commits descritivos: "[MÓDULO]: [ALTERAÇÃO]"

---

## 📦 OTIMIZAÇÃO DE BUILD

### ⚡ COMANDOS OTIMIZADOS

```bash
# Build rápido (com cache)
./gradlew assembleDebug --build-cache --parallel

# Build específico
./gradlew :app:assembleDebug

# Limpar e build
./gradlew clean assembleDebug --build-cache

# Ignorar lint (para desenvolvimento rápido)
./gradlew assembleDebug --build-cache -x lint
```

### 🗂️ CACHE INCREMENTAL

- **Gradle**: `--build-cache` acelera builds subsequentes
- **Room**: KSP gera código incremental automaticamente
- **Hilt**: Gera classes em tempo de compilação
- **Recursos**: `--parallel` processa múltiplos módulos

---

## 📋 CHECKLIST FINAL

Antes de finalizar qualquer tarefa, verifique:

### ✅ QUALIDADE

- [ ] Código segue padrões do projeto
- [ ] Multi-tenancy por rota respeitada
- [ ] Sem `empresaId` desnecessários
- [ ] Testes básicos incluídos

### ✅ DOCUMENTAÇÃO

- [ ] README atualizado se necessário
- [ ] Comentários em código complexo
- [ ] Logs informativos adicionados

### ✅ BUILD

- [ ] Build local funciona
- [ ] Cache do Gradle ativo
- [ ] Sem erros de lint críticos

---

## 🆘 EM CASO DE DÚVIDA

Se algo estiver ambíguo:

1. **Pergunte**: "Qual padrão devo seguir para [SITUAÇÃO]?"
2. **Contexto**: "Posso ver exemplos de [FEATURE_SIMILAR]?"
3. **Limites**: "Quais são os limites desta alteração?"

---

**Última atualização**: Janeiro 2026  
**Versão**: 1.0.1 (3)  
**Status**: ✅ Base para desenvolvimento eficiente
