# 🚨 PROMPT: DIAGNÓSTICO E CORREÇÃO - VALORES DECIMAIS MULTIPLICADOS POR 10

## 📋 CONTEXTO DO PROBLEMA

**App:** Gestão de Bilhares (Android Kotlin + Firebase)  
**Problema:** Valores decimais aparecem multiplicados por 10 na tela de acerto  
**Exemplo:** valor_mesa cadastrado como 1,50 aparece como 15,00 na tela de acerto  
**Comissão:** cadastrada como 0,60 aparece como 6,00  

**Observação crítica:** Problema ocorre APENAS com dados importados, não com dados criados no app.

---

## 🎯 HIPÓTESE INICIAL

**Dupla multiplicação por 100:**

1. **Importador:** Converte "1,50" (string BR) → `parseFloat("1.50") * 100` → `150` (centavos)
2. **App Android:** Recebe `150` → Trata como reais → Multiplica `* 100` novamente → `15000` centavos = `150,00` reais
3. **Tela de cadastro:** Pode ter lógica de conversão diferente (divide por 100) - por isso aparece correto
4. **Tela de acerto:** Usa valor bruto ou multiplica novamente - aparece 10x maior

---

## 🔍 FASE 1: ANÁLISE DO IMPORTADOR (10 MIN)

### **Tarefa 1.1: Localizar Script de Importação**

**Caminho provável:**
```
import-data/importar-automatico.js
import-data/importar-dados.js
import-data/processar-clientes.js
```

**Comando:**
```bash
# PowerShell
cd C:\Users\Rossiny\Desktop\2-GestaoBilhares\import-data
rg "valor_mesa|comissao" --type js -C 5
```

**Procurar por:**
- Conversão de string para número: `parseFloat()`, `Number()`, `toFixed()`
- Multiplicação por 100: `* 100`, `.multiply(100)`
- Formatação de moeda: `toLocaleString`, formatação BR

---

### **Tarefa 1.2: Analisar Conversão de Valores**

**Padrões suspeitos:**

```javascript
// ❌ ERRADO: Multiplica por 100 achando que é reais → centavos
const valorMesa = parseFloat(linha.valor_mesa.replace(',', '.')) * 100;

// ❌ ERRADO: String "1,50" vira 1.50, multiplica por 100 = 150
const comissao = parseFloat(linha.comissao.replace(',', '.')) * 100;

// ✅ CORRETO: Firestore deve armazenar em reais (Double)
const valorMesa = parseFloat(linha.valor_mesa.replace(',', '.'));
// Exemplo: "1,50" → 1.50 (reais como Double)
```

**Documentar:**
```markdown
[ ] Arquivo encontrado: ______________________________
[ ] Conversão de valor_mesa (linha __): ______________________________
[ ] Conversão de comissão (linha __): ______________________________
[ ] Multiplica por 100? [ ] SIM [ ] NÃO
[ ] Armazena como: [ ] centavos (Int) [ ] reais (Double)
```

---

### **Tarefa 1.3: Verificar Dados no Firestore**

**Acessar Firebase Console:**
```
https://console.firebase.google.com/project/gestaobilhares/firestore
```

**Verificar documento de cliente importado:**
```
Path: empresas/empresa_001/entidades/clientes/items/{clienteId}

Campos a verificar:
- valor_mesa: ________ (exemplo: 150 ou 1.5?)
- comissao: ________ (exemplo: 60 ou 0.6?)
- tipo do campo: Number (Double ou Int?)
```

**Comparar com cliente criado no app:**
```
Path: empresas/empresa_001/entidades/clientes/items/{clienteAppId}

Campos a verificar:
- valor_mesa: ________ (deveria ser 1.5 para R$ 1,50)
- comissao: ________ (deveria ser 0.6 para R$ 0,60)
```

---

## 🛠️ FASE 2: ANÁLISE DO APP ANDROID (15 MIN)

### **Tarefa 2.1: Localizar Entity Cliente**

**Caminho provável:**
```
data/src/main/java/com/example/gestaobilhares/data/local/entity/Cliente.kt
data/src/main/java/com/example/gestaobilhares/data/entities/Cliente.kt
```

**Comando:**
```bash
rg "data class Cliente|class ClienteEntity" --type kt -A 30
```

**Verificar:**
```kotlin
data class Cliente(
    val id: String,
    val nome: String,
    val valor_mesa: Double?,  // ← Verificar tipo
    val comissao: Double?,    // ← Verificar tipo
    // ...
)
```

**Documentar:**
```markdown
[ ] Entity encontrada: ______________________________
[ ] Tipo valor_mesa: [ ] Double [ ] Int [ ] Long [ ] Float
[ ] Tipo comissao: [ ] Double [ ] Int [ ] Long [ ] Float
```

---

### **Tarefa 2.2: Analisar Tela de Cadastro de Cliente**

**Caminho provável:**
```
ui/src/main/java/com/example/gestaobilhares/ui/clientes/ClienteCadastroScreen.kt
ui/src/main/java/com/example/gestaobilhares/ui/clientes/ClienteFormScreen.kt
```

**Comando:**
```bash
rg "valor_mesa|comissao" --type kt -C 10 | grep -E "toDouble|parseDouble|div|multiply"
```

**Procurar por:**

```kotlin
// Exemplo de SALVAMENTO (pode ter conversão)
val cliente = Cliente(
    valor_mesa = valorMesaInput.toDoubleOrNull() ?: 0.0, // ← Verifica conversão
    comissao = comissaoInput.toDoubleOrNull() ?: 0.0
)

// Exemplo de EXIBIÇÃO (pode ter formatação)
Text(text = "R$ ${cliente.valor_mesa.formatarMoeda()}") // ← Verifica formatação
```

---

### **Tarefa 2.3: Analisar Tela de Acerto**

**Caminho provável:**
```
ui/src/main/java/com/example/gestaobilhares/ui/acerto/AcertoScreen.kt
ui/src/main/java/com/example/gestaobilhares/ui/settlement/SettlementScreen.kt
```

**Comando:**
```bash
rg "valor_mesa|comissao" --type kt -C 10 ui/src/main/java/com/example/gestaobilhares/ui/acerto/
rg "valor_mesa|comissao" --type kt -C 10 ui/src/main/java/com/example/gestaobilhares/ui/settlement/
```

**Procurar por:**

```kotlin
// ❌ ERRADO: Multiplica por 100 achando que está em reais
val valorMesaCentavos = cliente.valor_mesa * 100

// ❌ ERRADO: Multiplica novamente ao exibir
Text(text = "R$ ${(cliente.valor_mesa * 100).formatarMoeda()}")

// ✅ CORRETO: Usa valor direto do Firestore
Text(text = "R$ ${cliente.valor_mesa.formatarMoeda()}")
```

**Documentar:**
```markdown
[ ] Tela de acerto encontrada: ______________________________
[ ] Usa valor_mesa diretamente? [ ] SIM [ ] NÃO
[ ] Multiplica por 100? [ ] SIM (LINHA __) [ ] NÃO
[ ] Formata como moeda? [ ] SIM (FUNÇÃO __) [ ] NÃO
```

---

## 🔬 FASE 3: TESTE E VALIDAÇÃO (10 MIN)

### **Tarefa 3.1: Teste com Logs**

**Adicionar logs temporários no app:**

```kotlin
// Na tela de cadastro (ClienteCadastroScreen.kt)
Log.d("DIAGNOSTICO", "Cadastro - valor_mesa INPUT: $valorMesaInput")
Log.d("DIAGNOSTICO", "Cadastro - valor_mesa SALVANDO: ${cliente.valor_mesa}")

// Na tela de acerto (AcertoScreen.kt)
Log.d("DIAGNOSTICO", "Acerto - valor_mesa FIRESTORE: ${cliente.valor_mesa}")
Log.d("DIAGNOSTICO", "Acerto - valor_mesa EXIBINDO: $valorExibido")
```

**Executar:**
```bash
# Capturar logs
adb logcat -s DIAGNOSTICO:D -c && adb logcat -s DIAGNOSTICO:D

# Teste 1: Criar cliente no app com valor R$ 1,50
# Teste 2: Abrir tela de acerto e verificar valor exibido
```

---

### **Tarefa 3.2: Teste com Cliente Importado**

**Verificar no Firestore:**
```
1. Abrir Firebase Console
2. Navegar até cliente importado
3. Verificar valor exato de valor_mesa e comissao
4. Anotar se é 150 (centavos) ou 1.5 (reais)
```

**Testar no app:**
```bash
1. Abrir app Android
2. Navegar para tela de acerto
3. Selecionar cliente importado
4. Capturar logs:
   adb logcat -s DIAGNOSTICO:D
```

---

## 🛠️ FASE 4: CORREÇÃO (DEPENDE DO DIAGNÓSTICO)

### **Cenário A: Importador Multiplica por 100 (MAIS PROVÁVEL)**

**Problema:** Importador armazena `150` (centavos) mas app espera `1.5` (reais)

**Correção no importador:**

```javascript
// ANTES (import-data/importar-automatico.js)
const valorMesa = parseFloat(linha.valor_mesa.replace(',', '.')) * 100; // ❌

// DEPOIS
const valorMesa = parseFloat(linha.valor_mesa.replace(',', '.')); // ✅
// Exemplo: "1,50" → 1.5 (reais como Double)
```

**Re-importar dados:**
```bash
cd import-data
node importar-automatico.js --reimportar
```

---

### **Cenário B: App Multiplica por 100 na Tela de Acerto**

**Problema:** Tela de acerto multiplica valor que já está correto

**Correção na tela:**

```kotlin
// ANTES (AcertoScreen.kt)
val valorExibido = cliente.valor_mesa * 100 // ❌

// DEPOIS
val valorExibido = cliente.valor_mesa // ✅
```

**Build e teste:**
```bash
.\gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

### **Cenário C: Problema de Formatação**

**Problema:** Função de formatação divide ou multiplica incorretamente

**Verificar função:**
```kotlin
fun Double.formatarMoeda(): String {
    // ❌ ERRADO: Divide por 100 achando que está em centavos
    val valor = this / 100
    return NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(valor)

    // ✅ CORRETO: Formata diretamente
    return NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(this)
}
```

---

## 📊 FASE 5: VALIDAÇÃO FINAL (5 MIN)

### **Checklist de Validação:**

```markdown
[ ] Cliente criado no app:
    - valor_mesa no Firestore: ______ (deveria ser 1.5)
    - Tela de cadastro: R$ 1,50 ✅
    - Tela de acerto: R$ 1,50 ✅

[ ] Cliente importado:
    - valor_mesa no Firestore: ______ (deveria ser 1.5)
    - Tela de cadastro: R$ 1,50 ✅
    - Tela de acerto: R$ 1,50 ✅ (era 15,00 antes)

[ ] Comissão:
    - comissao no Firestore: ______ (deveria ser 0.6)
    - Tela de acerto: R$ 0,60 ✅ (era 6,00 antes)
```

---

## 🚨 TROUBLESHOOTING

### **Problema 1: Não encontro onde está a conversão**

**Ação:**
```bash
# Buscar por multiplicação por 100
rg "\* 100|multiply\(100\)" --type js --type kt

# Buscar por conversão de string para número
rg "parseFloat|toDouble|toDoubleOrNull" --type js --type kt -C 3
```

---

### **Problema 2: Valores no Firestore estão corretos mas app exibe errado**

**Causa:** Problema está no app, não no importador

**Ação:** Focar na Fase 2 (App Android), verificar telas de exibição

---

### **Problema 3: Valores diferentes entre cadastro e acerto**

**Causa:** Telas usam lógicas diferentes de formatação

**Ação:** 
```bash
# Comparar funções de formatação
rg "formatarMoeda|formatCurrency|NumberFormat" --type kt -C 5
```

---

## 🎯 RESULTADO ESPERADO

**Após correção:**

✅ **Importador:** Armazena valores em reais como Double (1.5, 0.6)  
✅ **Firestore:** Contém valores corretos (1.5, 0.6)  
✅ **Tela de cadastro:** Exibe R$ 1,50 (já estava correto)  
✅ **Tela de acerto:** Exibe R$ 1,50 (agora corrigido, era 15,00)  
✅ **Comissão:** Exibe R$ 0,60 (agora corrigido, era 6,00)

---

## 📚 REFERÊNCIAS

**Arquivos críticos:**
- `import-data/importar-automatico.js` - Importador
- `data/.../entity/Cliente.kt` - Entity
- `ui/.../clientes/ClienteCadastroScreen.kt` - Cadastro
- `ui/.../acerto/AcertoScreen.kt` - Tela de acerto

**Comandos úteis:**
```bash
# Buscar conversões
rg "parseFloat|toDouble|* 100" --type js --type kt

# Ver dados no Firestore
firebase firestore:get empresas/empresa_001/entidades/clientes/items/

# Capturar logs do app
adb logcat -s DIAGNOSTICO:D
```

---

**FIM DO PROMPT** 🚀

*Tempo estimado: 40 minutos*  
*Estratégia: Análise paralela (Importador + App) para identificar onde está a duplicação*
