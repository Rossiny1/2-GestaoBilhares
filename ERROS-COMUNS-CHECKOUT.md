# 🔧 Erros Comuns ao Fazer Checkout de Branch

## ❌ Erro 1: "pathspec 'cursor/cursor-build-failure-fix-efaf' did not match any file(s)"

**Causa**: Branch não foi baixada do GitHub ainda.

**Solução**:
```powershell
git fetch origin
git checkout cursor/cursor-build-failure-fix-efaf
```

---

## ❌ Erro 2: "You have local changes that would be overwritten"

**Causa**: Você tem arquivos modificados localmente.

**Solução A - Descartar mudanças**:
```powershell
git checkout .
git checkout cursor/cursor-build-failure-fix-efaf
```

**Solução B - Salvar mudanças primeiro**:
```powershell
git add .
git commit -m "Minhas mudanças locais"
git checkout cursor/cursor-build-failure-fix-efaf
```

---

## ❌ Erro 3: "fatal: A branch named 'cursor/cursor-build-failure-fix-efaf' already exists"

**Causa**: Branch local já existe mas está desatualizada.

**Solução**:
```powershell
git checkout cursor/cursor-build-failure-fix-efaf
git pull origin cursor/cursor-build-failure-fix-efaf
```

---

## ❌ Erro 4: "error: pathspec 'cursor' did not match any file(s)"

**Causa**: Git interpretou como caminho de arquivo, não branch.

**Solução**: Use aspas ou escape:
```powershell
git checkout "cursor/cursor-build-failure-fix-efaf"
```

---

## ❌ Erro 5: "Permission denied" ou "Authentication failed"

**Causa**: Problema de autenticação com GitHub.

**Solução**: Verificar credenciais ou usar Personal Access Token.

---

## 🆘 Se Nenhum dos Erros Acima

**Me envie:**
1. A mensagem de erro completa
2. O resultado de: `git status`
3. O resultado de: `git branch -a`

**Vou te ajudar a resolver!**
