# 1. FLUXO PRINCIPAL DA APLICAÇÃO

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

### 2.1 CADASTRO DO CLIENTE
- Campos: CPF, Identidade, Endereço, Cliente Desde, Valor da Ficha, Comissão, Número do Contrato
- Regra: "Valor do débito" é apenas leitura, atualizado via acertos

### 2.2 MESAS DEPÓSITO
- Seleção de mesa disponível para locação
- Dialog: "Fichas Jogadas" ou "Valor Fixo"
- Retorna automaticamente após vinculação

### 2.3 CADASTRO DE MESA
- Campos obrigatórios: Número, Tamanho, Tipo, Estado de conservação

### 6. GERENCIAMENTO DE MESAS
- Visão geral de todas as mesas da empresa
- Cards por rota e depósito
- Ações: Adicionar/Excluir mesas (Admin)

### 7. METAS DE DESEMPENHO
- Vinculação: Ciclo de acerto + rota + colaborador
- Métricas: % clientes cobrados, Faturamento, Novas mesas, Média por mesa
