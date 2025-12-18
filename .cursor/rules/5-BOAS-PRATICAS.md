# 5️⃣ BOAS PRÁTICAS

> **Propósito**: Padrões de qualidade, codificação e processos de desenvolvimento.  
> **Última Atualização**: 18 Dezembro 2025  
> **Versão**: 1.0 (Consolidada)

---

## 🎯 PRINCÍPIOS ANDROID 2025

1.  **Reatividade**: Uso obrigatório de `StateFlow` e `SharedFlow`. Proibido `LiveData` em código novo.
2.  **Lifecycle-Aware**: Observar flows usando `collectAsStateWithLifecycle` (Compose) ou `repeatOnLifecycle` (Views).
3.  **Hilt DI**: Nenhuma classe deve instanciar suas próprias dependências.
4.  **Type-Safety**: Navigation SafeArgs para transições entre telas.

---

## 📝 PADRÕES DE CÓDIGO (BOM vs RUIM)

### ViewModels
*   ✅ **BOM**: Expor `StateFlow` único representando o estado da UI. Usar `flatMapLatest` para transformações reativas.
*   ❌ **RUIM**: Métodos `loadX()` que atualizam manualmente listas e não reagem a mudanças no banco.

### Repositories
*   ✅ **BOM**: Retornar `Flow<T>` do Room para que a UI atualize sozinha ao mudar o dado.
*   ❌ **RUIM**: Funções `suspend` que retornam listas estáticas (`List<T>`) para dados que mudam.

### Compose
*   ✅ **BOM**: Usar `key` em `LazyColumn` e `remember` para cálculos pesados.
*   ❌ **RUIM**: Realizar lógica de negócio ou queries dentro de Composable functions.

---

## 🧪 ESTRATÉGIA DE TESTES

*   **Pirâmide**: 70% Unitários (ViewModels/Utils), 20% Integração (Repositories/Room), 10% UI (Espresso).
*   **Mocks**: Usar Mockito-Kotlin (`whenever`, `verify`).
*   **Coroutines**: Usar `StandardTestDispatcher` e `advanceUntilIdle()` para testes determinísticos.

---

## 🔒 SEGURANÇA E PERFORMANCE

*   **Sensibilidade**: Usar `EncryptedSharedPreferences` para tokens de API.
*   **Logs**: Proibido `Log.d` em produção. Sempre usar `Timber` com a configuração de árvore correta.
*   **Database**: Adicionar índices em colunas usadas em filtros de busca (`rotaId`, `dataSincronizacao`).
*   **Imagens**: Comprimir imagens usando WebP antes do upload para o Firebase Storage.

---

## 🤝 CONVENÇÕES DE GIT

*   **Commits**: Seguir padrões de **Conventional Commits**:
    *   `feat(scope): ...` (Nova funcionalidade)
    *   `fix(scope): ...` (Correção de bug)
    *   `perf(scope): ...` (Otimização)
    *   `test(scope): ...` (Adição de testes)
*   **Branches**: `feature/nome-da-branch` ou `fix/nome-do-bug`.

---

## 🔗 Referências Próximas
*   [1-STATUS-GERAL.md](file:///C:/Users/Rossiny/Desktop/2-GestaoBilhares/.cursor/rules/1-STATUS-GERAL.md)
*   [4-ROADMAP-PRODUCAO.md](file:///C:/Users/Rossiny/Desktop/2-GestaoBilhares/.cursor/rules/4-ROADMAP-PRODUCAO.md)
