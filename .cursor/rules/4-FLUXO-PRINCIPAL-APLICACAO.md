# 4. FLUXO PRINCIPAL DA APLICAÇÃO

## 🎯 VISÃO GERAL

**Fluxo Principal (híbrido View + Compose)**: 
Login → Rotas → Clientes da Rota → Detalhes do Cliente → Acerto → Impressão

**Observação**: Telas estão migrando gradualmente para Jetpack Compose sem alterar aparência. Navegação segue `nav_graph.xml`. O menu principal é sempre visível para administradores aprovados e para o super admin `rossinys@gmail.com`.

## 📱 TELAS PRINCIPAIS

### **1. TELA "ROTAS" (RoutesScreen - Compose)**

**Card 1: Filtro de Acertos (Horizontal)**
- Filtro com rolagem horizontal para selecionar ciclo de acerto
- Lógica: Acertos são por rota (ex: "1º Acerto da Rota Zona Sul")
- Numeração anual e por rota (1º ao 12º)
- Estado padrão: Primeiro acerto do ano

**Card 2: Listagem e Consolidados das Rotas (Vertical)**
- Lista filtrada pelo ciclo selecionado
- Exibe: Título, Status, Faturamento, Clientes ativos, Mesas, Débitos, Pendências
- Visibilidade: Representantes veem apenas suas rotas
- Menu principal: Disponível para `ADMIN` aprovado e para `rossinys@gmail.com` (super admin)

**Ações**:
- Selecionar rota → Navega para lista de clientes
- Botão sincronizar → Sincroniza dados (pendente implementação)
- Menu principal → Acesso a funcionalidades administrativas

### **2. TELA "CLIENTES ROTA" (ClientListScreen - Compose)**

**Card 1: Informações da Rota e Ações**
- Nome da rota, pesquisa por cliente
- Filtros: Débito alto (>R$300), Sem acerto há 4+ meses
- Botões: "Iniciar Rota", "Finalizar Rota", "Novo Cliente"

**Card 2: Listagem de Clientes**
- Destaque visual: Vermelho (débito >R$300), Amarelo (sem acerto 4+ meses), Verde (demais)
- Exibe: Nome, endereço, débito, tempo desde último acerto
- Ação: Toque no cliente → Navega para detalhes

### **3. TELA "DETALHES DO CLIENTE" (ClientDetailScreen - Compose)**

**Card 1: Informações e Ações Rápidas**
- Número de mesas locadas
- Botões: WhatsApp, Telefone

**Card 2: Ações de Gerenciamento**
- Botões: "Novo Acerto", "Adicionar Mesa", "Retirar Mesa"
- Retirar mesa: Solicita relógio final e valor recebido

**Card 3: Mesas do Cliente**
- Lista de mesas vinculadas
- Exibe: Número, tipo, relógio inicial/final
- Ação: Editar mesa

**Card 4: Histórico de Acertos**
- Lista de acertos anteriores (últimos 3 por padrão)
- Exibe: Data, valor, status
- Ação: Ver detalhes do acerto
- **Botões de Filtro**:
  - "Recentes": Mostra últimos 3 acertos (padrão)
  - "Período Personalizado": Permite selecionar intervalo de datas para buscar acertos mais antigos do Firestore
- **Otimização**: Mantém apenas 3 acertos localmente para economizar espaço; busca históricos maiores sob demanda

### **4. TELA "ACERTO" (SettlementScreen - Compose)**

**Card 1: Informações do Cliente**
- Nome, endereço, débito atual

**Card 2: Mesas do Acerto**
- Lista de mesas incluídas no acerto
- Edição de relógio inicial/final
- Cálculo automático de valores

**Card 3: Valores Financeiros**
- Total recebido
- Despesas de viagem
- Comissões (3% motorista, 2% Iltair)
- Total geral

**Card 4: Métodos de Pagamento**
- PIX, Cartão, Cheque, Dinheiro
- Distribuição de valores
- Validação de total

**Card 5: Observações**
- Campo de texto livre
- Foto opcional

**Ações**:
- Salvar acerto → Salva localmente (offline-first)
- Gerar relatório → PDF + compartilhamento
- Imprimir → Impressão direta

### **5. TELA "RELATÓRIO DE ACERTO" (SettlementDetailScreen - Compose)**

**Conteúdo**:
- Dados do cliente
- Mesas incluídas
- Valores financeiros
- Métodos de pagamento
- Observações

**Ações**:
- Compartilhar via WhatsApp
- Imprimir PDF
- Voltar para detalhes do cliente

## 🔄 FLUXOS SECUNDÁRIOS

### **Gestão de Mesas**
- Cadastro de nova mesa
- Edição de mesa existente
- Vínculo/desvínculo de mesa a cliente
- Histórico de manutenções
- Mesas reformadas

### **Gestão de Ciclos**
- Visualização de ciclos
- Criação de novo ciclo
- Fechamento de ciclo
- Relatórios de ciclo

### **Gestão de Despesas**
- Cadastro de despesa
- Categorias e tipos
- Histórico de despesas
- Relatórios

### **Gestão de Colaboradores**
- Cadastro de colaborador
- Aprovação de colaborador
- Metas por colaborador
- Relatórios

### **Gestão de Contratos**
- Geração de contrato
- Assinatura eletrônica
- Aditivos contratuais
- Validação jurídica

## 🧭 NAVEGAÇÃO

### **Navigation Component**
- `nav_graph.xml` como fonte de verdade
- Navegação type-safe
- Deep links (futuro)

### **Padrão de Navegação**
```kotlin
// Compose
navController.navigate("client_detail/$clientId")

// Fragment
findNavController().navigate(R.id.clientDetailFragment, bundle)
```

## 📊 ESTADOS E FLUXOS DE DADOS

### **StateFlow Pattern**
```kotlin
// ViewModel
private val _clientes = MutableStateFlow<List<Cliente>>(emptyList())
val clientes: StateFlow<List<Cliente>> = _clientes.asStateFlow()

// Fragment/Screen
viewLifecycleOwner.lifecycleScope.launch {
    viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.clientes.collect { clientes ->
            // Atualizar UI
        }
    }
}
```

### **Offline-first**
- Dados sempre disponíveis localmente
- Operações funcionam offline
- Sincronização em background (quando implementada)

## 🎨 UI/UX

### **Material Design 3**
- Tema configurado
- Componentes modernos
- Cores e tipografia padronizadas

### **Feedback Visual**
- Loading states
- Error states
- Empty states
- Success states

### **Acessibilidade**
- Content descriptions
- Navegação por teclado
- Suporte a leitores de tela

