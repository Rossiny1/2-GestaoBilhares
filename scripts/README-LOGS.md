# 📱 Scripts PowerShell para Leitura de Logs Android

Scripts para ler logs do app Gestão Bilhares via ADB.

## 📋 Pré-requisitos

1. **Android SDK Platform Tools** instalado
2. **Caminho do ADB**: `C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe`
3. **Dispositivo Android conectado** via USB com depuração USB habilitada

## 🚀 Scripts Disponíveis

### 1. `ler-logs-android.ps1` - Logs Gerais do App

Leitura geral dos logs com filtros para AppRepository, AuthViewModel e LoginFragment.

**Uso básico:**
```powershell
.\ler-logs-android.ps1
```

**Salvar em arquivo:**
```powershell
.\ler-logs-android.ps1 -SalvarArquivo
```

**Filtrar apenas erros:**
```powershell
.\ler-logs-android.ps1 -Nivel E
```

**Parâmetros:**
- `-Filtro`: Regex para filtrar tags (padrão: "AppRepository|AuthViewModel|LoginFragment")
- `-Nivel`: Nível mínimo de log - D (Debug), E (Error), W (Warning), I (Info)
- `-SalvarArquivo`: Salvar logs em arquivo
- `-ArquivoSaida`: Nome do arquivo de saída (padrão: logs-android-YYYYMMDD-HHMMSS.txt)

---

### 2. `ler-logs-conversao.ps1` - Logs de Conversão

Foca nos logs relacionados à conversão de Colaborador do Firestore.

**Uso:**
```powershell
.\ler-logs-conversao.ps1
```

**Salvar em arquivo:**
```powershell
.\ler-logs-conversao.ps1 -SalvarArquivo
```

**Filtra:**
- CONVERSÃO
- toObject
- Gson
- getColaboradorByUid
- doc.data
- doc.getBoolean
- dataConvertida

---

### 3. `ler-logs-login.ps1` - Logs de Login

Foca nos logs de autenticação e login.

**Uso:**
```powershell
.\ler-logs-login.ps1
```

**Salvar em arquivo:**
```powershell
.\ler-logs-login.ps1 -SalvarArquivo
```

**Filtra:**
- LoginFragment
- AuthViewModel
- FirebaseAuth
- aprovado
- getColaboradorByUid
- createPendingColaborador

---

### 4. `ler-logs-completo.ps1` - Logs Completos

Leitura completa de todos os logs com opções avançadas.

**Uso básico:**
```powershell
.\ler-logs-completo.ps1
```

**Apenas erros:**
```powershell
.\ler-logs-completo.ps1 -Nivel Error
```

**Filtrar por tag específica:**
```powershell
.\ler-logs-completo.ps1 -Tag "AppRepository"
```

**Salvar e limitar linhas:**
```powershell
.\ler-logs-completo.ps1 -SalvarArquivo -LimiteLinhas 1000
```

**Parâmetros:**
- `-Nivel`: Debug, Info, Warning, Error, All
- `-Tag`: Filtrar por tag específica
- `-SalvarArquivo`: Salvar em arquivo
- `-ArquivoSaida`: Nome do arquivo
- `-LimparBuffer`: Limpar buffer antes de ler (padrão: true)
- `-LimiteLinhas`: Limitar número de linhas (0 = sem limite)

---

## 🎨 Cores dos Logs

Os scripts colorem automaticamente os logs:
- 🔴 **Vermelho**: Erros, exceções, falhas
- 🟡 **Amarelo**: Avisos, warnings
- 🟢 **Verde**: Sucessos, confirmações
- 🔵 **Ciano**: Logs de diagnóstico, Firestore
- ⚪ **Branco**: Logs normais

---

## 🔧 Solução de Problemas

### ADB não encontrado
Verifique se o caminho está correto no script ou ajuste a variável `$adbPath`.

### Nenhum dispositivo conectado
1. Conecte o dispositivo via USB
2. Habilite "Depuração USB" nas opções de desenvolvedor
3. Aceite a autorização de depuração no dispositivo

### Logs não aparecem
1. Verifique se o app está rodando no dispositivo
2. Tente limpar o buffer: `adb logcat -c`
3. Verifique se o nível de log está correto (use `-Nivel Debug` para ver tudo)

---

## 📝 Exemplos de Uso

### Diagnosticar problema de conversão:
```powershell
.\ler-logs-conversao.ps1 -SalvarArquivo
```

### Monitorar login em tempo real:
```powershell
.\ler-logs-login.ps1
```

### Ver todos os logs do app:
```powershell
.\ler-logs-android.ps1 -Nivel Debug
```

### Capturar apenas erros:
```powershell
.\ler-logs-completo.ps1 -Nivel Error -SalvarArquivo
```

---

## 📂 Localização dos Arquivos

Os scripts estão em: `/workspace/scripts/`

Os logs salvos serão criados no diretório atual onde o script foi executado.
