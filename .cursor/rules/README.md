# 📚 GESTÃO DE BILHARES - DOCUMENTAÇÃO ESTRATÉGICA

> **Objetivo**: Servir como base contextual tanto para humanos quanto para IAs que trabalharão neste repositório.
> **Nota**: 10/10 - Documentação otimizada para desenvolvimento eficiente.

---

## 🎯 VISÃO GERAL E SETUP RÁPIDO

### 📋 Resumo do Projeto

- **Tipo**: App Android nativo para gestão de bilhares
- **Stack**: Kotlin + Room + Hilt + Coroutines + Firebase
- **Arquitetura**: MVVM com repositórios especializados
- **Multi-tenancy**: Por rota (não por empresa)

### 🚀 Setup para Novos Desenvolvedores

```bash
# 1. Clonar e configurar ambiente
git clone https://github.com/Rossiny1/2-GestaoBilhares.git
cd 2-GestaoBilhares

# 2. Abrir no Cursor/VS Code
cursor .  # ou code .

# 3. Build inicial
./gradlew assembleDebug --build-cache --parallel

# 4. Variáveis de ambiente necessárias
ANDROID_HOME=./android-sdk  # SDK local
FIREBASE_CLI=./functions/node_modules/.bin/firebase-cli
```

### 🔄 Workflow de Desenvolvimento

1. **Branch principal**: `main` (produção)
2. **Branch de trabalho**: `trabalho-funcional` (desenvolvimento)
3. **Build com cache**: `./gradlew assembleDebug --build-cache --parallel`
4. **Testes**: Emulador Android ou dispositivo físico

---

## 🏗️ ARQUITETURA TÉCNICA

### 📦 Módulos Gradle (5)

```
app/          # UI principal e Activities
core/         # Utilidades, UserSessionManager, BluetoothPrinterHelper
data/         # Entities, DAOs, Repositories, Database
sync/         # Sincronização Firebase, Handlers, Orchestrator
ui/           # Componentes UI compartilhados
```

### 🔄 Fluxos Principais

```
Autenticação → UserSessionManager → Verificação Multi-tenancy
    ↓
Rotas → Ciclos → Clientes → Mesas → Acertos
    ↓
Sincronização → Firebase → Handlers → Repositórios
```

### 🛠️ Padrões Técnicos

- **Injeção**: Hilt (AndroidX)
- **Banco**: Room com migrations incrementais
- **Assincronia**: Coroutines + StateFlow
- **Validação**: Offline-first com sincronização posterior
- **Multi-tenancy**: `rotasPermitidas` JSON na entidade `Colaborador`

---

## 🔐 SEGURANÇA E MULTI-TENANCY

### 🔑 Controle de Acesso

```kotlin
// UserSessionManager - Gerencia rotas permitidas
val rotasPermitidas = userSessionManager.getRotasPermitidas()
val podeAcessar = userSessionManager.canAccessRota(rotaId)
```

### 📋 Regras de Negócio

- **Admin**: `rotasPermitidas = null` (acesso total)
- **Colaborador**: `rotasPermitidas = "[1,2,3]"` (JSON com IDs)
- **Validação**: Sempre verificar antes de operações críticas

### 🔥 Firestore Rules

- Isolamento por `empresaId` no documento
- Validação de claims customizados
- Apenas usuários autenticados podem escrever

---

## 📱 COMO USAR O PROJETO

### 🏃‍♂️ Execução Rápida

```bash
# 1. Build do APK Debug
./gradlew assembleDebug --build-cache -x lint

# 2. Instalar em dispositivo
./gradlew installDebug  # requer dispositivo conectado

# 3. Sincronização manual (se necessário)
./gradlew :sync:runSyncManual
```

### 🧪 Testes e Debug

- **Emulador**: Android Studio AVD
- **Dispositivo Físico**: ADB via USB
- **Logs**: `adb logcat -s GestaoBilhares`
- **Debug**: Breakpoints no Android Studio

---

## 🤖 COMO TRABALHAR COM IA NESTE PROJETO

### 📋 Regras para IAs (Cursor, Claude, GPT)

1. **Sempre anexar arquivos principais** ao pedir mudanças
2. **Trabalhar por módulo**: evite alterações cruzadas desnecessárias
3. **Commits pequenos**: uma feature por PR
4. **Respeitar multi-tenancy**: não adicionar `empresaId` onde não existe

### 🎯 Prompts Úteis

```
# Para criar nova feature:
"Crie [FEATURE] seguindo a arquitetura MVVM existente, 
utilizando Repository especializado e mantendo compatibilidade com multi-tenancy por rota."

# Para corrigir bugs:
"Analise o erro [ERRO] nos logs, verifique o arquivo [ARQUIVO] 
e proponha solução seguindo os padrões do projeto."
```

### ⚡ Otimização de Build

```bash
# Com cache (recomendado)
./gradlew assembleDebug --build-cache --parallel

# Sem cache (apenas para limpar)
./gradlew clean assembleDebug

# Apenas módulo específico
./gradlew :app:assembleDebug
```

---

## 📊 ESTRUTURA DE DADOS

### 👥 Entidades Principais

```
Colaborador (rotasPermitidas: String?)
├── Rota (id, nome, ativo)
├── Cliente (rotaId, latitude, longitude)
├── Mesa (numero, tipo, panoAtualId)
├── CicloAcerto (rotaId, dataInicio, dataFim)
└── Acerto (cicloId, clienteId, valores)
```

### 🔄 Repositórios Especializados

- `ColaboradorRepository`: gestão de colaboradores e rotas
- `MesaRepository`: operações com mesas e panos
- `CicloAcertoRepository`: ciclos e acertos financeiros
- `ContratoRepository`: contratos e aditivos

---

## 🚀 DEPLOY E RELEASE

### 📦 Geração de APK

```bash
# Debug (desenvolvimento)
./gradlew assembleDebug

# Release (produção)
./gradlew assembleRelease
```

### 🔧 Configuração de Assinatura

- **Keystore**: `gestaobilhares-release.jks`
- **Properties**: `keystore.properties` (NÃO committed)
- **Firebase**: Associado ao package `com.example.gestaobilhares`

---

## 📞 SUPORTE E CONTATO

### 🆘 Problemas Comuns

| Problema | Solução |
|-----------|----------|
| Build falha | `./gradlew clean assembleDebug --build-cache` |
| ADB não encontrado | Verifique `ANDROID_HOME` e PATH |
| Sincronização falha | Verifique conectividade e Firebase CLI |
| Permissão negada | `rotasPermitidas` não configurado |

### 📝 Como Reportar Issues

1. **Logs**: Anexar `build.log` ou `adb logcat`
2. **Passos**: Descrever passo a passo para reproduzir
3. **Ambiente**: Versão Android, Gradle, SO
4. **Branch**: Sempre trabalhar em branch específica

---

## 📈 ROADMAP E STATUS

### ✅ Concluído (v1.0.1)

- [x] Multi-tenancy por rota implementado
- [x] UserSessionManager com rotas permitidas
- [x] Migrações SQL atualizadas
- [x] Build funcional com cache
- [x] Branch `trabalho-funcional` estável

### 🔄 Em Progresso

- [ ] Interface para gerenciar rotas por usuário
- [ ] Validação de acesso em telas críticas
- [ ] Testes automatizados para multi-tenancy
- [ ] Documentação de API interna

### 🎯 Próximos Sprints

1. **Sprint 1**: Implementar validação visual de rotas
2. **Sprint 2**: Migrar telas existentes para validação
3. **Sprint 3**: Testes de integração e performance

---

**Última atualização**: Janeiro 2026  
**Versão**: 1.0.1 (3)  
**Status**: ✅ Estável para desenvolvimento
