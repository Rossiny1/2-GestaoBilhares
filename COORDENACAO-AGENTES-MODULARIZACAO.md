# 🤝 COORDENAÇÃO ENTRE AGENTES - MODULARIZAÇÃO

## 📋 VISÃO GERAL

Dois agentes trabalhando em paralelo para completar a modularização do projeto:

- **AGENTE 1**: Estrutura Domain e Repositories Especializados
- **AGENTE 2**: Refatoração AppRepository e Migração CicloAcertoRepository

## 🎯 DIVISÃO DE RESPONSABILIDADES

### **AGENTE 1 - ESTRUTURA DOMAIN**
**Arquivos que trabalha:**
- ✅ Criar pasta `data/repository/domain/`
- ✅ Mover repositories existentes para `domain/`
- ✅ Criar repositories faltantes em `domain/`
- ✅ Ajustar packages e imports dos repositories movidos/criados

**Arquivos que NÃO modifica:**
- ❌ `AppRepository.kt` (AGENTE 2 trabalha)
- ❌ `CicloAcertoRepository.kt` (AGENTE 2 trabalha)
- ❌ `RepositoryFactory.kt` (AGENTE 2 trabalha)
- ❌ Fragments/ViewModels (já migrados)

### **AGENTE 2 - REFATORAÇÃO APPREPOSITORY**
**Arquivos que trabalha:**
- ✅ `AppRepository.kt` (refatorar para delegar)
- ✅ `CicloAcertoRepository.kt` (migrar para usar AppRepository)
- ✅ `RepositoryFactory.kt` (atualizar para criar repositories especializados)
- ✅ 5 fragments que usam CicloAcertoRepository (simplificar instanciação)

**Arquivos que NÃO modifica:**
- ❌ Repositories na pasta `domain/` (AGENTE 1 trabalha)
- ❌ Não cria novos repositories (AGENTE 1 cria)
- ❌ Não move repositories (AGENTE 1 move)

## ⚠️ REGRAS DE COORDENAÇÃO

### **1. ORDEM DE EXECUÇÃO**

**FASE 1: AGENTE 1 trabalha primeiro**
- Criar estrutura `domain/`
- Mover/criar repositories especializados
- **AGENTE 2 aguarda** esta fase completar

**FASE 2: AGENTE 2 trabalha depois**
- Após AGENTE 1 completar, AGENTE 2 pode começar
- Refatorar AppRepository para usar repositories de `domain/`
- Migrar CicloAcertoRepository

### **2. PONTOS DE SINCRONIZAÇÃO**

**Checkpoint 1: AGENTE 1 completa estrutura**
- ✅ Pasta `domain/` criada
- ✅ Repositories movidos/criados
- ✅ Packages ajustados
- **AGENTE 2 pode iniciar**

**Checkpoint 2: AGENTE 2 completa refatoração**
- ✅ AppRepository delegando
- ✅ CicloAcertoRepository migrado
- ✅ Fragments atualizados
- **Ambos podem validar juntos**

### **3. CONFLITOS A EVITAR**

**NÃO HAVERÁ CONFLITOS porque:**
- ✅ AGENTE 1 trabalha em arquivos novos/movidos (domain/)
- ✅ AGENTE 2 trabalha em arquivos diferentes (AppRepository, CicloAcertoRepository)
- ✅ Não há sobreposição de arquivos modificados
- ✅ Ordem sequencial clara (AGENTE 1 → AGENTE 2)

### **4. COMUNICAÇÃO**

**AGENTE 1 deve:**
- Informar quando estrutura `domain/` estiver pronta
- Listar repositories criados/movidos
- Indicar se há algum problema

**AGENTE 2 deve:**
- Aguardar confirmação de AGENTE 1 antes de começar
- Informar quando refatoração estiver completa
- Indicar se precisa de ajustes nos repositories

## 📊 STATUS DE PROGRESSO

### **AGENTE 1 - ESTRUTURA DOMAIN**
- [ ] Pasta `domain/` criada
- [ ] ClienteRepository movido
- [ ] AcertoRepository movido
- [ ] AcertoMesaRepository movido
- [ ] CategoriaDespesaRepository movido
- [ ] MesaRepository criado
- [ ] RotaRepository criado
- [ ] DespesaRepository criado
- [ ] CicloRepository criado
- [ ] ColaboradorRepository criado
- [ ] ContratoRepository criado
- [ ] Packages ajustados
- [ ] Build validado

**Status**: ⏳ Aguardando comando para iniciar

### **AGENTE 2 - REFATORAÇÃO APPREPOSITORY**
- [ ] Aguardando AGENTE 1 completar
- [ ] RepositoryFactory atualizado
- [ ] AppRepository refatorado para delegar
- [ ] AppRepository reduzido para ~200-300 linhas
- [ ] CicloAcertoRepository migrado
- [ ] 5 fragments atualizados
- [ ] Imports removidos
- [ ] Build validado

**Status**: ⏳ Aguardando AGENTE 1 completar

## 🎯 RESULTADO FINAL ESPERADO

Após ambos agentes completarem:

- ✅ Estrutura `domain/` criada e organizada
- ✅ Repositories especializados funcionando
- ✅ AppRepository como Facade (~200-300 linhas)
- ✅ AppRepository delegando para especializados
- ✅ CicloAcertoRepository usando AppRepository
- ✅ Fragments simplificados
- ✅ Arquitetura híbrida modular completa
- ✅ Build passando sem erros

## 📝 NOTAS IMPORTANTES

1. **Trabalho sequencial**: AGENTE 2 depende de AGENTE 1 completar primeiro
2. **Sem conflitos**: Arquivos trabalhados são diferentes
3. **Validação conjunta**: Ambos validam build final juntos
4. **Comunicação clara**: Cada agente informa progresso e bloqueios

---

**Última atualização**: 2025-01-XX
**Status Coordenação**: ✅ Pronto para iniciar
**Próximo passo**: Aguardar comando do usuário para iniciar

