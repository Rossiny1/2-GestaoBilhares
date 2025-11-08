# Gestão Bilhares

Sistema completo de gestão para locação de mesas de bilhar, desenvolvido em Android com arquitetura moderna e offline-first.

## 📋 Índice

- [Sobre o Projeto](#sobre-o-projeto)
- [Funcionalidades](#funcionalidades)
- [Tecnologias](#tecnologias)
- [Arquitetura](#arquitetura)
- [Requisitos](#requisitos)
- [Instalação](#instalação)
- [Configuração](#configuração)
- [Estrutura do Projeto](#estrutura-do-projeto)
- [Uso](#uso)
- [Testes](#testes)
- [Contribuindo](#contribuindo)
- [Licença](#licença)

## 🎯 Sobre o Projeto

O **Gestão Bilhares** é um aplicativo Android completo para gerenciamento de locação de mesas de bilhar, desenvolvido seguindo as melhores práticas de desenvolvimento Android moderno. O sistema oferece funcionalidades completas de gestão de clientes, rotas, acertos, contratos com assinatura eletrônica, inventário e relatórios.

### Características Principais

- ✅ **Offline-First**: Funciona 100% offline com sincronização automática
- ✅ **Conformidade Jurídica**: Assinaturas eletrônicas conforme Lei 14.063/2020
- ✅ **Sincronização Bidirecional**: App ↔ Firestore em tempo real
- ✅ **Segurança**: Criptografia de dados sensíveis (CPF, CNPJ, senhas)
- ✅ **Performance**: Otimizações de banco, cache e build

## 🚀 Funcionalidades

### Sistema Principal

- **Autenticação**: Login com Firebase Auth (online/offline)
- **Rotas**: Gerenciamento de rotas de entrega com ciclos de acerto
- **Clientes**: Cadastro, listagem, detalhes e histórico
- **Acertos**: Processo completo de acerto com cálculo automático
- **Mesas**: Gerenciamento de mesas (depósito, vinculação, histórico)
- **Contratos**: Geração automática de contratos PDF com assinatura eletrônica
- **Inventário**: Estoque de panos e equipamentos
- **Veículos**: Histórico de abastecimento e manutenção
- **Metas**: Sistema de metas por colaborador
- **Relatórios**: Geração de PDFs para fechamento e acertos

### Sistema de Contratos

- Geração automática de contratos PDF
- Suporte a múltiplas mesas por contrato
- Captura e validação biométrica de assinaturas
- Conformidade jurídica completa (Lei 14.063/2020)
- Envio automático via WhatsApp
- Numeração automática (formato "2025-0002")

## 🛠️ Tecnologias

### Linguagem e Framework

- **Kotlin** 1.9+ (linguagem principal)
- **Android SDK** 24+ (minSdk), 34 (targetSdk)
- **Android Architecture Components**
  - ViewModel
  - StateFlow (substituição de LiveData)
  - Room Database
  - Navigation Component
  - WorkManager

### Bibliotecas Principais

- **Room**: Persistência local de dados
- **Firebase**:
  - Firestore (banco de dados cloud)
  - Firebase Auth (autenticação)
  - Firebase Storage (armazenamento de fotos)
- **iText7**: Geração de PDFs
- **Material Design**: Componentes de UI
- **Coroutines**: Programação assíncrona
- **KSP**: Processamento de anotações Kotlin

### Segurança

- **Android Keystore**: Criptografia AES-GCM (256 bits)
- **PBKDF2**: Hash de senhas (10.000 iterações)
- **Sanitização de Logs**: Proteção de dados sensíveis

## 🏗️ Arquitetura

### MVVM Modernizado

O projeto segue a arquitetura **MVVM** (Model-View-ViewModel) com as seguintes camadas:

```
┌─────────────────────────────────────────┐
│              View (Fragments)           │
│  - DataBinding                          │
│  - StateFlow observation                │
│  - repeatOnLifecycle                    │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│          ViewModel                       │
│  - Lógica de negócio                    │
│  - StateFlow para estado                │
│  - BaseViewModel (funcionalidades comuns)│
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│         Repository (AppRepository)       │
│  - Único ponto de acesso aos dados      │
│  - Criptografia/descriptografia         │
│  - Cache inteligente                    │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│         Data Layer                      │
│  - Room Database (local)                │
│  - Firestore (cloud)                    │
│  - SyncManagerV2 (sincronização)        │
└─────────────────────────────────────────┘
```

### Estratégia Offline-First

1. **Dados Locais**: Room Database como fonte primária
2. **Sincronização**: Bidirecional automática com Firestore
3. **Cache**: Sistema de cache inteligente com TTL
4. **Conflitos**: Resolução por timestamp (mais recente vence)

### Banco de Dados

- **Room Database** (versão 46)
- **27 entidades** de negócio sincronizadas
- **Índices estratégicos** para performance
- **Transações atômicas** para operações em lote
- **Migrations** versionadas e testadas

## 📋 Requisitos

### Desenvolvimento

- **Android Studio** Hedgehog (2023.1.1) ou superior
- **JDK** 11 ou superior
- **Gradle** 8.1+
- **Kotlin** 1.9+

### Runtime

- **Android** 7.0 (API 24) ou superior
- **Permissões**:
  - Internet (sincronização)
  - Storage (fotos e PDFs)
  - Bluetooth (impressão)

## 🔧 Instalação

### 1. Clone o Repositório

```bash
git clone <repository-url>
cd 2-GestaoBilhares
```

### 2. Configuração do Firebase

1. Crie um projeto no [Firebase Console](https://console.firebase.google.com/)
2. Adicione um app Android com package name: `com.example.gestaobilhares`
3. Baixe o arquivo `google-services.json`
4. Coloque o arquivo em `app/google-services.json`

### 3. Configuração do Build

O projeto já está configurado. Apenas sincronize o Gradle:

```bash
./gradlew build
```

### 4. Executar o App

```bash
./gradlew installDebug
```

Ou use o Android Studio:
1. Abra o projeto
2. Aguarde a sincronização do Gradle
3. Clique em "Run" (Shift+F10)

## ⚙️ Configuração

### Variáveis de Ambiente

O projeto usa configurações padrão do Firebase. Para produção, configure:

- **Firebase Project ID**: Definido em `google-services.json`
- **Empresa ID**: Configurado no primeiro login

### Permissões

As permissões necessárias já estão declaradas no `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
```

## 📁 Estrutura do Projeto

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/example/gestaobilhares/
│   │   │   ├── data/
│   │   │   │   ├── database/          # Room Database
│   │   │   │   ├── entities/          # Entidades do banco
│   │   │   │   ├── dao/               # Data Access Objects
│   │   │   │   └── repository/        # AppRepository
│   │   │   ├── ui/
│   │   │   │   ├── auth/              # Autenticação
│   │   │   │   ├── clients/           # Clientes
│   │   │   │   ├── routes/            # Rotas
│   │   │   │   ├── settlement/        # Acertos
│   │   │   │   ├── contracts/         # Contratos
│   │   │   │   └── ...                # Outras telas
│   │   │   ├── sync/                  # Sincronização
│   │   │   ├── utils/                 # Utilitários
│   │   │   └── workers/               # WorkManager
│   │   └── res/                       # Recursos
│   ├── test/                          # Testes unitários
│   └── androidTest/                   # Testes instrumentados
└── build.gradle.kts
```

## 📖 Uso

### Primeiro Acesso

1. Execute o app
2. Faça login com credenciais Firebase
3. Configure a empresa (primeira vez)
4. Importe dados do Firestore (se disponível)

### Fluxo Principal

1. **Login** → Autenticação (online/offline)
2. **Rotas** → Visualizar rotas e ciclos
3. **Clientes** → Selecionar rota e visualizar clientes
4. **Acerto** → Processar acerto do cliente
5. **Contrato** → Gerar e assinar contrato (se necessário)
6. **Sincronização** → Automática em background

### Funcionalidades Especiais

- **Offline**: O app funciona completamente offline
- **Sincronização**: Automática quando online
- **Assinatura Eletrônica**: Captura biométrica com validação
- **Impressão**: Suporte a impressoras Bluetooth

## 🧪 Testes

### Executar Testes

```bash
# Testes unitários
./gradlew test

# Testes instrumentados
./gradlew connectedAndroidTest

# Todos os testes
./gradlew check
```

### Cobertura de Testes

- ✅ **144 testes** implementados
- ✅ **100%** de cobertura em utilitários críticos
- ⏳ Testes de ViewModels (em desenvolvimento)
- ⏳ Testes de integração (planejados)

### Estrutura de Testes

```
app/src/
├── test/                              # Testes unitários
│   └── java/com/example/gestaobilhares/
│       └── utils/                     # Testes de utilitários
└── androidTest/                       # Testes instrumentados
    └── java/com/example/gestaobilhares/
        └── utils/                     # Testes de Android
```

## 🤝 Contribuindo

Veja o arquivo [CONTRIBUTING.md](CONTRIBUTING.md) para detalhes sobre como contribuir.

### Padrões de Código

- **Kotlin**: Seguir [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- **Arquitetura**: MVVM com StateFlow
- **Nomenclatura**: camelCase para variáveis, PascalCase para classes
- **Comentários**: Documentar funções complexas

## 📝 Changelog

Veja o arquivo [CHANGELOG.md](CHANGELOG.md) para histórico completo de mudanças.

## 📚 Documentação Adicional

- [Arquitetura Técnica](2-ARQUITETURA-TECNICA.md)
- [Regras de Negócio](3-REGRAS-NEGOCIO.md)
- [Procedimentos de Desenvolvimento](4-PROCEDIMENTOS-DESENVOLVIMENTO.md)
- [Status Atual do Projeto](5-STATUS-ATUAL-PROJETO.md)
- [Documentação de APIs](API_DOCUMENTATION.md)
- [Guia de Contribuição](CONTRIBUTING.md)

## 📄 Licença

Este projeto é proprietário. Todos os direitos reservados.

## 📞 Suporte

Para suporte, entre em contato através dos canais oficiais do projeto.

## 🙏 Agradecimentos

- Equipe de desenvolvimento
- Comunidade Android
- Firebase Team

---

**Desenvolvido com ❤️ usando Kotlin e Android Architecture Components**

