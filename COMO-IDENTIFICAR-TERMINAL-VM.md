# 🖥️ Como Identificar Terminal Local vs VM no Cursor

## 🔍 Identificação Rápida

### Terminal da VM (Cursor)
Quando você abre um terminal no Cursor, ele **JÁ É** o terminal da VM! 

**Características:**
- ✅ Você está no workspace `/workspace`
- ✅ Hostname geralmente é algo como `ubuntu`, `cursor`, ou similar
- ✅ Você tem acesso aos arquivos do projeto
- ✅ Comandos como `ls /workspace` funcionam

### Terminal Local (Seu PC)
- ❌ Não tem acesso ao `/workspace`
- ❌ Hostname do seu PC (ex: `seu-pc`, `DESKTOP-XXX`, etc)
- ❌ Não vê os arquivos do projeto Cursor

## 🧪 Como Verificar

Execute estes comandos no terminal:

```bash
# 1. Ver onde você está
pwd

# 2. Ver hostname
hostname

# 3. Ver usuário
whoami

# 4. Ver se o workspace existe
ls /workspace
```

### Resultado Esperado na VM:
```
pwd
/workspace

hostname
ubuntu  (ou cursor, ou similar)

whoami
ubuntu  (ou seu usuário na VM)

ls /workspace
app/  core/  data/  ... (seus arquivos do projeto)
```

## 📍 Como Abrir Terminal no Cursor

### Método 1: Atalho de Teclado
- **Windows/Linux**: `Ctrl + `` (Ctrl + crase/backtick)
- **Mac**: `Cmd + `` (Cmd + crase/backtick)

### Método 2: Menu
- **View** → **Terminal** (ou **Terminal** → **New Terminal**)

### Método 3: Command Palette
- `Ctrl+Shift+P` (ou `Cmd+Shift+P` no Mac)
- Digite: "Terminal: Create New Terminal"
- Enter

## ✅ Confirmação: Você Está na VM?

Execute este comando para confirmar:

```bash
# Script de verificação
echo "=== VERIFICAÇÃO DE AMBIENTE ==="
echo "Diretório atual: $(pwd)"
echo "Hostname: $(hostname)"
echo "Usuário: $(whoami)"
echo "Workspace existe: $([ -d /workspace ] && echo 'SIM ✅' || echo 'NÃO ❌')"
echo "Firebase CLI: $(which firebase 2>/dev/null && echo 'Instalado ✅' || echo 'Não encontrado ❌')"
```

### Se você ver:
- ✅ `/workspace` como diretório
- ✅ Workspace existe: SIM ✅
- ✅ Firebase CLI: Instalado ✅

**Então você está na VM!** 🎉

## 🚀 Executar Comandos Firebase

Agora que você sabe que está na VM, execute:

```bash
# 1. Configurar PATH (se necessário)
export PATH=$PATH:/home/ubuntu/.nvm/versions/node/v22.21.1/bin

# 2. Verificar Firebase
firebase --version

# 3. Fazer login
firebase login --no-localhost
```

## 🔧 Dica: Criar Alias

Para facilitar, adicione ao seu `~/.bashrc`:

```bash
# Adicionar ao final do ~/.bashrc
export PATH=$PATH:/home/ubuntu/.nvm/versions/node/v22.21.1/bin

# Alias útil
alias firebase-check='firebase --version && firebase login:list'
```

Depois execute:
```bash
source ~/.bashrc
```

## 📝 Resumo

| Característica | Terminal VM (Cursor) | Terminal Local |
|---------------|---------------------|----------------|
| Diretório | `/workspace` | Seu diretório local |
| Hostname | `ubuntu` ou similar | Nome do seu PC |
| Arquivos do projeto | ✅ Visíveis | ❌ Não visíveis |
| Firebase CLI | ✅ Instalado | ❌ Pode não estar |

## ❓ Ainda em Dúvida?

Execute este comando único:

```bash
[ -d /workspace ] && echo "✅ Você está na VM do Cursor!" || echo "❌ Você está no terminal local"
```

Se aparecer "✅ Você está na VM do Cursor!", pode prosseguir com o login do Firebase! 🚀
