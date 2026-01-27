# 🛠️ PROMPT: Unificar troca de pano (Acerto + Nova Reforma) e gerar card de mesa na Reforma

## 🎯 Objetivo

Hoje o sistema tem dois fluxos para **troca de pano**:

1. **Tela Nova Reforma**  
   - Quando troca o pano por aqui, é criado um **card da mesa** na tela **Reforma de Mesas** com o histórico da troca.  
   - Esse fluxo já funciona corretamente.

2. **Tela Acerto**  
   - Permite trocar o pano durante o acerto.  
   - O pano é atualizado/considerado na lógica de negócio, mas **não aparece** como card na tela Reforma de Mesas.  
   - Ou seja: o histórico de troca feito pelo Acerto não entra no mesmo pipeline da Nova Reforma.

> **Objetivo:** Fazer com que **toda troca de pano**, inclusive a feita na tela **Acerto**, gere um registro de reforma idêntico ao da tela **Nova Reforma**, para que o card da mesa apareça na tela de Reforma com o histórico completo, **sem quebrar o que já está funcionando** (estoque, panos disponíveis, cards atuais, etc.).

---

## 📌 Regras de negócio (comportamento desejado)

1. Toda troca de pano (Nova Reforma **ou** Acerto) deve:
   - Registrar um histórico de **Reforma/Manutenção** da mesa.  
   - Atualizar o pano atualmente vinculado à mesa (pano novo).  
   - Ser exibida como **card de mesa** na tela Reforma de Mesas, com informações suficientes para o usuário entender o que aconteceu.

2. A tela **Nova Reforma** já está correta e é a “fonte da verdade” de como a reforma deve ser salva e exibida.

3. A tela **Acerto** deve **reaproveitar a mesma lógica** de persistência/histórico da Nova Reforma, em vez de ter um fluxo paralelo.

4. A solução **não pode**:
   - Quebrar o fluxo de estoque/panos recém corrigido (cards de panos no estoque + panos disponíveis para troca).  
   - Duplicar registros de reforma.  
   - Alterar as assinaturas públicas já usadas em muitos lugares sem necessidade.

---

## 🧱 Passo 1 – Mapear o fluxo atual de “Nova Reforma”

1. Localize as classes/arquivos responsáveis pelo **fluxo de Nova Reforma**. Exemplos prováveis:
   - `NovaReformaFragment` / `NovaReformaViewModel`
   - Use cases / repositories: `ReformaRepository`, `MesaReformaRepository`, `MesaRepository`, etc.
   - Entidades: `ReformaMesa`, `HistoricoReforma`, `ManutencaoMesa`, ou similar.

2. Identifique claramente:
   - **Qual método** é chamado quando o usuário confirma uma troca de pano na Nova Reforma.
   - **Qual entidade** é persistida para gerar o card da mesa.
   - Quais campos mínimos são gravados (ex.: `mesaId`, `data`, `panoAntigoId`, `panoNovoId`, `motivo`, `observacao`, etc.).
   - Qual DAO/Repository é usado para persistir esse histórico.

3. NÃO altere o comportamento de Nova Reforma neste passo.  
   Apenas **documente** esse fluxo dentro do código (comentários ou um diagrama simples) para reutilizar depois.

---

## 🧩 Passo 2 – Extrair um “use case” único para registrar troca de pano

Crie um **use case / serviço de domínio** reutilizável, que concentre toda a lógica de registrar a troca de pano e atualizar a mesa, por exemplo:

```kotlin
enum class OrigemTrocaPano {
    NOVA_REFORMA,
    ACERTO
}

data class TrocaPanoParams(
    val mesaId: Long,
    val panoAntigoId: Long?,
    val panoNovoId: Long,
    val data: LocalDateTime,
    val origem: OrigemTrocaPano,
    val observacao: String?
)
```

Use case sugerido:

```kotlin
class RegistrarTrocaPanoUseCase(
    private val reformaRepository: ReformaRepository,
    private val mesaRepository: MesaRepository,
    // outros repos necessários
) {

    suspend operator fun invoke(params: TrocaPanoParams) {
        // 1. Persistir histórico de reforma (igual fluxo da Nova Reforma)
        // 2. Atualizar pano atual da mesa
        // 3. Garantir consistência com o que já existe (sem mudar regra atual)
    }
}
```

Regras importantes:

- A implementação interna desse use case deve ser **copiada/refatorada** a partir do que a Nova Reforma já faz hoje (mesmos campos, mesma entidade, mesma DAO que alimenta a tela Reforma).  
- **Não inventar esquema novo de histórico**; reaproveitar a mesma tabela/recurso visual de Reformas.

Depois disso, adapte a Nova Reforma para usar esse `RegistrarTrocaPanoUseCase` em vez de duplicar lógica dentro do ViewModel/Fragment.

---

## 🔁 Passo 3 – Integrar a tela Acerto nesse use case

Na tela **Acerto**:

1. Identifique o ponto exato onde hoje ocorre a “troca de pano” (provavelmente em um `ViewModel` de acertos, ex.: `AcertoViewModel`, `AcertoMesaViewModel`, etc.).
2. Colete os dados necessários:
   - `mesaId` (mesa em que o acerto está sendo lançado).
   - `panoAntigoId` ou alguma identificação do pano anterior (se disponível).
   - `panoNovoId` (pano recém escolhido / instalado).
   - data/hora do momento da troca (usar mesmo padrão da Nova Reforma).
   - observação (se fizer sentido reaproveitar alguma string da tela de acerto, ou deixar nulo).

3. Chame o use case unificado:

```kotlin
viewModelScope.launch {
    registrarTrocaPanoUseCase(
        TrocaPanoParams(
            mesaId = mesaId,
            panoAntigoId = panoAnteriorId,
            panoNovoId = panoNovoId,
            data = agora,
            origem = OrigemTrocaPano.ACERTO,
            observacao = observacao // opcional
        )
    )
}
```

4. Não altere nada na lógica de **estoque de panos** que já foi corrigida:
   - Se hoje o acerto já está atualizando disponibilidade do pano, mantenha essa lógica intacta.  
   - O `RegistrarTrocaPanoUseCase` deve focar em **histórico/reforma/mesa** e reaproveitar apenas o que Nova Reforma já fazia.

---

## 🚫 Passo 4 – Garantir que não haja duplicidade e nem regressão

Ao integrar Acerto:

1. Certifique-se de que a troca de pano via Acerto **não esteja chamando alguma lógica antiga de reforma** além do use case novo; do contrário você pode gravar **dois** históricos para a mesma troca.
2. Valide que:
   - Cards já existentes (de trocas feitas pela Nova Reforma) continuam iguais.  
   - Novos cards criados pelo Acerto aparecem na mesma lista/RecyclerView da tela Reforma de Mesas (mesmo DAO/Flow/consulta).
3. Se a tela Reforma filtra por tipo/origem, decida:
   - Se quer exibir todas as trocas (Nova Reforma + Acerto), inclua `OrigemTrocaPano.ACERTO` no filtro.  
   - Opcional: mostrar uma etiqueta no card (ex.: “Origem: Acerto” vs “Origem: Nova Reforma”).

---

## ✅ Passo 5 – Testes automatizados e manuais (seguindo AI_USAGE)

### 5.1. Testes automatizados

Crie/ajuste testes (unitários ou de instrumentação) para garantir que:

1. `RegistrarTrocaPanoUseCase`:
   - Persiste o histórico de reforma com os mesmos dados usados pela Nova Reforma.
   - Atualiza o pano atual da mesa corretamente.
   - Suporta `origem = NOVA_REFORMA` e `origem = ACERTO` sem diferenças indesejadas.

2. Fluxo Nova Reforma:
   - Continua chamando o use case e gerando card como antes.

3. Fluxo Acerto:
   - Ao simular uma troca de pano, o use case é chamado uma vez e o histórico é criado.

### 5.2. Execução de testes e build em Windows (comandos validados)

Use **apenas** comandos Gradle já validados no `AI_USAGE.md` para não bloquear autonomia em Windows:

```bash
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug --build-cache --parallel
.\gradlew.bat installDebug
```

Se precisar limpar antes de um build completo:

```bash
.\gradlew.bat clean assembleDebug --build-cache --parallel
```

---

## 🚀 Bloco de autonomia para a IA (Windows-safe)

Após aplicar as mudanças de código, execute este bloco de comandos em sequência, usando **exatamente** o formato abaixo para manter compatibilidade com o protocolo de autonomia em Windows:

```text
## 🔄 EXECUÇÃO AUTOMÁTICA (IA)

# Comandos Windows-safe (não usar ./gradlew nem comandos Unix)

.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug --build-cache --parallel
.\gradlew.bat installDebug
```

> Regra: Se qualquer comando acima falhar, **não inventar comandos novos**. Em vez disso, aplicar o Gate 4 do `AI_USAGE.md`: parar, analisar erro, listar alternativas e pedir confirmação humana antes de tentar de novo.

---

## ⚠️ Coisas que **não** devem ser feitas

- Não criar uma **segunda tabela** de histórico só para Acerto.  
- Não mexer na lógica que faz o card de panos no **Estoque** aparecer (já está validada).  
- Não introduzir `./gradlew` ou comandos Unix-like que quebrem autonomia no Windows; usar sempre o padrão `.\gradlew.bat` documentado em `AI_USAGE.md`.  
- Não introduzir mais `lifecycleScope.launch` em Dialogs/Fragments se o ViewModel já usa `viewModelScope`.

---

## ✍️ Entrega esperada

- Novo use case (ou serviço de domínio) centralizado, tipo `RegistrarTrocaPanoUseCase`.  
- Ajustes mínimos em:
  - Fluxo da **Nova Reforma** (para usar o use case).  
  - Fluxo da **Tela Acerto** (para usar o mesmo use case, com `origem = ACERTO`).  
- Todos os testes passando via comando Windows-safe:

```bash
.\gradlew.bat testDebugUnitTest
```

- Build e instalação de debug executados com sucesso:

```bash
.\gradlew.bat assembleDebug --build-cache --parallel
.\gradlew.bat installDebug
```

- Nenhum comportamento alterado fora da regra de **troca de pano → histórico de reforma**.
