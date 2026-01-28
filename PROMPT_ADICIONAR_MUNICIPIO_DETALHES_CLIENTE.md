# 🎯 PROMPT: ADICIONAR MUNICÍPIO-ESTADO NA TELA DETALHES DO CLIENTE

## 📋 CONTEXTO DO PROJETO

**App:** Gestão de Bilhares (Android Kotlin)  
**Arquitetura:** MVVM + Hilt + Room + Firebase + Jetpack Compose  
**Documentação obrigatória:** `.cursor/rules/AI_GUIDE_FINAL.md`, `.cursor/rules/PROJECT_CONTEXT_FULL.md`

---

## 🎯 OBJETIVO

**Adicionar exibição de município-estado** abaixo do endereço na **tela Detalhes do Cliente**.

**Requisitos:**
1. ✅ Exibir município no formato: `Francisco Sá-MG` ou `Brumado-BA`
2. ✅ Posicionar abaixo do endereço
3. ✅ Reduzir tamanho da fonte do nome do cliente
4. ✅ **CRÍTICO:** NÃO quebrar dados existentes da tela
5. ✅ Manter funcionalidades atuais (edição, exclusão, etc)

---

## 📂 ARQUIVOS RELEVANTES

### **Tela Compose (UI):**
```
Arquivo provável: ClientDetailsScreen.kt ou ClienteDetailScreen.kt
Localização: ui/src/main/java/com/example/gestaobilhares/ui/screens/cliente/
```

### **ViewModel:**
```
Arquivo: ClientViewModel.kt ou ClienteViewModel.kt
Localização: ui/src/main/java/com/example/gestaobilhares/ui/viewmodel/
```

### **Entity (Modelo de Dados):**
```
Arquivo: Cliente.kt ou ClienteEntity.kt
Localização: data/src/main/java/com/example/gestaobilhares/data/local/entity/
```

---

## 🔍 FASE 1: ANÁLISE ESTÁTICA (15 MIN)

### **Tarefa 1.1: Localizar Tela de Detalhes do Cliente**

**Comando:**
```bash
# PowerShell - Procurar arquivo da tela
rg "ClientDetails|ClienteDetails|Detalhes.*Cliente" --type kt ui/src/main/java/ -l
```

**Validar:**
```markdown
[ ] Arquivo encontrado: ______________________________
[ ] Contém Composable com detalhes do cliente
[ ] Usa ViewModel para dados
```

---

### **Tarefa 1.2: Verificar Estrutura Atual da Entity Cliente**

**Comando:**
```bash
# Procurar definição da entity Cliente
rg "data class Cliente|class ClienteEntity" --type kt data/src/main/java/ -A 20
```

**Documentar campos:**
```markdown
[ ] Entity encontrada: ______________________________
[ ] Campo cidade existe? [ ] SIM: nome ______ [ ] NÃO
[ ] Campo estado existe? [ ] SIM: nome ______ [ ] NÃO
[ ] Campo município existe? [ ] SIM: nome ______ [ ] NÃO
```

---

### **Tarefa 1.3: Analisar Layout Atual da Tela de Detalhes**

**Documentar:**
```markdown
[ ] Composable encontrado: @Composable fun _______________
[ ] Estilo atual do nome: MaterialTheme.typography._______
[ ] Estilo atual do endereço: MaterialTheme.typography._______
[ ] Layout usa Column/LazyColumn/Card?
```

---

## 🛠️ FASE 2: IMPLEMENTAÇÃO (20 MIN)

### **Tarefa 2.1: Verificar se Campos Existem na Entity**

**Se campos NÃO existem:**
```markdown
❌ PARE! Informe ao usuário:

"Os campos 'cidade' e 'estado' NÃO existem na entity Cliente.

Opções:
1. Usar campo existente (ex: 'municipio' ou 'endereco_completo')
2. Adicionar novos campos (requer migration do Room)

Qual campo existente contém município-estado?"
```

---

### **Tarefa 2.2: Implementar Exibição de Município-Estado**

**Código a ADICIONAR:**

```kotlin
Column(
    modifier = Modifier.padding(16.dp)
) {
    // Nome do cliente - FONTE REDUZIDA
    Text(
        text = cliente.nome,
        style = MaterialTheme.typography.headlineSmall, // MUDANÇA: Medium → Small
        fontWeight = FontWeight.Bold,
        maxLines = 2, // ADICIONAR: quebra nome longo
        overflow = TextOverflow.Ellipsis // ADICIONAR: ... se muito longo
    )

    Spacer(modifier = Modifier.height(8.dp))

    // Endereço
    if (!cliente.endereco.isNullOrBlank()) {
        Text(
            text = cliente.endereco,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(4.dp))
    }

    // MUNICÍPIO-ESTADO - NOVO
    val municipioEstado = buildMunicipioEstado(
        cidade = cliente.cidade,
        estado = cliente.estado
    )

    if (municipioEstado.isNotBlank()) {
        Text(
            text = municipioEstado,
            style = MaterialTheme.typography.bodySmall,
            color = Color.DarkGray,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
    }

    // Resto dos campos...
}
```

---

**Função auxiliar para formatar município-estado:**

```kotlin
// Adicionar no mesmo arquivo, fora do Composable principal
private fun buildMunicipioEstado(cidade: String?, estado: String?): String {
    return when {
        !cidade.isNullOrBlank() && !estado.isNullOrBlank() -> {
            "$cidade-$estado" // Ex: "Francisco Sá-MG"
        }
        !cidade.isNullOrBlank() -> cidade // Só cidade
        else -> "" // Nenhum dos dois
    }
}
```

---

## 🧪 FASE 3: VALIDAÇÃO (15 MIN)

### **Tarefa 3.1: Build e Instalação**

```bash
# PowerShell
cd C:\Users\Rossiny\Desktop\2-GestaoBilhares

# Build incremental
.\gradlew :app:assembleDebug

# Instalar
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

### **Tarefa 3.2: Teste Manual no App**

**Cenário 1: Cliente COM cidade e estado**

```markdown
1. [ ] Abrir app Android
2. [ ] Navegar para detalhes de cliente com cidade/estado
3. [ ] Verificar:
    ✅ Nome aparece (fonte menor)
    ✅ Endereço aparece
    ✅ Município-estado aparece no formato "Cidade-UF"
    ✅ Outros campos funcionam
```

**Cenário 2: Cliente SEM cidade/estado**

```markdown
4. [ ] Selecionar cliente sem cidade/estado
5. [ ] Verificar:
    ✅ Município-estado NÃO aparece (sem espaço vazio)
    ✅ Layout normal
```

**Cenário 3: Cliente com nome longo**

```markdown
6. [ ] Selecionar cliente com nome muito longo
7. [ ] Verificar:
    ✅ Nome quebra em 2 linhas
    ✅ Aparece "..." se ultrapassar 2 linhas
```

---

### **Tarefa 3.3: Validar Funcionalidades Existentes**

```markdown
[ ] Editar cliente: ✅ Funciona
[ ] Excluir cliente: ✅ Funciona
[ ] Voltar para lista: ✅ Funciona
[ ] Sincronização: ✅ Sem erros
```

---

## 📊 FASE 4: RELATÓRIO (5 MIN)

```markdown
## 📋 RELATÓRIO DE IMPLEMENTAÇÃO

### ✅ Mudanças Realizadas:

**Arquivo modificado:** ______________________________

**1. Redução da fonte do nome:**
   - ANTES: MaterialTheme.typography.headlineMedium
   - DEPOIS: MaterialTheme.typography.headlineSmall

**2. Adição de município-estado:**
   - Formato: "Cidade-UF" (ex: "Francisco Sá-MG")
   - Função: buildMunicipioEstado()

### ✅ Testes:

- [ ] Cliente COM cidade/estado: ✅
- [ ] Cliente SEM cidade/estado: ✅
- [ ] Nome longo: ✅
- [ ] Edição: ✅
- [ ] Exclusão: ✅

### 🎯 Status:

[ ] 🟢 Implementado e validado 100%
[ ] 🟡 Implementado com ressalvas
[ ] 🔴 Não implementado
```

---

## 🚨 TROUBLESHOOTING

### **Problema 1: Campos não existem**

```markdown
AÇÃO: Perguntar ao usuário qual campo usar
```

### **Problema 2: Build falha**

```markdown
AÇÃO: Verificar sintaxe da função buildMunicipioEstado()
```

### **Problema 3: Município não aparece**

```markdown
DIAGNÓSTICO:
adb logcat -s ClientViewModel:D -d | grep "cidade"
```

---

## ⚙️ PROTOCOLO DE EXECUÇÃO

1. ✅ FASE 1 (Análise) - LER código, NÃO modificar
2. ✅ FASE 2 (Implementação) - Mudanças cirúrgicas
3. ✅ FASE 3 (Validação) - Build + Testes obrigatórios
4. ✅ FASE 4 (Relatório) - Documentar

**Critérios de Parada:**
- ⛔ PARE se campos não existem (aguardar usuário)
- ⛔ PARE se build falhar 2x
- ⛔ PARE se quebrar funcionalidade existente

**Limites:**
- Máximo 1 build (análise completa)
- Máximo 2 builds (com ajustes)
- Zero mudanças em arquivos não relacionados

---

## 🎯 RESULTADO ESPERADO

✅ Tela exibe: Nome (menor), Endereço, Município-Estado, Outros campos
✅ Funcionalidades: Edição, Exclusão, Sincronização funcionam
✅ Casos extremos: Sem cidade, Nome longo tratados
✅ Código limpo: Função auxiliar, Espaçamentos consistentes

---

## 📚 REFERÊNCIAS

**Documentação:**
- `.cursor/rules/AI_GUIDE_FINAL.md`
- `.cursor/rules/PROJECT_CONTEXT_FULL.md`

**Typography (Material Design 3):**
- headlineSmall → ~24sp (nome do cliente)
- bodySmall → ~12sp (município-estado)

**Comandos:**
```bash
rg "ClientDetails" --type kt -l
rg "data class Cliente" --type kt -A 20
.\gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

**FIM DO PROMPT** 🚀

*Tempo estimado: 55 minutos*  
*Estratégia: Static Analysis + Surgical Implementation*
