# 🚀 PROMPT MESTRE: REFINAMENTOS FINAIS & NOVAS FEATURES (V3)

> **Contexto:** O projeto está estável, com build release funcional e testes passando. Agora precisamos implementar uma série de refinamentos de UI, lógica de negócios e correções pontuais antes do deploy final.
> **Role:** Atue como **Engenheiro Android Sênior** e **Especialista em UX/Refatoração**.
> **Meta:** Executar 11 tarefas com precisão cirúrgica, sem quebrar o que já funciona.

---

## 🚦 PROTOCOLO DE EXECUÇÃO (LEIA COM ATENÇÃO)

Você executará este roteiro em **11 ETAPAS SEQUENCIAIS**.

### 🛡️ REGRAS DE OURO (ANTI-LOOP & SEGURANÇA)
1.  **Atomicidade:** Para CADA tarefa listada abaixo, você deve:
    *   Implementar a mudança.
    *   Executar `./gradlew assembleDebug` (Build).
    *   Executar `./gradlew testDebugUnitTest` (Testes).
    *   **SÓ AVANCE** se ambos passarem. Se falhar, corrija imediatamente antes de ir para a próxima.
2.  **Zero Regressão:** Não altere a arquitetura MVVM nem a estrutura de pastas. Trabalhe *dentro* dos arquivos existentes sempre que possível.
3.  **Relatório Silencioso:** Não pare para me perguntar nada. Apenas siga o fluxo. Se travar em algo insolúvel após 3 tentativas, pule para a próxima e anote no relatório final.
4.  **Foco no Importador:** A tarefa 3 é em Node.js (`import-data/`), não no Android.

---

## 📋 LISTA DE TAREFAS (EXECUTE NESTA ORDEM)

### 1️⃣ Correção Visual do Progresso de Sincronização
*   **Problema:** O dialog pula de 0% para 100%.
*   **Ação:** Analise o `SyncWorker` e `SyncViewModel`. Garanta que o `setProgressAsync` (WorkManager) ou o `StateFlow` de progresso esteja emitindo valores intermediários (ex: a cada entidade processada ou a cada passo do loop).
*   **Validação:** O código deve ter chamadas de atualização de progresso dentro dos loops de processamento.

### 2️⃣ Histórico Unificado de Panos (Reforma vs Acerto)
*   **Problema:** "Trocar Pano" na tela de Acerto não registra no histórico de "Reforma de Mesas".
*   **Ação:** Ao confirmar a troca de pano no `AcertoViewModel` (ou Repository correspondente), insira também um registro na entidade/tabela de `Reforma` ou `HistoricoMesa`.
*   **Regra:** O registro deve ser idêntico ao gerado pela tela "Nova Reforma".
*   **Validação:** Teste unitário verificando se uma troca de pano invoca o método de inserção de histórico.

### 3️⃣ Importador: Capitalização de Nomes (Node.js)
*   **Problema:** Nomes importados vêm despadronizados.
*   **Ação:** No script `import-data/importar_automatico.js`, crie uma função `formatarNome(nome)`:
    *   Primeira letra de cada palavra em Maiúscula.
    *   Conectivos (da, de, do, dos, das, e) devem ficar em minúscula.
    *   Ex: "JOAO DA SILVA" -> "João da Silva".
*   **Validação:** Execute o script localmente (se possível) ou valide a regex/lógica.

### 4️⃣ UI: Detalhes do Cliente (Texto Cortado)
*   **Problema:** Campos "Último Acerto" e "Débito Atual" cortam o texto.
*   **Ação:** No XML do layout de detalhes do cliente (`fragment_cliente_detalhes.xml` ou similar):
    *   Ajuste `android:layout_width` para `wrap_content` ou use `app:layout_constrainedWidth="true"`.
    *   Verifique margens e constraints. Garanta que o valor (R$) não empurre o label para fora ou vice-versa.

### 5️⃣ Lógica de Estoque: Criação de Panos
*   **Problema:** Digitar "3" na quantidade cria 1 pano com quantidade 3 (ou comportamento similar errado).
*   **Ação:**
    *   No ViewModel/UseCase de criação de estoque: Se quantidade = 3, faça um loop criando 3 entidades distintas (cada uma com seu ID/Número único).
    *   **Validação:** Adicione verificação `if (panoRepository.exists(numero))` antes de salvar para evitar duplicidade. Retorne erro se já existir.

### 6️⃣ UI: Ícone de Localização
*   **Problema:** Ícone pequeno e sem feedback visual de status.
*   **Ação:**
    *   Aumente o tamanho do ícone (ex: de 24dp para 32dp ou ajuste escala).
    *   Lógica condicional (BindingAdapter ou no `onBind`):
        *   Se `latitude != null && longitude != null`: `tint = Green`
        *   Senão: `tint = White` (ou cor padrão).

### 7️⃣ UX: Sincronização Offline
*   **Problema:** Botão sync não avisa se estiver offline.
*   **Ação:** No `OnClickListener` do botão de sync:
    *   Verifique `NetworkUtils.isNetworkAvailable()`.
    *   Se `false`: Exiba `Toast` "Conecte-se à internet para sincronizar" e NÃO inicie o worker.

### 8️⃣ Filtro de Histórico de Ciclos
*   **Problema:** Botão de filtro existe mas não funciona. Padrão deve ser 12 meses.
*   **Ação:**
    *   No `CicloAcertoRepository`, crie query: `getCyclesAfter(date: Long)`.
    *   No ViewModel, defina o filtro padrão para `Calendar.add(Calendar.YEAR, -1)`.
    *   Conecte o botão de filtro (ano) para alterar essa data base.

### 9️⃣ Contratos: Assinatura Obrigatória
*   **Problema:** Gera PDF sem assinatura.
*   **Ação:** No método de gerar contrato/aditivo:
    *   Verifique se o campo/bitmap de assinatura está preenchido.
    *   Se vazio -> Retorne erro/Exception "Assinatura obrigatória" e não gere o arquivo.

### 🔟 UI: Edição de Equipamentos
*   **Problema:** Falta botão de editar.
*   **Ação:**
    *   No layout do item da lista de equipamentos (`item_equipamento.xml`), adicione um botão/ícone de "Lápis".
    *   No Adapter, configure o callback `onEditClick`.
    *   No Fragment, abra o dialog/tela de cadastro preenchido com os dados do item clicado.

### 1️⃣1️⃣ Lógica de Ciclos: Reset Anual
*   **Problema:** Ciclos devem reiniciar contagem (#1) ao mudar de ano.
*   **Ação:** Analise a lógica de "Iniciar Ciclo" (`CicloAcertoRepository` ou UseCase):
    *   Busque o *último* ciclo da rota.
    *   Lógica:
        ```kotlin
        val ultimoCiclo = repo.getLastCiclo(rotaId)
        val anoAtual = Calendar.getInstance().get(Calendar.YEAR)
        val anoUltimo = ultimoCiclo?.data?.year // (pseudo-código)

        val novoNumero = if (ultimoCiclo == null || anoAtual > anoUltimo) {
            1 // Novo ano ou primeiro ciclo da vida -> Começa do 1
        } else {
            ultimoCiclo.numero + 1 // Mesmo ano -> Incrementa
        }
        ```
    *   **Validação:** Crie um teste unitário simulando mudança de ano (mockando a data atual).

---

## 🏁 ENTREGA FINAL

Após concluir as 11 etapas:
1.  Execute um **Build Release Final**: `./gradlew assembleRelease`.
2.  Gere um arquivo `RELATORIO_MUDANCAS_V3.md` contendo:
    *   Checklist das 11 tarefas (Concluído/Pendente).
    *   Lista de arquivos modificados.
    *   Resultado dos testes finais.

**🚀 COMANDO DE INÍCIO:**
Pode começar pela Tarefa 1. Execute com cautela e qualidade de Sênior. Boa sorte.
