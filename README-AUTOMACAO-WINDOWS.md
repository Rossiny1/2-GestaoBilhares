# 🤖 Automação de Build e Instalação - Windows

Este guia é específico para usuários Windows. Para Linux/Mac, veja `README-AUTOMACAO.md`.

## 📋 Scripts Disponíveis para Windows

### 1. `scripts/auto-commit-on-build-success.ps1` ou `.bat`
**O que faz:** Automaticamente faz commit e push das mudanças quando o build passa.

**Quando roda:** Automaticamente após `installDebug` ou `assembleDebug` bem-sucedido.

**Como funciona:**
- Verifica se há mudanças não commitadas
- Faz commit automático com mensagem timestampada
- Faz push para o repositório remoto

### 2. `scripts/auto-install-debug.ps1` ou `.bat`
**O que faz:** Verifica mudanças remotas e instala o app automaticamente.

**Uso PowerShell:**
```powershell
.\scripts\auto-install-debug.ps1
```

**Uso Batch:**
```cmd
scripts\auto-install-debug.bat
```

**Como funciona:**
- Verifica se há atualizações no repositório remoto
- Se houver, faz pull automaticamente
- Compila e instala o app no dispositivo conectado

### 3. `scripts/watch-and-install.ps1` ou `.bat`
**O que faz:** Monitora mudanças remotas continuamente e instala automaticamente.

**Uso PowerShell (Recomendado):**
```powershell
.\scripts\watch-and-install.ps1
```

**Uso Batch:**
```cmd
scripts\watch-and-install.bat
```

**Como funciona:**
- Roda em loop verificando mudanças a cada 30 segundos
- Quando detecta mudanças, faz pull e instala automaticamente
- Pressione `Ctrl+C` para parar

## 🚀 Configuração Inicial no Windows

### 1. Habilitar execução de scripts PowerShell (se necessário)

Abra PowerShell como **Administrador** e execute:
```powershell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

### 2. Verificar se Git está instalado
```cmd
git --version
```

### 3. Verificar se Gradle está funcionando
```cmd
gradlew.bat --version
```

### 4. Conectar seu celular Android
- Conecte via USB
- Ative "Depuração USB" nas opções de desenvolvedor
- Verifique se está detectado:
```cmd
adb devices
```

## 🔄 Fluxo de Trabalho Automatizado

### Cenário 1: Instalação sob demanda
1. ✅ Eu faço as correções no código
2. ✅ Build passa automaticamente
3. ✅ Script faz commit e push automaticamente
4. 🔄 **Você roda localmente:** `.\scripts\auto-install-debug.ps1`
5. ✅ App é instalado automaticamente no seu celular

### Cenário 2: Monitoramento contínuo
1. 🔄 **Você roda localmente:** `.\scripts\watch-and-install.ps1`
2. ✅ Script monitora mudanças remotas continuamente
3. ✅ Quando eu fizer mudanças e commitar, o script detecta
4. ✅ Pull e instalação acontecem automaticamente
5. ✅ Seu app sempre atualizado!

## 📱 Requisitos Windows

- Windows 10/11
- Git para Windows instalado
- PowerShell 5.1+ (já vem com Windows)
- Android SDK Platform Tools (para `adb`)
- Dispositivo Android conectado via USB
- Depuração USB ativada no dispositivo

## ⚙️ Personalização

### Alterar intervalo de verificação (watch-and-install.ps1):
Edite a linha `Start-Sleep -Seconds 30` para o intervalo desejado (em segundos).

### Alterar intervalo de verificação (watch-and-install.bat):
Edite a linha `timeout /t 30` para o intervalo desejado (em segundos).

## 🐛 Troubleshooting Windows

### Erro: "execution of scripts is disabled"
```powershell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

### Script PowerShell não executa:
- Certifique-se de usar `.ps1` e não apenas `ps1`
- Use caminho completo: `.\scripts\auto-install-debug.ps1`

### Erro "gradlew.bat não encontrado":
Certifique-se de estar na raiz do projeto:
```cmd
cd C:\caminho\para\seu\projeto
```

### Dispositivo não detectado:
```cmd
adb devices
```
Se não aparecer, verifique:
- Cabo USB conectado
- Depuração USB ativada
- Drivers USB instalados

### Git não encontrado:
Adicione Git ao PATH ou use Git Bash:
```bash
# No Git Bash:
./scripts/auto-install-debug.sh
```

## 💡 Dicas Windows

1. **Use PowerShell ISE** para editar scripts facilmente
2. **Use Git Bash** se preferir scripts `.sh` (funciona no Windows também)
3. **Crie atalhos** na área de trabalho para os scripts mais usados
4. **Use Task Scheduler** para rodar `watch-and-install.ps1` na inicialização

## 📝 Notas

- Scripts `.ps1` são mais poderosos e recomendados
- Scripts `.bat` são mais simples mas têm funcionalidades limitadas
- Ambos fazem a mesma coisa - escolha o que preferir
- O commit automático funciona em ambos os sistemas
