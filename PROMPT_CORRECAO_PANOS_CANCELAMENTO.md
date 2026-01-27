# 🩺 Prompt cirúrgico (Android/Kotlin): Corrigir cancelamento ao criar Panos em Lote

## Contexto
Ao criar panos pela tela **Estoque** (Dialog `AddPanosLoteDialog`), os logs mostram:
- `StockViewModel: Validando duplicidade...`
- Em seguida: `CANCELOU / Job was cancelled`.
- Resultado: os panos **não são inseridos** e os cards **não aparecem**.

Isso indica **cancelamento de coroutine por lifecycle/escopo** e/ou **fechamento prematuro do dialog**.

## Objetivo
Corrigir definitivamente o fluxo para que:
1) A operação de inserir panos **não seja cancelada** quando o usuário clica no botão.
2) O dialog **só feche após sucesso real** (quando a inserção terminar).
3) O `StockFragment` receba atualização via Flow/StateFlow e renderize os cards.

## Diagnóstico provável
1) `AddPanosLoteDialog` pode estar criando um `StockViewModel` no escopo do próprio dialog (`by viewModels()`), e ao fechar o dialog o ViewModel é destruído → `viewModelScope` cancela.
2) Mesmo sem auto-dismiss do `AlertDialog`, ainda existe `dismiss()` chamado cedo demais (logo após disparar uma operação assíncrona), causando o mesmo efeito.

## Tarefas obrigatórias (faça **todas**)

### 1) Garantir que o Dialog usa o MESMO ViewModel do StockFragment
No arquivo `AddPanosLoteDialog.kt`:

- Encontre a declaração do ViewModel. Se estiver assim, é suspeito:
```kotlin
private val viewModel: StockViewModel by viewModels()
```

- Substitua por UMA das opções corretas abaixo (escolher conforme arquitetura do app):

**Opção A (preferida se o dialog é aberto pelo StockFragment como parentFragment):**
```kotlin
private val viewModel: StockViewModel by viewModels({ requireParentFragment() })
```

**Opção B (se o StockViewModel é compartilhado em nível de Activity):**
```kotlin
private val viewModel: StockViewModel by activityViewModels()
```

**Opção C (se usa Navigation e o VM está amarrado a um navGraph):**
```kotlin
private val viewModel: StockViewModel by navGraphViewModels(R.id.<SEU_NAV_GRAPH_ID>)
```

> Importante: após aplicar, adicione um log para validar que é a mesma instância do VM no fragment e no dialog:
```kotlin
Log.d("AddPanosLoteDialog", "VM hash=" + System.identityHashCode(viewModel))
```

### 2) Remover qualquer `dismiss()` “cego” logo após chamar o ViewModel
No `AddPanosLoteDialog.kt`, dentro do clique do botão positivo:

- Se existir algo assim, REMOVER:
```kotlin
viewModel.adicionarPanosLote(panos)
dismiss()
```

O dialog **não pode** fechar até confirmar o sucesso.

### 3) Implementar um canal de resultado (sucesso/erro) no StockViewModel
No `StockViewModel.kt`, crie um fluxo de eventos (não-state) para avisar o dialog.

**Implementação recomendada (SharedFlow):**
```kotlin
sealed class AddPanosResult {
    data object Success : AddPanosResult()
    data class Error(val message: String) : AddPanosResult()
}

private val _addPanosResult = MutableSharedFlow<AddPanosResult>(extraBufferCapacity = 1)
val addPanosResult = _addPanosResult.asSharedFlow()
```

No método `adicionarPanosLote(panos)`:
- Emita `Success` **somente depois** de concluir validação + inserções.
- Em caso de erro, emita `Error`.
- Em caso de `CancellationException`, logue stacktrace e emita `Error` (ou ignore, mas logue com stacktrace).

Exemplo:
```kotlin
fun adicionarPanosLote(panos: List<PanoEstoque>) {
    viewModelScope.launch {
        try {
            Log.d("StockViewModel", "Validando duplicidade...")
            // ... validações ...
            // ... inserções ...

            _addPanosResult.tryEmit(AddPanosResult.Success)
        } catch (e: CancellationException) {
            Log.e("StockViewModel", "CANCEL", e) // stacktrace real
            _addPanosResult.tryEmit(AddPanosResult.Error("Operação cancelada"))
        } catch (e: Exception) {
            Log.e("StockViewModel", "ERRO", e)
            _addPanosResult.tryEmit(AddPanosResult.Error(e.message ?: "Erro ao adicionar panos"))
        }
    }
}
```

### 4) No Dialog, coletar o resultado e só então fechar
No `AddPanosLoteDialog.kt`:

- Ao clicar em "Criar", desabilitar botão e mostrar estado “Criando...”.
- NÃO fechar o dialog nesse momento.
- Coletar `viewModel.addPanosResult` usando `viewLifecycleOwner.lifecycleScope` + `repeatOnLifecycle`.

Exemplo:
```kotlin
viewLifecycleOwner.lifecycleScope.launch {
    viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.addPanosResult.collect { result ->
            when (result) {
                is AddPanosResult.Success -> {
                    Toast.makeText(requireContext(), "Panos criados!", Toast.LENGTH_SHORT).show()
                    dismiss()
                }
                is AddPanosResult.Error -> {
                    // Reabilitar botão e mostrar erro
                }
            }
        }
    }
}
```

### 5) Garantir que o dialog NÃO auto-dismiss ao clicar no positivo
Se o dialog é um `MaterialAlertDialogBuilder`, mantenha o padrão:
- `.setPositiveButton("Criar", null)`
- em `setOnShowListener`, pegue o botão e faça `setOnClickListener`.

Mas atenção: o `dismiss()` só deve acontecer no `collect` do sucesso (passo 4).

## Critérios de aceite
- Ao criar 3 panos, o log deve mostrar **Validação OK** e logs de inserção.
- Não pode aparecer `Job was cancelled` ao clicar em criar.
- Cards de panos aparecem imediatamente na tela de estoque.

## O que NÃO fazer
- Não mexer novamente em DAO/Room para tentar “forçar” Flow.
- Não adicionar `lifecycleScope.launch` em volta da chamada do ViewModel só para “esperar”.
- Não chamar `dismiss()` dentro do clique do botão.

## Entrega esperada
- Um patch com as mudanças em:
  - `AddPanosLoteDialog.kt`
  - `StockViewModel.kt`
- Com logs mínimos para validar o VM compartilhado e o fluxo de sucesso.
