# 🚀 Otimização de Performance - Cursor/Gradle

## Problema Identificado
Após atualização do Cursor, o terminal está muito lento durante execução de comandos Gradle.

## 🔧 Soluções Recomendadas (em ordem de prioridade)

### 1. **Windows Defender - CRÍTICO** ⚠️
O Windows Defender pode estar escaneando arquivos durante o build, causando lentidão extrema.

**Solução:**
1. Abra PowerShell **como Administrador**
2. Execute:
```powershell
Add-MpPreference -ExclusionPath "C:\Users\Rossiny\Desktop\2-GestaoBilhares"
Add-MpPreference -ExclusionPath "$env:USERPROFILE\.gradle"
```

**OU via Interface:**
- Windows Security → Virus & threat protection → Manage settings → Exclusions
- Adicione a pasta do projeto e `C:\Users\Rossiny\.gradle`

### 2. **Parar Daemons Gradle Órfãos**
Execute antes de builds:
```powershell
.\gradlew --stop
```

Para matar processos Java manualmente:
```powershell
Get-Process | Where-Object {$_.ProcessName -eq "java"} | Stop-Process -Force
```

### 3. **Limpar Cache do Gradle** (se necessário)
```powershell
.\gradlew cleanBuildCache
# OU limpar manualmente:
Remove-Item -Path "$env:USERPROFILE\.gradle\caches" -Recurse -Force
```

### 4. **Usar Gradle Daemon** (mais rápido que --no-daemon)
Seu `gradle.properties` já está configurado com `org.gradle.daemon=true`.

**Evite usar `--no-daemon`** a menos que seja absolutamente necessário. O daemon é muito mais rápido.

### 5. **Verificar Configuração de Memória**
Seu `gradle.properties` está configurado com:
- `-Xmx8g` (8GB para Gradle)
- `-Xmx4g` (4GB para Kotlin)

Se você tem menos de 16GB de RAM, considere reduzir:
```properties
org.gradle.jvmargs=-Xmx4g -Dfile.encoding=UTF-8 -XX:+UseG1GC
kotlin.daemon.jvmargs=-Xmx2g -XX:+UseG1GC
```

### 6. **Desabilitar VFS Watch no Windows**
Já está configurado: `org.gradle.vfs.watch=false` ✅

### 7. **Usar WSL2 (Opcional - Mais Rápido)**
Se disponível, executar builds no WSL2 é significativamente mais rápido que PowerShell no Windows.

### 8. **Verificar Antivírus de Terceiros**
Se usar antivírus além do Windows Defender, adicione as mesmas exclusões.

## 📊 Comandos Úteis

### Verificar processos Java rodando:
```powershell
Get-Process | Where-Object {$_.ProcessName -eq "java"} | Format-Table ProcessName, Id, CPU, @{Name="Mem(MB)";Expression={[math]::Round($_.WorkingSet64/1MB,2)}}
```

### Verificar memória disponível:
```powershell
$os = Get-CimInstance Win32_OperatingSystem
$totalRAM = [math]::Round($os.TotalVisibleMemorySize / 1MB, 2)
$freeRAM = [math]::Round($os.FreePhysicalMemory / 1MB, 2)
Write-Host "Total: $totalRAM GB | Livre: $freeRAM GB"
```

### Limpar tudo e reiniciar:
```powershell
.\gradlew --stop
Get-Process | Where-Object {$_.ProcessName -eq "java"} | Stop-Process -Force
.\gradlew clean
```

## 🎯 Solução Mais Provável

**A causa mais comum é o Windows Defender escaneando arquivos durante o build.**

Execute como Administrador:
```powershell
Add-MpPreference -ExclusionPath "C:\Users\Rossiny\Desktop\2-GestaoBilhares"
Add-MpPreference -ExclusionPath "$env:USERPROFILE\.gradle"
```

Depois reinicie o Cursor e teste novamente.

## ⚡ Build Otimizado

Para builds mais rápidos, use:
```powershell
.\gradlew assembleDebug --parallel --build-cache
```

Evite `--no-daemon` a menos que seja necessário para debug.

