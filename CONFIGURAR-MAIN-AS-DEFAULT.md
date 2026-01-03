# 🔧 Como Configurar `main` como Branch Padrão no GitHub

## Passo a Passo

1. **Acesse o repositório no GitHub:**
   https://github.com/Rossiny1/2-GestaoBilhares

2. **Vá para Settings:**
   - Clique em "Settings" (no topo do repositório)

3. **Acesse Branches:**
   - No menu lateral esquerdo, clique em "Branches"

4. **Altere a Default branch:**
   - Encontre a seção "Default branch"
   - Clique no ícone de editar (lápis) ao lado de `release/v1.0.0`
   - Selecione `main` da lista
   - Clique em "Update"
   - Confirme a alteração

5. **Pronto!**
   - Agora `main` é a branch padrão
   - Novos clones e forks usarão `main` como base

---

## Alternativa: Via GitHub CLI (se tiver instalado)

```bash
gh api repos/Rossiny1/2-GestaoBilhares -X PATCH -f default_branch=main
```

---

**Nota:** Esta ação precisa ser feita manualmente no GitHub, não pode ser feita via Git CLI.
