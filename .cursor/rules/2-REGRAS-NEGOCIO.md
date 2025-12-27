# 2️⃣ REGRAS DE NEGÓCIO

> **Propósito**: Definição da lógica operacional, financeira e multi-tenancy.  
> **Última Atualização**: Dezembro 2025  
> **Versão**: 3.0

---

## 🏛️ PRINCÍPIOS FUNDAMENTAIS
1.  **Offline-First**: O app funciona 100% sem rede. Room é a fonte da verdade.
2.  **Multi-Tenancy**: Isolamento total de dados por `companyId`.
3.  **Integridade**: Nenhum dado crítico (Mesas, Clientes) é excluído, apenas marcado como inativo.
4.  **Sincronização**: Incremental. O dado local mais recente é preservado, mas o servidor governa os timestamps globais.

---

## 📦 FLUXO OPERACIONAL

### 1. Gestão de Rotas e Ciclos
*   **Rotas**: Grupos de clientes atribuídos a colaboradores.
*   **Ciclos**: Períodos (quinzenais/mensais) para fechamento financeiro.

### 2. Clientes e Mesas
*   **Clientes**: Pertencem a uma Rota.
*   **Mesas**: Vinculadas a Clientes. Tipos: SINUCA, BILHAR, MISTO.

### 3. Acerto Financeiro (Settlement)
*   **Cálculo**: (Relógio Final - Relógio Inicial) * Valor da Ficha.
*   **Relógio com Defeito**: Em caso de falha no relógio, o sistema calcula a média de fichas dos últimos 5 acertos para projetar o subtotal.
*   **Divisão**: Comissão automática baseada no contrato (ex: 50%).
*   **Despesas**: Descontadas do montante bruto antes da divisão ou conforme contrato.

### 4. Contratos e Assinaturas
*   **Padrão**: Geração de PDF conforme Lei 14.063/2020.
*   **Assinatura**: Coletada digitalmente e vinculada aos metadados do dispositivo.

---

## 🏢 REGRAS DE MULTI-TENANCY
*   Toda entidade possui um `companyId`.
*   Filtros obrigatórios em todas as queries Firestore e Room para garantir que um usuário nunca veja dados de outra empresa.
*   Filtro de Rotas: Colaboradores só veem clientes das rotas que lhes foram atribuídas.
