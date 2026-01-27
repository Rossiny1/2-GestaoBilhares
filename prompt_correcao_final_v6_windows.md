# 🚀 PROMPT MESTRE: CORREÇÃO DE REGRESSÕES & ENTREGA RELEASE (V6 - WINDOWS COMPATIBLE)

> **Contexto:** Identificamos 3 regressões críticas na UI e Lógica após as últimas mudanças. Precisamos corrigir isso e garantir uma entrega final estável.
> **Objetivo:** Corrigir os erros, validar com testes e entregar o APK Release final.
> **Role:** Engenheiro Android Sênior & DevOps.

---

## 🛡️ PROTOCOLO DE SEGURANÇA ATUALIZADO (WINDOWS POWERSHELL)

Atenção: O ambiente de execução foi atualizado. Você **DEVE** usar os comandos compatíveis com Windows PowerShell listados abaixo. O uso de comandos Linux (./gradlew) pode ser bloqueado.

### ✅ COMANDOS PERMITIDOS (USE APENAS ESTES)
*   **Build Debug:** `.\gradlew.bat assembleDebug`
*   **Build Release:** `.\gradlew.bat assembleRelease`
*   **Testes Unitários:** `.\gradlew.bat testDebugUnitTest`
*   **Limpeza:** `.\gradlew.bat clean`
*   **Comandos Específicos:** `.\gradlew.bat :app:testDebugUnitTest` (exemplo)

### ❌ COMANDOS PROIBIDOS
*   `./gradlew` (Linux/Mac)
*   `sudo`
*   `chmod`

---

## 📋 TAREFAS DE CORREÇÃO (EXECUÇÃO SEQUENCIAL)

### 1️⃣ Correção: "Job was canceled" (Panos em Lote)
*   **Problema:** Criar panos falha com `JobCancellationException`.
*   **Solução Técnica:**
    *   No ViewModel (`EstoqueViewModel`), use `viewModelScope.launch` com `try-catch` robusto.
    *   **Crucial:** Use `SupervisorJob()` ou `supervisorScope`.
*   **Validação:** `.\gradlew.bat :app:testDebugUnitTest --tests "*EstoqueViewModelTest*"`

### 2️⃣ Correção: Filtro de Ciclos (Estado Vazio)
*   **Problema:** UI não limpa quando o ano selecionado não tem dados.
*   **Solução Técnica:**
    *   Garanta que `StateFlow` emita `Success(emptyList())`.
*   **Validação:** `.\gradlew.bat :app:testDebugUnitTest --tests "*CicloAcertoViewModelTest*"`

### 3️⃣ Correção: UI Detalhes Cliente (Layout Quebrado)
*   **Problema:** Textos cortados em "Última Visita" e "Débito".
*   **Solução Técnica:**
    *   Ajuste XML para `wrap_content` e constraints flexíveis.
*   **Validação:** Compilação do layout (`.\gradlew.bat :app:compileDebugKotlin`).

---

## 🏁 ENTREGA FINAL (DEPLOYMENT)

Após finalizar as 3 correções com testes passando:

1.  **Sanity Check:** Execute todos os testes:
    *   `.\gradlew.bat testDebugUnitTest`
2.  **Build Release:** Gere o artefato final assinado:
    *   `.\gradlew.bat assembleRelease`
3.  **Relatório Final:**
    *   Liste os arquivos alterados.
    *   Confirme: "Build Release: SUCESSO".
    *   Confirme: "Testes: 100% PASSANDO".

**🚀 COMANDO:** Inicie a Tarefa 1 usando os comandos Windows (`.\gradlew.bat`).
