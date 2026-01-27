# 🎓 GUIA DE DIAGNÓSTICO - DESENVOLVEDOR SÊNIOR

> **Para desenvolvedores humanos e IAs**  
> **Metodologia científica adaptada ao desenvolvimento Android**  
> **Última atualização**: 24/01/2026

---

## 🎯 OBJETIVO

Sair do **loop de tentativa-erro** para **diagnóstico preciso → correção cirúrgica**.

---

## 🧠 PRINCÍPIOS FUNDAMENTAIS

### 1. Nunca Adivinhe, Sempre Meça

**Errado:**
- "Provavelmente é um problema de lifecycle"
- "Pode ser concorrência"
- "Vou tentar mudar X pra ver se resolve"

**Correto:**
- "O código na linha 455 não preenche o campo, portanto..."
- "A variável X está null, comprovado pela leitura do construtor"
- "O método antigo tinha groupBy, o atual não tem"

---

### 2. Método Científico Adaptado

```
1. OBSERVAR → Sintoma e contexto
2. CLASSIFICAR → Regressão ou Mistério?
   ├─ REGRESSÃO → Static Analysis (leia código)
   └─ MISTÉRIO → Dynamic Analysis (logs)
3. DIAGNOSTICAR → Identificar causa raiz
4. CORRIGIR → Alteração cirúrgica
5. VALIDAR → Confirmar correção
```

---

### 3. Hierarquia de Confiabilidade

**Do mais para o menos confiável:**

1. **Código-fonte** (static) → O que ESTÁ escrito
2. **Logs do Logcat** (dynamic) → O que EXECUTOU
3. **Database Inspector** → O que FOI SALVO
4. **Memória/suposição** → O que ACHAMOS

**Regra:** Comece sempre pelo nível 1 (código-fonte).

---

## 📊 CLASSIFICAÇÃO DE BUGS

### Bug Tipo A: REGRESSÃO

**Sintoma:** "Funcionava antes", "parou de funcionar"

**Método:** Static Analysis (arqueologia de código)

**Passos:**
1. Perguntar: "Onde o campo era preenchido antes?"
2. Buscar no código: `rg "usuarioId" --type kt -C 5`
3. Comparar: código antigo vs atual
4. Identificar: linha removida/alterada

**Tempo esperado:** 5-10 minutos  
**Builds:** 1 (validação)

---

### Bug Tipo B: MISTÉRIO

**Sintoma:** "Código parece certo mas falha"

**Método:** Dynamic Analysis (logs de execução)

**Passos:**
1. Instrumentar pontos críticos
2. Compilar e executar
3. Analisar logs
4. Identificar onde fluxo quebra

**Tempo esperado:** 15-30 minutos  
**Builds:** 2 (diagnóstico + validação)

---

## 🔍 RECEITAS DE DIAGNÓSTICO

### Caso 1: Campo está null

**Static Analysis:**
```kotlin
// 1. Encontrar onde objeto é criado
rg "HistoricoManutencao(" --type kt -C 3

// 2. Ver construtor
data class HistoricoManutencao(
    val mesaId: Long,
    val usuarioId: Long?  // ← Nullable?
)

// 3. Ver onde é instanciado
HistoricoManutencao(
    mesaId = 1
    // ❌ FALTA: usuarioId
)
```

**Diagnóstico:** Campo não está sendo passado.  
**Solução:** Adicionar `usuarioId = userSession.currentUser.id`

---

### Caso 2: Dados não aparecem na tela

**Static Analysis em camadas:**
```kotlin
// CAMADA 1: DAO - Query correta?
@Query("SELECT * FROM historico WHERE acertoId = :id")
// ✅ Verificar: tem GROUP BY? tem WHERE correto?

// CAMADA 2: ViewModel - Transforma certo?
val items = repository.getHistorico(id)
    .groupBy { it.mesaId }  // ← Está agrupando?

// CAMADA 3: UI - Observa certo?
items.collectAsState()  // ✅ StateFlow correto
```

---

### Caso 3: Regressão (funcionava antes)

**Arqueologia:**
```bash
# 1. Buscar onde campo era usado
rg "usuarioId" --type kt -C 5

# 2. Buscar método de agrupamento
rg "groupBy" --type kt

# 3. Ver histórico Git (se disponível)
git log --all --oneline --grep="historico"
```

**Análise:**
- Código antigo tinha `groupBy { it.mesaId }`
- Código atual retorna lista plana
- **Causa:** Refatoração removeu agrupamento

---

## 🛑 ANTI-PADRÕES

### ❌ Tentativa e Erro
```kotlin
// Tentativa 1
viewModelScope.launch { }  // ❌ Não funcionou

// Tentativa 2
GlobalScope.launch { }  // ❌ Não funcionou

// Tentativa 3
lifecycleScope.launch { }  // ❌ Não funcionou

// ❌ PROBLEMA: 3 tentativas SEM diagnóstico
```

### ✅ Diagnóstico Científico
```kotlin
// 1. Ler código
viewModelScope.launch {
    salvarDados()  // ← Essa função está sendo chamada?
}

// 2. Adicionar log SE necessário
Log.d("DEBUG", "Função INICIADA")
viewModelScope.launch {
    Log.d("DEBUG", "DENTRO da coroutine")
    salvarDados()
}

// 3. Log mostra: "INICIADA" aparece, "DENTRO" não
// 4. Diagnóstico: Coroutine está sendo cancelada
// 5. Causa: ViewModel destruído antes de executar
```

---

## ⏱️ MÉTRICAS DE QUALIDADE

### Antes (Tentativa-Erro)
- ❌ 6 tentativas
- ❌ 10+ builds
- ❌ 2 horas
- ❌ Alta frustração

### Depois (Diagnóstico Científico)
- ✅ Diagnóstico preciso
- ✅ 1-2 builds
- ✅ 15-30 minutos
- ✅ Baixa frustração

---

## 🎓 MENTALIDADE SÊNIOR

### Júnior diz:
- "Vou tentar mudar X"
- "No Stack Overflow alguém disse..."
- "Acho que é..."

### Sênior diz:
- "O código na linha 455 mostra que..."
- "Vou adicionar log no ponto X para confirmar Y"
- "A evidência indica que..."

---

## 📋 CHECKLIST FINAL

Antes de propor solução:

- [ ] Classifiquei o bug (Regressão vs Mistério)?
- [ ] Usei Static Analysis primeiro?
- [ ] Se Regressão: busquei código antigo?
- [ ] Identifiquei causa raiz (arquivo + linha)?
- [ ] Fiz menos de 2 builds?
- [ ] Tenho evidência (código ou log)?

**Se faltou algum:** Volte ao diagnóstico.

---

## 🏆 REGRA DE OURO

> **"Diagnóstico preciso em 10 minutos > Tentativas por 2 horas"**

Sempre diagnostique antes de corrigir. **Sempre.**

---

*Desenvolvido a partir de casos reais - Bug de Cards (Jan/2026)*
