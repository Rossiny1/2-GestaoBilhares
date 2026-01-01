# 🖥️ Como Abrir Terminal da VM no Cursor

## ⚠️ Problema

Quando você abre terminal no Cursor, abre **PowerShell** (Windows local), mas precisamos do terminal da **VM** (Linux).

## ✅ Soluções

### Solução 1: Command Palette (Mais Fácil)

1. **Pressione**: `Ctrl+Shift+P` (Windows) ou `Cmd+Shift+P` (Mac)

2. **Digite**: `Terminal: Select Default Profile`

3. **Escolha um perfil Linux**:
   - `Git Bash` (se tiver Git instalado)
   - `WSL` (se tiver Windows Subsystem for Linux)
   - `bash` (se disponível)
   - Qualquer opção que não seja "PowerShell"

4. **Abra novo terminal**: 
   - `Ctrl+Shift+`` (Ctrl + Shift + crase)
   - Ou: View → Terminal → New Terminal

5. **Verifique**:
   ```bash
   pwd
   ```
   Deve mostrar: `/workspace` ✅

### Solução 2: Configurar Terminal Padrão

1. **Abra Settings**: `Ctrl+,` (ou `Cmd+,`)

2. **Procure**: `terminal.integrated.defaultProfile.windows`

3. **Altere para**: `"Git Bash"` ou `"WSL"`

4. **Salve** (Ctrl+S)

5. **Feche e reabra** o terminal

### Solução 3: Verificar Conexão Remota

1. **Olhe na barra inferior** do Cursor (status bar)

2. **Deve aparecer**:
   - `SSH: cursor` 
   - `Remote: cursor`
   - Ou similar indicando conexão remota

3. **Se não aparecer**:
   - O Cursor pode não estar conectado à VM
   - Tente: Command Palette → `Remote-SSH: Connect to Host`

### Solução 4: Usar Git Bash Diretamente

Se você tem Git instalado:

1. **Abra Git Bash** (fora do Cursor)
2. **Navegue até o workspace** (se necessário)
3. **Ou use SSH** para conectar na VM

## 🔍 Como Identificar se Está na VM

### ✅ Terminal da VM (Correto):
```bash
ubuntu@cursor:/workspace$ pwd
/workspace

ubuntu@cursor:/workspace$ hostname
cursor
```

### ❌ PowerShell Local (Errado):
```powershell
PS C:\Users\...> pwd
Path
----
C:\Users\SeuUsuario
```

## 🎯 Teste Rápido

Execute no terminal:

```bash
pwd && hostname && echo "Workspace existe: $([ -d /workspace ] && echo 'SIM ✅' || echo 'NÃO ❌')"
```

**Se mostrar**:
- `/workspace`
- `cursor` (ou similar)
- `SIM ✅`

**Então você está na VM!** 🎉

## 📋 Checklist

- [ ] Terminal bash/Linux aberto (não PowerShell)
- [ ] `pwd` mostra `/workspace`
- [ ] `hostname` mostra `cursor` ou similar
- [ ] Pronto para executar comandos Firebase!

## 🚀 Depois de Acessar Terminal da VM

Execute:

```bash
export PATH=$PATH:/home/ubuntu/.nvm/versions/node/v22.21.1/bin
firebase login --no-localhost 2>&1 | tee ~/firebase-login-output.txt
```

E siga as instruções do guia `LOGIN-FIREBASE-VM-COMPLETO.md`!
