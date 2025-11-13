# 📋 PLANO DE TRABALHO - AGENTE 1: ESTRUTURA DOMAIN E REPOSITORIES ESPECIALIZADOS

## 🎯 OBJETIVO
Criar a estrutura `domain/` e organizar/criar repositories especializados por domínio, preparando a base para que o AppRepository delegue para eles.

## 📍 CONTEXTO
- **Status Atual**: Repositories estão em `data/repository/` diretamente
- **Meta**: Organizar em `data/repository/domain/` conforme arquitetura híbrida modular
- **Trabalho Paralelo**: Outro agente trabalhará na refatoração do AppRepository simultaneamente

## ⚠️ REGRAS CRÍTICAS PARA TRABALHO HARMONIOSO

### **NÃO MODIFICAR:**
- ❌ **NÃO modificar** `AppRepository.kt` (outro agente está trabalhando nele)
- ❌ **NÃO modificar** `CicloAcertoRepository.kt` (outro agente está trabalhando nele)
- ❌ **NÃO modificar** fragments ou ViewModels (já migrados)
- ❌ **NÃO modificar** `RepositoryFactory.kt` (será atualizado depois)

### **PODE MODIFICAR:**
- ✅ Criar pasta `data/repository/domain/`
- ✅ Mover/criar repositories especializados na pasta `domain/`
- ✅ Ajustar imports e packages dos repositories movidos
- ✅ Criar repositories que não existem (MesaRepository, RotaRepository, etc.)

## 📋 TAREFAS DETALHADAS

### **FASE 1: Criar Estrutura Domain (PRIORIDADE ALTA)**

1. **Criar pasta `domain/`**
   - Caminho: `app/src/main/java/com/example/gestaobilhares/data/repository/domain/`
   - Criar arquivo `.gitkeep` se necessário

### **FASE 2: Mover Repositories Existentes (PRIORIDADE ALTA)**

Mover os seguintes repositories de `data/repository/` para `data/repository/domain/`:

1. **ClienteRepository.kt**
   - De: `app/src/main/java/com/example/gestaobilhares/data/repository/ClienteRepository.kt`
   - Para: `app/src/main/java/com/example/gestaobilhares/data/repository/domain/ClienteRepository.kt`
   - Ajustar package: `package com.example.gestaobilhares.data.repository.domain`

2. **AcertoRepository.kt**
   - De: `app/src/main/java/com/example/gestaobilhares/data/repository/AcertoRepository.kt`
   - Para: `app/src/main/java/com/example/gestaobilhares/data/repository/domain/AcertoRepository.kt`
   - Ajustar package: `package com.example.gestaobilhares.data.repository.domain`

3. **AcertoMesaRepository.kt**
   - De: `app/src/main/java/com/example/gestaobilhares/data/repository/AcertoMesaRepository.kt`
   - Para: `app/src/main/java/com/example/gestaobilhares/data/repository/domain/AcertoMesaRepository.kt`
   - Ajustar package: `package com.example.gestaobilhares.data.repository.domain`

4. **CategoriaDespesaRepository.kt**
   - De: `app/src/main/java/com/example/gestaobilhares/data/repository/CategoriaDespesaRepository.kt`
   - Para: `app/src/main/java/com/example/gestaobilhares/data/repository/domain/CategoriaDespesaRepository.kt`
   - Ajustar package: `package com.example.gestaobilhares.data.repository.domain`

### **FASE 3: Criar Repositories Faltantes (PRIORIDADE ALTA)**

Criar os seguintes repositories na pasta `domain/` baseando-se nos métodos do AppRepository:

1. **MesaRepository.kt** (NOVO)
   - Extrair métodos de Mesa do AppRepository
   - Métodos a incluir:
     - `obterMesaPorId(id: Long)`
     - `obterMesasPorCliente(clienteId: Long)`
     - `obterMesasDisponiveis()`
     - `inserirMesa(mesa: Mesa): Long`
     - `atualizarMesa(mesa: Mesa)`
     - `deletarMesa(mesa: Mesa)`
     - `vincularMesaACliente(mesaId: Long, clienteId: Long)`
     - `vincularMesaComValorFixo(mesaId: Long, clienteId: Long, valorFixo: Double)`
     - `desvincularMesaDeCliente(mesaId: Long)`
     - `retirarMesa(mesaId: Long)`
     - `atualizarRelogioMesa(...)`
     - `obterMesasPorClienteDireto(clienteId: Long)`
     - `buscarMesasPorRota(rotaId: Long)`
     - `obterTodasMesas()`
   - Construtor: `constructor(private val mesaDao: MesaDao)`

2. **RotaRepository.kt** (NOVO)
   - Extrair métodos de Rota do AppRepository
   - Métodos a incluir:
     - `obterTodasRotas()`
     - `obterRotasAtivas()`
     - `getRotasResumoComAtualizacaoTempoReal()`
     - `obterRotaPorId(id: Long)`
     - `inserirRota(rota: Rota): Long`
     - `atualizarRota(rota: Rota)`
     - `deletarRota(rota: Rota)`
     - `desativarRota(rotaId: Long, timestamp: Long)`
     - `ativarRota(rotaId: Long, timestamp: Long)`
     - `atualizarStatus(rotaId: Long, status: String, timestamp: Long)`
     - `atualizarCicloAcerto(rotaId: Long, ciclo: Int, timestamp: Long)`
     - `iniciarCicloRota(rotaId: Long, ciclo: Int, dataInicio: Long, timestamp: Long)`
     - `finalizarCicloRota(rotaId: Long, dataFim: Long, timestamp: Long)`
     - `existeRotaComNome(nome: String, excludeId: Long)`
     - `contarRotasAtivas()`
   - Construtor: `constructor(private val rotaDao: RotaDao)`

3. **DespesaRepository.kt** (NOVO - se não existir)
   - Extrair métodos de Despesa do AppRepository
   - Métodos a incluir:
     - `obterTodasDespesas()`
     - `obterDespesasPorRota(rotaId: Long)`
     - `obterDespesaPorId(id: Long)`
     - `inserirDespesa(despesa: Despesa): Long`
     - `atualizarDespesa(despesa: Despesa)`
     - `deletarDespesa(despesa: Despesa)`
     - `calcularTotalPorRota(rotaId: Long)`
     - `calcularTotalGeral()`
     - `contarDespesasPorRota(rotaId: Long)`
     - `deletarDespesasPorRota(rotaId: Long)`
     - `buscarDespesasPorCicloId(cicloId: Long)`
     - `buscarDespesasPorRotaECicloId(rotaId: Long, cicloId: Long)`
     - `buscarDespesasGlobaisPorCiclo(ano: Int, numero: Int)`
     - `somarDespesasGlobaisPorCiclo(ano: Int, numero: Int)`
   - Construtor: `constructor(private val despesaDao: DespesaDao)`

4. **CicloRepository.kt** (NOVO - extrair de CicloAcertoRepository)
   - Extrair métodos de Ciclo do AppRepository e CicloAcertoRepository
   - Métodos a incluir:
     - `obterTodosCiclos()`
     - `obterCiclosPorRota(rotaId: Long)`
     - `obterCicloPorId(cicloId: Long)`
     - `buscarCicloAtivo(rotaId: Long)`
     - `inserirOuAtualizarCiclo(ciclo: CicloAcertoEntity): Long`
     - `atualizarStatusCiclo(cicloId: Long, status: StatusCicloAcerto)`
     - Métodos de cálculo e relatórios relacionados a ciclos
   - Construtor: `constructor(private val cicloAcertoDao: CicloAcertoDao)`
   - **NOTA**: Este repository será usado pelo CicloAcertoRepository depois da migração

5. **ColaboradorRepository.kt** (NOVO)
   - Extrair métodos de Colaborador do AppRepository
   - Métodos a incluir:
     - `obterTodosColaboradores()`
     - `obterColaboradorPorId(id: Long)`
     - `inserirColaborador(colaborador: Colaborador): Long`
     - `atualizarColaborador(colaborador: Colaborador)`
     - `deletarColaborador(colaborador: Colaborador)`
     - Métodos relacionados a metas e performance
   - Construtor: `constructor(private val colaboradorDao: ColaboradorDao)`

6. **ContratoRepository.kt** (NOVO)
   - Extrair métodos de Contrato do AppRepository
   - Métodos a incluir:
     - `buscarContratosPorCliente(clienteId: Long)`
     - `buscarContratoAtivoPorCliente(clienteId: Long)`
     - `inserirContrato(contrato: ContratoLocacao): Long`
     - `atualizarContrato(contrato: ContratoLocacao)`
     - `buscarMesasPorContrato(contratoId: Long)`
     - Métodos relacionados a aditivos e assinaturas
   - Construtor: `constructor(private val contratoLocacaoDao: ContratoLocacaoDao, private val aditivoContratoDao: AditivoContratoDao)`

### **FASE 4: Verificar e Ajustar Imports (PRIORIDADE MÉDIA)**

Após mover/criar repositories:

1. Verificar se há erros de import nos repositories movidos
2. Ajustar imports internos se necessário
3. Garantir que todos os repositories em `domain/` têm package correto

### **FASE 5: Documentação (PRIORIDADE BAIXA)**

1. Adicionar comentários nos repositories criados explicando o domínio
2. Documentar métodos principais

## 📝 PADRÃO DE CRIAÇÃO DE REPOSITORY

```kotlin
package com.example.gestaobilhares.data.repository.domain

import com.example.gestaobilhares.data.dao.[Domain]Dao
import com.example.gestaobilhares.data.entities.[Entity]
import kotlinx.coroutines.flow.Flow

/**
 * Repository especializado para domínio [Domain]
 * 
 * Responsável por todas as operações relacionadas a [Entity]
 * Este repository é usado pelo AppRepository através de delegação.
 */
class [Domain]Repository constructor(
    private val [domain]Dao: [Domain]Dao
) {
    // Métodos extraídos do AppRepository
    fun obterTodos(): Flow<List<[Entity]>> = [domain]Dao.obterTodos()
    
    suspend fun obterPorId(id: Long): [Entity]? = [domain]Dao.obterPorId(id)
    
    suspend fun inserir([entity]: [Entity]): Long {
        return [domain]Dao.inserir([entity])
    }
    
    // ... outros métodos
}
```

## ✅ CHECKLIST DE VALIDAÇÃO

Antes de considerar completo, verificar:

- [ ] Pasta `domain/` criada
- [ ] ClienteRepository movido para `domain/`
- [ ] AcertoRepository movido para `domain/`
- [ ] AcertoMesaRepository movido para `domain/`
- [ ] CategoriaDespesaRepository movido para `domain/`
- [ ] MesaRepository criado em `domain/`
- [ ] RotaRepository criado em `domain/`
- [ ] DespesaRepository criado em `domain/`
- [ ] CicloRepository criado em `domain/`
- [ ] ColaboradorRepository criado em `domain/`
- [ ] ContratoRepository criado em `domain/`
- [ ] Todos os packages ajustados para `com.example.gestaobilhares.data.repository.domain`
- [ ] Nenhum erro de compilação nos repositories criados/movidos
- [ ] Imports ajustados corretamente

## 🚨 IMPORTANTE

- **NÃO modificar AppRepository.kt** - outro agente está trabalhando nele
- **NÃO modificar CicloAcertoRepository.kt** - outro agente está trabalhando nele
- **NÃO modificar RepositoryFactory.kt** - será atualizado depois
- **Focar apenas em criar/mover repositories na pasta domain/**

## 🎯 RESULTADO ESPERADO

Após completar este plano:

- ✅ Estrutura `domain/` criada e organizada
- ✅ Todos os repositories especializados na pasta `domain/`
- ✅ Repositories prontos para serem usados pelo AppRepository via delegação
- ✅ Base sólida para o outro agente refatorar o AppRepository

## 📌 ORDEM DE EXECUÇÃO RECOMENDADA

1. Criar pasta `domain/`
2. Mover repositories existentes (ClienteRepository, AcertoRepository, etc.)
3. Criar repositories faltantes (MesaRepository, RotaRepository, etc.)
4. Verificar imports e ajustar packages
5. Validar que não há erros de compilação

---

**Status**: Aguardando comando para iniciar
**Prioridade**: ALTA
**Tempo estimado**: 2-3 horas
**Risco de conflito**: BAIXO (trabalha em arquivos novos/movidos, não modifica AppRepository)

