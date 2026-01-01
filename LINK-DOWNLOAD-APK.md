# 🔗 Como Obter Link de Download do APK

## ✅ APK Gerado com Sucesso!

O APK está disponível em:
- `app/build/outputs/apk/debug/app-debug.apk` (25MB)
- `b/outputs/apk/debug/app-debug.apk` (25MB)

---

## 🚀 Opções para Disponibilizar Download

### **Opção 1: Baixar Diretamente via Cursor (Mais Fácil)**

1. **No Cursor**, abra o **Explorer** (painel esquerdo)
2. **Navegue até**: `app/build/outputs/apk/debug/`
3. **Clique com botão direito** em `app-debug.apk`
4. **Selecione**: `Download` ou `Save As...`
5. **Pronto!** O APK será baixado para seu computador

---

### **Opção 2: GitHub Releases (Recomendado para Compartilhar)**

#### Via GitHub CLI (se autenticado):
```bash
gh release create v1.0.0 \
  app/build/outputs/apk/debug/app-debug.apk \
  --title "APK Debug v1.0.0" \
  --notes "APK para download e instalação"
```

Depois acesse: `https://github.com/SEU_USUARIO/SEU_REPO/releases`

#### Via Interface Web:
1. Vá para: `https://github.com/SEU_USUARIO/SEU_REPO/releases`
2. Clique em "Draft a new release"
3. Faça upload do APK
4. Publique a release
5. Compartilhe o link da release

---

### **Opção 3: Google Drive / Dropbox**

1. **Faça upload** do APK para Google Drive ou Dropbox
2. **Compartilhe o link** (pode ser público ou com acesso específico)
3. **Pronto!** Qualquer pessoa com o link pode baixar

---

### **Opção 4: Servidor HTTP Local (Para Rede Local)**

Se você quer servir o APK na sua rede local:

```bash
# Na VM
python3 scripts/servir-apk.py

# Descobrir IP da VM
hostname -I

# Acessar de outro dispositivo na mesma rede:
# http://IP_DA_VM:8000/app-debug.apk
```

---

### **Opção 5: Commit Temporário no Git (Não Recomendado)**

⚠️ **Atenção**: APKs não devem ficar no Git, mas pode ser útil temporariamente:

```bash
# Adicionar APK temporariamente
git add app/build/outputs/apk/debug/app-debug.apk
git commit -m "APK temporário para download"
git push

# Baixar via GitHub
# Depois remover do Git:
git rm --cached app/build/outputs/apk/debug/app-debug.apk
git commit -m "Remove APK do Git"
git push
```

---

## 📋 Recomendação

**Para uso pessoal**: Use a **Opção 1** (Cursor Explorer) - mais rápido e simples

**Para compartilhar**: Use a **Opção 2** (GitHub Releases) - mais profissional

---

## 🆘 Problemas?

- **APK não encontrado**: Execute `./gradlew :app:assembleDebug`
- **Não consegue baixar**: Verifique permissões do arquivo
- **Link não funciona**: Tente outra opção da lista

---

## ✅ Próximos Passos Após Baixar

1. **Transfira o APK** para seu celular (USB, email, etc.)
2. **Ative "Fontes desconhecidas"** nas configurações do Android
3. **Abra o arquivo** no celular
4. **Toque em "Instalar"**

---

**💡 Dica**: A forma mais rápida é usar o Cursor Explorer para baixar diretamente!
