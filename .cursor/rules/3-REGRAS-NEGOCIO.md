# 3. REGRAS DE NEGÓCIO

## 🎯 PRINCÍPIOS FUNDAMENTAIS

### **1. Offline-first**
- App deve funcionar 100% offline
- Dados sempre disponíveis localmente (Room Database)
- Sincronização é complementar, não bloqueante

### **2. Centralização e Simplificação**
- **AppRepository como Facade**: Ponto único de acesso para ViewModels
- **Repositories Especializados**: Organizados por domínio de negócio
- **BaseViewModel**: Funcionalidades comuns centralizadas
- **StateFlow Unificado**: Padrão consistente em toda aplicação

### **3. Arquitetura Híbrida Modular**
- AppRepository delega para repositories especializados
- ViewModels usam apenas AppRepository (compatibilidade preservada)
- Trabalho paralelo possível sem conflitos

## 📋 REGRAS DE NEGÓCIO POR DOMÍNIO

### **Clientes**

1. **Cadastro**:
   - Nome obrigatório
   - CPF/CNPJ único
   - Endereço completo obrigatório

2. **Débitos**:
   - Cálculo automático baseado em acertos
   - Destaque visual para débitos > R$ 300
   - Alertas para clientes sem acerto há 4+ meses

3. **Mesas**:
   - Cliente pode ter múltiplas mesas
   - Cada mesa tem relógio inicial/final
   - Valores calculados automaticamente

### **Acertos**

1. **Criação**:
   - Vinculado a um cliente
   - Pode incluir múltiplas mesas
   - Valores calculados automaticamente

2. **Cálculos**:
   - Total recebido
   - Despesas de viagem
   - Comissões (3% motorista, 2% Iltair)
   - Total geral

3. **Métodos de Pagamento**:
   - PIX, Cartão, Cheque, Dinheiro
   - Discriminação por método
   - Validação de valores

### **Rotas**

1. **Gestão**:
   - Rotas ativas/inativas
   - Clientes vinculados por rota
   - Status de rota (iniciada/finalizada)

2. **Ciclos**:
   - Acertos numerados por rota (1º ao 12º)
   - Numeração anual
   - Estado padrão: Primeiro acerto do ano

3. **Filtros**:
   - Por ciclo de acerto
   - Por status de rota
   - Por representante (se aplicável)

### **Despesas**

1. **Categorias**:
   - Categorias pré-definidas
   - Tipos por categoria
   - Validação de valores

2. **Associação**:
   - Despesas por rota
   - Despesas por ciclo
   - Despesas globais

3. **Cálculos**:
   - Total por categoria
   - Total por rota/ciclo
   - Total geral

### **Colaboradores**

1. **Aprovação**:
   - Colaboradores pendentes de aprovação
   - Aprovação por administrador
   - Níveis de acesso (ADMIN, USER)

2. **Metas**:
   - Metas por colaborador
   - Acompanhamento de desempenho
   - Relatórios

### **Contratos**

1. **Geração**:
   - Contratos de locação
   - Aditivos contratuais
   - Validação jurídica

2. **Assinaturas**:
   - Assinatura do locatário
   - Assinatura do representante legal
   - Validação biométrica (Lei 14.063/2020)

3. **Integridade**:
   - Hash SHA-256
   - Logs de auditoria
   - Metadados do dispositivo

## 🔐 VALIDAÇÕES E SEGURANÇA

### **Assinatura Eletrônica (Lei 14.063/2020)**

1. **Metadados Obrigatórios**:
   - Timestamp
   - Device ID
   - IP (se disponível)
   - Pressão do traçado
   - Velocidade do traçado

2. **Validação Biométrica**:
   - Características do traçado
   - Validação de presença física
   - Logs de auditoria

3. **Integridade**:
   - Hash SHA-256
   - Verificação de alterações
   - Logs jurídicos

### **Acesso e Permissões**

1. **Níveis de Acesso**:
   - **ADMIN**: Acesso completo
   - **USER**: Acesso limitado
   - **Super Admin**: `rossinys@gmail.com` (acesso total)

2. **Menu Principal**:
   - Visível para ADMIN aprovado
   - Visível para super admin
   - Lógica centralizada em `UserSessionManager`

## 📊 RELATÓRIOS E IMPRESSÃO

### **Relatórios de Acerto**

1. **Conteúdo**:
   - Dados do cliente
   - Mesas incluídas
   - Valores financeiros
   - Métodos de pagamento
   - Observações

2. **Formato**:
   - PDF gerado com iTextPDF
   - Compartilhamento via WhatsApp
   - Impressão direta

### **Relatórios de Fechamento**

1. **Conteúdo**:
   - Resumo por modalidade (PIX, Cartão, etc.)
   - Total recebido
   - Despesas de viagem
   - Comissões
   - Total geral

2. **Cálculos**:
   - Total geral = Total recebido - Despesas - Comissões
   - Validação de valores
   - Discriminação por método de pagamento

## 🚫 RESTRIÇÕES E VALIDAÇÕES

1. **Dados Obrigatórios**:
   - Nome do cliente
   - CPF/CNPJ
   - Endereço completo
   - Valores numéricos válidos

2. **Validações Financeiras**:
   - Valores não podem ser negativos
   - Total deve bater com métodos de pagamento
   - Comissões calculadas automaticamente

3. **Validações de Estado**:
   - Rota deve estar iniciada para criar acerto
   - Cliente deve existir para vincular mesa
   - Ciclo deve estar ativo para acertos

## 📝 OBSERVAÇÕES IMPORTANTES

1. **Offline-first**: Todas as operações funcionam offline
2. **Sincronização**: Será implementada ao final (não bloqueia uso)
3. **Compatibilidade**: ViewModels não precisam mudar (AppRepository como Facade)
4. **Modularização**: Código organizado por domínio facilita manutenção
