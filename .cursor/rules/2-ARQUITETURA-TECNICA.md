# 2. ARQUITETURA TÉCNICA

## 🏗️ PADRÕES DE DESENVOLVIMENTO

### **Linguagem e Framework**

- **Kotlin** como linguagem principal
- **Android Architecture Components** (ViewModel, LiveData, Room)
- **Navigation Component** para navegação
- **Hilt** para injeção de dependência
- **Material Design** para UI

### **Arquitetura MVVM**

- **Model**: Room Database (Entities, DAOs)
- **View**: Fragments com DataBinding
- **ViewModel**: Lógica de negócio e estado
- **Repository**: Abstração da camada de dados

## 🗄️ BANCO DE DADOS

### **Entidades Principais**

- `Cliente`: Dados dos clientes
- `Mesa`: Mesas de bilhar disponíveis
- `Rota`: Rotas de entrega
- `Acerto`: Transações de acerto
- `Despesa`: Despesas por rota/ciclo
- `ContratoLocacao`: Contratos de locação
- `SignaturePoint`: Pontos de assinatura

### **Relacionamentos**

- Cliente → Mesa (1:N)
- Rota → Cliente (1:N)
- Cliente → Acerto (1:N)
- Contrato → Mesa (1:N)

## 🔐 SEGURANÇA E VALIDAÇÃO

### **Assinatura Eletrônica**

- **SignatureView**: Captura de assinatura manual
- **SignatureStatistics**: Validação biométrica
- **DocumentIntegrityManager**: Hash SHA-256
- **LegalLogger**: Logs jurídicos para auditoria
- **SignatureMetadataCollector**: Metadados do dispositivo

### **Validação Jurídica (Lei 14.063/2020)**

- Captura de metadados (timestamp, device ID, IP, pressão, velocidade)
- Geração de hash SHA-256 para integridade
- Logs jurídicos completos para auditoria
- Validação de características biométricas
- Confirmação de presença física do locatário

## 📱 COMPONENTES UI

### **Fragments Principais**

- `RoutesFragment`: Listagem de rotas
- `ClientListFragment`: Clientes por rota
- `ClientDetailFragment`: Detalhes do cliente
- `SettlementFragment`: Tela de acerto
- `ContractGenerationFragment`: Geração de contrato
- `SignatureCaptureFragment`: Captura de assinatura

### **Adapters**

- `ClientListAdapter`: Lista de clientes
- `MesasAcertoAdapter`: Mesas no acerto
- `RoutesAdapter`: Lista de rotas

### **Dialogs**

- `ContractFinalizationDialog`: Finalização de contrato
- `SettlementSummaryDialog`: Resumo do acerto

## 🔄 FLUXO DE DADOS

### **Estados e Navegação**

- SafeArgs para passagem de parâmetros
- SharedPreferences para configurações
- Flow para dados reativos
- Coroutines para operações assíncronas

### **PDF e Relatórios**

- **iText7** para geração de PDFs
- **ContractPdfGenerator**: Contratos de locação
- **PdfReportGenerator**: Relatórios de acerto
- **ClosureReportPdfGenerator**: Relatórios de fechamento

## 🛠️ FERRAMENTAS DE DESENVOLVIMENTO

### **Build e Deploy**

- Gradle para build
- APK de debug para testes
- Logcat para debugging
- ADB para conexão com dispositivo

### **Logs e Debug**

- Logs detalhados em componentes críticos
- Sistema de auditoria jurídica
- Validação de integridade de dados
