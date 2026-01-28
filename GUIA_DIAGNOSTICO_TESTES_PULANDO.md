# 🔧 DIAGNÓSTICO RÁPIDO - TESTES PULANDO

> **Problema:** IA está demorando porque alguns testes estão sendo pulados  
> **Solução:** Diagnóstico científico ANTES de tentar corrigir  
> **Tempo esperado:** 5-10 minutos para identificar causa raiz

---

## 🎯 PROTOCOLO IMEDIATO (5 MINUTOS)

### PASSO 1: IDENTIFICAR QUAIS TESTES ESTÃO PULANDO (30 segundos)

```bash
# Rodar testes com output detalhado
./gradlew :app:testDebugUnitTest --info | grep -E "(SKIPPED|IGNORED|PASSED|FAILED)"

# OU rodar e salvar output
./gradlew :app:testDebugUnitTest > test_output.txt 2>&1

# Ver resumo
grep -E "tests? (skipped|ignored)" test_output.txt
```

**SAÍDA ESPERADA:**
```
X tests completed, Y skipped
```

---

### PASSO 2: IDENTIFICAR A CAUSA (2 minutos - STATIC ANALYSIS)

#### 🔍 Causa 1: Anotação @Ignore

```bash
# Buscar testes ignorados explicitamente
rg "@Ignore" --type kt -C 3

# OU
rg "@Disabled" --type kt -C 3  # JUnit 5
```

**SE ENCONTRAR:**
```kotlin
@Ignore("Motivo aqui")  // ← CAUSA IDENTIFICADA!
@Test
fun `meu teste`() { ... }
```

**SOLUÇÃO:**
```kotlin
// REMOVER a anotação @Ignore
@Test
fun `meu teste`() { ... }
```

---

#### 🔍 Causa 2: assumeTrue/assumeThat falhando

```bash
# Buscar assume* no código de testes
rg "assumeTrue|assumeThat|assumeFalse" --type kt -C 5
```

**SE ENCONTRAR:**
```kotlin
@Test
fun `meu teste`() {
    assumeTrue(Build.VERSION.SDK_INT >= 28)  // ← PODE ESTAR PULANDO!
    // ... resto do teste
}
```

**DIAGNÓSTICO:**
- `assumeTrue(false)` = teste é PULADO (não falha)
- Diferente de `assertTrue(false)` que FALHA

**SOLUÇÃO:**
```kotlin
@Test
fun `meu teste`() {
    // REMOVER assume* se não for necessário
    // OU ajustar condição
    // ... resto do teste
}
```

---

#### 🔍 Causa 3: Testes dependendo de ambiente

```bash
# Buscar condições de ambiente
rg "System.getProperty|System.getenv" app/src/test/ --type kt -C 3
```

**SE ENCONTRAR:**
```kotlin
@Test
fun `meu teste`() {
    val isCI = System.getenv("CI") == "true"
    assumeTrue(isCI)  // ← Pula se NÃO estiver em CI
    // ...
}
```

**SOLUÇÃO:**
```kotlin
@Test
fun `meu teste`() {
    // REMOVER dependência de ambiente
    // OU rodar com: CI=true ./gradlew test
}
```

---

#### 🔍 Causa 4: Configuração JUnit incorreta

```bash
# Ver build.gradle.kts do app
cat app/build.gradle.kts | grep -A 10 "test {"
```

**SE ENCONTRAR:**
```kotlin
tasks.test {
    useJUnitPlatform {
        excludeTags("slow", "integration")  // ← PODE ESTAR EXCLUINDO!
    }
}
```

**SOLUÇÃO:**
```kotlin
tasks.test {
    useJUnitPlatform()
    // Remover excludeTags se não for intencional
}
```

---

### PASSO 3: CONFIRMAR CAUSA RAIZ (1 minuto)

```bash
# Rodar teste específico que está pulando
./gradlew :app:testDebugUnitTest --tests "NomeDoTesteQueEstaPulando" --info

# Ver output detalhado
# Procurar por:
# - "SKIPPED" 
# - "AssumptionViolatedException"
# - "Test ignored"
```

---

## 🚀 SOLUÇÕES RÁPIDAS (POR PRIORIDADE)

### ✅ SOLUÇÃO 1: Remover @Ignore/Disabled (MAIS COMUM)

```bash
# 1. Encontrar todos os @Ignore
rg "@Ignore|@Disabled" app/src/test/ --type kt -l

# 2. Abrir cada arquivo e remover anotação
# (Fazer manualmente ou com sed)

# 3. Rodar testes novamente
./gradlew :app:testDebugUnitTest
```

**TEMPO:** 2-3 minutos  
**BUILDS:** 1

---

### ✅ SOLUÇÃO 2: Remover assume* desnecessários

```bash
# 1. Encontrar assumes
rg "assume(True|That|False)" app/src/test/ --type kt -l

# 2. Analisar se são necessários
# - Se teste funciona sem assume: REMOVER
# - Se precisa de condição: AJUSTAR

# 3. Rodar testes
./gradlew :app:testDebugUnitTest
```

**TEMPO:** 3-5 minutos  
**BUILDS:** 1

---

### ✅ SOLUÇÃO 3: Ajustar configuração Gradle

```kotlin
// app/build.gradle.kts

android {
    // ...

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true  // Mock Android framework

            all {
                // Garantir que todos os testes rodem
                it.testLogging {
                    events("passed", "skipped", "failed")
                    showStandardStreams = true
                }
            }
        }
    }
}
```

**TEMPO:** 2 minutos  
**BUILDS:** 1

---

## 📊 TEMPLATE DE RELATÓRIO PARA IA

```markdown
## 🔍 DIAGNÓSTICO DE TESTES PULANDO

### Evidências Coletadas
\`\`\`bash
# Output do gradle
./gradlew :app:testDebugUnitTest
> 12 tests completed, 4 skipped
\`\`\`

### Testes que estão pulando
1. **ValorDecimalConverterTest.valor_mesa** → SKIPPED
2. **AcertoViewModelTest.calcular_total** → SKIPPED
3. [...]

### Causa Raiz Identificada
\`\`\`kotlin
// Arquivo: app/src/test/.../ValorDecimalConverterTest.kt
@Ignore("TODO: Implementar mock")  // ← AQUI!
@Test
fun \`valor_mesa deve...\`() { ... }
\`\`\`

**DIAGNÓSTICO:** Anotação @Ignore presente

### Solução Aplicada
\`\`\`kotlin
// ANTES
@Ignore("TODO: Implementar mock")
@Test
fun \`valor_mesa deve...\`() { ... }

// DEPOIS
@Test  // Removido @Ignore
fun \`valor_mesa deve...\`() { ... }
\`\`\`

### Validação
\`\`\`bash
./gradlew :app:testDebugUnitTest
> 12 tests completed, 0 skipped ✅
\`\`\`

**TEMPO TOTAL:** 5 minutos  
**BUILDS:** 1
```

---

## 🎓 REGRAS DE OURO

### ✅ FAÇA:
1. **Static Analysis PRIMEIRO** (buscar @Ignore, assume*)
2. **Identificar causa ANTES de corrigir**
3. **Rodar teste específico** para confirmar
4. **Máximo 1 build** para validar

### ❌ NÃO FAÇA:
1. ❌ Tentar "corrigir" sem diagnosticar
2. ❌ Rodar gradle múltiplas vezes sem evidência
3. ❌ Assumir que teste está correto
4. ❌ Ignorar output do gradle

---

## 🚨 SE TESTES CONTINUAM PULANDO

### Diagnóstico Avançado (5 minutos)

```bash
# 1. Ver TODOS os detalhes
./gradlew :app:testDebugUnitTest --debug > debug.log 2>&1

# 2. Buscar "skip" no log
grep -i "skip\|ignore\|assume" debug.log

# 3. Buscar exceções
grep -i "exception" debug.log | grep -i "assume"

# 4. Verificar JUnit version
grep "junit" app/build.gradle.kts
```

**Possíveis causas ocultas:**
- Conflito entre JUnit 4 e 5
- Mock não configurado (Mockk/Mockito)
- Dependência faltando
- Teste mal escrito (sem assertions)

---

## 💡 DICA PARA IA

**Se você é uma IA executando testes:**

1. **PARE** de rodar testes repetidamente
2. **EXECUTE** diagnóstico Static Analysis:
   ```bash
   rg "@Ignore|@Disabled|assumeTrue" app/src/test/ --type kt -C 3
   ```
3. **ANALISE** output do último build:
   ```bash
   grep -E "tests? completed|skipped" build_output.txt
   ```
4. **IDENTIFIQUE** causa raiz (arquivo + linha)
5. **CORRIJA** cirurgicamente
6. **VALIDE** com 1 build apenas

**NÃO rodar gradle mais de 2 vezes sem diagnóstico!**

---

## 📋 CHECKLIST RÁPIDO

Antes de rodar novamente:
- [ ] Busquei @Ignore/@Disabled no código?
- [ ] Busquei assume* no código?
- [ ] Vi output do último gradle test?
- [ ] Identifiquei QUAL teste está pulando?
- [ ] Identifiquei ARQUIVO + LINHA da causa?
- [ ] Tenho evidência (não suposição)?

**Se QUALQUER item é NÃO:** Volte ao diagnóstico!

---

## 🏁 COMANDO FINAL PARA IA

```bash
# 1. DIAGNÓSTICO (NÃO PULAR!)
echo "=== DIAGNÓSTICO TESTES PULANDO ===" > diagnostico.txt
rg "@Ignore|@Disabled" app/src/test/ --type kt -C 2 >> diagnostico.txt
rg "assumeTrue|assumeThat" app/src/test/ --type kt -C 2 >> diagnostico.txt
./gradlew :app:testDebugUnitTest 2>&1 | grep -E "tests? completed|skipped" >> diagnostico.txt
cat diagnostico.txt

# 2. IDENTIFICAR CAUSA
# (Ler diagnostico.txt e encontrar arquivo + linha)

# 3. CORRIGIR (exemplo)
# Remover @Ignore do arquivo identificado

# 4. VALIDAR (APENAS 1 BUILD!)
./gradlew :app:testDebugUnitTest --info | grep -E "(PASSED|FAILED|SKIPPED)"

# CRITÉRIO: 0 skipped
```

---

**TEMPO TOTAL ESPERADO:** 5-10 minutos  
**BUILDS NECESSÁRIOS:** 1-2 máximo  
**METODOLOGIA:** Static Analysis → Diagnóstico → Correção Cirúrgica

---

*Baseado em GUIA_DIAGNOSTICO_SENIOR_FINAL.md - Sempre diagnosticar antes de corrigir!*
