# Regras para Criação de Scripts PowerShell

## ⚠️ REGRAS CRÍTICAS

### 1. **Encoding e Caracteres Especiais**
- ❌ **NUNCA** use caracteres acentuados (ç, ã, é, etc.) em strings dentro de scripts PowerShell
- ✅ **SEMPRE** use apenas ASCII simples em mensagens e strings
- ✅ Use `[System.IO.File]::ReadAllText()` e `WriteAllText()` com encoding UTF8 explícito para arquivos
- ✅ Para Write-Host, use apenas caracteres ASCII: "correcao", "concluido", "processando"

**Exemplo ERRADO:**
```powershell
Write-Host "=== CORREÇÃO CONCLUÍDA ===" -ForegroundColor Green
```

**Exemplo CORRETO:**
```powershell
Write-Host "=== CORRECAO CONCLUIDA ===" -ForegroundColor Green
```

### 2. **Compatibilidade de Versão do PowerShell**
- ❌ **NUNCA** assuma que `ForEach-Object -Parallel` está disponível (requer PowerShell 7+)
- ❌ **NUNCA** use recursos avançados sem verificar a versão primeiro
- ✅ **SEMPRE** escreva scripts compatíveis com PowerShell 5.1+ (padrão no Windows)
- ✅ Use loops `foreach` simples ao invés de `-Parallel` para máxima compatibilidade
- ✅ Se precisar de paralelismo, use `Start-Job` com cuidado (veja regra 3)

**Exemplo ERRADO:**
```powershell
$files | ForEach-Object -Parallel { ... } -ThrottleLimit 20
```

**Exemplo CORRETO:**
```powershell
foreach ($file in $files) {
    # Processar arquivo
}
```

### 3. **Start-Job e Serialização**
- ❌ **NUNCA** passe objetos complexos (arrays de hashtables, objetos customizados) para `Start-Job`
- ✅ **SEMPRE** passe apenas tipos primitivos (string, int, bool) ou arrays simples de strings
- ✅ Se precisar passar dados complexos, serialize para JSON ou use variáveis `$using:`
- ✅ **SEMPRE** limpe jobs após uso: `Get-Job | Remove-Job`

**Exemplo ERRADO:**
```powershell
Start-Job -ScriptBlock { ... } -ArgumentList $file, @(@{Pattern='...'; Replacement='...'})
```

**Exemplo CORRETO:**
```powershell
# Processar sequencialmente ou passar apenas strings
foreach ($file in $files) {
    Process-File -Path $file.FullName
}
```

### 4. **Manipulação de Arquivos**
- ✅ **SEMPRE** use `[System.IO.File]::ReadAllText()` e `WriteAllText()` para controle total de encoding
- ✅ **SEMPRE** especifique encoding UTF8 explicitamente: `[System.Text.Encoding]::UTF8`
- ✅ **SEMPRE** verifique se diretórios existem antes de criar arquivos: `Test-Path`
- ✅ **SEMPRE** use `-ErrorAction SilentlyContinue` em operações que podem falhar (Remove-Item, Copy-Item)

**Exemplo CORRETO:**
```powershell
$content = [System.IO.File]::ReadAllText($filePath, [System.Text.Encoding]::UTF8)
# ... processar conteúdo ...
[System.IO.File]::WriteAllText($filePath, $content, [System.Text.Encoding]::UTF8)
```

### 5. **Tratamento de Erros**
- ✅ **SEMPRE** use `try-catch` em operações de arquivo
- ✅ **SEMPRE** defina `$ErrorActionPreference = "Stop"` no início para capturar erros
- ✅ **SEMPRE** forneça mensagens de erro informativas
- ✅ **SEMPRE** verifique se caminhos existem antes de usar: `if (Test-Path $path)`

**Exemplo CORRETO:**
```powershell
$ErrorActionPreference = "Stop"
try {
    $content = [System.IO.File]::ReadAllText($filePath, [System.Text.Encoding]::UTF8)
} catch {
    Write-Host "ERRO ao processar $filePath : $_" -ForegroundColor Red
    continue
}
```

### 6. **Performance e Eficiência**
- ✅ Para muitos arquivos, processe sequencialmente com feedback de progresso
- ✅ Use contadores e mensagens periódicas: `if ($processed % 50 -eq 0) { Write-Host ... }`
- ✅ Evite operações desnecessárias (não recrie diretórios que já existem)
- ✅ Use `-Force` em operações que podem sobrescrever

**Exemplo CORRETO:**
```powershell
$processed = 0
foreach ($file in $files) {
    $processed++
    if ($processed % 50 -eq 0) {
        Write-Host "Processando: $processed/$totalFiles arquivos..." -ForegroundColor Cyan
    }
    # Processar arquivo
}
```

### 7. **Padrão de Template para Scripts PowerShell**
```powershell
# Script para [DESCRICAO]
$ErrorActionPreference = "Stop"

# Configuracoes
$rootPath = Split-Path -Parent $PSScriptRoot

# Validacoes
if (-not (Test-Path $rootPath)) {
    Write-Host "ERRO: Caminho nao encontrado: $rootPath" -ForegroundColor Red
    exit 1
}

# Processamento principal
try {
    # Operacoes aqui
    Write-Host "Concluido!" -ForegroundColor Green
} catch {
    Write-Host "ERRO: $_" -ForegroundColor Red
    exit 1
}
```

## ✅ CHECKLIST ANTES DE CRIAR SCRIPTS

- [ ] Todos os caracteres são ASCII (sem acentos)
- [ ] Script é compatível com PowerShell 5.1+
- [ ] Uso de `[System.IO.File]` para leitura/escrita com encoding UTF8
- [ ] Verificações de `Test-Path` antes de operações
- [ ] Tratamento de erros com `try-catch`
- [ ] Mensagens de progresso para operações longas
- [ ] `$ErrorActionPreference = "Stop"` no início
- [ ] Validação de caminhos e parâmetros
- [ ] Limpeza de recursos temporários (jobs, arquivos)

## 📝 NOTAS IMPORTANTES

- PowerShell no Windows geralmente é versão 5.1 (não PowerShell 7+)
- Caracteres especiais causam problemas de parsing mesmo com UTF8
- Scripts devem ser robustos e funcionar mesmo com estruturas parciais
- Sempre teste scripts em ambiente isolado antes de aplicar em produção

