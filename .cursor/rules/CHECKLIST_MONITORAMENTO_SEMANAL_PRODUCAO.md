# 📊 CHECKLIST DE MONITORAMENTO SEMANAL - PRODUÇÃO

> **App:** Gestão de Bilhares  
> **Versão Security Rules:** 27/01/2026 (Estrutura Hierárquica)  
> **Responsável:** Time de Desenvolvimento  
> **Frequência:** Semanal (toda segunda-feira)

---

## 🎯 OBJETIVO

Garantir saúde, performance e segurança do app em produção através de monitoramento sistemático da sincronização Firebase e Security Rules.

---

## 📅 CHECKLIST SEMANAL

### **Semana de: ___/___/2026**

---

## 🔥 **1. FIREBASE CONSOLE - VIOLATIONS (15 min)**

**Link:** https://console.firebase.google.com/project/gestaobilhares/firestore/rules

### **1.1 Verificar Violations**

```markdown
[ ] Acessar: Firebase Console → Firestore Database → Rules → Violations tab

[ ] Verificar quantidade de violations nos últimos 7 dias:
    - 0-10 violations: ✅ NORMAL (testar edge cases ocasionais)
    - 11-50 violations: ⚠️ ATENÇÃO (investigar padrão)
    - 50+ violations: 🚨 CRÍTICO (ação imediata necessária)

[ ] Registrar número exato: _______ violations

[ ] Se > 10 violations, anotar:
    - Collection mais afetada: _________________
    - Operação bloqueada (read/write/delete): _________________
    - Usuário/UID mais frequente: _________________
```

### **1.2 Analisar Padrões de Violations**

```markdown
[ ] Violations concentradas em uma collection específica?
    → SIM: Collection _____________ (investigar Security Rules)
    → NÃO: Violations distribuídas (normal, edge cases)

[ ] Violations de um único usuário/rota?
    → SIM: Usuário _____________ pode ter permissões incorretas
    → NÃO: Múltiplos usuários (pode ser regra muito restritiva)

[ ] Horário concentrado das violations?
    → SIM: Horário _______ (pode ser operação em lote/sync)
    → NÃO: Distribuído (padrão normal de uso)
```

### **1.3 Ações Recomendadas**

```markdown
[ ] Se padrão identificado:
    → Revisar Security Rules para collection afetada
    → Verificar permissões do usuário no Firestore
    → Ajustar rules se necessário e testar

[ ] Se sem padrão claro (< 10 violations):
    → ✅ Normal, apenas documentar e seguir
```

---

## 📊 **2. FIRESTORE DATABASE - SAÚDE DOS DADOS (10 min)**

**Link:** https://console.firebase.google.com/project/gestaobilhares/firestore/data

### **2.1 Verificar Estrutura Hierárquica**

```markdown
[ ] Navegar para: empresas/empresa_001/

[ ] Verificar estrutura correta:
    ✅ colaboradores/{uid} → Documentos existem
    ✅ entidades/rotas/items/{id} → Documentos existem
    ✅ entidades/clientes/items/{id} → Documentos existem
    ✅ entidades/mesas/items/{id} → Documentos existem
    ✅ entidades/acertos/items/{id} → Documentos existem
    ✅ entidades/despesas/items/{id} → Documentos existem

[ ] ❌ VERIFICAR SE NÃO EXISTE documentos na RAIZ (fora de empresas/):
    - Se existir: Collections incorretas criadas → LIMPAR IMEDIATAMENTE
    - Comando: node import-data/limpar-dados-incorretos.js
```

### **2.2 Contagem de Documentos (Crescimento)**

```markdown
[ ] Anotar quantidade de documentos em cada collection:

    Collection          | Semana Atual | Semana Anterior | Variação
    --------------------|--------------|-----------------|----------
    Colaboradores       | _________    | _________       | _____
    Rotas              | _________    | _________       | _____
    Clientes           | _________    | _________       | _____
    Mesas              | _________    | _________       | _____
    Acertos            | _________    | _________       | _____
    Despesas           | _________    | _________       | _____

[ ] Crescimento anormal detectado? (> 50% em uma semana)
    → SIM: Collection _____________ (investigar causa)
    → NÃO: Crescimento orgânico normal
```

### **2.3 Verificar Campos Obrigatórios**

```markdown
[ ] Selecionar 3 documentos aleatórios de cada collection

[ ] Verificar campos obrigatórios presentes:
    ✅ empresa_id (todas as entidades)
    ✅ rota_id (clientes, mesas, acertos, despesas)
    ✅ dataUltimaAtualizacao (todas as entidades)
    ✅ firebase_uid (colaboradores)

[ ] Se algum campo faltando:
    → Documentar ID do documento: _________________
    → Investigar versão do app que criou (campo version?)
```

---

## 🚀 **3. PERFORMANCE E OPERAÇÕES (10 min)**

**Link:** https://console.firebase.google.com/project/gestaobilhares/firestore/usage

### **3.1 Métricas de Uso**

```markdown
[ ] Acessar: Firebase Console → Firestore Database → Usage

[ ] Registrar métricas dos últimos 7 dias:

    Métrica                     | Valor        | Status
    ----------------------------|--------------|----------
    Document Reads              | _________    | [ ] OK / [ ] Alto
    Document Writes             | _________    | [ ] OK / [ ] Alto
    Document Deletes            | _________    | [ ] OK / [ ] Alto
    Storage (MB)                | _________    | [ ] OK / [ ] Alto

[ ] Comparar com semana anterior:
    → Aumento > 30%: ⚠️ Investigar causa
    → Aumento < 30%: ✅ Crescimento normal
```

### **3.2 Identificar Queries Lentas**

```markdown
[ ] No app Android, verificar logs de performance:
    → adb logcat -s FirestoreSync:D | grep "took"

[ ] Queries demorando > 2 segundos?
    → SIM: Collection _____________ (considerar índice Firestore)
    → NÃO: Performance adequada

[ ] Se queries lentas detectadas:
    [ ] Acessar: Firebase Console → Firestore → Indexes
    [ ] Criar índice composto se recomendado pelo Firebase
```

---

## 🔐 **4. SEGURANÇA E MULTI-TENANCY (10 min)**

### **4.1 Teste Manual Multi-Tenancy**

```markdown
[ ] Login com 2 usuários de rotas diferentes:

    Usuário A:
    - Email: _________________
    - Rotas permitidas: _________________

    Usuário B:
    - Email: _________________
    - Rotas permitidas: _________________

[ ] Verificar isolamento:
    ✅ Usuário A vê APENAS clientes da rota A
    ✅ Usuário B vê APENAS clientes da rota B
    ✅ Usuário A NÃO consegue editar dados da rota B
    ✅ Usuário B NÃO consegue editar dados da rota A

[ ] Se isolamento falhar:
    → 🚨 CRÍTICO: Revisar belongsToUserRoute() nas Security Rules
    → Testar imediatamente com script: node import-data/testar-security-rules.js
```

### **4.2 Verificar Aprovações Pendentes**

```markdown
[ ] Acessar: empresas/empresa_001/colaboradores/

[ ] Filtrar por: aprovado == false

[ ] Quantidade de colaboradores aguardando aprovação: _______

[ ] Se > 5 pendentes há mais de 7 dias:
    → Notificar admin para revisar e aprovar/rejeitar
```

---

## 📱 **5. APP ANDROID - SAÚDE (15 min)**

### **5.1 Teste de Sincronização End-to-End**

```markdown
[ ] Abrir app Android em dispositivo de teste

[ ] Executar fluxo completo:
    1. [ ] Login com usuário teste
    2. [ ] Criar nova rota "Rota Teste ___/___"
    3. [ ] Criar novo cliente "Cliente Teste ___/___"
    4. [ ] Criar nova mesa #999
    5. [ ] Criar novo acerto R$ 100
    6. [ ] Criar nova despesa R$ 50

[ ] Verificar Firebase Console:
    ✅ Todos os 5 documentos apareceram no Firestore
    ✅ Campos obrigatórios presentes (empresa_id, rota_id)
    ✅ Timestamps corretos (dataUltimaAtualizacao)

[ ] Tempo de sincronização:
    → < 2 segundos: ✅ Excelente
    → 2-5 segundos: ⚠️ Aceitável (monitorar)
    → > 5 segundos: 🚨 Lento (investigar rede/rules)
```

### **5.2 Verificar Logs do App**

```bash
# PowerShell
adb logcat -s FirestoreSync:D FirebaseAuth:D *:E -d > logs_producao_semanal.txt
```

```markdown
[ ] Erros encontrados nos logs?
    → NÃO: ✅ App saudável
    → SIM: Anotar erros abaixo

    Erros encontrados:
    1. _________________________________________________
    2. _________________________________________________
    3. _________________________________________________

[ ] PERMISSION_DENIED encontrado?
    → 🚨 CRÍTICO: Seguir protocolo de correção emergencial
```

---

## 📈 **6. MÉTRICAS DE NEGÓCIO (5 min)**

### **6.1 Indicadores de Uso**

```markdown
[ ] Registrar indicadores da semana:

    Indicador                          | Valor
    -----------------------------------|----------
    Novos colaboradores criados        | _______
    Novos clientes cadastrados         | _______
    Acertos financeiros registrados    | _______
    Despesas registradas               | _______
    Rotas ativas                       | _______

[ ] Comparar com semana anterior:
    → Crescimento positivo: ✅ Adoção do app aumentando
    → Estável: ✅ Uso consistente
    → Queda > 20%: ⚠️ Investigar causa (bug? treinamento?)
```

---

## 🔄 **7. AÇÕES IDENTIFICADAS**

```markdown
### Problemas Identificados Esta Semana:

1. _________________________________________________
   Severidade: [ ] Crítico  [ ] Alto  [ ] Médio  [ ] Baixo
   Ação tomada: __________________________________________
   Responsável: _______________  Prazo: ___/___/2026

2. _________________________________________________
   Severidade: [ ] Crítico  [ ] Alto  [ ] Médio  [ ] Baixo
   Ação tomada: __________________________________________
   Responsável: _______________  Prazo: ___/___/2026
```

---

## ✅ **8. RESUMO DA SEMANA**

```markdown
[ ] Status Geral do App:
    [ ] 🟢 Saudável (nenhum problema crítico)
    [ ] 🟡 Atenção (problemas menores identificados)
    [ ] 🔴 Crítico (ação imediata necessária)

[ ] Principais conquistas:
    - _________________________________________________

[ ] Principais desafios:
    - _________________________________________________

[ ] Recomendações para próxima semana:
    - _________________________________________________
```

---

## 🚨 **PROTOCOLO DE EMERGÊNCIA**

### **PERMISSION_DENIED em massa:**

```bash
# Capturar logs
adb logcat -s FirebaseFirestore:D > logs_emergencia.txt

# Analisar
grep "PERMISSION_DENIED" logs_emergencia.txt -A 10
```

### **Dados na estrutura incorreta:**

```bash
cd import-data
node limpar-dados-incorretos.js
```

### **Rollback emergencial de Rules:**

```bash
cd C:\Users\Rossiny\Desktop\2-GestaoBilhares
Copy-Item firestore.rules.backup.27-01-2026 -Destination firestore.rules
firebase deploy --only firestore:rules
```

---

## 📚 **REFERÊNCIAS RÁPIDAS**

### **Links Úteis:**
- Firebase Console: https://console.firebase.google.com/project/gestaobilhares
- Firestore Rules: https://console.firebase.google.com/project/gestaobilhares/firestore/rules
- Documentação: `.cursor/rules/README.md`

### **Scripts Úteis:**
```bash
# Testar Security Rules
node import-data/testar-security-rules.js

# Limpar dados incorretos
node import-data/limpar-dados-incorretos.js

# Deploy de rules
node import-data/deploy-security-rules-v2.js
```

---

## 📝 **HISTÓRICO DE MONITORAMENTO**

| Semana        | Status | Violations | Principais Problemas | Ações Tomadas |
|---------------|--------|------------|----------------------|---------------|
| 27/01 - 02/02 | 🟢     | 0          | Nenhum               | Deploy inicial |
| 03/02 - 09/02 | ____   | ____       | ____                 | ____          |
| 10/02 - 16/02 | ____   | ____       | ____                 | ____          |

---

## 🎯 **OBJETIVOS DE LONGO PRAZO**

### **Mês 1 (Fevereiro/2026):**
- [ ] Zero violations críticas
- [ ] Tempo de sincronização < 2s
- [ ] 100% uptime do app
- [ ] Documentar casos extremos encontrados

### **Trimestre 1 (Jan-Mar/2026):**
- [ ] Implementar alertas automáticos (Firebase → Email)
- [ ] Testes automatizados com Firebase Emulator
- [ ] Otimizar queries lentas (índices compostos)
- [ ] Dashboard de métricas de negócio

---

**Checklist criado em:** 27/01/2026  
**Versão:** 1.0  
**Próxima revisão:** 27/02/2026

---

*Monitoramento sistemático = App saudável e usuários satisfeitos! 📊✅*
