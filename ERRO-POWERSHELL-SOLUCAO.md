# ⚠️ Erro no PowerShell - Solução

## 🔍 Problema Identificado

Você tentou executar comandos Linux/bash no **PowerShell do Windows**, mas esses comandos são para o **terminal da VM do Cursor** (Linux).

## ✅ Solução: Onde Executar os Comandos

### ❌ NÃO execute no PowerShell do Windows
Os comandos como:
- `export PATH=...`
- `firebase login --no-localhost`
- `./firebase-setup.sh`

São comandos **Linux/bash** e não funcionam no PowerShell.

### ✅ Execute no Terminal do Cursor (VM)

Os comandos devem ser executados no **terminal integrado do Cursor**, que já está na VM Linux.

## 🖥️ Como Abrir o Terminal Correto

### No Cursor:

1. **Atalho de Teclado**: `Ctrl + `` (Ctrl + crase/backtick)
2. **Menu**: View → Terminal
3. **Command Palette**: `Ctrl+Shift+P` → "Terminal: Create New Terminal"

### Verificar se está no terminal correto:

Execute no terminal do Cursor:
```bash
pwd
```

**Deve mostrar**: `/workspace` (não um caminho do Windows como `C:\...`)

## 📋 Comandos Corretos para o Terminal do Cursor

### 1. Verificar Firebase CLI
```bash
export PATH=$PATH:/home/ubuntu/.nvm/versions/node/v22.21.1/bin
firebase --version
```

### 2. Fazer Login
```bash
export PATH=$PATH:/home/ubuntu/.nvm/versions/node/v22.21.1/bin
firebase login --no-localhost
```

### 3. Usar o Script Helper
```bash
./firebase-setup.sh check
./firebase-setup.sh login
```

## 🔄 Se Você Quiser Usar PowerShell (PC Local)

Se você quiser instalar/configurar Firebase CLI no seu **PC local Windows**, use estes comandos no PowerShell:

### Instalar Firebase CLI no Windows (via npm)
```powershell
npm install -g firebase-tools
```

### Verificar instalação
```powershell
firebase --version
```

### Fazer login
```powershell
firebase login
```

**Mas atenção**: O Firebase CLI no PC local é **separado** do da VM. Você precisa fazer login em ambos se quiser usar em ambos os lugares.

## 🎯 Recomendação

**Use o terminal do Cursor** (VM) para:
- ✅ Desenvolvimento do projeto
- ✅ Executar comandos Firebase
- ✅ Testar o app Android
- ✅ Usar todas as ferramentas instaladas na VM

**Use PowerShell** apenas se:
- Você quiser instalar Firebase CLI no PC local também
- Você quiser fazer algo específico no Windows

## 🔍 Como Identificar o Terminal Correto

### Terminal do Cursor (VM - Linux):
```bash
$ pwd
/workspace

$ hostname
cursor

$ ls
app/  core/  data/  ...
```

### PowerShell (Windows Local):
```powershell
PS C:\> pwd
Path
----
C:\Users\SeuUsuario

PS C:\> hostname
SEU-PC-NOME
```

## ✅ Próximos Passos

1. **Abra o terminal no Cursor** (`Ctrl + ``)
2. **Verifique que está em `/workspace`**:
   ```bash
   pwd
   ```
3. **Execute os comandos Firebase**:
   ```bash
   export PATH=$PATH:/home/ubuntu/.nvm/versions/node/v22.21.1/bin
   firebase login --no-localhost
   ```

## 📝 Resumo

| Onde | O Que | Comandos |
|------|-------|----------|
| **Terminal Cursor** | VM Linux | `export PATH=...`, `firebase login --no-localhost` |
| **PowerShell** | PC Windows | `npm install -g firebase-tools`, `firebase login` |

**Use o Terminal do Cursor para tudo relacionado ao projeto!** 🚀
