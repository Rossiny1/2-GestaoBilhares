# 🔐 FLUXO COMPLETO DO GOOGLE SIGN-IN - GESTÃO BILHARES

## 📋 **RESUMO DO FLUXO**

### **CENÁRIO 1: PRIMEIRO ACESSO (USUÁRIO NOVO)**

```
1. Usuário clica em "Entrar com Google"
2. Seleciona conta Google
3. Sistema tenta login online (Firebase)
4. Se online falhar → Tenta login offline
5. Se não encontrar usuário → Cria automaticamente
6. Usuário fica "PENDENTE DE APROVAÇÃO"
7. Admin aprova via "Gerenciar Colaboradores"
8. Usuário pode fazer login normalmente
```

### **CENÁRIO 2: USUÁRIO JÁ CADASTRADO**

```
1. Usuário clica em "Entrar com Google"
2. Sistema encontra perfil existente
3. Verifica se está aprovado
4. Se aprovado → Login direto
5. Se não aprovado → "Aguardando aprovação"
```

### **CENÁRIO 3: MODO OFFLINE**

```
1. Usuário clica em "Entrar com Google"
2. Sistema detecta sem internet
3. Verifica banco local
4. Se encontrado e aprovado → Login offline
5. Se não encontrado → Cria novo perfil pendente
```

## 🔧 **CONFIGURAÇÃO NECESSÁRIA**

### **1. Firebase Console**
- ✅ Google Sign-In ativado
- ✅ Web Client ID configurado
- ✅ google-services.json atualizado

### **2. Código Android**
- ✅ Web Client ID no LoginFragment
- ✅ Dependência play-services-auth
- ✅ AuthViewModel com lógica híbrida

## 📱 **MENSAGENS PARA O USUÁRIO**

### **Sucesso Online:**
- "Login realizado com sucesso!"

### **Sucesso Offline:**
- "Login realizado (modo offline)"

### **Novo Usuário:**
- "Conta criada com sucesso! Aguarde aprovação do administrador."

### **Pendente Aprovação:**
- "Sua conta está aguardando aprovação do administrador."

### **Erro:**
- "Erro no login com Google: [detalhes]"

## 👨‍💼 **FLUXO DO ADMINISTRADOR**

### **1. Aprovar Colaborador**
```
1. Acessar "Gerenciar Colaboradores"
2. Ver lista de "Pendentes"
3. Clicar em "Aprovar"
4. Definir credenciais de acesso
5. Salvar
```

### **2. Configurar Metas**
```
1. Editar colaborador
2. Clicar "Gerenciar Metas"
3. Definir metas por ciclo/rota
4. Salvar
```

## 🔄 **SINCRONIZAÇÃO**

### **Online → Offline:**
- Dados do Firebase sincronizados com banco local

### **Offline → Online:**
- Dados locais enviados para Firebase quando conexão restaurada

## ⚠️ **PONTOS IMPORTANTES**

1. **Primeiro acesso sempre cria perfil pendente**
2. **Aprovação obrigatória pelo admin**
3. **Funciona online e offline**
4. **Dados sincronizados automaticamente**
5. **Logs detalhados para debug**

## 🐛 **SOLUÇÃO DE PROBLEMAS**

### **Erro "Google Sign-In falhou":**
1. Verificar Web Client ID
2. Verificar google-services.json
3. Verificar conectividade
4. Verificar logs do Logcat

### **Usuário não aparece na lista:**
1. Verificar se foi criado no banco local
2. Verificar logs de criação
3. Verificar se email está correto

### **Aprovação não funciona:**
1. Verificar se colaborador existe
2. Verificar se admin tem permissões
3. Verificar logs de aprovação
