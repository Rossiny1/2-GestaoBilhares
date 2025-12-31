# 📱 Como Disponibilizar APK para Download

## 🚀 Opção 1: Servidor HTTP Local (Mais Rápido)

### Na VM:
```bash
python3 scripts/servir-apk.py
```

Isso iniciará um servidor na porta 8000. Você verá:
- Link local: `http://localhost:8000/apk`
- Link direto: `http://localhost:8000/app-debug.apk`

### Para acessar de outro computador:

1. **Descobrir o IP da VM:**
```bash
hostname -I
# ou
ip addr show | grep "inet " | grep -v 127.0.0.1
```

2. **Acessar no navegador:**
```
http://IP_DA_VM:8000
```

3. **Ou link direto:**
```
http://IP_DA_VM:8000/app-debug.apk
```

---

## 🌐 Opção 2: GitHub Releases (Recomendado para Distribuição)

### 1. Criar Release no GitHub:
```bash
# Tag da versão
git tag -a v1.0.0 -m "Release v1.0.0"
git push origin v1.0.0

# Ou via GitHub CLI
gh release create v1.0.0 app/build/outputs/apk/debug/app-debug.apk --title "Release v1.0.0" --notes "APK para download"
```

### 2. Acessar no GitHub:
- Vá para: `https://github.com/SEU_USUARIO/SEU_REPO/releases`
- Baixe o APK da release

---

## 📤 Opção 3: Serviços de Compartilhamento Temporário

### transfer.sh (via linha de comando):
```bash
curl --upload-file app/build/outputs/apk/debug/app-debug.apk https://transfer.sh/app-debug.apk
```

Isso retornará um link temporário válido por alguns dias.

### Outros serviços:
- **WeTransfer**: https://wetransfer.com
- **Google Drive**: Upload manual e compartilhar link
- **Dropbox**: Upload manual e compartilhar link

---

## 🔧 Opção 4: Servidor HTTP com ngrok (Para acesso externo)

Se você tem ngrok instalado:

```bash
# Terminal 1: Iniciar servidor
python3 scripts/servir-apk.py

# Terminal 2: Expor via ngrok
ngrok http 8000
```

O ngrok fornecerá um link público temporário.

---

## ✅ Recomendação

Para uso rápido e simples:
1. **Use a Opção 1** (servidor HTTP local)
2. Descubra o IP da VM
3. Acesse de qualquer dispositivo na mesma rede

Para distribuição pública:
1. **Use a Opção 2** (GitHub Releases)
2. Mais profissional e permanente

---

## 🆘 Problemas?

- **Porta em uso**: Use outra porta: `python3 scripts/servir-apk.py 8080`
- **Não consegue acessar**: Verifique firewall da VM
- **APK não encontrado**: Execute `./gradlew :app:assembleDebug` primeiro
