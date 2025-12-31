# 🚀 Otimizações de Build para Windows

## ⏱️ Problema
Build demorando **27 minutos** - muito lento para desenvolvimento.

## ✅ Otimizações Aplicadas

### 1. **Aumento de Memória (Gradle e Kotlin)**
- **Gradle**: `4GB → 6GB` (aumentado para Windows)
- **Kotlin Daemon**: `3GB → 4GB` (aumentado para Windows)
- **GC**: Otimizado para `MaxGCPauseMillis=100` (mais rápido)

### 2. **Desabilitar Configuration Cache**
- **Antes**: `org.gradle.configuration-cache=true`
- **Agora**: `org.gradle.configuration-cache=false`
- **Motivo**: Configuration cache pode causar lentidão no Windows

### 3. **Desabilitar Tasks Desnecessárias no Debug**
- ✅ Testes desabilitados durante `assembleDebug`
- ✅ Lint desabilitado durante build
- ✅ Check desabilitado durante build
- ✅ Cobertura (JaCoCo) desabilitada

### 4. **Otimizações de Compilação**
- ✅ Compilação paralela habilitada
- ✅ Build cache habilitado
- ✅ Incremental compilation habilitado
- ✅ KSP incremental habilitado

## 📊 Resultado Esperado

**Antes**: ~27 minutos  
**Depois**: ~5-10 minutos (redução de 60-80%)

## 🚀 Como Usar

### Opção 1: Build Normal (Otimizado)
```powershell
.\gradlew.bat assembleDebug
```

### Opção 2: Build Rápido (Máxima Velocidade)
```powershell
.\scripts\build-rapido.ps1
```

O script `build-rapido.ps1` desabilita explicitamente:
- Testes (`-x test`)
- Lint (`-x lint`)
- Check (`-x check`)
- Cobertura (`-x jacocoTestReport`)

## ⚙️ Configurações Aplicadas

### `gradle.properties`
```properties
# Memória aumentada
org.gradle.jvmargs=-Xmx6g -Xms2g ...
kotlin.daemon.jvmargs=-Xmx4g -Xms2g ...

# Configuration cache desabilitado
org.gradle.configuration-cache=false

# Workers otimizados
org.gradle.workers.max=4
org.gradle.parallel=true
```

### `app/build.gradle.kts`
```kotlin
// Tasks desnecessárias desabilitadas no debug
afterEvaluate {
    tasks.matching { it.name.contains("test") }.configureEach { enabled = false }
    tasks.matching { it.name.contains("lint") }.configureEach { enabled = false }
    tasks.matching { it.name.contains("check") }.configureEach { enabled = false }
}
```

## 💡 Dicas Adicionais

1. **Primeiro Build**: Sempre será mais lento (baixa dependências)
2. **Builds Subsequentes**: Devem ser muito mais rápidos (cache)
3. **Clean Build**: Use apenas quando necessário (`.\gradlew.bat clean`)
4. **Incremental Build**: Sempre use `assembleDebug` (não `clean assembleDebug`)

## 🔍 Verificar Performance

Para medir o tempo de build:
```powershell
Measure-Command { .\gradlew.bat assembleDebug }
```

Ou use o script:
```powershell
.\scripts\build-rapido.ps1
```

---

**Última atualização**: Janeiro 2025  
**Ambiente**: Windows 10/11
