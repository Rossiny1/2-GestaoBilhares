# 🔐 Acesso SSH à VM do Cursor

## ⚠️ Informação Importante

A VM do Cursor **NÃO é acessível via SSH externo** por padrão. Ela é gerenciada automaticamente pelo Cursor através de uma conexão remota interna.

## 🔍 Informações da VM

### Dados Coletados:

- **Hostname**: `cursor`
- **Usuário**: `ubuntu`
- **Sistema**: Linux (container Docker em VM KVM)
- **Workspace**: `/workspace`

### IP Interno:

A VM tem um IP interno, mas ele **não é acessível externamente** porque:
- A VM está em uma rede privada gerenciada pelo Cursor
- Não há servidor SSH configurado para acesso externo
- O acesso é feito através do protocolo próprio do Cursor

## ✅ Como Acessar a VM

### Opção 1: Terminal Integrado do Cursor (Recomendado)

O Cursor já fornece acesso à VM através do terminal integrado:

1. **Abra terminal no Cursor**: `Ctrl+Shift+``
2. **Configure para bash**: Command Palette → `Terminal: Select Default Profile` → Escolha `Git Bash` ou `WSL`
3. **Você já está na VM!**

### Opção 2: Verificar Conexão Remota

1. **Olhe na barra inferior** do Cursor
2. Deve aparecer: `SSH: cursor` ou `Remote: cursor`
3. Isso indica que você está conectado à VM

### Opção 3: Command Palette

1. **Command Palette**: `Ctrl+Shift+P`
2. **Digite**: `Remote-SSH: Connect to Host`
3. Se houver configuração, aparecerá a opção

## 🚫 Por Que Não Há SSH Externo?

A VM do Cursor é:
- **Gerenciada automaticamente** pelo Cursor
- **Isolada em rede privada** para segurança
- **Acessível apenas** através do protocolo do Cursor
- **Não exposta** para acesso SSH externo

## ✅ Solução: Usar Terminal do Cursor

Como você precisa executar comandos na VM, a melhor solução é:

### 1. Configurar Terminal para Bash

```bash
# No Cursor, Command Palette (Ctrl+Shift+P)
# Digite: Terminal: Select Default Profile
# Escolha: Git Bash ou WSL
```

### 2. Verificar Conexão

```bash
pwd
# Deve mostrar: /workspace

hostname
# Deve mostrar: cursor
```

### 3. Executar Comandos Firebase

```bash
export PATH=$PATH:/home/ubuntu/.nvm/versions/node/v22.21.1/bin
firebase login --no-localhost 2>&1 | tee /workspace/firebase-url-gerada.txt
```

## 🔧 Se Precisar de Acesso SSH Real

Se você realmente precisar de acesso SSH externo, você precisaria:

1. **Configurar servidor SSH** na VM (não recomendado/possível)
2. **Expor porta** através do Cursor (não suportado)
3. **Usar túnel SSH** (complexo e não necessário)

**Mas isso não é necessário!** O terminal do Cursor já fornece acesso completo à VM.

## 📋 Resumo

- ❌ **Não há IP/Senha SSH externo** - a VM não é acessível via SSH externo
- ✅ **Use o terminal do Cursor** - já está conectado à VM
- ✅ **Configure para bash** - Command Palette → Select Default Profile
- ✅ **Execute comandos normalmente** - você já está na VM!

## 🎯 Próximo Passo

1. **Configure terminal para bash** (se ainda não fez)
2. **Execute o comando Firebase** no terminal do Cursor
3. **A URL será salva** no arquivo `firebase-url-gerada.txt`

**Você não precisa de SSH externo - o Cursor já fornece acesso à VM!** 🚀
