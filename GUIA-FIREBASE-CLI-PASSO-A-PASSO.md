# 🔥 Guia Passo a Passo: Configuração do Firebase CLI

## ✅ Status Atual

- [x] Firebase CLI instalado (versão 15.1.0)
- [x] PATH configurado no `~/.bashrc`
- [ ] Login no Firebase (próximo passo)
- [ ] Projeto Firebase selecionado
- [ ] Teste do Firebase Test Lab

---

## 📋 Passo 1: Verificar Instalação

Execute no terminal do Cursor:

```bash
# 1. Configurar PATH (se necessário)
export PATH=$PATH:/home/ubuntu/.nvm/versions/node/v22.21.1/bin

# 2. Verificar versão
firebase --version
```

**Resultado esperado**: `15.1.0`

---

## 🔐 Passo 2: Fazer Login no Firebase

### Opção A: Login Interativo (Recomendado)

Execute:

```bash
export PATH=$PATH:/home/ubuntu/.nvm/versions/node/v22.21.1/bin
firebase login --no-localhost
```

**O que vai acontecer:**

1. Uma URL será exibida, algo como:
   ```
   Visit this URL on this device:
   https://accounts.google.com/o/oauth2/auth?client_id=...
   
   Enter authorization code:
   ```

2. **Copie a URL** e abra no seu navegador local (não na VM)

3. Faça login com sua conta Google (a mesma que você usa no PC local)

4. Após o login, você receberá um **código de autorização**

5. **Cole o código** no terminal e pressione Enter

6. Pronto! Login concluído ✅

### Opção B: Usar Token do PC Local (Alternativa)

Se você já está logado no seu PC local:

**No seu PC local:**
```bash
# Ver token atual
cat ~/.config/firebase/tokens.json
```

**Na VM (via terminal do Cursor):**
```bash
# Criar diretório
mkdir -p ~/.config/firebase

# Copiar o conteúdo do tokens.json do PC local
# (você precisará fazer isso manualmente via copy/paste)
```

---

## ✅ Passo 3: Verificar Login

Após fazer login, verifique:

```bash
export PATH=$PATH:/home/ubuntu/.nvm/versions/node/v22.21.1/bin

# Ver contas logadas
firebase login:list
```

**Resultado esperado**: Sua conta Google listada

---

## 📁 Passo 4: Listar Projetos Firebase

```bash
export PATH=$PATH:/home/ubuntu/.nvm/versions/node/v22.21.1/bin

# Listar todos os projetos
firebase projects:list
```

Isso mostrará todos os projetos Firebase associados à sua conta.

---

## 🎯 Passo 5: Selecionar Projeto Firebase

Se você já tem um projeto Firebase:

```bash
export PATH=$PATH:/home/ubuntu/.nvm/versions/node/v22.21.1/bin

# Selecionar projeto (substitua <project-id> pelo ID do seu projeto)
firebase use <project-id>

# Ou adicionar alias
firebase use --add
```

**Exemplo:**
```bash
firebase use meu-projeto-android
```

---

## 🧪 Passo 6: Testar Firebase Test Lab

Agora você pode testar seu app Android no Firebase Test Lab:

```bash
export PATH=$PATH:/home/ubuntu/.nvm/versions/node/v22.21.1/bin

# Verificar se o APK existe
ls -lh /workspace/b/outputs/apk/debug/app-debug.apk

# Executar teste no Test Lab
firebase test android run \
  --app /workspace/b/outputs/apk/debug/app-debug.apk \
  --device model=Pixel2,version=28 \
  --device model=NexusLowRes,version=25 \
  --timeout 5m
```

**Dispositivos disponíveis:**
- `Pixel2` (Android 28)
- `Pixel3` (Android 28, 29)
- `NexusLowRes` (Android 25)
- `Nexus5X` (Android 26, 27, 28)

---

## 🔧 Passo 7: Configuração Permanente (Opcional)

Para não precisar exportar o PATH toda vez, adicione ao `~/.bashrc`:

```bash
# Verificar se já está adicionado
grep "nvm/versions/node" ~/.bashrc

# Se não estiver, adicionar (já foi feito, mas verifique)
echo 'export PATH=$PATH:/home/ubuntu/.nvm/versions/node/v22.21.1/bin' >> ~/.bashrc

# Recarregar
source ~/.bashrc
```

Ou crie um alias:

```bash
# Adicionar ao ~/.bashrc
echo 'alias firebase-cli="export PATH=\$PATH:/home/ubuntu/.nvm/versions/node/v22.21.1/bin && firebase"' >> ~/.bashrc
source ~/.bashrc

# Usar depois:
firebase-cli --version
```

---

## 📝 Comandos Úteis

```bash
# Verificar versão
firebase --version

# Ver ajuda
firebase --help

# Ver comandos disponíveis
firebase help

# Ver projetos
firebase projects:list

# Ver projeto atual
firebase use

# Ver contas logadas
firebase login:list

# Fazer logout (se necessário)
firebase logout

# Ver informações do projeto
firebase projects:list
```

---

## 🚨 Troubleshooting

### Problema: "Command not found: firebase"

**Solução:**
```bash
export PATH=$PATH:/home/ubuntu/.nvm/versions/node/v22.21.1/bin
```

### Problema: "No authorized accounts"

**Solução:** Faça login novamente:
```bash
firebase login --no-localhost
```

### Problema: "Cannot run login in non-interactive mode"

**Solução:** Execute o comando em um terminal interativo (não via script automatizado). O terminal do Cursor é interativo, então deve funcionar.

### Problema: Token expirado

**Solução:** Faça login novamente:
```bash
firebase login --no-localhost
```

---

## ✅ Checklist de Configuração

- [ ] Firebase CLI instalado e funcionando
- [ ] Login realizado com sucesso
- [ ] Projetos Firebase listados
- [ ] Projeto selecionado (se aplicável)
- [ ] Teste do Firebase Test Lab executado (opcional)

---

## 🎯 Próximos Passos Após Configuração

1. **Testar app no Firebase Test Lab**
2. **Configurar CI/CD** para testes automatizados
3. **Integrar com GitHub Actions** (se usar)
4. **Configurar notificações** de resultados de teste

---

## 📚 Referências

- [Firebase CLI Documentation](https://firebase.google.com/docs/cli)
- [Firebase Test Lab](https://firebase.google.com/docs/test-lab)
- [Firebase Authentication](https://firebase.google.com/docs/cli#authentication)

---

## 💡 Dica Final

Crie um script helper para facilitar:

```bash
cat > ~/firebase-helper.sh << 'EOF'
#!/bin/bash
export PATH=$PATH:/home/ubuntu/.nvm/versions/node/v22.21.1/bin
firebase "$@"
EOF

chmod +x ~/firebase-helper.sh

# Usar:
~/firebase-helper.sh --version
~/firebase-helper.sh login:list
```

Ou adicione ao PATH permanentemente (já foi feito no `~/.bashrc`).

---

**Agora você está pronto para começar! Execute o Passo 2 para fazer login.** 🚀
