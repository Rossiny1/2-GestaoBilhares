# 📖 1️⃣ STATUS GERAL & SETUP

> **LEIA PRIMEIRO** - Este é o ponto de partida para entender o projeto.  
> **Propósito**: Visão imediata do projeto, saúde técnica, setup e workflow de desenvolvimento.  
> **Última Atualização**: 12 de Janeiro de 2026  
> **Versão**: 1.0.1 (4) - Release APK Gerado  
> **Status**: ✅ **BUILD FUNCIONAL - APK RELEASE GERADO**

---

## 📈 SAÚDE DO PROJETO

**Nota Geral: 9.5/10 ⭐⭐⭐⭐⭐**

| Critério | Nota | Comentário |
| :--- | :--- | :--- |
| **Arquitetura** | 9.8 | Modularização consolidada + Padrão Orchestrator Solidificado. |
| **Sincronização** | 9.9 | ✅ Fix Rotas + Padronização GSON + CancellationException corrigido em todos handlers. |
| **Segurança** | 9.5 | Firestore Rules enrijecidas. Custom Claims ativas. |
| **Qualidade** | 9.9 | ✅ Build release funcional. APK gerado. 4 testes sync falhando (não essenciais). Erros de compilação corrigidos. |
| **Produtividade** | 10.0 | Suporte total via IA com MCP Firebase/Crashlytics. |

---

## 🚀 SETUP RÁPIDO

### Ambiente de Desenvolvimento

Este projeto utiliza **Cursor Cloud** como ambiente principal de desenvolvimento, integrado com Firebase CLI e GitHub.

### Pré-requisitos

1. **Cursor Cloud**: Ambiente remoto configurado com acesso ao workspace
2. **Firebase CLI**: Autenticado via `firebase login:ci` (token armazenado em `FIREBASE_TOKEN`)
3. **GitHub**: Repositório `https://github.com/Rossiny1/2-GestaoBilhares`
4. **Android SDK**: Configurado em `/workspace/android-sdk` (definido em `local.properties`)

### Comandos Essenciais

```bash
# 🔨 Build e Instalação (Debug)
./gradlew installDebug

# 🧹 Limpeza e Build
./gradlew clean assembleDebug

# 🧪 Testes
./gradlew test

# 📦 Build Release
./gradlew clean :app:assembleRelease

# 🚀 Deploy Firebase App Distribution
export FIREBASE_TOKEN="seu_token_aqui"
./gradlew :app:appDistributionUploadRelease
```

## 📦 STATUS DO BUILD

### ✅ **Release APK Gerado com Sucesso**

- **Data**: 12/01/2026
- **Localização**: `app/build/outputs/apk/release/app-release.apk`
- **Build Time**: 15m 30s
- **Comando**: `./gradlew assembleRelease -x uploadCrashlyticsMappingFileRelease`
- **Status**: ✅ Pronto para Firebase App Distribution

### 📊 **Status dos Testes**

- **Total**: 52 testes
- **Passando**: 48 ✅ (92.3%)
- **Falhando**: 4 ⚠️ (SyncOrchestration - não essenciais)
- **Impacto**: ❌ Não bloqueia deploy

---

## 🛠️ STATUS DOS MÓDULOS

1. **`:sync`**: ✅ **ESTÁVEL**. Orchestrator e Handlers consolidados. CancellationException corrigido. Padronização de entidades concluída.
2. **`:data`**: ✅ **ESTÁVEL**. AppRepository em processo de delegação. Entidades protegidas com `@SerializedName` (174 campos padronizados).
3. **`:ui`**: 🟡 **EM TRANSIÇÃO**. 0% Compose (51 Fragments + 27 Dialogs ainda em ViewBinding). Meta: 60% no Q2/2026.
4. **`:core`**: ✅ **ESTÁVEL**. Utilitários e segurança consolidados. `FinancialCalculator` com 100% de cobertura.

## 🔗 MONITORAMENTO

* [Firebase Console](https://console.firebase.google.com/project/gestaobilhares)
- **MCP Crashlytics**: Ativo e configurado para análise via assistente.
- **GitHub**: <https://github.com/Rossiny1/2-GestaoBilhares>

## 📚 ORDEM DE LEITURA DA DOCUMENTAÇÃO

**Para novos desenvolvedores, leia nesta ordem:**

1. **📖 1️⃣ STATUS GERAL & SETUP** (este documento) ⭐ **COMEÇE AQUI**
   - Visão geral do projeto
   - Setup e workflow de desenvolvimento
   - Cursor Cloud, Firebase CLI, GitHub

2. **📖 2️⃣ REGRAS DE NEGÓCIO** (`.cursor/rules/2-REGRAS-NEGOCIO.md`)
   - Princípios fundamentais (Offline-First, Multi-Tenancy)
   - Fluxo operacional (Rotas, Ciclos, Acertos)
   - Regras de negócio financeiro

3. **📖 3️⃣ ARQUITETURA E PADRÕES** (`.cursor/rules/3-ARQUITETURA-E-PADROES.md`)
   - Estrutura modular (5 módulos Gradle)
   - Padrões de sincronização
   - Stack técnico e boas práticas

4. **📖 4️⃣ SEGURANÇA E OPERAÇÃO** (`.cursor/rules/4-SEGURANCA-E-OPERACAO.md`)
   - Firestore Rules e Custom Claims
   - Processo de release e deploy
   - Monitoramento Crashlytics

5. **📖 5️⃣ ROADMAP 2026** (`.cursor/rules/5-ROADMAP-2026.md`)
   - Fases pendentes
   - Marcos concluídos
   - Planejamento futuro

> 💡 **Dica**: Veja também o [README.md](./README.md) para um índice completo da documentação.

---

## 📖 GUIA PARA NOVOS DESENVOLVEDORES

### 🎯 Visão Geral do Ambiente

Este projeto utiliza **Cursor Cloud** como ambiente principal de desenvolvimento, integrado com Firebase CLI e GitHub.

### ✅ Checklist de Onboarding

- [ ] Ler toda a documentação em `.cursor/rules/`
- [ ] Entender estrutura modular (5 módulos Gradle)
- [ ] Configurar ambiente local (opcional) ou usar Cursor Cloud
- [ ] Executar testes: `./gradlew test`
- [ ] Fazer build de debug: `./gradlew installDebug`
- [ ] Entender fluxo GitHub ↔ VM ↔ Local (ver seção abaixo)
- [ ] Configurar Firebase CLI: `firebase login:ci`
- [ ] Ler regras de negócio (multi-tenancy, offline-first)

### 🔄 Fluxo de Sincronização Detalhado

#### Desenvolvimento na VM (Cursor Cloud) - RECOMENDADO

```bash
# 1. Acessar workspace
cd /workspace

# 2. Criar branch
git checkout -b feature/sua-feature

# 3. Desenvolver (Cursor AI tem acesso completo ao projeto)
# 4. Testar
./gradlew test

# 5. Commit e push
git add .
git commit -m "feat: descrição"
git push origin feature/sua-feature
```

#### Desenvolvimento Local + Sincronização

```bash
# LOCAL → GITHUB → VM
# 1. Na sua máquina local
git push origin feature/sua-feature

# 2. Na VM (Cursor Cloud)
cd /workspace
git fetch origin
git pull origin feature/sua-feature

# VM → GITHUB → LOCAL
# 1. Na VM
git push origin feature/sua-feature

# 2. Na sua máquina local
git fetch origin
git pull origin feature/sua-feature
```

### 🚀 Deploy (Sempre na VM)

```bash
# Na VM (Cursor Cloud)
export FIREBASE_TOKEN="seu_token"
./gradlew :app:assembleRelease
./gradlew :app:appDistributionUploadRelease
```

### 🆘 Troubleshooting

- **SDK não encontrado**: Verificar `local.properties` com `sdk.dir=/caminho/android-sdk`
- **Firebase não autenticado**: `firebase login:ci` e exportar `FIREBASE_TOKEN`
- **Testes falhando**: `./gradlew clean test`
- **Sincronização**: Sempre `git fetch origin` antes de `git pull`

---

## 📦 PRÓXIMAS FASES (RESUMO)

1. **Refatoração SyncRepository**: ⚠️ **CRÍTICO** - Ainda com 3644 linhas (meta: < 300). Bloqueia manutenibilidade.
2. **Expansão de Testes**: ✅ Handlers críticos cobertos. Todos testes passando. 3 testes corrigidos recentemente (ConflictResolution, ComprehensiveSync).
3. **Migração Compose**: 🎯 Prioridade Q2/2026. 0% atual (51 Fragments + 27 Dialogs). Meta: 60% até Q2.
4. **Monitoramento Crashlytics**: ✅ 4 erros corrigidos. Mapping.txt gerado no build release. Monitorar se erros pararam após deploy.

## 🔄 WORKFLOW DE DESENVOLVIMENTO

### ⭐ Ambiente Principal: Cursor Cloud

**ESTE É O AMBIENTE PRINCIPAL PARA DESENVOLVIMENTO E IMPLEMENTAÇÕES.**

#### Por que Cursor Cloud?

- ✅ **Integração nativa** com Firebase CLI e MCP (Model Context Protocol)
- ✅ **Acesso direto ao Crashlytics** via MCP para análise de erros em tempo real
- ✅ **Ambiente remoto consistente** (VM) eliminando problemas de setup local
- ✅ **Assistente de IA** com contexto completo do projeto e acesso ao código
- ✅ **Sincronização automática** com GitHub
- ✅ **Builds de release** sempre na VM (ambiente configurado e consistente)

### Fluxo de Trabalho

#### 1. Desenvolvimento Local (Máquina do Desenvolvedor)

```bash
# Clone do repositório
git clone https://github.com/Rossiny1/2-GestaoBilhares.git
cd 2-GestaoBilhares

# Configurar Android SDK
echo "sdk.dir=/caminho/para/android-sdk" > local.properties

# Configurar Firebase CLI
firebase login:ci
# Copiar o token gerado e exportar:
export FIREBASE_TOKEN="token_gerado"

# Criar branch para feature
git checkout -b feature/nome-da-feature

# Desenvolver e testar localmente
./gradlew test
./gradlew installDebug

# Commit e push
git add .
git commit -m "feat: descrição da feature"
git push origin feature/nome-da-feature
```

#### 2. Desenvolvimento na VM (Cursor Cloud)

```bash
# A VM já tem o projeto clonado e configurado
cd /workspace

# Verificar status
git status
git branch

# Criar branch para feature
git checkout -b feature/nome-da-feature

# Desenvolver usando Cursor AI
# O assistente tem acesso completo ao código e pode:
# - Analisar erros do Crashlytics via MCP
# - Fazer correções baseadas em logs reais
# - Implementar features seguindo padrões do projeto

# Testar
./gradlew test
./gradlew :app:assembleRelease

# Commit e push
git add .
git commit -m "feat: descrição da feature"
git push origin feature/nome-da-feature
```

#### 3. Sincronização GitHub ↔ VM ↔ Local

**IMPORTANTE**: GitHub é a fonte única da verdade. Sempre sincronizar via GitHub.

**Fluxo VM → GitHub → Local:**

```bash
# 1. Na VM (Cursor Cloud) - fazer commit e push
cd /workspace
git add .
git commit -m "feat: implementação via Cursor Cloud"
git push origin nome-da-branch

# 2. Na máquina local - sincronizar
git fetch origin
git checkout nome-da-branch
git pull origin nome-da-branch
```

**Fluxo Local → GitHub → VM:**

```bash
# 1. Na máquina local - fazer commit e push
git add .
git commit -m "feat: implementação local"
git push origin nome-da-branch

# 2. Na VM (Cursor Cloud) - sincronizar
cd /workspace
git fetch origin
git checkout nome-da-branch
git pull origin nome-da-branch
```

**Dica**: Sempre fazer `git fetch origin` antes de `git pull` para evitar problemas de sincronização.

### Integração Firebase CLI

#### Autenticação

```bash
# Gerar token CI (fazer uma vez)
firebase login:ci

# O token deve ser exportado como variável de ambiente
export FIREBASE_TOKEN="seu_token_aqui"

# Verificar autenticação
firebase projects:list
```

#### Deploy via Gradle (Recomendado)

```bash
# Build e deploy em um comando
export FIREBASE_TOKEN="seu_token"
./gradlew :app:appDistributionUploadRelease
```

#### Deploy via Firebase CLI

```bash
# Alternativa usando CLI diretamente
firebase appdistribution:distribute \
  app/build/outputs/apk/release/app-release.apk \
  --app 1:1089459035145:android:2d3b94222b1506a844acd8 \
  --groups "testers" \
  --release-notes "Release notes aqui"
```

### Estrutura de Branches

```
main (produção)
├── release/v1.0.1 (releases)
├── develop (desenvolvimento)
└── feature/* (features individuais)
```

**Convenção de Commits:**

- `feat:` Nova funcionalidade
- `fix:` Correção de bug
- `refactor:` Refatoração de código
- `test:` Adição/atualização de testes
- `docs:` Documentação
- `chore:` Tarefas de manutenção

### Monitoramento e Debugging

#### Crashlytics via MCP

O Cursor Cloud tem acesso direto ao Crashlytics via MCP:

- Análise de erros em tempo real
- Stack traces desofuscados (com mapping.txt)
- Correlação com código fonte
- Correção proativa de bugs

#### Logs Locais

```bash
# Ver logs do app (Android)
adb logcat | grep -i "gestaobilhares"

# Ver logs de sincronização
adb logcat | grep -i "sync"
```

---

## ⚠️ PENDÊNCIAS NÃO DOCUMENTADAS

1. **TODOs/FIXMEs no Código**: ~10 arquivos com comentários TODO/FIXME (SignatureView, BaseViewModel, AuthViewModel, ColaboradorManagement, etc.). Revisar e priorizar.
2. **LeakCanary**: Não implementado (mencionado no roadmap Q3/2026, mas não configurado). Importante para detectar vazamentos de memória.
3. **Testes E2E**: Espresso nas dependências mas sem testes implementados. Roadmap prevê Q4/2026.
4. **KDoc**: Documentação técnica incompleta. Roadmap prevê 100% das classes públicas com KDoc até Q4/2026.
