# 🔐 Instruções para Login no Firebase CLI

## ⚠️ Problema

O ambiente atual não permite entrada interativa, então o login precisa ser feito manualmente.

## ✅ Solução 1: Executar Script Manualmente (Recomendado)

Execute o script que foi criado:

```bash
cd /workspace
./firebase-login.sh
```

O script irá:
1. Abrir uma URL no navegador
2. Você fará login com sua conta Google
3. Você receberá um código de autorização
4. Cole o código quando solicitado

## ✅ Solução 2: Comando Direto

Execute diretamente no terminal:

```bash
export PATH=$PATH:/home/ubuntu/.nvm/versions/node/v22.21.1/bin
firebase login --no-localhost
```

**O que vai acontecer:**
1. Uma URL será exibida (algo como `https://accounts.google.com/o/oauth2/auth?...`)
2. Abra essa URL no seu navegador
3. Faça login com sua conta Google
4. Você receberá um código de autorização
5. Volte ao terminal e cole o código quando solicitado
6. Pronto! Login concluído

## ✅ Solução 3: Usar Token do PC Local

Se você já está logado no seu PC local, pode copiar o token:

### No seu PC local:
```bash
# Ver o token atual
cat ~/.config/firebase/tokens.json
```

### Na VM:
```bash
# Criar diretório de configuração
mkdir -p ~/.config/firebase

# Copiar o conteúdo do tokens.json do PC local para a VM
# (você precisará fazer isso manualmente via copy/paste ou scp)
```

Depois, você pode usar o token diretamente:
```bash
export PATH=$PATH:/home/ubuntu/.nvm/versions/node/v22.21.1/bin
firebase use --token <SEU_TOKEN>
```

## ✅ Solução 4: Login via Token CI (Para automação)

Se você precisa de um token para scripts/CI:

```bash
export PATH=$PATH:/home/ubuntu/.nvm/versions/node/v22.21.1/bin
firebase login:ci --no-localhost
```

Este comando também requer interação, mas gera um token que pode ser usado em scripts.

## 🔍 Verificar Login

Após fazer login, verifique:

```bash
export PATH=$PATH:/home/ubuntu/.nvm/versions/node/v22.21.1/bin

# Ver contas logadas
firebase login:list

# Ver projetos disponíveis
firebase projects:list

# Selecionar um projeto
firebase use <project-id>
```

## 📝 Exemplo Completo

```bash
# 1. Configurar PATH
export PATH=$PATH:/home/ubuntu/.nvm/versions/node/v22.21.1/bin

# 2. Fazer login
firebase login --no-localhost

# 3. (Abrir URL no navegador, fazer login, copiar código)

# 4. (Colar código no terminal)

# 5. Verificar
firebase login:list
firebase projects:list
```

## 🚀 Depois do Login

Após fazer login, você pode usar o Firebase Test Lab:

```bash
# Testar app Android
firebase test android run \
  --app /workspace/b/outputs/apk/debug/app-debug.apk \
  --device model=Pixel2,version=28 \
  --timeout 5m
```

## ❓ Problemas Comuns

### "Cannot run login in non-interactive mode"
- **Solução**: Execute o comando em um terminal interativo (não via script automatizado)

### "Command not found: firebase"
- **Solução**: Configure o PATH:
  ```bash
  export PATH=$PATH:/home/ubuntu/.nvm/versions/node/v22.21.1/bin
  ```

### Token expirado
- **Solução**: Faça login novamente com `firebase login --no-localhost`

## 📚 Referências

- [Firebase CLI Documentation](https://firebase.google.com/docs/cli)
- [Firebase Authentication](https://firebase.google.com/docs/cli#authentication)
