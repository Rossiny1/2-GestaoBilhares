# ⏪ PROMPT: ROLLBACK ESTRATÉGICO & NOVA ABORDAGEM (V9)

> **Situação:** Tentamos corrigir um bug (quantidade de panos incorreta) e acabamos quebrando uma funcionalidade crítica (cards de panos sumiram).
> **Estratégia:** Em vez de tentar consertar o estado atual quebrado, vamos **reverter a lógica de criação** para o ponto onde os cards apareciam, e então aplicar a correção da quantidade de forma segura.
> **Role:** Engenheiro de Software Sênior (Foco em Git e Lógica de Negócios).

---

## 📉 ETAPA 1: O DIAGNÓSTICO DO ERRO (MENTAL)

Antes de codar, entenda o que aconteceu:

* **Estado Anterior:** O usuário pedia 3 panos -> O sistema criava os panos (apareciam no card), mas a quantidade interna ou contagem estava errada (ex: criava 1 item com qtd=3 ou 2 itens).
* **Estado Atual:** O usuário pede 3 panos -> O sistema não mostra nada (cards invisíveis).
* **Conclusão:** A alteração recente quebrou a reatividade da UI ou o salvamento do objeto.

---

## 🛠️ PLANO DE AÇÃO (EXECUÇÃO)

### 1️⃣ Passo 1: Análise e Reversão (Manual ou via Git)

* **Ação:** Analise o arquivo `StockViewModel.kt` e `StockRepository.kt`.
* **Busca:** Procure as alterações recentes no método `adicionarPanosLote` (ou similar).
* **Objetivo:** Identifique o código que fazia a inserção antes. Se não puder usar `git revert`, **reescreva o método** para a forma simples:
  * Recebe lista -> Insere no DAO -> Notifica sucesso.
  * **Remova** qualquer lógica complexa de validação ou transformação que tenha sido adicionada recentemente e que possa estar silenciando o sucesso.

### 2️⃣ Passo 2: A Correção da Quantidade (O jeito certo)

Agora que a lógica básica de inserção foi restaurada, implemente a correção da quantidade **diretamente no loop de criação**, antes de chamar o repositório.

**Lógica Correta (No ViewModel ou UseCase):**

```kotlin
fun criarPanos(tipo: String, quantidade: Int) {
    val listaParaSalvar = mutableListOf<PanoEntity>()

    // O LOOP SIMPLES E SEGURO
    for (i in 1..quantidade) {
        val novoPano = PanoEntity(
            tipo = tipo,
            status = "DISPONIVEL", // Hardcoded para garantir visibilidade
            numero = calcularProximoNumero(), // Garanta que isso não gere IDs duplicados
            dataCriacao = System.currentTimeMillis()
        )
        listaParaSalvar.add(novoPano)
    }

    // Chama o repositório UMA VEZ com a lista pronta
    repository.inserirLote(listaParaSalvar)
    // Atualiza LiveData/StateFlow para a UI reagir
    _refreshTrigger.value = true 
}
```

### 3️⃣ Passo 3: Validação Visual

* **Verificação:** Garanta que após o `insert`, o método não faça um "early return" ou cancele o escopo (causa do erro "Job Canceled" antigo). Use `viewModelScope.launch` padrão.

---

## 🚫 O QUE NÃO FAZER

* Não tente "consertar" o código atual se ele estiver muito complexo/sujo. Reescreva a função de adicionar lote para ser **simples e burra** (Recebe Lista -> Salva -> Fim).
* Não adicione validações de Sync agora. O foco é aparecer na tela.

**🚀 COMANDO:** Execute o Passo 1 e 2. Entregue o código corrigido e simples.
