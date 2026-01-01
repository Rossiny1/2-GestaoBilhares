# 🚀 Solução Rápida para Erro de Build Local

## ❌ Erro
```
Unresolved reference: converterTimestampParaDate
Unresolved reference: entityToMap
```

## ✅ Solução Mais Rápida

### Opção 1: Sincronizar via Git (RECOMENDADO)
```powershell
git pull origin main
```

### Opção 2: Script Simples
```powershell
.\scripts\corrigir-erro-simples.ps1
```

### Opção 3: Script Completo
```powershell
.\scripts\corrigir-erro-build-local-v2.ps1
```

## 🔍 Se os Scripts Derem Erro

Execute manualmente:

1. **Verificar se o import existe:**
```powershell
Select-String -Path "sync\src\main\java\com\example\gestaobilhares\sync\SyncRepository.kt" -Pattern "import.*DateUtils"
```

2. **Se não existir, adicione manualmente após a linha 12:**
```kotlin
import com.example.gestaobilhares.core.utils.DateUtils
```

3. **Verificar se a função entityToMap existe:**
```powershell
Select-String -Path "sync\src\main\java\com\example\gestaobilhares\sync\SyncRepository.kt" -Pattern "private fun.*entityToMap"
```

4. **Se não existir, adicione após a linha 96 (após a definição do gson):**
```kotlin
private fun <T> entityToMap(entity: T): MutableMap<String, Any> {
    val json = gson.toJson(entity)
    @Suppress("UNCHECKED_CAST")
    val map = gson.fromJson(json, Map::class.java) as? Map<String, Any> ?: emptyMap()
    return map.mapKeys { it.key.toString() }.mapValues { entry ->
        val value = entry.value
        when {
            value is Date -> value.time
            value is java.time.LocalDateTime -> value.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            else -> value
        }
    }.toMutableMap()
}
```

## 📋 Verificar Correção

```powershell
.\gradlew.bat compileDebugKotlin --console=plain 2>&1 | Select-String -Pattern "error:|Unresolved" | Select-Object -First 10
```

Se não mostrar erros, está corrigido! ✅
