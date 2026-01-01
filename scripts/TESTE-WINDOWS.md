# 🧪 Guia de Teste dos Scripts no Windows

## ⚠️ Problemas Comuns e Soluções

### 1. Erro: "execution of scripts is disabled"

**Solução:**
```powershell
# Abra PowerShell como Administrador e execute:
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

### 2. Erro: "Não é um repositório Git"

**Solução:**
- Certifique-se de estar na **raiz do projeto** (onde está a pasta `.git`)
- Verifique com: `Test-Path .git`

### 3. Erro: "gradlew.bat não encontrado"

**Solução:**
- Certifique-se de estar na **raiz do projeto**
- Verifique se o arquivo existe: `Test-Path gradlew.bat`

### 4. Erro ao fazer git pull/push

**Solução:**
- Verifique sua conexão com a internet
- Verifique suas credenciais Git:
  ```powershell
  git config --list
  ```

### 5. Erro ao instalar app (adb)

**Solução:**
- Verifique se o dispositivo está conectado:
  ```cmd
  adb devices
  ```
- Certifique-se de que a depuração USB está ativada no celular

## 📋 Checklist de Verificação

Antes de executar os scripts, verifique:

- [ ] Está na raiz do projeto (pasta com `.git` e `gradlew.bat`)
- [ ] Git está instalado e configurado
- [ ] PowerShell permite execução de scripts
- [ ] Dispositivo Android conectado (se for instalar)
- [ ] Conexão com internet (para git pull/push)

## 🧪 Teste Passo a Passo

### 1. Teste Básico - Verificar Ambiente

```powershell
# Verificar se está na raiz do projeto
pwd
Test-Path .git
Test-Path gradlew.bat

# Verificar Git
git --version
git status

# Verificar dispositivo (se for instalar)
adb devices
```

### 2. Teste de Sincronização

```powershell
# Teste simples de sincronização
.\scripts\sync-all-changes.ps1
```

**O que deve acontecer:**
- Verificar status do repositório
- Fazer pull se houver mudanças
- Fazer push se houver commits locais
- Mostrar resumo final

### 3. Teste de Instalação

```powershell
# Teste de instalação
.\scripts\auto-install-debug.ps1
```

**O que deve acontecer:**
- Verificar mudanças remotas
- Fazer pull se necessário
- Compilar e instalar app
- Mostrar sucesso ou erro

### 4. Teste de Monitoramento

```powershell
# Teste de monitoramento (deixe rodar por 1-2 minutos)
.\scripts\watch-and-install.ps1
```

**Pressione Ctrl+C para parar**

## 🐛 Reportar Erros

Se encontrar erros, copie a mensagem completa e inclua:

1. **Comando executado:**
   ```
   .\scripts\sync-all-changes.ps1
   ```

2. **Mensagem de erro completa:**
   ```
   (cole aqui a mensagem de erro)
   ```

3. **Versão do PowerShell:**
   ```powershell
   $PSVersionTable
   ```

4. **Versão do Git:**
   ```cmd
   git --version
   ```

## 💡 Dicas

- Use **PowerShell ISE** para editar scripts facilmente
- Use **Git Bash** se preferir scripts `.sh` (funciona no Windows)
- Crie **atalhos** na área de trabalho para scripts mais usados
