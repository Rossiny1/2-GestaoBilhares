# Script completo para instalar Firebase CLI
# Recarrega PATH e instala com verificacoes

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Instalacao Firebase CLI - Versao Completa" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Recarregar PATH
Write-Host "[1/4] Recarregando variaveis de ambiente..." -ForegroundColor Yellow
$env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
Write-Host "[OK] PATH recarregado" -ForegroundColor Green
Write-Host ""

# Verificar Node.js
Write-Host "[2/4] Verificando Node.js..." -ForegroundColor Yellow
try {
    $nodeVersion = node --version 2>&1 | Out-String
    if ($LASTEXITCODE -eq 0) {
        Write-Host "[OK] Node.js: $nodeVersion" -ForegroundColor Green
    } else {
        throw "Node.js nao encontrado"
    }
} catch {
    Write-Host "[ERRO] Node.js nao encontrado!" -ForegroundColor Red
    Write-Host "Feche e reabra o PowerShell e tente novamente" -ForegroundColor Yellow
    exit 1
}

# Verificar npm
Write-Host "Verificando npm..." -ForegroundColor Yellow
try {
    $npmVersion = npm --version 2>&1 | Out-String
    if ($LASTEXITCODE -eq 0) {
        Write-Host "[OK] npm: $npmVersion" -ForegroundColor Green
    } else {
        throw "npm nao encontrado"
    }
} catch {
    Write-Host "[ERRO] npm nao encontrado!" -ForegroundColor Red
    exit 1
}

Write-Host ""

# Verificar se Firebase CLI ja esta instalado
Write-Host "[3/4] Verificando Firebase CLI..." -ForegroundColor Yellow
try {
    $firebaseVersion = firebase --version 2>&1 | Out-String
    if ($LASTEXITCODE -eq 0) {
        Write-Host "[OK] Firebase CLI ja esta instalado: $firebaseVersion" -ForegroundColor Green
        Write-Host ""
        Write-Host "Proximos passos:" -ForegroundColor Cyan
        Write-Host "1. Fazer login: firebase login" -ForegroundColor White
        Write-Host "2. Deploy indices: .\deploy-indices-firestore.ps1" -ForegroundColor White
        exit 0
    }
} catch {
    Write-Host "[AVISO] Firebase CLI nao encontrado. Instalando..." -ForegroundColor Yellow
}

Write-Host ""

# Instalar Firebase CLI
Write-Host "[4/4] Instalando Firebase CLI..." -ForegroundColor Yellow
Write-Host "Isso pode levar 1-3 minutos. Aguarde..." -ForegroundColor Gray
Write-Host ""

try {
    # Instalar com output visivel
    npm install -g firebase-tools 2>&1 | ForEach-Object {
        if ($_ -match "firebase-tools@") {
            Write-Host $_ -ForegroundColor Green
        } elseif ($_ -match "error|Error|ERROR") {
            Write-Host $_ -ForegroundColor Red
        } else {
            Write-Host $_ -ForegroundColor Gray
        }
    }
    
    Write-Host ""
    
    # Verificar se instalou
    $npmList = npm list -g firebase-tools 2>&1 | Out-String
    if ($npmList -match "firebase-tools@") {
        Write-Host "[OK] Firebase CLI instalado!" -ForegroundColor Green
        
        # Adicionar caminho do npm ao PATH da sessao
        $npmPrefix = npm config get prefix 2>&1 | Out-String
        $npmPrefix = $npmPrefix.Trim()
        if ($npmPrefix -and (Test-Path $npmPrefix)) {
            $env:Path += ";$npmPrefix"
            Write-Host "[OK] Caminho do npm adicionado ao PATH" -ForegroundColor Green
        }
        
        # Tentar localizar firebase.cmd
        $firebasePaths = @(
            "$npmPrefix\firebase.cmd",
            "$env:APPDATA\npm\firebase.cmd",
            "$env:LOCALAPPDATA\npm\firebase.cmd"
        )
        
        foreach ($path in $firebasePaths) {
            if (Test-Path $path) {
                $dir = Split-Path $path
                $env:Path += ";$dir"
                Write-Host "[OK] Firebase encontrado em: $path" -ForegroundColor Green
                break
            }
        }
        
        # Verificar novamente
        Start-Sleep -Seconds 2
        try {
            $firebaseVersion = firebase --version 2>&1 | Out-String
            if ($LASTEXITCODE -eq 0) {
                Write-Host ""
                Write-Host "========================================" -ForegroundColor Green
                Write-Host "  Instalacao concluida com sucesso!" -ForegroundColor Green
                Write-Host "========================================" -ForegroundColor Green
                Write-Host ""
                Write-Host "Versao instalada: $firebaseVersion" -ForegroundColor Cyan
                Write-Host ""
                Write-Host "Proximos passos:" -ForegroundColor Cyan
                Write-Host "1. Fazer login: firebase login" -ForegroundColor White
                Write-Host "2. Deploy indices: .\deploy-indices-firestore.ps1" -ForegroundColor White
                Write-Host ""
                Write-Host "NOTA: Se 'firebase' nao funcionar, feche e reabra o PowerShell" -ForegroundColor Yellow
            } else {
                Write-Host "[AVISO] Firebase instalado mas nao esta no PATH" -ForegroundColor Yellow
                Write-Host "Feche e reabra o PowerShell e execute: firebase --version" -ForegroundColor Yellow
            }
        } catch {
            Write-Host "[AVISO] Firebase instalado mas nao esta no PATH" -ForegroundColor Yellow
            Write-Host "Feche e reabra o PowerShell e execute: firebase --version" -ForegroundColor Yellow
        }
    } else {
        Write-Host "[ERRO] Falha na verificacao da instalacao" -ForegroundColor Red
        Write-Host "Tente executar manualmente: npm install -g firebase-tools" -ForegroundColor Yellow
        exit 1
    }
} catch {
    Write-Host "[ERRO] Erro ao instalar: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "Tente executar manualmente: npm install -g firebase-tools" -ForegroundColor Yellow
    exit 1
}

