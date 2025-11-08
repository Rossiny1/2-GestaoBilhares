# Changelog

Todas as mudanças notáveis neste projeto serão documentadas neste arquivo.

O formato é baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.0.0/),
e este projeto adere ao [Semantic Versioning](https://semver.org/lang/pt-BR/).

## [1.0.0] - 2025-01-08

### Adicionado

#### Funcionalidades Principais
- Sistema completo de gestão de bilhares
- Autenticação online/offline com Firebase Auth
- Gerenciamento de rotas, clientes e mesas
- Processo completo de acerto
- Geração automática de contratos PDF
- Sistema de assinatura eletrônica com validação biométrica
- Sincronização bidirecional App ↔ Firestore
- Sistema de inventário (panos e equipamentos)
- Histórico de veículos (abastecimento e manutenção)
- Sistema de metas por colaborador
- Geração de relatórios PDF

#### Segurança
- Criptografia AES-GCM (256 bits) para dados sensíveis
- Hash PBKDF2 para senhas (10.000 iterações)
- Sanitização automática de logs
- Sistema de logging condicional (DEBUG vs RELEASE)

#### Conformidade Jurídica
- Metadados completos de assinatura (Lei 14.063/2020)
- Hash SHA-256 de integridade
- Logs de auditoria completos
- Validação biométrica de assinaturas
- Estrutura de presença física

#### Performance
- Otimizações de banco de dados (índices, queries, transações)
- Sistema de cache inteligente
- Otimizações de build (Gradle, KSP, incremental compilation)
- Remoção de runBlocking (prevenção de ANR)

#### Testes
- 144 testes implementados
- 100% de cobertura em utilitários críticos
- Testes unitários para utilitários
- Testes instrumentados para Android

#### Documentação
- README.md completo
- Documentação de APIs
- Guia de contribuição
- Changelog

### Modificado

#### Arquitetura
- Migração de LiveData para StateFlow
- Implementação de BaseViewModel
- Centralização de funcionalidades no AppRepository
- Uso de repeatOnLifecycle para observação de StateFlow

#### Banco de Dados
- Database version 46
- 27 entidades sincronizadas
- Migrations versionadas e testadas
- Otimizações de queries

#### Sincronização
- SyncManagerV2 com sincronização bidirecional completa
- Resolução de conflitos por timestamp
- Cache ilimitado para funcionamento offline
- Workers de background para sincronização automática

### Corrigido

#### Bugs
- Delay na atualização de rotas e clientes após importação
- Delay na exibição do resumo de acerto
- Problemas de sincronização de mesas
- Erros de validação em contratos

#### Performance
- Queries lentas otimizadas
- Remoção de runBlocking em funções suspend
- Otimização de build time
- Melhoria de uso de memória

### Segurança

#### Vulnerabilidades Corrigidas
- Senhas hardcoded removidas
- Validação de senha fortalecida
- Criptografia de dados sensíveis implementada
- Logs sanitizados em produção

## [0.9.0] - 2024-12-XX

### Adicionado
- Sistema de sincronização inicial
- Estrutura básica de contratos
- Sistema de autenticação

### Modificado
- Migração para StateFlow
- Otimizações iniciais de banco

## [0.8.0] - 2024-11-XX

### Adicionado
- Sistema de acertos
- Gerenciamento de mesas
- Histórico de veículos

### Modificado
- Refatoração de ViewModels
- Melhorias de UI

## [0.7.0] - 2024-10-XX

### Adicionado
- Sistema de rotas
- Gerenciamento de clientes
- Sistema de inventário

### Modificado
- Estrutura inicial do projeto
- Configuração do Firebase

---

## Tipos de Mudanças

- **Adicionado**: Novas funcionalidades
- **Modificado**: Mudanças em funcionalidades existentes
- **Depreciado**: Funcionalidades que serão removidas
- **Removido**: Funcionalidades removidas
- **Corrigido**: Correções de bugs
- **Segurança**: Vulnerabilidades corrigidas

## Versões

- **MAJOR**: Mudanças incompatíveis na API
- **MINOR**: Novas funcionalidades compatíveis
- **PATCH**: Correções de bugs compatíveis

---

**Última atualização**: 2025-01-08

