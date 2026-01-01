# 🔗 Solução Definitiva: URL Completa do Firebase

## ⚠️ Situação Atual

- Terminal do Cursor abre PowerShell (Windows local)
- Não consegue acessar terminal da VM diretamente
- URL do Firebase aparece truncada com "..."

## ✅ Solução 1: Mudar Terminal Padrão do Cursor

### Passo a Passo:

1. **Abra Command Palette**: `Ctrl+Shift+P` (ou `Cmd+Shift+P`)

2. **Digite**: `Terminal: Select Default Profile`

3. **Escolha**: 
   - `Git Bash` (se tiver Git instalado)
   - `WSL` (se tiver WSL instalado)
   - Ou qualquer perfil Linux disponível

4. **Abra novo terminal**: `Ctrl+Shift+`` (ou View → Terminal)

5. **Agora execute**:
   ```bash
   export PATH=$PATH:/home/ubuntu/.nvm/versions/node/v22.21.1/bin
   firebase login --no-localhost 2>&1 | tee ~/firebase-url.txt
   ```

6. **Veja a URL completa**:
   ```bash
   cat ~/firebase-url.txt | grep "https://"
   ```

## ✅ Solução 2: Usar PowerShell Local (Mais Rápido)

### No PowerShell do seu PC:

```powershell
# 1. Instalar Firebase CLI (se não tiver)
npm install -g firebase-tools

# 2. Fazer login (abre navegador automaticamente)
firebase login

# 3. Depois copiar token para VM
firebase login:ci
```

Isso gera um **token** que você pode usar na VM.

## ✅ Solução 3: Gerar URL Manualmente (Método Alternativo)

Como não consigo gerar automaticamente, você pode:

### Opção A: Copiar URL Truncada e Completar

1. **Copie a URL truncada** do terminal (mesmo com "...")
2. **Cole em um editor de texto** (Notepad, VS Code)
3. A URL pode estar completa lá, apenas o terminal não mostra

### Opção B: Usar Navegador para Gerar URL

1. Acesse: https://console.firebase.google.com/
2. Faça login
3. Vá em Configurações do Projeto → Contas de Serviço
4. Gere uma nova chave privada
5. Use essa chave na VM

## ✅ Solução 4: Configurar Cursor para Usar Terminal Remoto

### Verificar se Cursor está em modo remoto:

1. **Olhe na barra inferior** do Cursor
2. Deve aparecer algo como: `SSH: cursor` ou `Remote`
3. Se não aparecer, o Cursor pode não estar conectado à VM

### Se não estiver conectado:

1. **Command Palette**: `Ctrl+Shift+P`
2. **Digite**: `Remote-SSH: Connect to Host`
3. **Ou**: `Remote: Connect to Host`
4. Configure a conexão com a VM

## 🎯 Solução Mais Prática (Recomendada)

### Use o PowerShell Local:

1. **No PowerShell do seu PC**, execute:
   ```powershell
   npm install -g firebase-tools
   firebase login
   ```

2. Isso abre o navegador automaticamente e você faz login

3. **Depois, na VM**, você pode usar o token ou fazer login novamente

## 📋 Instruções Detalhadas para PowerShell

### Passo 1: Instalar Firebase CLI no Windows

```powershell
# Verificar se Node.js está instalado
node --version

# Se não estiver, instale Node.js primeiro
# Depois instale Firebase CLI
npm install -g firebase-tools
```

### Passo 2: Fazer Login

```powershell
firebase login
```

Isso vai:
1. Abrir seu navegador automaticamente
2. Pedir para fazer login
3. Autorizar o Firebase CLI
4. Voltar ao PowerShell automaticamente

### Passo 3: Verificar

```powershell
firebase login:list
firebase projects:list
```

## 🔄 Depois, na VM

Depois de fazer login no PowerShell local, você pode:

1. **Copiar o token** (se necessário)
2. **Ou fazer login na VM também** (recomendado)

## 💡 Dica: Configurar Terminal Padrão

Para sempre usar bash no Cursor:

1. **Settings** (`Ctrl+,`)
2. **Procure**: `terminal.integrated.defaultProfile.windows`
3. **Configure**: `"Git Bash"` ou `"WSL"`
4. **Salve**

Agora sempre abrirá bash em vez de PowerShell!
