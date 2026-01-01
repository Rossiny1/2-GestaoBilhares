# 🖥️ Como Acessar Terminal da VM (Quando PowerShell Abre)

## ⚠️ Problema Identificado

Quando você abre o terminal no Cursor, ele abre **PowerShell local** (Windows), não o terminal da VM Linux.

## ✅ Soluções para Acessar a VM

### Solução 1: Usar Command Palette

1. Pressione `Ctrl+Shift+P` (ou `Cmd+Shift+P` no Mac)
2. Digite: **"Terminal: Select Default Profile"**
3. Escolha: **"Git Bash"** ou **"WSL"** (se tiver instalado)
4. Ou procure por: **"Terminal: New Terminal"** e escolha o perfil correto

### Solução 2: Configurar Terminal Padrão

1. Abra Settings: `Ctrl+,` (ou `Cmd+,`)
2. Procure por: `terminal.integrated.defaultProfile`
3. Configure para: `bash` ou `wsl` (se disponível)

### Solução 3: Usar SSH para Conectar na VM

Se a VM tem acesso SSH, você pode conectar:

```powershell
# No PowerShell
ssh ubuntu@<ip-da-vm>
```

### Solução 4: Usar WSL (Windows Subsystem for Linux)

Se você tem WSL instalado:

```powershell
# No PowerShell
wsl
```

Isso abre um terminal Linux.

## 🔗 Solução Alternativa: Gerar URL Manualmente

Como você precisa da URL completa, vou criar um arquivo com a URL:

### Opção A: Executar Script na VM (via minha execução)

Eu posso executar o comando na VM e salvar a URL em um arquivo que você pode abrir no Cursor.

### Opção B: Usar Firebase Login via Token

Você pode gerar um token no seu PC local e usar na VM:

**No PowerShell (PC local):**
```powershell
firebase login:ci
```

Isso gera um token que você pode usar.

## 🎯 Solução Imediata: Arquivo com URL

Vou executar o comando na VM e salvar a URL completa em um arquivo que você pode abrir no Cursor:

1. **Abra o arquivo** `firebase-url-completa.txt` no Cursor
2. **Copie a URL completa** de lá
3. **Cole no navegador**

## 📝 Próximos Passos

1. **Verifique se há arquivo** `firebase-url-completa.txt` no workspace
2. **Abra o arquivo** no Cursor
3. **Copie a URL completa**
4. **Cole no navegador e faça login**
