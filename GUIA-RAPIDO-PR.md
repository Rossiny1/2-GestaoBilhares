# 🚀 Guia Rápido - Fluxo de PR Automático

## 📋 O Que Foi Configurado

✅ **Cloud Agent** → Cria PR automaticamente após build bem-sucedido  
✅ **Você localmente** → Recebe PR e faz merge quando quiser

---

## 🎯 Passo a Passo SIMPLES

### **1. Instalar GitHub CLI (Uma vez só)**

**Windows:**
```powershell
winget install --id GitHub.cli
```

**Ou baixar:** https://cli.github.com/

### **2. Autenticar GitHub (Uma vez só)**

```bash
gh auth login
```

Siga as instruções na tela.

### **3. Verificar se funcionou**

```bash
gh auth status
```

---

## 🔄 Como Funciona Agora

### **No Cloud Agent (Automático):**
1. Eu faço mudanças
2. Build passa ✅
3. **PR é criado automaticamente** no GitHub
4. Você recebe notificação

### **No Seu Ambiente Local (Você faz):**

#### **Opção 1: Via Cursor (Mais Fácil) ⭐**

1. Abra o Cursor
2. Pressione **Ctrl+Shift+G**
3. Você verá o PR listado
4. Clique em **"Merge"** ou **"Pull Changes"**
5. ✅ Pronto! Mudanças sincronizadas

#### **Opção 2: Via GitHub Desktop**

1. Abra GitHub Desktop
2. Clique em **"Fetch origin"**
3. Você verá o PR
4. Clique em **"Pull request"** → **"Merge"**
5. ✅ Pronto!

#### **Opção 3: Via Git CLI**

```bash
# Ver PRs disponíveis
gh pr list

# Ver detalhes do PR
gh pr view

# Fazer merge do PR mais recente
gh pr merge --merge

# Atualizar local
git pull origin main
```

---

## 📝 Comandos Rápidos

### **Ver PRs Pendentes:**
```bash
gh pr list
```

### **Ver Detalhes do PR:**
```bash
gh pr view
```

### **Fazer Merge do PR:**
```bash
gh pr merge --merge
```

### **Atualizar Projeto Local:**
```bash
git pull origin main
```

---

## ✅ Checklist de Configuração

- [ ] GitHub CLI instalado (`gh --version`)
- [ ] GitHub CLI autenticado (`gh auth status`)
- [ ] Cursor configurado (ou GitHub Desktop)
- [ ] Testar: Ver se PRs aparecem

---

## 🎉 Pronto!

Agora o fluxo é:
1. **Cloud Agent** cria PR automaticamente
2. **Você** recebe notificação
3. **Você** faz merge quando quiser
4. **Mudanças** sincronizadas!

**Muito mais seguro e organizado! 🚀**
