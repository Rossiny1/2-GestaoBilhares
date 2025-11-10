# 3. FLUXO PRINCIPAL DA APLICAÇÃO

> **Documento de fluxos** - Visão geral das telas principais, fluxos de navegação e regras de negócio essenciais.

## 🎯 VISÃO GERAL

**Fluxo Principal:** Login → Rotas → Clientes da Rota → Detalhes do Cliente → Acerto → Impressão

## 📱 TELAS PRINCIPAIS

### 1. TELA "ROTAS"

**Card 1: Filtro de Acertos (Horizontal)**
- Filtro com rolagem horizontal para selecionar ciclo de acerto
- Lógica: Acertos são por rota (ex: "1º Acerto da Rota Zona Sul")
- Numeração anual e por rota (1º ao 12º)
- Estado padrão: Primeiro acerto do ano

**Card 2: Listagem e Consolidados das Rotas (Vertical)**
- Lista filtrada pelo ciclo selecionado
- Exibe: Título, Status, Faturamento, Clientes ativos, Mesas, Débitos, Pendências
- Visibilidade: Representantes veem apenas suas rotas

### 2. TELA "CLIENTES ROTA"

**Card 1: Informações da Rota e Ações**
- Nome da rota, pesquisa por cliente
- Filtros: Débito alto (>R$300), Sem acerto há 4+ meses
- Botões: "Iniciar Rota", "Finalizar Rota", "Novo Cliente"

**Card 2: Listagem de Clientes**
- Destaque visual: Vermelho (débito >R$300), Amarelo (sem acerto 4+ meses), Verde (demais)
- Exibe: Nome, endereço, débito, tempo desde último acerto

### 3. TELA "DETALHES DO CLIENTE"

**Card 1: Informações e Ações Rápidas**
- Número de mesas locadas
- Botões: WhatsApp, Telefone

**Card 2: Ações de Gerenciamento**
- Botões: "Novo Acerto", "Adicionar Mesa", "Retirar Mesa"
- Retirar mesa: Solicita relógio final e valor recebido

**Card 3: Histórico de Acertos**
- Lista de acertos passados
- Campo observação editável por administradores

### 4. TELA "ACERTO"

**Card 2 - Mesas**
- Lógica "Relógio com Defeito": Cálculo baseado na média de fichas dos últimos acertos

**Card 3 - Totais**
- Múltiplos pagamentos: Dialog para discriminar valores por método

**Card 4 - Diversos**
- Campos: Pano trocado, Tipo de acerto, Representante, Observação

### 5. TELA "IMPRESSÃO"
- Aparece após salvar acerto
- Ações: Compartilhar via WhatsApp, Imprimir impressora térmica 58mm

## 🔧 TELAS DE SUPORTE

### CADASTRO DO CLIENTE
- Campos: CPF, Identidade, Endereço, Cliente Desde, Valor da Ficha, Comissão, Número do Contrato
- Regra: "Valor do débito" é apenas leitura, atualizado via acertos

### MESAS DEPÓSITO
- Seleção de mesa disponível para locação
- Dialog: "Fichas Jogadas" ou "Valor Fixo"
- Retorna automaticamente após vinculação

### CADASTRO DE MESA
- Campos obrigatórios: Número, Tamanho, Tipo, Estado de conservação

### GERENCIAMENTO DE MESAS
- Visão geral de todas as mesas da empresa
- Cards por rota e depósito
- Ações: Adicionar/Excluir mesas (Admin)

### METAS DE DESEMPENHO
- Vinculação: Ciclo de acerto + rota + colaborador
- Métricas: % clientes cobrados, Faturamento, Novas mesas, Média por mesa

## 💰 REGRAS DE NEGÓCIO ESSENCIAIS

### Ciclos de Acerto
- **Por Rota**: Cada rota tem seus próprios ciclos
- **Numeração Anual**: 1º ao 12º acerto por ano
- **Geração Automática**: Novo ciclo criado ao clicar "Iniciar Rota"
- **Estado Padrão**: Primeiro acerto do ano selecionado automaticamente
- **Status do Ciclo**: EM_ANDAMENTO, FINALIZADO, CANCELADO, PLANEJADO

### Tipos de Pagamento
- **Fichas Jogadas**: Percentual da receita (padrão 40%)
- **Valor Fixo**: Valor mensal definido
- **Múltiplos Pagamentos**: Dialog para discriminar valores por método
- **Métodos Suportados**: PIX, Cartão, Cheque, Dinheiro

### Cálculos e Validações
- **Relógio com Defeito**: Usa média de fichas dos últimos acertos válidos
- **Débitos**: Soma dos débitos pendentes da rota no período
- **Pendências**: Clientes com débito >R$400 OU sem acerto há 4+ meses

### Filtros de Clientes
- **Débito Alto**: >R$300 (linha vermelha)
- **Sem Acerto**: 4+ meses (linha amarela)
- **Demais Casos**: Linha verde
- **Exibição Padrão**: Clientes com mesas locadas OU débitos pendentes

### Estados das Mesas
- **Depósito**: Mesas disponíveis para locação
- **Locada**: Mesa vinculada a um cliente
- **Inativa**: Mesa retirada ou com problema

### Movimentação de Mesas
- **Vincular**: Mesa sai do depósito → vai para cliente
- **Retirar**: Mesa volta para depósito

## 🧭 NAVEGAÇÃO E FLUXOS

### Fluxo Principal de Navegação
- **Login** → **Rotas** → **Clientes da Rota** → **Detalhes do Cliente** → **Acerto** → **Impressão**
- **Botão de Retorno**: Sempre volta para tela anterior no stack
- **ClientDetailFragment**: Botão de retorno sempre vai para ClientListFragment
- **Controle de Stack**: popUpTo e popUpToInclusive para limpeza do stack

### Navegação por Localização
- **Ícone de Localização**: Clique abre apps de navegação
- **Coordenadas**: Latitude e longitude do cliente
- **Apps Suportados**: Google Maps, Waze, qualquer app de mapas

### Fluxos de Contrato
- **Geração** → **Assinatura** → **Envio WhatsApp** → **Retorno para Cliente**
- **Aditivo**: Mesa adicional → Assinatura → Envio → Retorno
- **Distrato**: Retirada de mesa → Assinatura → Envio → Retorno

## 📋 CONTRATOS DE LOCAÇÃO

### Geração Automática
- **Trigger**: Após vincular mesa ao cliente
- **Numeração**: Formato "2025-0002"
- **Dados**: Preenchimento automático do cliente e equipamentos

### Tipos de Contrato
- **Valor Fixo**: Valor mensal definido
- **Percentual**: % da receita (padrão 40%)
- **Múltiplos Equipamentos**: Suporte a várias mesas

## 📊 RELATÓRIOS E IMPRESSÃO

### Relatórios de Acerto
- **PDF**: Geração automática após salvar
- **WhatsApp**: Compartilhamento via mensagem
- **Impressão**: Impressora térmica 58mm

### Relatórios de Fechamento
- **Por Ciclo**: Dados de um acerto específico
- **Anual**: Consolidação de todos os acertos do ano
- **Gráficos**: Pizza de receitas por rota e despesas por tipo

---

**Última atualização**: 2025-01-09

