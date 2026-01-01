# 🔥 Login Firebase na VM - Guia Completo

## 🎯 Objetivo

Fazer login no Firebase usando `firebase login --no-localhost` na VM, copiar a URL completa e autorizar no navegador.

## ⚠️ Problema Atual

- Terminal do Cursor abre PowerShell local (Windows)
- Precisamos acessar o terminal da VM (Linux)
- URL aparece truncada com "..."

## ✅ Solução: Acessar Terminal da VM

### Método 1: Command Palette (Recomendado)

1. **Pressione**: `Ctrl+Shift+P` (ou `Cmd+Shift+P` no Mac)

2. **Digite**: `Terminal: Select Default Profile`

3. **Escolha**: 
   - `Git Bash` (se tiver Git instalado)
   - `WSL` (se tiver WSL instalado)
   - Ou qualquer perfil que mostre "bash" ou "Linux"

4. **Abra novo terminal**: `Ctrl+Shift+`` (ou View → Terminal)

5. **Verifique que está na VM**:
   ```bash
   pwd
   ```
   Deve mostrar: `/workspace` (não `C:\...`)

### Método 2: Verificar Conexão Remota

1. **Olhe na barra inferior** do Cursor
2. Deve aparecer: `SSH: cursor` ou `Remote: cursor`
3. Se não aparecer, o Cursor pode não estar conectado à VM

### Método 3: Configurar Terminal Padrão

1. **Abra Settings**: `Ctrl+,` (ou `Cmd+,`)
2. **Procure**: `terminal.integrated.defaultProfile.windows`
3. **Configure para**: `"Git Bash"` ou `"WSL"`
4. **Salve e reinicie o terminal**

## 🔥 Passo a Passo: Login Firebase

### Passo 1: Acessar Terminal da VM

Siga um dos métodos acima para abrir terminal bash/Linux.

### Passo 2: Verificar que Está na VM

```bash
pwd
hostname
```

**Deve mostrar**:
- `pwd`: `/workspace`
- `hostname`: `cursor` ou similar

### Passo 3: Configurar PATH

```bash
export PATH=$PATH:/home/ubuntu/.nvm/versions/node/v22.21.1/bin
```

### Passo 4: Executar Login e Salvar URL

```bash
firebase login --no-localhost 2>&1 | tee ~/firebase-login-output.txt
```

**O que isso faz**:
- Executa o login normalmente
- **Salva TUDO** (incluindo URL completa) no arquivo `~/firebase-login-output.txt`
- Você pode ver a URL completa depois, mesmo que apareça truncada no terminal

### Passo 5: Ver URL Completa

**Opção A: Ver arquivo completo**
```bash
cat ~/firebase-login-output.txt
```

**Opção B: Extrair apenas a URL**
```bash
grep -oE "https://[^[:space:]]+" ~/firebase-login-output.txt | head -1
```

**Opção C: Abrir arquivo no Cursor**
- No Cursor, abra o arquivo: `~/firebase-login-output.txt`
- Ou: `firebase-login-output.txt` na raiz do workspace
- Procure pela linha que começa com `https://accounts.google.com/...`
- Copie a URL completa

### Passo 6: Autorizar no Navegador

1. **Copie a URL completa** do arquivo
2. **Cole no navegador** do seu notebook
3. **Faça login** com sua conta Google
4. **Copie o código** de autorização que aparecer
5. **Volte ao terminal** e cole o código quando solicitado
6. **Pressione Enter**

### Passo 7: Verificar Login

```bash
firebase login:list
```

Deve mostrar sua conta Google logada! ✅

## 🛠️ Script Helper (Automático)

Criei um script que faz tudo automaticamente:

```bash
./gerar-url-completa-firebase.sh
```

Este script:
1. Executa o login
2. Salva tudo em arquivo
3. Mostra a URL completa automaticamente

## 📋 Resumo Rápido

```bash
# 1. Acessar terminal da VM (bash, não PowerShell)
# 2. Configurar PATH
export PATH=$PATH:/home/ubuntu/.nvm/versions/node/v22.21.1/bin

# 3. Executar login e salvar
firebase login --no-localhost 2>&1 | tee ~/firebase-login-output.txt

# 4. Ver URL completa
cat ~/firebase-login-output.txt | grep "https://"

# 5. Copiar URL, abrir no navegador, fazer login, copiar código
# 6. Voltar ao terminal e colar código
# 7. Verificar
firebase login:list
```

## 🔍 Troubleshooting

### "Command not found: firebase"
```bash
export PATH=$PATH:/home/ubuntu/.nvm/versions/node/v22.21.1/bin
```

### "Cannot run login in non-interactive mode"
- Você precisa estar em um terminal **interativo** (bash)
- Não funciona em scripts automatizados
- Use o terminal do Cursor (bash), não PowerShell

### URL ainda aparece truncada
- Use o método com `tee` para salvar em arquivo
- A URL completa estará no arquivo, mesmo que apareça truncada no terminal

### Não consigo acessar terminal bash
- Tente: Command Palette → `Terminal: Select Default Profile` → Escolha bash/WSL
- Ou configure em Settings: `terminal.integrated.defaultProfile.windows`

## ✅ Checklist

- [ ] Terminal bash/Linux aberto (não PowerShell)
- [ ] PATH configurado
- [ ] Comando `firebase login --no-localhost` executado
- [ ] URL completa copiada do arquivo
- [ ] Login feito no navegador
- [ ] Código de autorização copiado
- [ ] Código colado no terminal
- [ ] Login verificado com `firebase login:list`

## 🚀 Pronto para Começar!

1. **Abra terminal bash** no Cursor (não PowerShell)
2. **Execute os comandos** acima
3. **Siga as instruções** passo a passo

Vamos tentar? 🎯
