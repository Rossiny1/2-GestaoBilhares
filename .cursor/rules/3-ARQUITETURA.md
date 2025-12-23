# 3️⃣ ARQUITETURA TÉCNICA

> **Propósito**: Definição da estrutura técnica, modularização e padrões de comunicação.  
> **Última Atualização**: Dezembro 2025  
> **Versão**: 3.0 (Atualizada - Segurança Implementada)

---

## 🛠️ STACK TECNOLÓGICO (Android 2025)

*   **Linguagem**: Kotlin 100% (Idiomático e Type-safe).
*   **Interface**: Jetpack Compose (Migração em ~35.8%) + View System (ViewBinding).
*   **Arquitetura**: MVVM (Model-View-ViewModel) + Clean Architecture approach.
*   **Gerenciamento de Estado**: StateFlow e SharedFlow (Reativo). Proibido LiveData em código novo.
*   **Injeção de Dependência**: Hilt (Standard oficial Google) - 100% migrado.
*   **Banco de Dados**: Room (SQLite) com Flow Support, KSP para code generation.
*   **Cloud/Backend**: Firebase (Firestore, Auth, Storage, Functions, Crashlytics, Analytics, Performance).
*   **Sincronização**: WorkManager para sincronização em background.
*   **Versionamento**: `versionCode = 2`, `versionName = "1.0.0"`.

---

## 📐 MODULARIZAÇÃO GRADLE

O projeto é dividido em 5 módulos para garantir isolamento e build speed:

1.  **`:app`**: 
    - Application class (`GestaoBilharesApplication`)
    - MainActivity
    - Notifications
    - CrashlyticsTree
    - Navegação principal

2.  **`:ui`**: 
    - Fragments (View System)
    - Composables (Jetpack Compose)
    - ViewModels (MVVM)
    - Adapters (RecyclerView)
    - Dialogs e componentes comuns
    - Recursos (layouts, navigation, values)

3.  **`:data`**: 
    - Repositórios (AppRepository como Facade + repositories especializados em `domain/`)
    - DAOs (Room)
    - Entidades Room
    - Database (AppDatabase, Converters)
    - Módulos Hilt (DatabaseModule, RepositoryModule)

4.  **`:sync`**: 
    - SyncRepository (sincronização bidirecional)
    - SyncManager (agendamento)
    - SyncWorker (WorkManager)
    - Utilitários de rede

5.  **`:core`**: 
    - Utilitários transversais (NetworkUtils, DateUtils, DataValidator, etc.)
    - Geração de PDFs
    - Upload de imagens
    - Helpers de impressão
    - SecurePreferencesHelper
    - UserSessionManager

---

## 🔄 MOTOR DE SINCRONIZAÇÃO (Sync Engine)

### Padrão Incremental
Utilizamos um sistema de `last_update_timestamp` para minimizar o tráfego de dados (98% de economia).

*   **Pull (Cloud -> Local)**: Busca apenas itens modificados desde o último sync bem-sucedido.
*   **Push (Local -> Cloud)**: Envia operações da fila local pendente.
*   **Resiliência**: Tratamento de FK (Foreign Keys) antes da inserção para evitar inconsistências.
*   **Multi-tenancy**: Filtra dados por `empresaId` e `rotasAtribuidas` do usuário.

### Fila Offline
1.  Operação é salva no Room com flag `sync_status = PENDING`.
2.  `SyncWorker` é agendado via WorkManager (triggers: Internet On, Charging, periódico).
3.  Após sucesso no Firestore, status local muda para `SYNCED`.
4.  Retry automático em caso de falha.

### Estrutura de Dados no Firestore
*   **Multi-tenancy**: `empresas/{empresaId}/entidades/{collectionName}/items/{itemId}`
*   **Coleções LEGADO**: `ciclos`, `despesas`, `acertos`, `mesas`, `rotas`, `clientes` (✅ Regras de segurança enrijecidas - Dezembro 2025)
*   **Custom Claims**: Gerenciadas automaticamente via Firebase Functions (`onUserCreated`, `onCollaboratorUpdated`, `onColaboradorRotaUpdated`)

---

## 📡 MONITORAMENTO E LOGS

### Timber (Logging Moderno)
*   **Debug**: `DebugTree` com logs detalhados e tags automáticas.
*   **Produção**: `CrashlyticsTree` que envia apenas erros e alertas graves para o Firebase.
*   **⚠️ ATENÇÃO**: 20+ arquivos ainda usam `android.util.Log` diretamente. Deve ser substituído por Timber.

### Logs de Crash
*   **Logcat**: Captura erros fatais em tempo real.
*   **Crashlytics**: Agrupamento e análise de stack traces em produção.
*   **Performance Monitoring**: Rastreamento de performance de operações críticas.

### MCP Crashlytics (Monitoramento via IA)
*   **Integração**: Servidor MCP configurado para acesso direto ao Crashlytics via assistente de IA.
*   **Funcionalidades**: Consulta de problemas críticos, análise de stack traces, filtros por versão/dispositivo.
*   **Uso**: O assistente pode acessar dados do Crashlytics automaticamente para análise e debugging.
*   **Documentação Completa**: Ver `documentation/CONFIGURACAO-MCP-CRASHLYTICS.md`

## 🏗️ ARQUITETURA DE REPOSITORIES

### Estrutura Híbrida Modular
*   **AppRepository**: Facade central (~1910 linhas, meta: 200-300 linhas).
*   **Repositories Especializados**: Em `data/repository/domain/` (ClienteRepository, AcertoRepository, MesaRepository, etc.).
*   **Delegação**: AppRepository deve delegar para repositories especializados (parcialmente implementado).

### Padrão de Acesso
*   **ViewModels**: Acessam apenas `AppRepository` (não DAOs diretamente).
*   **Fragments**: Usam ViewModels (não acessam repositories diretamente).
*   **Hilt**: Gerencia injeção de dependências automaticamente.

---

## 🔗 Referências Próximas
*   [4-ROADMAP-PRODUCAO.md](file:///C:/Users/Rossiny/Desktop/2-GestaoBilhares/.cursor/rules/4-ROADMAP-PRODUCAO.md)
*   [2-REGRAS-NEGOCIO.md](file:///C:/Users/Rossiny/Desktop/2-GestaoBilhares/.cursor/rules/2-REGRAS-NEGOCIO.md)
