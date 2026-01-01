# 🔧 Solução para Erro no Git Pull

## ❌ Erro: `@vscode.git.Git (1-3130)`

Este erro acontece quando o Git integrado do Cursor/VS Code tem problemas.

---

## ✅ SOLUÇÃO: Usar o Terminal

### **Passo 1: Abrir Terminal no Cursor**

1. Pressione: `Ctrl + '` (aspas simples)
   - Ou: `View > Terminal`
   - Ou: Menu superior → Terminal → New Terminal

### **Passo 2: Verificar se está na branch correta**

Digite:
```powershell
git branch
```

**Você deve ver**: `* cursor/cursor-build-failure-fix-efaf`

**Se não estiver nessa branch**, mude:
```powershell
git checkout cursor/cursor-build-failure-fix-efaf
```

### **Passo 3: Fazer Pull pelo Terminal**

Digite:
```powershell
git pull
```

**Se der erro de conflito ou mudanças locais**, use:
```powershell
git pull origin cursor/cursor-build-failure-fix-efaf
```

---

## 🔄 Se Ainda Der Erro

### **Erro: "You have local changes"**

**Solução: Descartar mudanças locais e atualizar**
```powershell
git checkout .
git pull
```

**O que isso faz**: Remove suas mudanças locais e baixa as atualizações do GitHub.

---

### **Erro: "Branch não encontrada"**

**Solução: Buscar branches remotas**
```powershell
git fetch
git checkout cursor/cursor-build-failure-fix-efaf
git pull
```

---

### **Erro: "Permission denied" ou "Authentication failed"**

**Solução: Verificar autenticação**

Se pedir usuário/senha:
- **Usuário**: Seu usuário do GitHub
- **Senha**: Use um **Personal Access Token** (não sua senha normal)

**Como criar Personal Access Token:**
1. GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic)
2. Generate new token
3. Dê um nome e selecione permissões: `repo`
4. Copie o token e use como senha

---

## 🎯 Método Alternativo: Forçar Atualização

Se nada funcionar, use este método:

```powershell
# 1. Ver o que mudou
git status

# 2. Descartar tudo local
git reset --hard

# 3. Buscar atualizações
git fetch origin

# 4. Atualizar para a versão do GitHub
git reset --hard origin/cursor/cursor-build-failure-fix-efaf
```

**⚠️ ATENÇÃO**: Isso apaga TODAS as mudanças locais! Use só se não tiver nada importante localmente.

---

## 📋 Checklist de Troubleshooting

- [ ] Terminal está aberto no Cursor?
- [ ] Está na pasta correta do projeto?
- [ ] Está na branch correta? (`git branch`)
- [ ] Tentou `git pull` pelo terminal?
- [ ] Se deu erro, qual foi a mensagem exata?

---

## 🆘 Se Nada Funcionar

**Me envie:**
1. A mensagem de erro completa
2. O resultado de: `git status`
3. O resultado de: `git branch`

**Vou te ajudar a resolver!** 😊

---

## 💡 Dica

**Sempre use o Terminal para Git** quando a interface gráfica der erro. É mais confiável!
