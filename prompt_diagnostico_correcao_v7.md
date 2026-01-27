# 🔍 PROMPT DE DIAGNÓSTICO E CORREÇÃO: REGRESSÕES PÓS-CORREÇÃO (V7)

> **Contexto:** As últimas correções resolveram o problema de "Job canceled", mas introduziram regressões graves na UI de Panos e na lógica de Troca. Além disso, o filtro de ciclos permanece inoperante.
> **Status:** Crítico - Funcionalidades que funcionavam pararam de funcionar.
> **Role:** Engenheiro Android Sênior (Especialista em RecyclerView e LiveData).

---

## 🚨 PROBLEMAS RELATADOS & DIAGNÓSTICO HIPOTÉTICO

### 1️⃣ Panos Criados não aparecem na lista (Card invisível)
*   **Sintoma:** O usuário cria os panos com sucesso (sem erro), mas a UI não atualiza para mostrar os novos cards.
*   **Causa Provável:**
    *   **Observer Perdido:** O `LiveData/StateFlow` da lista de panos não está sendo observado corretamente pelo Fragment.
    *   **Lista Imutável:** O Adapter pode estar recebendo uma nova lista, mas o `DiffUtil` não detectou mudança (ex: referência de memória idêntica).
    *   **Filtro Inadvertido:** A query do Room pode estar filtrando por status "DISPONIVEL" e os novos panos nasceram com outro status (ex: "CRIADO" ou null).
*   **Ação de Correção:**
    1.  Verifique o `StockFragment` e `StockViewModel`. Garanta que a lista exposta seja atualizada após a inserção.
    2.  Se estiver usando `submitList`, envie uma **nova instância** da lista: `adapter.submitList(ArrayList(novaLista))`.

### 2️⃣ Troca de Panos: Panos indisponíveis
*   **Sintoma:** Ao tentar trocar, a lista de panos disponíveis para escolha está vazia ou incompleta. Antes criava 2 em vez de 3, agora nem aparecem.
*   **Causa Provável:**
    *   **Erro de Status:** A query para "Panos Disponíveis" busca `status = 'DISPONIVEL'`. Se a criação em lote falhou em definir esse status (ficou null ou string vazia), eles nunca aparecerão.
    *   **Regra de Negócio:** Pode haver uma regra que "Reserva" o pano recém-criado, impedindo seu uso imediato.
*   **Ação de Correção:**
    1.  Inspecione o método `adicionarPanosLote` no Repository. **Force** o status inicial para o Enum correto (`StatusPano.DISPONIVEL`).
    2.  Verifique a Query DAO: `SELECT * FROM panos WHERE status = :status`.

### 3️⃣ Filtro de Histórico de Ciclos (Inoperante)
*   **Sintoma:** Selecionar um ano não filtra nada.
*   **Causa Provável:**
    *   **Binding Desconectado:** O Listener do botão de filtro não está chamando o método `setFiltro(ano)` no ViewModel.
    *   **Lógica de Data:** A comparação no banco de dados (SQLite) pode estar falhando se as datas estiverem em Timestamp (Long) e o filtro tentar comparar Strings ou vice-versa.
*   **Ação de Correção:**
    1.  Adicione Logs no método `aplicarFiltro` do ViewModel para ver se ele é chamado.
    2.  Revise a Query `@Query` no DAO. Se usar datas como Long, converta o ano (ex: 2026) para `startMillis` (01/01/2026) e `endMillis` (31/12/2026) e use `WHERE data BETWEEN :start AND :end`.

---

## 🛠️ PLANO DE EXECUÇÃO (SEQUENCIAL)

Você deve investigar e corrigir na seguinte ordem:

### TAREFA A: Validar Criação e Status dos Panos
1.  Abra `StockRepository` e `PanoEntity`. Confirme se o campo `status` tem valor padrão.
2.  No `adicionarPanosLote`, logue o status de cada objeto antes de salvar.
3.  **Correção:** Garanta explicitamente `pano.status = Status.DISPONIVEL` antes do `insertAll`.

### TAREFA B: Consertar Atualização da Lista (Adapter)
1.  No `StockFragment`, verifique o observer.
2.  **Correção:** Force a atualização da UI. Se usar `SharedFlow`, mude para `StateFlow` ou `LiveData` para garantir que o último estado seja retido (replay).

### TAREFA C: Implementar Filtro de Datas por Range (Timestamp)
1.  No `CicloAcertoDao`, altere a busca por ano para busca por intervalo de tempo.
2.  No ViewModel, calcule `calendarStart` e `calendarEnd` com base no ano selecionado.

---

## 🚫 RESTRIÇÕES E COMANDOS

*   **Ambiente:** Windows (Use `.\gradlew.bat`).
*   **Loop:** Se uma correção falhar o teste 2 vezes, **pare** e peça ajuda humana. Não tente adivinhar código complexo.
*   **Relatório:** Gere um arquivo `RELATORIO_DIAGNOSTICO_V7.md` ao final.

**🚀 COMANDO:** Inicie pela TAREFA A (Validar Status dos Panos).
