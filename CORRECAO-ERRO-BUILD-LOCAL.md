# 🔧 Correção de Erro de Build Local

## ❌ Erro Encontrado

```
Unresolved reference: converterTimestampParaDate
Unresolved reference: entityToMap
```

## 🔍 Causa

O código local está desatualizado em relação à VM. O arquivo `SyncRepository.kt` local tem referências sem o prefixo correto ou está faltando código.

## ✅ Solução

O código na VM já está correto. O problema é que o código local precisa ser sincronizado.

### Opção 1: Sincronizar via Git (Recomendado)

```powershell
# No PowerShell local
git pull origin main
# ou
git fetch
git merge origin/main
```

### Opção 2: Verificar se todas as referências estão corretas

O arquivo `SyncRepository.kt` deve ter:
- ✅ Import: `import com.example.gestaobilhares.core.utils.DateUtils`
- ✅ Função `entityToMap` definida (linha ~96)
- ✅ Todas as chamadas usando `DateUtils.converterTimestampParaDate(...)`

## 📋 Verificação Rápida

Execute no PowerShell local:

```powershell
# Verificar imports
Select-String -Path "sync\src\main\java\com\example\gestaobilhares\sync\SyncRepository.kt" -Pattern "import.*DateUtils"

# Verificar se todas as chamadas têm prefixo DateUtils
Select-String -Path "sync\src\main\java\com\example\gestaobilhares\sync\SyncRepository.kt" -Pattern "converterTimestampParaDate" | Select-String -NotMatch "DateUtils\."

# Verificar se entityToMap está definida
Select-String -Path "sync\src\main\java\com\example\gestaobilhares\sync\SyncRepository.kt" -Pattern "private fun.*entityToMap"
```

## 🚀 Próximos Passos

1. Sincronize o código via Git
2. Execute o build novamente: `.\gradlew.bat compileDebugKotlin`
3. Se ainda houver erros, verifique as linhas específicas mencionadas no erro
