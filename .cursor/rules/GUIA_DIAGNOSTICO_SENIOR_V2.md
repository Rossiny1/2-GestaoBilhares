# 🔬 GUIA DE DIAGNÓSTICO - DESENVOLVEDOR ANDROID SÊNIOR V2.0

> **Metodologia científica para diagnóstico de bugs em projetos Android**  
> **Objetivo:** Sair do loop de tentativa e erro → Diagnóstico preciso → Correção cirúrgica  
> **Última atualização**: 24/01/2026 - **V2.0 com Data Lineage**

---

## 🎯 PRINCÍPIOS FUNDAMENTAIS

### 1. Data Lineage (Rastreabilidade de Dados)
Para qualquer bug onde um dado está errado, null ou sumiu, você deve mapear:
1. **Origem:** Onde o dado nasce? (Ex: API, Input do Usuário, Room)
2. **Transformação:** Por onde ele passa? (Ex: Mappers, UseCases, ViewModels)
3. **Destino:** Onde ele deveria chegar? (Ex: Tela, Banco de Dados)

**Objetivo:** Encontrar o **Ponto de Perda** exato.

### 2. A Regra do "Estado Anterior"
Muitos bugs acontecem porque o estado *antes* da ação já estava inconsistente.
- ❌ Logar apenas o erro: "Erro ao salvar: null"
- ✅ Logar o estado antes: "Tentando salvar objeto: {id=1, nome=null}"

### 3. Fontes de Verdade
1. **Logs do Logcat** (tempo real)
2. **Debugger** (estado exato)
3. **Database Inspector** (estado persistido)
4. **Código-fonte** (intenção)

---

## 🔬 METODOLOGIA DE DIAGNÓSTICO - 7 PASSOS

### Passo 1: Reprodução Mínima
Reduza o cenário ao mínimo necessário para reproduzir o bug.
- Ambiente limpo (sem dados antigos)
- Caminho feliz (1 cliente, 1 mesa, 1 ação)
- Sem concorrência (modo avião se for bug local)

### Passo 2: Instrumentação com Data Lineage
Adicione logs para rastrear o dado em cada etapa da viagem.

```kotlin
// ORIGEM
Log.d("[DIAGNOSTICO]", "📍 1. Origem (UI): input=\${input}")

// TRANSFORMAÇÃO
val dto = mapper.toDto(input)
Log.d("[DIAGNOSTICO]", "🔄 2. Transformação (DTO): dto=\${dto}")

// DESTINO
Log.d("[DIAGNOSTICO]", "🎯 3. Destino (DB): salvando...")
repository.save(dto)
```

### Passo 3: Identificar o Ponto de Perda
Analise os logs sequencialmente:
- Passo 1 OK? ✅
- Passo 2 OK? ❌ (DTO está com campo null)
- **Conclusão:** O bug está no Mapper ou no Input, não no Repository.

### Passo 4: Hipótese Baseada em Evidência
Formule a teoria: "O campo X se perde no Mapper Y porque a condição Z é falsa".

### Passo 5: Teste Isolado
Crie um teste unitário que reproduza *apenas* essa falha no Mapper.

### Passo 6: Correção Cirúrgica
Corrija apenas o ponto identificado. Não refatore o mundo.

### Passo 7: Validação
Rode o fluxo novamente e verifique se o Ponto de Perda foi eliminado.

---

## 🛠️ FERRAMENTAS E TÉCNICAS

### Taxonomia de Bugs (Guia Rápido)

| Tipo de Bug | Ferramenta Principal | O que buscar |
|---|---|---|
| **UI / Renderização** | Layout Inspector | Visibility=GONE, Height=0, Alpha=0 |
| **Dados / Null** | Logs (Data Lineage) | Ponto onde valor vira null |
| **Fluxo / Lógica** | Logs de Decisão | Qual branch do `if` executou? |
| **Persistência** | Database Inspector | O dado chegou no SQLite? |
| **Crash** | Logcat (Stacktrace) | Qual linha lançou a exceção? |
| **Performance** | Profiler | Memory Leaks, Main Thread Block |

### Padrão de Logs Recomendado

Use uma TAG consistente para filtrar facilmente:

```kotlin
private const val TAG = "[DIAGNOSTICO]"

fun processar() {
    Log.d(TAG, "🚀 Iniciando processo...")
    // ...
    if (erro) {
        Log.e(TAG, "❌ Falha no passo X: \${detalhe}")
    } else {
        Log.d(TAG, "✅ Sucesso no passo X")
    }
}
```

Filtrar no terminal:
```bash
adb logcat -s [DIAGNOSTICO]:D -v time
```

---

## 🎓 MENTALIDADE SÊNIOR

**Júnior:**
- "Vou tentar mudar X."
- "Acho que é bug no Room."
- "Vou atualizar o Gradle."

**Sênior:**
- "O dado entra na função A com valor, mas sai da função B como null."
- "A query do Room está correta, mas o parâmetro enviado está vazio."
- "O log prova que a coroutine foi cancelada antes de terminar."

**Mantra:**
> "Não corrija o que você não consegue medir."

---

*Use este guia para evitar loops de tentativa e erro. Diagnóstico preciso economiza horas de trabalho.*
