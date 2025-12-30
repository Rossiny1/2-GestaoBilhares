# ⚡ Comando Rápido - Fazer Merge do PR

## 🎯 Método Mais Rápido (Terminal)

### **No Cursor, abra o terminal:**
- Pressione: `Ctrl+`` (backtick)

### **Execute estes 2 comandos:**
```powershell
gh pr merge 1 --merge
git pull origin release/v1.0.0
```

✅ **Pronto! Mudanças importadas!**

---

## 🔄 Método Alternativo (Command Palette)

1. `Ctrl+Shift+P`
2. Digite: `Git: Pull`
3. Enter

✅ **Pronto!**

---

## 📋 Verificar se Funcionou

```powershell
git log --oneline -3
```

Deve mostrar commits recentes.

---

**Simples assim! 🚀**
