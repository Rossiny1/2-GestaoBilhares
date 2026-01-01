# 🔧 Soluções para Erros no Login Firebase

## ⚠️ Erros Comuns e Soluções

### Erro 1: "Cannot run login in non-interactive mode"

**Causa**: Terminal não está em modo interativo

**Solução**:
```bash
# Certifique-se de estar em um terminal interativo (bash)
# Não execute via script automatizado
# Execute diretamente no terminal:

export PATH=$PATH:/home/ubuntu/.nvm/versions/node/v22.21.1/bin
firebase login --no-localhost
```

### Erro 2: "Command not found: firebase"

**Causa**: PATH não configurado

**Solução**:
```bash
export PATH=$PATH:/home/ubuntu/.nvm/versions/node/v22.21.1/bin
firebase --version  # Verificar se funciona
```

### Erro 3: "Error: An unexpected error has occurred"

**Causa**: Problema de conexão ou permissões

**Soluções**:
```bash
# 1. Verificar conexão
ping google.com

# 2. Limpar cache do Firebase
rm -rf ~/.config/firebase

# 3. Tentar novamente
firebase login --no-localhost
```

### Erro 4: "Network error" ou timeout

**Causa**: Problema de rede

**Solução**:
```bash
# Verificar conectividade
curl -I https://firebase.google.com

# Tentar novamente
firebase login --no-localhost
```

### Erro 5: "Permission denied"

**Causa**: Problema de permissões

**Solução**:
```bash
# Verificar permissões
ls -la ~/.config/firebase 2>/dev/null

# Criar diretório se não existir
mkdir -p ~/.config/firebase
chmod 755 ~/.config/firebase
```

## 🔍 Diagnóstico

Execute estes comandos para diagnosticar:

```bash
# 1. Verificar Firebase CLI
export PATH=$PATH:/home/ubuntu/.nvm/versions/node/v22.21.1/bin
firebase --version

# 2. Verificar Node.js
node --version
npm --version

# 3. Verificar PATH
echo $PATH | grep nvm

# 4. Verificar permissões
which firebase
ls -la $(which firebase)
```

## ✅ Solução Alternativa: Login via Token

Se o login interativo não funcionar, você pode usar token:

### No seu PC local (PowerShell):
```powershell
firebase login:ci
```

Isso gera um token que você pode usar.

### Na VM:
```bash
# Usar o token (substitua <TOKEN> pelo token gerado)
export FIREBASE_TOKEN=<TOKEN>
firebase projects:list
```

## 🎯 Método Mais Confiável

Se continuar tendo problemas, tente:

```bash
# 1. Limpar tudo
rm -rf ~/.config/firebase

# 2. Reinstalar Firebase CLI (se necessário)
npm install -g firebase-tools

# 3. Tentar login novamente
export PATH=$PATH:/home/ubuntu/.nvm/versions/node/v22.21.1/bin
firebase login --no-localhost
```

## 📋 Me Envie a Mensagem de Erro

Para ajudar melhor, me envie:
1. A mensagem de erro completa
2. O comando que você executou
3. A saída completa do terminal

Assim posso dar uma solução específica! 🔍
