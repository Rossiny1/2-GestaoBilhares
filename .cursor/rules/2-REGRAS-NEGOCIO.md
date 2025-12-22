# 2️⃣ REGRAS DE NEGÓCIO

> **Propósito**: Definição da lógica operacional e financeira da plataforma.  
> **Última Atualização**: Janeiro 2025  
> **Versão**: 2.0 (Atualizada)

---

## 🏛️ PRINCÍPIOS FUNDAMENTAIS

### 1. Offline-First (Prioridade Local)
*   O aplicativo deve ser 100% funcional sem internet.
*   **Room Database** é a "Fonte da Verdade" (Single Source of Truth).
*   Sincronização ocorre via **WorkManager** em background ou manual.

### 2. Integridade de Dados
*   Nenhum dado é excluído permanentemente pelo usuário (apenas Soft Delete).
*   Validação de chaves estrangeiras (FK) obrigatória antes de salvar qualquer entidade.
*   Conflitos resolvidos por timestamp: o dado mais recente vence (Last-Write-Wins).

---

## 📦 ENTIDADES E LÓGICA

### 📊 Gestão de Rotas e Ciclos
*   **Rotas**: Divisão geográfica/administrativa de clientes.
*   **Ciclos de Acerto**: Períodos de tempo (geralmente quinzenais) onde os acertos das mesas são realizados.
*   **Status da Rota**: EM_ANDAMENTO, CONCLUIDA, AGUARDANDO_SYNC.

### 👥 Clientes e Mesas
*   **Clientes**: Vinculados a uma Rota. Ativos ou Inativos.
*   **Mesas**: Vinculadas a um Cliente.
    *   Tipos: SINUCA, BILHAR, MISTO.
    *   Dados de Leitura: Relógio Inicial e Final.

### 💰 Fluxo de Acerto (Settlement)
1.  **Leitura**: Informar Relógio Final da mesa.
2.  **Cálculo**: (Relógio Final - Relógio Inicial) * Valor da Ficha.
3.  **Divisão**: Percentual acordado (ex: 50/50) entre empresa e parceiro.
4.  **Despesas**: Descontadas do subtotal se autorizado.
5.  **Finalização**: Geração de comprovante PDF e registro de débito/crédito.

### 📑 Documentos e Assinatura
*   **Contratos**: Registro formal da parceria.
*   **Assinatura Eletrônica**: Coleta de assinatura em tela (baseada na Lei 14.063/2020).
*   **Relatórios PDF**: Gerados localmente para compartilhamento imediato via WhatsApp.

---

## 🎯 GESTÃO DE METAS
*   **Tipos de Meta**: FATURAMENTO, NOVOS_CLIENTES, VISITAS.
*   **Acompanhamento**: Visualização de progresso (%) com indicadores visuais (✅/❌).
*   **Histórico**: Registro mensal de metas batidas para cálculos de comissão.

## 🏢 MULTI-TENANCY E SEGURANÇA
*   **Estrutura de Dados**: Dados organizados por empresa (`empresas/{empresaId}/entidades/`).
*   **Acesso**: Usuários só podem acessar dados da própria empresa (via `companyId` no token).
*   **Rotas Atribuídas**: Usuários podem ter acesso apenas a rotas específicas (`rotasAtribuidas` no token).
*   **Admin Global**: Usuário `rossinys@gmail.com` tem acesso total.

## 📱 FUNCIONALIDADES PRINCIPAIS
*   **Gestão de Clientes**: Cadastro, edição, histórico de acertos.
*   **Gestão de Mesas**: Tipos (SINUCA, BILHAR, MISTO), leituras de relógio, reformas, vendas.
*   **Gestão de Rotas**: Organização geográfica de clientes, ciclos de acerto.
*   **Acertos Financeiros**: Cálculo automático, divisão percentual, descontos de despesas.
*   **Contratos**: Geração de contratos, aditivos, assinatura eletrônica (Lei 14.063/2020).
*   **Relatórios**: Geração de PDFs localmente para compartilhamento.
*   **Despesas**: Categorização e tipos de despesas operacionais.
*   **Estoque**: Controle de panos, equipamentos, veículos.
*   **Metas**: Acompanhamento de metas de faturamento, novos clientes e visitas.

---

## 🔗 Referências Próximas
*   [3-ARQUITETURA.md](file:///C:/Users/Rossiny/Desktop/2-GestaoBilhares/.cursor/rules/3-ARQUITETURA.md)
*   [1-STATUS-GERAL.md](file:///C:/Users/Rossiny/Desktop/2-GestaoBilhares/.cursor/rules/1-STATUS-GERAL.md)
