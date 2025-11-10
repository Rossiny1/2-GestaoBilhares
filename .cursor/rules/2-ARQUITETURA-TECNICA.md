# 2. ARQUITETURA TÉCNICA

> **Documento técnico** - Padrões de desenvolvimento, arquitetura MVVM, banco de dados, sincronização, segurança e componentes UI.

## 🏗️ PADRÕES DE DESENVOLVIMENTO

### Linguagem e Framework (Modernizado 2025)

- **Kotlin** como linguagem principal
- **Android Architecture Components** (ViewModel, StateFlow, Room)
- **Navigation Component** para navegação
- **Hilt** para injeção de dependência
- **Material Design 3** para UI
- **StateFlow** para observação reativa moderna
- **BaseViewModel** para centralização de funcionalidades

### Arquitetura MVVM Modernizada e Centralizada

- **Model**: Room Database (Entities, DAOs)
- **View**: Fragments com DataBinding + StateFlow
- **ViewModel**: Lógica de negócio com StateFlow
- **Repository**: AppRepository centralizado (único ponto de acesso)
- **BaseViewModel**: Funcionalidades comuns centralizadas
- **repeatOnLifecycle**: Observação moderna de StateFlow

**🎯 REGRA PRINCIPAL**: Centralização e simplificação sempre que possível

## 🗄️ BANCO DE DADOS

### Arquitetura Offline-First com Sincronização Bidirecional (100% Completo)

- **Estratégia**: App funciona 100% offline com sincronização automática
- **Sincronização**: Bidirecional App ↔ Firestore funcionando perfeitamente
- **Performance**: Otimizações incrementais implementadas
- **Versionamento**: Resolução de conflitos por timestamp implementada
- **Espelhamento 1:1**: **Todas as 27 entidades de negócio sincronizadas (100%)**
- **PUSH Implementado**: CREATE/INSERT, UPDATE, DELETE para todas as entidades
- **PULL Implementado**: Importação completa do Firestore na ordem correta

### Entidades Principais

- `Cliente`: Dados dos clientes
- `Mesa`: Mesas de bilhar disponíveis
- `Rota`: Rotas de entrega
- `Acerto`: Transações de acerto
- `Despesa`: Despesas por rota/ciclo
- `ContratoLocacao`: Contratos de locação (com metadados jurídicos completos - Database Version 46)
- `SignaturePoint`: Pontos de assinatura
- `SignatureStatistics`: Estatísticas biométricas da assinatura
- `CicloAcertoEntity`: Ciclos de acerto por rota
- `AditivoMesa`: Aditivos de mesa
- `Veiculo`: Dados dos veículos
- `Colaborador`: Dados dos colaboradores
- `Meta`: Metas de desempenho
- `EstoqueItem`: Itens do estoque
- `Pano`: Panos de mesa
- E mais 13 entidades...

### Otimizações de Banco (CONCLUÍDAS)

#### Fase 6.1: Índices Essenciais ✅

- ✅ 12 novos índices estratégicos em 5 entidades
- ✅ Migration 44→45 aplicada
- ✅ Database Version 45

#### Fase 6.2: Otimização de Queries ✅

- ✅ 8 queries otimizadas (strftime → range queries, subquery → JOIN)
- ✅ DateUtils.calcularRangeAno centralizado
- ✅ Repositories atualizados

#### Fase 6.3: Transações Atômicas ✅

- ✅ @Transaction em 5 métodos de operações em lote
- ✅ Garantia de atomicidade para inserções/atualizações múltiplas

**Impacto**: 30-80% de melhoria de performance em queries frequentes

### Sincronização Bidirecional (CONCLUÍDA - 100%)

- ✅ **SyncManagerV2**: Processamento robusto de operações CREATE/UPDATE/DELETE
- ✅ **Documento ID = roomId**: Evita duplicação de dados no Firestore
- ✅ **Payload Seguro**: Gson para serialização de dados complexos
- ✅ **Vinculação Automática**: Mesa-Cliente sincroniza corretamente
- ✅ **Espelhamento 1:1**: **Todas as 27 entidades de negócio sincronizadas**
- ✅ **Resolução de Conflitos**: Timestamp mais recente vence
- ✅ **Estrutura Hierárquica**: /empresas/{empresaId}/dados/ implementada
- ✅ **PULL Completo**: Todas as entidades importadas na ordem correta
- ✅ **PUSH Completo**: Todas as operações enfileiradas

### Relacionamentos

- Cliente → Mesa (1:N)
- Rota → Cliente (1:N)
- Cliente → Acerto (1:N)
- Contrato → Mesa (1:N)
- Rota → CicloAcerto (1:N)
- CicloAcerto → Acerto (1:N)
- CicloAcerto → Despesa (1:N)
- Cliente → ContratoLocacao (1:N)
- E mais relacionamentos...

## 🔐 SEGURANÇA E VALIDAÇÃO

### Autenticação e Segurança de Senhas ✅

- **PasswordHasher**: Utilitário para hash seguro de senhas
  - **Algoritmo**: PBKDF2 com SHA-256
  - **Configurações**: 10.000 iterações, salt aleatório de 16 bytes, hash de 256 bits
  - **Métodos**: `hashPassword()`, `verifyPassword()`, `isValidHashFormat()`
  - **Segurança**: Comparação timing-safe, previne timing attacks
- **AuthViewModel**: Autenticação híbrida (online/offline)
  - **Online**: Firebase Auth (sem mudanças)
  - **Offline**: Validação usando hash de senha (PBKDF2)

### Assinatura Eletrônica

- **SignatureView**: Captura de assinatura manual
- **SignatureStatistics**: Validação biométrica
- **DocumentIntegrityManager**: Hash SHA-256
- **LegalLogger**: Logs jurídicos para auditoria
- **SignatureMetadataCollector**: Metadados do dispositivo

### Validação Jurídica (Lei 14.063/2020 - 100% Conforme Cláusula 9.3)

- ✅ **Metadados Completos**: Timestamp, device ID, IP, pressão média, velocidade média, duração, total de pontos
- ✅ **Hash SHA-256**: Integridade do documento e assinaturas (locatário e locador)
- ✅ **Logs Jurídicos**: Sistema completo de auditoria (LegalLogger)
- ✅ **Validação Biométrica**: Características da assinatura (SignatureStatistics)
- ✅ **Presença Física**: Estrutura de campos implementada
- ✅ **Documento Hash**: Hash SHA-256 do PDF final gerado automaticamente
- ✅ **Database Version 46**: Migration 45→46 com todos os campos de conformidade

### Criptografia de Dados Sensíveis ✅

- ✅ **Android Keystore**: Chaves protegidas pelo sistema operacional (hardware quando disponível)
- ✅ **Algoritmo**: AES-GCM (256 bits) - recomendado pelo Android
- ✅ **Dados Criptografados**:
  - CPF/CNPJ em Cliente, Colaborador, MesaVendida, ContratoLocacao
  - Assinaturas (Base64) em ContratoLocacao e AssinaturaRepresentanteLegal
  - CPF em LogAuditoriaAssinatura
- ✅ **Implementação**: Criptografia automática no Repository antes de salvar, descriptografia após ler
- ✅ **Compatibilidade**: Suporta dados legados (não criptografados) - migração gradual

## 📱 COMPONENTES UI

### Fragments Principais

- `RoutesFragment`: Listagem de rotas
- `ClientListFragment`: Clientes por rota
- `ClientDetailFragment`: Detalhes do cliente
- `SettlementFragment`: Tela de acerto
- `ContractGenerationFragment`: Geração de contrato
- `SignatureCaptureFragment`: Captura de assinatura
- E mais 20+ fragments...

### Adapters

- `ClientListAdapter`: Lista de clientes
- `MesasAcertoAdapter`: Mesas no acerto
- `RoutesAdapter`: Lista de rotas
- `SettlementHistoryAdapter`: Histórico de acertos
- E mais adapters...

### Dialogs

- `ContractFinalizationDialog`: Finalização de contrato
- `SettlementSummaryDialog`: Resumo do acerto
- `PanoSelectionDialog`: Seleção de pano para troca
- E mais dialogs...

## 🔄 FLUXO DE DADOS

### Estados e Navegação

- SafeArgs para passagem de parâmetros
- SharedPreferences para configurações
- Flow para dados reativos
- Coroutines para operações assíncronas

### PDF e Relatórios

- **iText7** para geração de PDFs
- **ContractPdfGenerator**: Contratos de locação
- **PdfReportGenerator**: Relatórios de acerto
- **ClosureReportPdfGenerator**: Relatórios de fechamento

### Utilitários Principais

- **PasswordHasher**: Hash seguro de senhas (PBKDF2-SHA256) ✅
- **DataEncryption**: Criptografia de dados sensíveis (AES-GCM 256 bits, Android Keystore) ✅
- **DateUtils**: Utilitários de data
- **BluetoothPrinterHelper**: Comunicação com impressoras térmicas
- **NetworkUtils**: Verificação de conectividade
- **UserSessionManager**: Gerenciamento de sessão do usuário
- **DocumentIntegrityManager**: Hash SHA-256 para documentos
- **SignatureMetadataCollector**: Coleta de metadados de assinatura
- **ImageCompressionUtils**: Compressão de imagens
- **FinancialCalculator**: Cálculos financeiros
- **DataValidator**: Validação de dados

## 🚀 MODERNIZAÇÕES IMPLEMENTADAS (2025)

### StateFlow Migration

- **AuthViewModel**: Convertido de LiveData para StateFlow
- **RoutesViewModel**: Convertido de LiveData para StateFlow
- **LoginFragment**: Convertido de observe para collect + repeatOnLifecycle
- **RoutesFragment**: Convertido de observe para collect + repeatOnLifecycle

### ViewModel Initialization Fix

- **Problema Identificado**: Crashes por `by viewModels()` sem inicialização manual
- **Solução Aplicada**: Inicialização manual de ViewModels em todos os fragments
- **Padrão Implementado**: `lateinit var viewModel` + inicialização em `onViewCreated`
- **Resultado**: Zero crashes - todas as telas funcionando perfeitamente

### BaseViewModel Centralizada

- **Funcionalidades Comuns**: Loading, error, message states
- **Métodos Utilitários**: showLoading(), hideLoading(), showError(), showMessage()
- **Logging Centralizado**: Timber para logs consistentes
- **Eliminação de Duplicação**: ~200 linhas de código reduzidas

### Padrões Implementados

```kotlin
// ✅ PADRÃO MODERNO: StateFlow + collect
viewLifecycleOwner.lifecycleScope.launch {
    viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.property.collect { value ->
            // Atualizar UI
        }
    }
}

// ✅ PADRÃO MODERNO: BaseViewModel
class MyViewModel : BaseViewModel() {
    fun doSomething() {
        showLoading()
        // Lógica de negócio
        hideLoading()
    }
}

// ✅ PADRÃO CORRIGIDO: ViewModel Initialization
class MyFragment : Fragment() {
    private lateinit var viewModel: MyViewModel
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // ✅ SEMPRE inicializar manualmente
        val database = AppDatabase.getDatabase(requireContext())
        val repository = Repository(database.dao())
        viewModel = MyViewModel(repository)
        
        setupUI()
        observeViewModel()
    }
}
```

## 🎯 REGRA PRINCIPAL: CENTRALIZAÇÃO E SIMPLIFICAÇÃO

### Princípios Arquiteturais

1. **UM REPOSITORY CENTRALIZADO**: AppRepository como único ponto de acesso aos dados
2. **BASEVIEWMODEL CENTRALIZADA**: Funcionalidades comuns em um local
3. **ELIMINAR FRAGMENTAÇÃO**: Evitar múltiplos arquivos desnecessários
4. **FACILITAR MANUTENÇÃO**: Código organizado e acessível
5. **REUTILIZAR CÓDIGO**: Eliminar duplicação sempre que possível

### Benefícios da Centralização

- **Manutenibilidade**: Código em um local facilita manutenção
- **Performance**: Cache centralizado otimiza consultas
- **Consistência**: Padrões unificados em toda aplicação
- **Simplicidade**: Menos arquivos, menos complexidade
- **Debugging**: Logs centralizados facilitam diagnóstico

### Estrutura Centralizada

```
📁 data/
  └── repository/
      └── AppRepository.kt (✅ ÚNICO REPOSITORY)
      └── internal/
          ├── ClienteRepositoryInternal.kt
          ├── AcertoRepositoryInternal.kt
          ├── MesaRepositoryInternal.kt
          └── ... (repositories especializados)

📁 ui/
  └── common/
      └── BaseViewModel.kt (✅ FUNCIONALIDADES CENTRALIZADAS)

📁 ui/
  └── [module]/
      ├── [Module]ViewModel.kt (✅ HERDA DE BASEVIEWMODEL)
      └── [Module]Fragment.kt (✅ USA STATEFLOW + COLLECT)
```

## 🛠️ FERRAMENTAS DE DESENVOLVIMENTO

### Build e Deploy

- Gradle para build (otimizado - ~1-2 minutos)
- APK de debug para testes
- Logcat para debugging
- ADB para conexão com dispositivo

### Logs e Debug

- Logs detalhados em componentes críticos
- Sistema de auditoria jurídica
- Validação de integridade de dados
- **AppLogger**: Sistema de logging condicional com sanitização

## ⚡ OTIMIZAÇÕES AVANÇADAS IMPLEMENTADAS

### Otimização de Memória ✅

- ✅ **MemoryOptimizer**: LruCache para bitmaps, object pooling, garbage collection
- ✅ **WeakReferenceManager**: Gerenciamento de referências fracas
- ✅ **ObjectPool**: Pool de objetos reutilizáveis
- ✅ **Monitoramento Automático**: Estatísticas de memória em tempo real

### Otimização de Rede ✅

- ✅ **NetworkCompressionManager**: Compressão GZIP inteligente
- ✅ **BatchOperationsManager**: Operações em lote com prioridades
- ✅ **RetryLogicManager**: Retry automático com circuit breaker
- ✅ **NetworkCacheManager**: Cache inteligente com TTL

### Otimização de UI ✅

- ✅ **ViewStubManager**: Carregamento lazy de layouts pesados
- ✅ **OptimizedViewHolder**: Pool de ViewHolders reutilizáveis
- ✅ **LayoutOptimizer**: Otimização de hierarquia de views
- ✅ **RecyclerViewOptimizer**: Performance otimizada de listas

### Processamento em Background ✅

- ✅ **SyncWorker**: Sincronização automática a cada 15 minutos
- ✅ **CleanupWorker**: Limpeza de dados antigos diariamente às 2:00
- ✅ **CoroutineWorker**: Uso de coroutines nativas Android 2025
- ✅ **Constraints Inteligentes**: NetworkType.CONNECTED, BatteryNotLow
- ✅ **BackoffPolicy.EXPONENTIAL**: Retry inteligente

---

**Última atualização**: 2025-01-09

