# 🔍 DIAGNÓSTICO VISUAL - PROBLEMAS NOS CARDS

> **Data:** 24/01/2026  
> **Problema:** 3 issues visuais identificadas nos cards de reforma

---

## 📊 **RESULTADO 1: HEADER_MESA**

```bash
rg "HEADER_MESA" --type kt -C 10
```

**Saída:**

```
ui\src\main\java\com\example\gestaobilhares\ui\mesas\MesasReformadasViewModel.kt
141-                            numeroMesa = mesa?.numero?.toIntOrNull() ?: 0,
142-                            descricao = "🏓 Mesa ${mesa?.numero} - ${cardsOrdenados.size} manutenção(ões)",
143-                            data = cardsOrdenados.firstOrNull()?.data ?: 0L,
144:                            origem = "HEADER_MESA",
145-                            observacoes = null
146-                        )
147-
```

**❌ PROBLEMA:** `origem = "HEADER_MESA"` aparece como texto na UI porque o Adapter não trata este caso.

---

## 📊 **RESULTADO 2: ESTRUTURA ReformaCard**

```bash
rg "data class ReformaCard" --type kt -A 20
```

**Saída:**

```
ui\src\main\java\com\example\gestaobilhares\ui\mesas\MesasReformadasViewModel.kt
195-}

196-// Data class para o card
197:data class ReformaCard(
198-    val id: Long,
199-    val mesaId: Long,
200-    val numeroMesa: Int,
201-    val descricao: String,
202-    val data: Long,
203-    val origem: String, // "NOVA_REFORMA", "ACERTO", "ACERTO_LEGACY"
204-    val observacoes: String?
205-)
206-
```

**❌ PROBLEMA:** Campo `responsavel` não existe na data class, então não é exibido no card.

---

## 📊 **RESULTADO 3: ARQUIVOS DO ADAPTER**

```bash
rg "class.*Adapter.*Reforma\|ReformaCard" --type kt -l
```

**Saída:**

```
ui\src\main\java\com\example\gestaobilhares\ui\mesas\MesasReformadasViewModel.kt
ui\src\main\java\com\example\gestaobilhares\ui\mesas\MesasReformadasAdapter.kt
```

**Arquivo principal:** `MesasReformadasAdapter.kt`

---

## 📊 **RESULTADO 4: MÉTODO bind() DO ADAPTER**

```bash
rg "fun bind" --type kt MesasReformadasAdapter.kt -C 15
```

**Saída:**

```kotlin
fun bind(card: ReformaCard) {
    binding.apply {
        // Número da mesa
        tvNumeroMesa.text = "Mesa ${card.numeroMesa}"
        
        // Data
        tvDataReforma.text = dateTimeFormat.format(Date(card.data))
        
        // Tipo da mesa (usando para mostrar origem)
        when (card.origem) {
            "NOVA_REFORMA" -> {
                tvTipoMesa.text = "Reforma Manual"
            }
            "ACERTO" -> {
                tvTipoMesa.text = "Acerto"
            }
            "ACERTO_LEGACY" -> {
                tvTipoMesa.text = "Acerto (Legacy)"
            }
            else -> {
                tvTipoMesa.text = card.origem  // ❌ AQUI MOSTRA "HEADER_MESA"
            }
        }
        
        // Itens reformados (usando para descrição)
        tvItensReformados.text = card.descricao
        
        // Total de reformas (não aplicável para cards individuais)
        tvTotalReformas.visibility = View.GONE
        
        // Observações (se houver)
        if (!card.observacoes.isNullOrBlank()) {
            tvObservacoes.text = "Observações: ${card.observacoes}"
            tvObservacoes.visibility = View.VISIBLE
        } else {
            tvObservacoes.visibility = View.GONE
        }
        
        // Click listener
        root.setOnClickListener {
            onItemClick(card)
        }
    }
}
```

---

## 🔍 **ANÁLISE DOS 3 PROBLEMAS**

### **Problema 1: "HEADER_MESA" aparecendo como texto**

- **Causa:** `else -> { tvTipoMesa.text = card.origem }` mostra literalmente "HEADER_MESA"
- **Solução:** Adicionar tratamento específico para `HEADER_MESA`

### **Problema 2: Responsável mostra "Acerto" em vez de "rossinys"**

- **Causa:** Campo `responsavel` não existe na `ReformaCard`
- **Solução:** Adicionar campo `responsavel` na data class

### **Problema 3: Header aparecendo como card separado**

- **Causa:** Header é tratado como card normal no Adapter
- **Solução:** Criar layout diferente para headers ou ocultar campos desnecessários

---

## 🔧 **SOLUÇÕES PROPOSTAS**

### **1. Adicionar campo responsavel em ReformaCard**

```kotlin
data class ReformaCard(
    val id: Long,
    val mesaId: Long,
    val numeroMesa: Int,
    val descricao: String,
    val data: Long,
    val origem: String,
    val responsavel: String? = null,  // ✅ ADICIONAR
    val observacoes: String?
)
```

### **2. Tratar HEADER_MESA no Adapter**

```kotlin
when (card.origem) {
    "HEADER_MESA" -> {
        // ✅ TRATAR HEADER ESPECIALMENTE
        tvTipoMesa.text = "📋 Agrupamento"
        tvNumeroMesa.text = card.descricao  // "🏓 Mesa X - Y manutenções"
        tvDataReforma.visibility = View.GONE
        tvItensReformados.visibility = View.GONE
        tvObservacoes.visibility = View.GONE
    }
    "NOVA_REFORMA" -> {
        tvTipoMesa.text = "Reforma Manual"
    }
    // ... outros casos
}
```

### **3. Passar responsavel no ViewModel**

```kotlin
val card = ReformaCard(
    // ... outros campos
    responsavel = historico.responsavel,  // ✅ ADICIONAR
    // ...
)
```

---

## 🎯 **PLANO DE CORREÇÃO**

1. **Adicionar campo `responsavel`** em `ReformaCard`
2. **Atualizar criação de cards** para incluir `responsavel`
3. **Tratar `HEADER_MESA`** no `bind()` do Adapter
4. **Testar visualização** dos cards

---

**Status:** 🔍 **Diagnóstico completo, pronto para correção**
