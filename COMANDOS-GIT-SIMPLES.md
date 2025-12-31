# 📝 Comandos Git Simples - Guia Rápido

## 🎯 Comandos que Você Vai Usar

### **1. Ver o Status (O que mudou?)**
```powershell
git status
```
Mostra quais arquivos foram modificados.

---

### **2. Baixar Atualizações do GitHub (PULL)**
```powershell
git pull
```
**O que faz**: Baixa todas as mudanças que eu fiz e atualiza seus arquivos.

**Quando usar**: Sempre que eu disser "faça pull" ou quando quiser atualizar.

---

### **3. Ver Diferenças (O que mudou?)**
```powershell
git diff
```
Mostra linha por linha o que mudou nos arquivos.

---

### **4. Descartar Mudanças Locais (CUIDADO!)**
```powershell
git checkout .
```
**O que faz**: Desfaz TODAS as mudanças locais e volta para a versão do GitHub.

**Quando usar**: Quando você modificou algo por engano e quer voltar ao estado original.

**⚠️ ATENÇÃO**: Isso apaga suas mudanças locais! Use com cuidado.

---

### **5. Salvar Mudanças Locais (COMMIT)**
```powershell
git add .
git commit -m "Descrição do que mudou"
```
**O que faz**: Salva suas mudanças localmente.

**Quando usar**: Quando você fez mudanças e quer salvar.

---

## 🔄 Fluxo Completo (Passo a Passo)

### **Cenário 1: Você quer apenas atualizar (PULL)**
```powershell
# 1. Ver o que vai mudar (opcional)
git status

# 2. Baixar atualizações
git pull

# 3. Testar build
.\gradlew.bat compileDebugKotlin
```

### **Cenário 2: Você tem mudanças locais e quer atualizar**
```powershell
# 1. Ver suas mudanças
git status

# 2. Opção A: Descartar suas mudanças e atualizar
git checkout .
git pull

# 2. Opção B: Salvar suas mudanças primeiro, depois atualizar
git add .
git commit -m "Minhas mudanças"
git pull
```

### **Cenário 3: Conflito (você e eu mudamos o mesmo arquivo)**
```powershell
# 1. Tentar atualizar
git pull

# 2. Se der conflito, o Git vai avisar
# 3. Abra o arquivo no Cursor
# 4. O Cursor mostra os conflitos
# 5. Escolha qual versão manter
# 6. Ou me avise e eu resolvo
```

---

## 🎨 Via Cursor (Interface Gráfica)

### **Fazer Pull:**
1. `Ctrl + Shift + P`
2. Digite: `Git: Pull`
3. Enter

### **Ver Status:**
1. `Ctrl + Shift + G` (abre Source Control)
2. Veja os arquivos modificados

### **Descartar Mudanças:**
1. `Ctrl + Shift + G`
2. Clique no arquivo
3. Clique em "Discard Changes"

---

## 💡 Dicas

1. **Sempre faça pull antes de trabalhar** - garante que está atualizado
2. **Se der erro, não entre em pânico** - me avise e eu ajudo
3. **Use `git status` para ver o que está acontecendo**
4. **Se não tiver certeza, me pergunte antes de fazer algo destrutivo**

---

## 🆘 Comandos de Emergência

### **"Eu baguncei tudo, quero voltar ao estado do GitHub"**
```powershell
git checkout .
git pull
```

### **"Quero ver o que mudou desde a última atualização"**
```powershell
git fetch
git diff HEAD origin/main
```

### **"Quero desfazer o último commit local"**
```powershell
git reset --soft HEAD~1
```

---

## ✅ Checklist Antes de Fazer Pull

- [ ] Fechei todos os arquivos abertos no Cursor
- [ ] Salvei minhas mudanças (se quiser mantê-las)
- [ ] Estou na branch correta (geralmente `main`)
- [ ] Tenho backup (se necessário)

---

**Lembre-se**: Git é como um sistema de backup inteligente. Não tenha medo de experimentar, mas sempre me avise se algo der errado!
