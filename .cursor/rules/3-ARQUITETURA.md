# 3️⃣ ARQUITETURA TÉCNICA

> **Propósito**: Definição da estrutura técnica, modularização e padrões de comunicação.  
> **Última Atualização**: 18 Dezembro 2025  
> **Versão**: 1.0 (Consolidada)

---

## 🛠️ STACK TECNOLÓGICO (Android 2025)

*   **Linguagem**: Kotlin 100% (Idiomático e Type-safe).
*   **Interface**: Jetpack Compose (Migração em 35.8%) + View System.
*   **Arquitetura**: MVVM (Model-View-ViewModel) + Clean Architecture approach.
*   **Gerenciamento de Estado**: StateFlow e SharedFlow (Reativo).
*   **Injeção de Dependência**: Hilt (Standard oficial Google).
*   **Banco de Dados**: Room (SQLite) com Flow Support.
*   **Cloud/Backend**: Firebase (Firestore, Auth, Storage, Functions).

---

## 📐 MODULARIZAÇÃO GRADLE

O projeto é dividido em 5 módulos para garantir isolamento e build speed:

1.  **`:app`**: Orquestrador, Application class, Notifications e navegação principal.
2.  **`:ui`**: Camada de visualização (Fragments, Composables e ViewModels).
3.  **`:data`**: Repositórios, DAOs, Entidades Room e lógica de persistência local.
4.  **`:sync`**: Motor de sincronização incremental, WorkManager e integração Firestore.
5.  **`:core`**: Utilitários transversais, extensões e modelos base de domínio.

---

## 🔄 MOTOR DE SINCRONIZAÇÃO (Sync Engine)

### Padrão Incremental
Utilizamos um sistema de `last_update_timestamp` para minimizar o tráfego de dados.

*   **Pull (Cloud -> Local)**: Busca apenas itens modificados desde o último sync bem-sucedido.
*   **Push (Local -> Cloud)**: Envia operações da fila local pendente.
*   **Resiliência**: Tratamento de FK (Foreign Keys) antes da inserção para evitar inconsistências.

### Fila Offline
1.  Operação é salva no Room com flag `sync_status = PENDING`.
2.  `SyncWorker` é agendado (triggers: Internet On, Charging).
3.  Após sucesso no Firestore, status local muda para `SYNCED`.

---

## 📡 MONITORAMENTO E LOGS

### Timber (Logging Moderno)
*   **Debug**: `DebugTree` com logs detalhados e tags automáticas.
*   **Produção**: `CrashlyticsTree` que envia apenas erros e alertas graves para o Firebase.

### Logs de Crash
*   **Logcat**: Captura erros fatais em tempo real.
*   **Crashlytics**: Agrupamento e análise de stack traces em produção.

---

## 🔗 Referências Próximas
*   [4-ROADMAP-PRODUCAO.md](file:///C:/Users/Rossiny/Desktop/2-GestaoBilhares/.cursor/rules/4-ROADMAP-PRODUCAO.md)
*   [2-REGRAS-NEGOCIO.md](file:///C:/Users/Rossiny/Desktop/2-GestaoBilhares/.cursor/rules/2-REGRAS-NEGOCIO.md)
