# 🔀 Como Fazer Merge do PR no Cursor - Comandos Corretos

## ❌ Comando Incorreto
- ~~Ctrl+Shift+G~~ (não existe no Cursor)

## ✅ Comandos Válidos no Cursor

---

## 📋 Opção 1: Via Interface do Cursor

### **Passo a Passo:**

1. **Abra o Cursor**

2. **Abra o Source Control:**
   - Pressione `Ctrl+Shift+G` (Git) - **Este abre o painel Git, não o PR**
   - Ou clique no ícone de Git na barra lateral esquerda
   - Ou: **View → Source Control**

3. **No painel Source Control, procure por:**
   - Botão **"..."** (três pontos) no topo
   - Ou menu **"Pull, Push"**
   - Ou **"Sync Changes"**

4. **Clique em "Pull"** ou **"Sync"**
   - Isso vai buscar mudanças do GitHub

5. **Se houver PR, aparecerá uma notificação**
   - Clique na notificação
   - Ou vá em **"..."** → **"Pull Requests"**

6. **Veja o PR #1** na lista

7. **Clique em "Merge"** ou **"Checkout"**

8. ✅ **Pronto!**

---

## 📋 Opção 2: Via Command Palette (Mais Confiável)

### **Passo a Passo:**

1. **Abra o Command Palette:**
   - Pressione `Ctrl+Shift+P` (Windows/Linux)
   - Ou `Cmd+Shift+P` (Mac)

2. **Digite e selecione:**
   ```
   Git: Pull
   ```
   Ou:
   ```
   Git: Sync
   ```

3. **Isso vai buscar mudanças do GitHub**

4. **Se houver PR, o Cursor mostrará opções**

5. **Selecione fazer merge ou pull**

6. ✅ **Pronto!**

---

## 📋 Opção 3: Via Terminal Integrado do Cursor

### **Passo a Passo:**

1. **Abra o Terminal no Cursor:**
   - `Ctrl+`` (backtick)
   - Ou: **Terminal → New Terminal**

2. **Execute os comandos:**
   ```powershell
   # Ver PRs
   gh pr list
   
   # Fazer merge do PR #1
   gh pr merge 1 --merge
   
   # Atualizar projeto
   git pull origin release/v1.0.0
   ```

3. ✅ **Pronto!**

---

## 📋 Opção 4: Via GitHub Desktop (Mais Visual)

Se o Cursor não mostrar PRs facilmente:

1. **Abra GitHub Desktop**

2. **Clique em "Fetch origin"**

3. **Veja o PR #1** na interface

4. **Clique em "Pull request" → "Merge"**

5. **Volte ao Cursor e faça:**
   - `Ctrl+Shift+P` → `Git: Pull`

6. ✅ **Pronto!**

---

## 🎯 Comandos do Cursor - Referência Rápida

| Ação | Atalho | Onde |
|------|--------|------|
| **Abrir Source Control** | `Ctrl+Shift+G` | Painel Git |
| **Command Palette** | `Ctrl+Shift+P` | Menu de comandos |
| **Terminal** | `Ctrl+`` | Terminal integrado |
| **Pull** | `Ctrl+Shift+P` → `Git: Pull` | Command Palette |
| **Sync** | `Ctrl+Shift+P` → `Git: Sync` | Command Palette |

---

## 💡 Método Mais Confiável (Recomendado)

### **Via Terminal do Cursor:**

1. `Ctrl+`` (abrir terminal)

2. Execute:
   ```powershell
   gh pr merge 1 --merge
   git pull origin release/v1.0.0
   ```

3. ✅ **Pronto!**

---

## 🔍 Verificar se Funcionou

Depois de fazer merge:

```powershell
# No terminal do Cursor:
git log --oneline -5
```

Você deve ver os commits recentes.

---

## 📝 Resumo Ultra Simples

**Método mais fácil:**
1. `Ctrl+Shift+P` (Command Palette)
2. Digite: `Git: Pull`
3. Enter
4. ✅ Pronto!

**Ou via terminal:**
1. `Ctrl+`` (Terminal)
2. `gh pr merge 1 --merge`
3. `git pull origin release/v1.0.0`
4. ✅ Pronto!

---

**Use o método que preferir! 🚀**
