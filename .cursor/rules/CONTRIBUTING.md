# Guia de Contribuição

## 🤝 Como Contribuir

Obrigado por considerar contribuir com o projeto Gestão Bilhares! Este documento fornece diretrizes e padrões para contribuições.

## 📋 Índice

- [Código de Conduta](#código-de-conduta)
- [Como Contribuir](#como-contribuir)
- [Padrões de Código](#padrões-de-código)
- [Estrutura de Commits](#estrutura-de-commits)
- [Processo de Pull Request](#processo-de-pull-request)
- [Testes](#testes)
- [Documentação](#documentação)

## 📜 Código de Conduta

### Nossos Valores

- **Respeito**: Trate todos com respeito e profissionalismo
- **Colaboração**: Trabalhe em equipe e compartilhe conhecimento
- **Qualidade**: Mantenha altos padrões de código e testes
- **Comunicação**: Seja claro e objetivo nas comunicações

## 🚀 Como Contribuir

### 1. Reportar Bugs

Ao reportar um bug, inclua:

- **Descrição clara** do problema
- **Passos para reproduzir**
- **Comportamento esperado** vs **comportamento atual**
- **Screenshots** (se aplicável)
- **Ambiente**: Android version, device, etc.

### 2. Sugerir Funcionalidades

Para sugerir novas funcionalidades:

- Descreva a **funcionalidade** proposta
- Explique o **caso de uso** e **valor**
- Considere **impacto** e **complexidade**
- Discuta antes de implementar

### 3. Contribuir com Código

1. **Fork** o repositório
2. **Clone** seu fork
3. Crie uma **branch** para sua feature
4. Faça suas **alterações**
5. **Teste** suas alterações
6. **Commit** seguindo os padrões
7. **Push** para seu fork
8. Abra um **Pull Request**

## 💻 Padrões de Código

### Kotlin Style Guide

Seguir as [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html):

#### Nomenclatura

```kotlin
// Classes: PascalCase
class ClienteRepository

// Funções: camelCase
fun obterClientePorId(id: Long)

// Variáveis: camelCase
val clienteId: Long
val nomeCliente: String

// Constantes: UPPER_SNAKE_CASE
const val MAX_RETRY_ATTEMPTS = 3

// Packages: lowercase
package com.example.gestaobilhares.ui.clients
```

#### Formatação

```kotlin
// Indentação: 4 espaços
class Example {
    fun method() {
        if (condition) {
            // código
        }
    }
}

// Linhas: máximo 120 caracteres
// Quebras de linha quando necessário
val longVariableName = someVeryLongMethodCall(
    parameter1,
    parameter2,
    parameter3
)
```

#### Estrutura de Arquivo

```kotlin
// 1. Package declaration
package com.example.gestaobilhares.ui.clients

// 2. Imports (agrupados)
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider

// 3. Classe/Função
class ClientListFragment : Fragment() {
    // ...
}
```

### Arquitetura MVVM

#### ViewModel

```kotlin
class ClientListViewModel(
    private val repository: AppRepository
) : BaseViewModel() {
    
    // StateFlow para estado
    private val _clientes = MutableStateFlow<List<Cliente>>(emptyList())
    val clientes: StateFlow<List<Cliente>> = _clientes.asStateFlow()
    
    // Funções suspend para operações assíncronas
    suspend fun carregarClientes(rotaId: Long) {
        repository.obterClientesPorRota(rotaId)
            .collect { clientes ->
                _clientes.value = clientes
            }
    }
}
```

#### Fragment

```kotlin
class ClientListFragment : Fragment() {
    
    private val viewModel: ClientListViewModel by viewModels()
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Observar StateFlow com repeatOnLifecycle
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.clientes.collect { clientes ->
                    // Atualizar UI
                }
            }
        }
    }
}
```

### Repository Pattern

```kotlin
// ✅ CORRETO: Usar suspend functions
suspend fun obterClientePorId(id: Long): Cliente? {
    return clienteDao.obterClientePorId(id)
}

// ✅ CORRETO: Retornar Flow para observação
fun obterClientesPorRota(rotaId: Long): Flow<List<Cliente>> {
    return clienteDao.obterClientesPorRota(rotaId)
}

// ❌ EVITAR: runBlocking
fun obterClienteSync(id: Long): Cliente? {
    return runBlocking { clienteDao.obterClientePorId(id) }
}
```

### Tratamento de Erros

```kotlin
// ✅ CORRETO: Try-catch com logs
suspend fun salvarCliente(cliente: Cliente): Result<Long> {
    return try {
        val id = repository.inserirCliente(cliente)
        Result.success(id)
    } catch (e: Exception) {
        AppLogger.e("ClientViewModel", "Erro ao salvar cliente", e)
        Result.failure(e)
    }
}

// ✅ CORRETO: Logs sanitizados
AppLogger.d("Tag", "Cliente salvo: ${cliente.nome}") // CPF será sanitizado automaticamente
```

### Comentários e Documentação

```kotlin
/**
 * Obtém cliente por ID com descriptografia automática.
 * 
 * @param id ID do cliente
 * @return Cliente descriptografado ou null se não encontrado
 */
suspend fun obterClientePorId(id: Long): Cliente? {
    // ✅ FASE 12.3: Descriptografar dados sensíveis
    return decryptCliente(clienteDao.obterClientePorId(id))
}
```

## 📝 Estrutura de Commits

### Formato

```
<tipo>(<escopo>): <descrição curta>

<descrição detalhada (opcional)>

<rodapé (opcional)>
```

### Tipos

- `feat`: Nova funcionalidade
- `fix`: Correção de bug
- `docs`: Documentação
- `style`: Formatação (não afeta código)
- `refactor`: Refatoração
- `test`: Testes
- `chore`: Tarefas de manutenção

### Exemplos

```bash
# Feature
feat(clientes): adiciona filtro por status

# Bug fix
fix(acerto): corrige cálculo de débito atual

# Documentação
docs(README): adiciona seção de instalação

# Refatoração
refactor(repository): remove runBlocking de métodos suspend

# Testes
test(auth): adiciona testes para login offline
```

## 🔄 Processo de Pull Request

### Checklist

Antes de abrir um PR, verifique:

- [ ] Código segue os padrões do projeto
- [ ] Testes adicionados/atualizados
- [ ] Todos os testes passam
- [ ] Documentação atualizada
- [ ] Sem warnings críticos
- [ ] Commits seguem o padrão
- [ ] Branch atualizada com main

### Template de PR

```markdown
## Descrição
Breve descrição das mudanças

## Tipo de Mudança
- [ ] Bug fix
- [ ] Nova funcionalidade
- [ ] Breaking change
- [ ] Documentação

## Como Testar
Passos para testar as mudanças

## Screenshots (se aplicável)

## Checklist
- [ ] Código revisado
- [ ] Testes passando
- [ ] Documentação atualizada
```

## 🧪 Testes

### Testes Unitários

```kotlin
// ✅ CORRETO: Teste unitário simples
@Test
fun `deve calcular débito atual corretamente`() = runTest {
    // Arrange
    val cliente = Cliente(id = 1L, debitoAtual = 100.0)
    
    // Act
    val resultado = calculator.calcularDebitoAtual(cliente)
    
    // Assert
    assertEquals(100.0, resultado)
}
```

### Testes Instrumentados

```kotlin
// ✅ CORRETO: Teste instrumentado
@RunWith(AndroidJUnit4::class)
class DataEncryptionTest {
    
    @Test
    fun deveCriptografarEDescriptografarDados() {
        val dados = "12345678901" // CPF
        val criptografado = DataEncryption.encrypt(dados)
        val descriptografado = DataEncryption.decrypt(criptografado)
        
        assertEquals(dados, descriptografado)
    }
}
```

### Cobertura

- **Mínimo**: 70% de cobertura
- **Ideal**: 80%+ de cobertura
- **Crítico**: 100% para utilitários de segurança

## 📚 Documentação

### Comentários de Código

```kotlin
// ✅ BOM: Comentário explicativo
// Calcula o débito atual considerando acertos pendentes
private fun calcularDebitoAtual(cliente: Cliente): Double {
    // ...
}

// ❌ RUIM: Comentário óbvio
// Incrementa o contador
contador++
```

### Documentação de Funções

```kotlin
/**
 * Salva acerto com mesas vinculadas.
 * 
 * @param acerto Dados do acerto
 * @param mesas Lista de mesas do acerto
 * @return ID do acerto salvo
 * @throws DatabaseException se houver erro no banco
 */
suspend fun salvarAcertoComMesas(
    acerto: Acerto,
    mesas: List<AcertoMesa>
): Long {
    // ...
}
```

## 🎯 Boas Práticas

### Performance

- ✅ Use `Flow` para observação reativa
- ✅ Use `suspend` functions para operações assíncronas
- ✅ Evite `runBlocking` em código de produção
- ✅ Use cache para dados frequentemente acessados

### Segurança

- ✅ Criptografe dados sensíveis automaticamente
- ✅ Sanitize logs em produção
- ✅ Valide inputs do usuário
- ✅ Use Android Keystore para chaves

### Manutenibilidade

- ✅ Mantenha funções pequenas (< 50 linhas)
- ✅ Evite duplicação de código
- ✅ Use nomes descritivos
- ✅ Documente código complexo

## ❓ Dúvidas?

Se tiver dúvidas sobre como contribuir:

1. Consulte a documentação existente
2. Revise código similar no projeto
3. Abra uma issue para discussão
4. Entre em contato com a equipe

## 🙏 Agradecimentos

Obrigado por contribuir com o Gestão Bilhares! Sua contribuição é muito valiosa.

---

**Última atualização**: 2025-01-08

