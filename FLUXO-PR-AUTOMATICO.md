# 🔀 Fluxo de PR Automático

## 🎯 Como Funciona

1. **Cloud Agent termina task** → Cria PR no GitHub
2. **Cursor local detecta PR** → "Pull Latest Changes" (Ctrl+Shift+G)
3. **OU: GitHub Desktop / CLI** → `git pull origin main`
4. **Mudanças sincronizadas localmente**

---

## 📋 Configuração Inicial

### 1. Instalar GitHub CLI (se ainda não tiver)

**Windows:**
```powershell
# Via winget
winget install --id GitHub.cli

# Ou baixar de: https://cli.github.com/
```

**Linux/Mac:**
```bash
# Ubuntu/Debian
sudo apt install gh

# Mac
brew install gh
```

### 2. Autenticar GitHub CLI

```bash
gh auth login
```

Siga as instruções para autenticar.

### 3. Verificar Autenticação

```bash
gh auth status
```

---

## 🔄 Fluxo Automático

### **No Cloud Agent (VM):**

Quando o build passa:
1. ✅ Script `create-pr-on-success.ps1` ou `.sh` roda automaticamente
2. ✅ Faz commit das mudanças
3. ✅ Faz push da branch
4. ✅ Cria PR automaticamente (ou atualiza PR existente)
5. ✅ PR fica pronto para revisão

### **No Seu Ambiente Local:**

#### **Opção 1: Via Cursor (Mais Fácil)**

1. Abra o Cursor
2. Pressione **Ctrl+Shift+G** (ou clique em "Pull Latest Changes")
3. Cursor detecta o PR automaticamente
4. Clique em "Merge" ou "Pull Changes"
5. ✅ Mudanças sincronizadas!

#### **Opção 2: Via GitHub Desktop**

1. Abra GitHub Desktop
2. Clique em **"Fetch origin"**
3. Você verá o PR listado
4. Clique em **"Pull request"** → **"Merge"**
5. ✅ Mudanças sincronizadas!

#### **Opção 3: Via Git CLI**

```bash
# 1. Ver PRs disponíveis
gh pr list

# 2. Ver detalhes do PR
gh pr view <número>

# 3. Fazer merge do PR
gh pr merge <número> --merge

# 4. Atualizar local
git pull origin main
```

#### **Opção 4: Via Script Automático**

```powershell
# Windows
.\scripts\sync-all-changes.ps1

# Linux/Mac
./scripts/sync-all-changes.sh
```

---

## 🎨 Vantagens do Fluxo com PR

### ✅ **Melhor que Commit Direto:**

1. **Revisão antes de merge**
   - Você vê o que mudou antes de aplicar
   - Pode testar localmente primeiro

2. **Histórico mais limpo**
   - Commits organizados em PRs
   - Fácil de reverter se necessário

3. **Trabalho em equipe**
   - Outros podem revisar
   - Discussões sobre mudanças

4. **Segurança**
   - Não aplica mudanças sem aprovação
   - Pode rejeitar PRs se necessário

---

## 📝 Estrutura do PR Automático

Cada PR criado automaticamente terá:

**Título:**
```
Auto-PR: Correções e Otimizações - 2025-12-30 14:30
```

**Descrição:**
- ✅ Build passou com sucesso
- ✅ Todas as correções aplicadas
- ✅ Otimizações de performance
- ✅ Scripts de automação

---

## 🔧 Configuração Avançada

### Mudar Branch Base do PR

Edite os scripts:
- `scripts/create-pr-on-success.ps1`
- `scripts/create-pr-on-success.sh`

Procure por `--base main` e mude para sua branch preferida:
```bash
--base sua-branch-aqui
```

### Personalizar Título/Descrição do PR

Edite a seção `PR_TITLE` e `PR_BODY` nos scripts.

---

## 🐛 Troubleshooting

### Erro: "GitHub CLI não encontrado"

**Solução:**
```bash
# Instalar GitHub CLI
# Windows: winget install --id GitHub.cli
# Linux: sudo apt install gh
# Mac: brew install gh
```

### Erro: "GitHub CLI não autenticado"

**Solução:**
```bash
gh auth login
```

### Erro: "Não é possível criar PR da branch main"

**Solução:**
- O script não cria PR de `main`/`master` por segurança
- Use uma branch de feature (ex: `cursor/cursor-build-failure-fix-efaf`)

### PR não aparece no Cursor

**Solução:**
1. Verifique se o PR foi criado: `gh pr list`
2. Force refresh no Cursor: **Ctrl+Shift+R**
3. Ou use GitHub Desktop/CLI para fazer merge

---

## 📊 Comparação: PR vs Commit Direto

| Aspecto | PR Automático | Commit Direto |
|---------|---------------|---------------|
| Revisão | ✅ Sim | ❌ Não |
| Segurança | ✅ Alta | ⚠️ Média |
| Histórico | ✅ Limpo | ⚠️ Pode ficar bagunçado |
| Reversão | ✅ Fácil | ⚠️ Mais difícil |
| Trabalho em equipe | ✅ Ideal | ⚠️ Limitado |

---

## 💡 Dicas

1. **Revisar PRs antes de merge**
   - Veja o que mudou
   - Teste localmente se necessário

2. **Usar Cursor para PRs**
   - Mais visual e fácil
   - Integração nativa

3. **Monitorar PRs**
   ```bash
   # Ver todos os PRs
   gh pr list
   
   # Ver PR específico
   gh pr view <número>
   ```

4. **Fazer merge rápido**
   - Se confiar nas mudanças, pode fazer merge direto
   - PRs automáticos são geralmente seguros

---

## 🚀 Próximos Passos

1. ✅ Instalar GitHub CLI
2. ✅ Autenticar: `gh auth login`
3. ✅ Testar: Fazer um build e ver se PR é criado
4. ✅ Configurar Cursor para detectar PRs
5. ✅ Pronto! Fluxo automático funcionando

---

**Agora você tem um fluxo profissional e seguro! 🎉**
