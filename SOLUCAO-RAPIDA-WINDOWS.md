# ⚡ Solução Rápida - Build Falhando no Windows

## 🎯 Problema Mais Comum (99% dos casos)

**`local.properties` não existe ou está com caminho errado.**

---

## ✅ Solução em 2 Passos

### **PASSO 1: Criar `local.properties`**

**Opção A: Script Automático (Recomendado)**
```powershell
.\scripts\criar-local-properties-windows.ps1
```

**Opção B: Manual**
1. Crie arquivo `local.properties` na raiz do projeto
2. Adicione (ajuste o caminho):
   ```properties
   sdk.dir=C:\\Users\\SeuUsuario\\AppData\\Local\\Android\\Sdk
   ```
   **⚠️ Use `\\` (duas barras) no Windows!**

### **PASSO 2: Testar Build**
```powershell
.\gradlew.bat compileDebugKotlin
```

---

## 🔍 Se Ainda Falhar

Execute diagnóstico completo:
```powershell
.\scripts\diagnostico-build-local.ps1
```

Me envie o resultado!

---

## 📋 Checklist Rápido

- [ ] `local.properties` existe?
- [ ] Caminho usa `\\` (duas barras)?
- [ ] Android SDK existe no caminho?
- [ ] Java instalado? (`java -version`)

---

**99% das vezes é só criar o `local.properties`! 🚀**
