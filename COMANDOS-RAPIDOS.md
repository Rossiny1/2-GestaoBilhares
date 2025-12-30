# ⚡ Comandos Rápidos - Referência

## 🔧 Configuração Inicial (Uma vez só)

```bash
# 1. Instalar GitHub CLI
winget install --id GitHub.cli  # Windows
# ou: https://cli.github.com/

# 2. Autenticar
gh auth login

# 3. Verificar
gh auth status
```

---

## 📥 Receber Mudanças (Quando PR for criado)

### **Via Cursor:**
```
Ctrl+Shift+G → Ver PR → Merge
```

### **Via GitHub Desktop:**
```
Fetch origin → Pull request → Merge
```

### **Via CLI:**
```bash
# Ver PRs
gh pr list

# Fazer merge
gh pr merge --merge

# Atualizar local
git pull origin main
```

---

## 🔍 Verificar Status

```bash
# Ver PRs pendentes
gh pr list

# Ver detalhes do PR
gh pr view

# Ver status do Git
git status
```

---

## 🚀 Tudo em Um Comando

```bash
# Ver PRs e fazer merge do mais recente
gh pr list && gh pr merge --merge && git pull origin main
```

---

**Simples assim! 🎯**
