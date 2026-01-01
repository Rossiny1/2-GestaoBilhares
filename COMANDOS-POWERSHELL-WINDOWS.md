# 💻 Comandos Firebase CLI para PowerShell (Windows Local)

## ⚠️ Importante

Estes comandos são para instalar/configurar Firebase CLI no seu **PC Windows local**. 

Para o projeto na VM do Cursor, use os comandos Linux no terminal do Cursor.

## 📦 Instalação no Windows (PowerShell)

### Pré-requisitos
- Node.js instalado no Windows
- npm disponível

### 1. Instalar Firebase CLI
```powershell
npm install -g firebase-tools
```

### 2. Verificar Instalação
```powershell
firebase --version
```

### 3. Fazer Login
```powershell
firebase login
```

Isso abrirá o navegador automaticamente para autenticação.

### 4. Verificar Login
```powershell
firebase login:list
```

### 5. Listar Projetos
```powershell
firebase projects:list
```

## 🔄 Diferenças: PowerShell vs Terminal Cursor

| Ação | PowerShell (Windows) | Terminal Cursor (VM) |
|------|---------------------|----------------------|
| Instalar | `npm install -g firebase-tools` | ✅ Já instalado |
| Login | `firebase login` | `firebase login --no-localhost` |
| PATH | Não precisa configurar | `export PATH=$PATH:...` |
| Scripts | `.ps1` | `.sh` |

## 🎯 Quando Usar Cada Um

### Use PowerShell (Windows) se:
- Você quer Firebase CLI no PC local
- Você quer gerenciar projetos Firebase do Windows
- Você não está trabalhando no projeto do Cursor

### Use Terminal Cursor (VM) se:
- Você está desenvolvendo o projeto Android
- Você quer testar no Firebase Test Lab
- Você quer usar as ferramentas da VM

## ✅ Recomendação

**Para este projeto, use o Terminal do Cursor!** 

O Firebase CLI já está instalado e configurado na VM. Basta:
1. Abrir terminal no Cursor (`Ctrl + ``)
2. Executar os comandos Linux que forneci
