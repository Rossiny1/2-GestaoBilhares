# 🔥 Firebase CLI no PowerShell (Solução Rápida)

## ✅ Solução Mais Rápida: Usar PowerShell Local

Como você está no PowerShell, vamos usar isso a seu favor!

## 📋 Passo a Passo no PowerShell

### 1. Verificar Node.js

```powershell
node --version
```

Se não tiver Node.js, instale de: https://nodejs.org/

### 2. Instalar Firebase CLI

```powershell
npm install -g firebase-tools
```

### 3. Fazer Login (Abre Navegador Automaticamente!)

```powershell
firebase login
```

**Isso vai:**
- ✅ Abrir seu navegador automaticamente
- ✅ Mostrar a página de login do Google
- ✅ Você faz login normalmente
- ✅ Autoriza o Firebase CLI
- ✅ Volta ao PowerShell automaticamente

**Não precisa copiar URL!** O PowerShell faz tudo automaticamente!

### 4. Verificar Login

```powershell
firebase login:list
firebase projects:list
```

## 🎯 Vantagens de Usar PowerShell Local

- ✅ Abre navegador automaticamente
- ✅ Não precisa copiar URL
- ✅ Processo mais simples
- ✅ Funciona perfeitamente

## 🔄 Depois, na VM

Depois de fazer login no PowerShell, você tem duas opções:

### Opção A: Fazer Login na VM Também

Quando conseguir acessar terminal da VM:
```bash
export PATH=$PATH:/home/ubuntu/.nvm/versions/node/v22.21.1/bin
firebase login --no-localhost
```

### Opção B: Usar Token

Se precisar usar token na VM:
```powershell
# No PowerShell, gere token
firebase login:ci
```

Depois use o token na VM.

## ✅ Pronto!

Execute no PowerShell:

```powershell
firebase login
```

E siga as instruções na tela! 🚀
