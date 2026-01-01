# 🔀 Como Fazer Merge do PR - Passo a Passo

## 🎯 Você tem 3 opções (escolha a mais fácil para você)

---

## 📋 Opção 1: Via Cursor (Mais Fácil) ⭐

### **Passo a Passo:**

1. **Abra o Cursor**

2. **Pressione `Ctrl+Shift+G`**
   - Ou clique no ícone de Git na barra lateral
   - Ou vá em: **View → Source Control**

3. **Você verá o PR listado:**
   - Nome: **"Cursor build failure fix"**
   - Status: DRAFT ou OPEN

4. **Clique no PR** para ver detalhes

5. **Clique em "Merge"** ou **"Pull Changes"**
   - Pode aparecer como botão "Merge Pull Request"
   - Ou "Sync" / "Pull Latest Changes"

6. **Confirme o merge** (se pedir)

7. ✅ **Pronto!** Mudanças importadas!

---

## 📋 Opção 2: Via GitHub Desktop

### **Passo a Passo:**

1. **Abra GitHub Desktop**

2. **Clique em "Fetch origin"** (ou `Ctrl+Shift+F`)
   - Isso busca atualizações do GitHub

3. **Você verá uma notificação** sobre o PR

4. **Clique em "Pull request"** na interface

5. **Veja o PR #1** na lista

6. **Clique em "Merge pull request"**

7. **Confirme o merge**

8. ✅ **Pronto!** Mudanças importadas!

---

## 📋 Opção 3: Via Git CLI (Terminal)

### **Passo a Passo:**

1. **Abra PowerShell ou Git Bash**

2. **Navegue até a pasta do projeto:**
   ```powershell
   cd C:\caminho\do\seu\projeto
   ```

3. **Ver PRs disponíveis:**
   ```powershell
   gh pr list
   ```
   Você verá algo como:
   ```
   1  Cursor build failure fix  cursor/cursor-build-failure-fix-efaf  DRAFT
   ```

4. **Ver detalhes do PR:**
   ```powershell
   gh pr view 1
   ```

5. **Fazer merge do PR:**
   ```powershell
   gh pr merge 1 --merge
   ```
   *(O número 1 é o ID do PR)*

6. **Atualizar seu projeto local:**
   ```powershell
   git pull origin main
   ```
   *(Ou `git pull origin cursor/cursor-build-failure-fix-efaf` se estiver nessa branch)*

7. ✅ **Pronto!** Mudanças importadas!

---

## 🎯 Qual Opção Escolher?

| Opção | Facilidade | Recomendado Para |
|-------|------------|------------------|
| **Cursor** | ⭐⭐⭐⭐⭐ | Quem usa Cursor |
| **GitHub Desktop** | ⭐⭐⭐⭐ | Quem prefere interface visual |
| **CLI** | ⭐⭐⭐ | Quem gosta de terminal |

---

## ✅ Verificar se Funcionou

Depois de fazer merge, verifique:

```powershell
# Ver últimos commits
git log --oneline -5

# Ver status
git status
```

Você deve ver commits como:
- `fix: Corrige erro de task installDebug...`
- `docs: Adiciona resposta sobre importação...`
- `perf(VM): Otimizações críticas...`

---

## 🐛 Problemas Comuns

### **Erro: "PR não encontrado"**

```powershell
# Buscar PRs novamente
gh pr list --all

# Ou verificar branch
git branch -a
```

### **Erro: "Já está atualizado"**

Significa que você já tem as mudanças! Verifique:
```powershell
git log --oneline -3
```

### **Erro: "Conflitos de merge"**

Se houver conflitos:
```powershell
# Ver conflitos
git status

# Resolver manualmente ou aceitar mudanças remotas
git reset --hard origin/main
```

---

## 📝 Comandos Rápidos (Referência)

```powershell
# Ver PRs
gh pr list

# Ver PR específico
gh pr view 1

# Fazer merge
gh pr merge 1 --merge

# Atualizar local
git pull origin main
```

---

## 🎉 Depois do Merge

Após fazer merge com sucesso:

1. ✅ Mudanças importadas
2. ✅ Projeto atualizado
3. ✅ Pronto para instalar app:
   ```powershell
   .\gradlew.bat installDebug
   ```

---

**Escolha a opção mais fácil para você e siga os passos! 🚀**
