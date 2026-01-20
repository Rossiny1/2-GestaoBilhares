# 🔧 PROMPT MESTRE: CORREÇÃO SYNC, IMPORTAÇÃO E BUILD FINAL

> **Contexto:** O app está funcional em Release, mas apresenta **travamento na UI de sincronização**, **erro de encoding na importação de dados** e **inconsistência na contagem de clientes**.
> **Objetivo Final:** Corrigir todos os bugs, garantir Build Debug/Release com sucesso e 100% dos testes passando.
> **Role:** Engenheiro Android Sênior & Especialista em Dados/Node.js.

---

## 🚨 DIAGNÓSTICO E RESOLUÇÃO (3 FASES)

Você deve executar as correções na ordem abaixo. Não avance se a etapa anterior falhar.

### 🛑 FASE 1: DESTRAVAR UI DE SINCRONIZAÇÃO (Prioridade Crítica)

**O Problema:** O dialog de sincronização chega a 100% mas não fecha (`dismiss()`), bloqueando o usuário (vide screenshot).
**Ação Necessária:**
1.  **Auditar `SyncViewModel` / `SyncFragment`:** Localize a lógica que observa o estado `WorkInfo.State.SUCCEEDED`.
2.  **Garantir `dismiss()`:** O fechamento do dialog deve ocorrer no bloco `finally` ou ser garantido via `StateFlow` na Main Thread.
3.  **Timeout de Segurança:** Implemente um timeout (ex: 3 segundos após 100%) para fechar o dialog forçadamente caso o evento principal falhe.

### 🔣 FASE 2: CORRIGIR SCRIPT DE IMPORTAÇÃO (Node.js)

**O Problema:** Dados importados apresentam caracteres corrompidos (`Ã£`) e contagem errada de clientes (70 vs 112).
**Ação Necessária no `importar_automatico.js`:**
1.  **Encoding:** Forçar leitura do CSV em UTF-8 ou usar biblioteca `iconv-lite` para converter de Windows-1252.
2.  **Tipagem Forte:** Garantir que `rota_id` seja salvo como `Number` (Int/Long) no Firestore, e não String.
3.  **Campo `ativo`:** Forçar `ativo: true` para todos os clientes importados, garantindo que o App (Room) os contabilize corretamente.

### ✅ FASE 3: VALIDAÇÃO FINAL E BUILD (Critério de Sucesso)

Após aplicar as correções acima, você deve entregar o projeto no estado "Verde":

1.  **Testes Unitários:**
    *   Execute `./gradlew testDebugUnitTest`
    *   Todos os testes devem passar (incluindo os do `AuthViewModel` refatorado).
2.  **Build Debug:**
    *   Execute `./gradlew assembleDebug`
    *   Deve compilar sem erros.
3.  **Build Release:**
    *   Execute `./gradlew assembleRelease`
    *   Deve gerar o APK assinado corretamente.

---

## 📝 REGRAS DE EXECUÇÃO

*   **Não quebre o que funciona:** A refatoração do `AuthViewModel` foi um sucesso, não introduza regressões nela.
*   **Commits:** Faça um commit para cada fase concluída (`fix: sync dialog freeze`, `fix: import script encoding`, `chore: release build`).
*   **Relatório:** Ao final, liste exatamente quais arquivos foram alterados e confirme o status dos 3 builds (Test, Debug, Release).

**🚀 COMANDO:** Inicie a execução da **FASE 1** agora. Me reporte o progresso a cada etapa.
