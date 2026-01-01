# Firebase CLI - Instalação e Configuração

## ✅ Instalação Concluída

O Firebase CLI foi instalado com sucesso na VM do Cursor!

### Detalhes da Instalação

- **Versão instalada**: 15.1.0
- **Localização**: `/home/ubuntu/.nvm/versions/node/v22.21.1/bin/firebase`
- **Node.js**: v22.21.1
- **npm**: 10.9.4

### Configuração do PATH

O PATH foi atualizado no `~/.bashrc` para incluir o diretório bin do Node.js. Em novas sessões de terminal, o comando `firebase` estará disponível automaticamente.

Para usar na sessão atual:
```bash
export PATH=$PATH:/home/ubuntu/.nvm/versions/node/v22.21.1/bin
```

## 🔐 Próximos Passos - Fazer Login

Como você já tem o Firebase configurado no seu PC local, você pode:

### Opção 1: Fazer Login Interativo (Recomendado)
```bash
export PATH=$PATH:/home/ubuntu/.nvm/versions/node/v22.21.1/bin
firebase login
```

Isso abrirá uma página no navegador para autenticação. Como estamos em uma VM, você pode:
- Usar o token de autenticação
- Ou fazer login via token do seu PC local

### Opção 2: Usar Token do PC Local

Se você já está logado no seu PC local, pode copiar o token:

**No seu PC local:**
```bash
# Ver tokens salvos
cat ~/.config/firebase/tokens.json
```

**Na VM:**
```bash
# Criar diretório de configuração
mkdir -p ~/.config/firebase

# Colar o token no arquivo (você precisará fazer isso manualmente)
# ou usar firebase login:ci para gerar um novo token
```

### Opção 3: Login com Token CI (Para automação)
```bash
export PATH=$PATH:/home/ubuntu/.nvm/versions/node/v22.21.1/bin
firebase login:ci
```

Isso gera um token que pode ser usado em scripts e CI/CD.

## 📋 Comandos Úteis

```bash
# Verificar versão
firebase --version

# Ver projetos disponíveis
firebase projects:list

# Inicializar projeto Firebase no diretório atual
firebase init

# Fazer deploy
firebase deploy

# Ver ajuda
firebase --help

# Listar comandos disponíveis
firebase help
```

## 🧪 Usar Firebase Test Lab

Agora que o Firebase CLI está instalado, você pode usar o Firebase Test Lab para testar seu app Android:

```bash
# Fazer login primeiro
firebase login

# Executar teste no Test Lab
firebase test android run \
  --app /workspace/b/outputs/apk/debug/app-debug.apk \
  --device model=Pixel2,version=28 \
  --device model=NexusLowRes,version=25 \
  --timeout 5m
```

### Verificar Projetos Firebase

```bash
# Listar projetos
firebase projects:list

# Selecionar projeto
firebase use <project-id>
```

## 🔧 Troubleshooting

### Comando não encontrado
Se o comando `firebase` não for encontrado em uma nova sessão:
```bash
export PATH=$PATH:/home/ubuntu/.nvm/versions/node/v22.21.1/bin
```

Ou adicione ao `~/.bashrc` (já foi feito):
```bash
echo 'export PATH=$PATH:/home/ubuntu/.nvm/versions/node/v22.21.1/bin' >> ~/.bashrc
source ~/.bashrc
```

### Verificar instalação
```bash
which firebase
firebase --version
```

## 📚 Documentação

- Firebase CLI: https://firebase.google.com/docs/cli
- Firebase Test Lab: https://firebase.google.com/docs/test-lab
- Comandos disponíveis: `firebase help`

## ✅ Status

- [x] Firebase CLI instalado
- [x] PATH configurado
- [ ] Login realizado (próximo passo)
- [ ] Projeto Firebase configurado (se necessário)
