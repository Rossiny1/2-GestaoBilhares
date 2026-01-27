# 🩺 PROMPT CIRÚRGICO: CORREÇÃO "DIALOG FECHADO PREMATURAMENTE" (V14)

> **Diagnóstico Baseado em Logs Reais:**
> O log mostra claramente o sucesso da criação lógica (`Total de panos criados: 3`), mas logo em seguida:
> `=== CANCELOU: Operação cancelada pelo usuário ===`
> `Provável causa: Dialog fechado antes da conclusão`

> **Causa Raiz Identificada:**
> O `StockViewModel` ou o `AddPanosLoteDialog` está disparando um cancelamento (JobCancellation) porque o Dialog está sendo fechado (`dismiss()`) **ANTES** do `viewModelScope.launch` terminar a inserção no banco. Quando o LifecycleOwner (Dialog) morre, o Job associado a ele é cancelado.

---

## 🛠️ A CORREÇÃO OBRIGATÓRIA (3 PASSOS)

Você deve corrigir a ordem de execução no `AddPanosLoteDialog.kt` e no `StockViewModel.kt`.

### 1️⃣ Passo 1: ViewModel - Use `viewModelScope` desvinculado da UI
*   No `StockViewModel.adicionarPanosLote`:
*   **Mude** o escopo de lançamento para `viewModelScope.launch` (que sobrevive ao Dialog) mas **GARANTA** que ele não dependa do ciclo de vida da View.
*   **Melhor ainda:** Use `NonCancellable` dentro do launch para operações de escrita críticas, ou apenas garanta que o Dialog só feche **APÓS** receber o evento de sucesso.

### 2️⃣ Passo 2: Dialog - Fechar só no Sucesso
*   Vá em `AddPanosLoteDialog.kt`.
*   Procure onde o botão "Criar" chama `dismiss()`. **REMOVA O DISMISS IMEDIATO.**
*   **Lógica Correta:**
    1.  Botão Clicado -> Chama ViewModel -> Mostra Loading (ProgressBar).
    2.  ViewModel processa -> Emite Evento Sucesso (LiveData/SharedFlow).
    3.  Dialog observa Evento Sucesso -> Chama `dismiss()` -> Mostra Toast.

### 3️⃣ Passo 3: Prevenir "Duplo Clique"
*   Desabilite o botão "Criar" assim que for clicado para evitar que o usuário tente fechar ou reenviar enquanto processa.

---

## 📝 EXEMPLO DE CÓDIGO (COPIE A LÓGICA)

**No Dialog (Errado - Atual provável):**
```kotlin
btnSalvar.setOnClickListener {
    viewModel.salvar(...)
    dismiss() // <--- O ASSASSINO SILENCIOSO
}
```

**No Dialog (Correto):**
```kotlin
btnSalvar.setOnClickListener {
    btnSalvar.isEnabled = false // Trava
    progressBar.isVisible = true // Feedback
    viewModel.salvar(...)
    // NÃO CHAMA DISMISS AQUI
}

// No Observer:
viewModel.sucessoSalvar.observe(viewLifecycleOwner) {
    dismiss() // Agora sim pode morrer
}
```

**🚀 COMANDO:** Aplique esta correção de fluxo assíncrono. O problema não é o banco, é o ciclo de vida da UI matando a thread.
