# 3. REGRAS DE NEGÓCIO

## 💰 SISTEMA DE ACERTOS

### **Ciclos de Acerto**

- **Por Rota**: Cada rota tem seus próprios ciclos
- **Numeração Anual**: 1º ao 12º acerto por ano
- **Geração Automática**: Novo ciclo criado ao clicar "Iniciar Rota"
- **Estado Padrão**: Primeiro acerto do ano selecionado automaticamente

### **Tipos de Pagamento**

- **Fichas Jogadas**: Percentual da receita (padrão 40%)
- **Valor Fixo**: Valor mensal definido
- **Múltiplos Pagamentos**: Dialog para discriminar valores por método

### **Cálculos e Validações**

- **Relógio com Defeito**: Usa média de fichas dos últimos acertos válidos
- **Débitos**: Soma dos débitos pendentes da rota no período
- **Pendências**: Clientes com débito >R$400 OU sem acerto há 4+ meses

## 🎯 FILTROS E DESTAQUES

### **Filtros de Clientes**

- **Débito Alto**: >R$300 (linha vermelha)
- **Sem Acerto**: 4+ meses (linha amarela)
- **Demais Casos**: Linha verde
- **Exibição Padrão**: Clientes com mesas locadas OU débitos pendentes

### **Filtros de Rotas**

- **Por Ciclo**: Filtro horizontal de acertos
- **Por Usuário**: Representantes veem apenas suas rotas
- **Status**: Em Andamento / Finalizada

## 🏢 GESTÃO DE MESAS

### **Estados das Mesas**

- **Depósito**: Mesas disponíveis para locação
- **Locada**: Mesa vinculada a um cliente
- **Inativa**: Mesa retirada ou com problema

### **Movimentação**

- **Vincular**: Mesa sai do depósito → vai para cliente
- **Retirar**: Mesa volta para depósito
- **Cadastro**: Todas as mesas criadas no depósito

### **Tipos de Mesa**

- **Sinuca**: Mesa de sinuca
- **Pembolim**: Mesa de pembolim
- **Jukebox**: Mesa de jukebox
- **Pool/Snooker**: Outros tipos

## 📋 CONTRATOS DE LOCAÇÃO

### **Geração Automática**

- **Trigger**: Após vincular mesa ao cliente
- **Numeração**: Formato "2025-0002"
- **Dados**: Preenchimento automático do cliente e equipamentos

### **Tipos de Contrato**

- **Valor Fixo**: Valor mensal definido
- **Percentual**: % da receita (padrão 40%)
- **Múltiplos Equipamentos**: Suporte a várias mesas

### **Validação Jurídica**

- **Assinatura Eletrônica Simples**: Conforme Lei 14.063/2020
- **Metadados**: Timestamp, device ID, IP, pressão, velocidade
- **Integridade**: Hash SHA-256 do documento e assinatura
- **Auditoria**: Logs jurídicos completos

## 📊 RELATÓRIOS E IMPRESSÃO

### **Relatórios de Acerto**

- **PDF**: Geração automática após salvar
- **WhatsApp**: Compartilhamento via mensagem
- **Impressão**: Impressora térmica 58mm

### **Relatórios de Fechamento**

- **Por Ciclo**: Dados de um acerto específico
- **Anual**: Consolidação de todos os acertos do ano
- **Gráficos**: Pizza de receitas por rota e despesas por tipo

## 🎯 METAS DE DESEMPENHO

### **Estrutura**

- **Vínculo**: Ciclo de acerto + rota + colaborador
- **Métricas**: % clientes cobrados, faturamento, novas mesas, média por mesa
- **Acompanhamento**: Comparação com metas definidas

## 🔐 SEGURANÇA E AUDITORIA

### **Logs Jurídicos**

- **Eventos**: Assinatura, geração de contrato, alterações
- **Metadados**: Device ID, IP, timestamp, pressão, velocidade
- **Integridade**: Hash SHA-256 para verificação
- **Auditoria**: Trilha completa de eventos

### **Validações**

- **Assinatura**: Características biométricas válidas
- **Documento**: Integridade verificada por hash
- **Presença**: Confirmação de presença física do locatário
