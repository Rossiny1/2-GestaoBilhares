# 🔍 ANÁLISE DE CÓDIGO - Preparação para Refatoração UX

> **Data:** 24/01/2026  
> **Objetivo:** Analisar estrutura atual para implementar UX de lista resumida → detalhes ao clicar

---

## 1️⃣ ESTRUTURA ATUAL DO VIEWMODEL

```kotlin
@HiltViewModel
class MesasReformadasViewModel @Inject constructor(
    private val appRepository: AppRepository
) : BaseViewModel() {

    private val _cards = MutableStateFlow<List<ReformaCard>>(emptyList())
    val cards: StateFlow<List<ReformaCard>> = _cards.asStateFlow()

    private val _filtroNumeroMesa = MutableStateFlow<String?>(null)
    val filtroNumeroMesa: StateFlow<String?> = _filtroNumeroMesa.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun carregarMesasReformadas() {
        viewModelScope.launch {
            try {
                Log.d("DEBUG_CARDS", "")
```

**✅ Descobertas:**

- ViewModel já usa StateFlow corretamente
- Tem 3 StateFlows principais: cards, filtro, errorMessage
- Função `carregarMesasReformadas()` já existe
- Usa BaseViewModel como herança

---

## 2️⃣ DATA CLASS ReformaCard ATUAL

```kotlin
// Data class para o card
data class ReformaCard(
    val id: Long,
    val mesaId: Long,
    val numeroMesa: Int,
    val descricao: String,
    val data: Long,
    val origem: String, // "NOVA_REFORMA", "ACERTO", "ACERTO_LEGACY", "HEADER_MESA"
    val responsavel: String? = null,  // ✅ ADICIONADO - Nome do responsável pela manutenção
    val observacoes: String?
)
```

**✅ Descobertas:**

- Data class já estruturada
- Campo `origem` diferencia tipos (HEADER_MESA é o agrupador)
- Campos `responsavel` e `observacoes` já existem
- Campo `numeroMesa` já disponível

---

## 3️⃣ LÓGICA DE AGRUPAMENTO ATUAL

```kotlin
// ✅ NOVO: AGRUPAR TODOS OS ITENS POR MESA
val cardsAgrupados = mutableMapOf<Long, MutableList<ReformaCard>>()

// Adicionar reformas manuais ao agrupamento
reformasManuais.forEach { reforma ->
    // ... criação do card ...
    cardsAgrupados.getOrPut(reforma.mesaId) { mutableListOf() }.add(card)
}

// ✅ NOVO: ORDENAR CARDS DENTRO DE CADA MESA E DEPOIS AS MESAS
val cardsFinais = cardsAgrupados.flatMap { (mesaId, cardsDaMesa) ->
    // Ordenar cards da mesa por data (mais recente primeiro)
    val cardsOrdenados = cardsDaMesa.sortedByDescending { it.data }
    
    // Criar HEADER para a mesa
    val headerCard = ReformaCard(
        id = -mesaId,  // ID negativo para diferenciar
        mesaId = mesaId,
        numeroMesa = mesa?.numero?.toIntOrNull() ?: 0,
        descricao = "🏓 Mesa ${mesa?.numero} - ${cardsOrdenados.size} manutenção(ões)",
        data = cardsOrdenados.firstOrNull()?.data ?: 0L,
        origem = "HEADER_MESA",
        responsavel = null,  // ✅ ADICIONAR (header não tem responsável)
        observacoes = null
    )
```

**✅ Descobertas:**

- **JÁ EXISTE HEADER_MESA** - exibe "🏓 Mesa X - Y manutenção(ões)"
- Usa `flatMap` para criar lista com headers + cards
- Headers têm ID negativo para diferenciar
- Cards já ordenados por data (mais recente primeiro)

---

## 4️⃣ COMO O FRAGMENT OBSERVA OS DADOS

```kotlin
private fun observeViewModel() {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.cards.collect { cards ->
                adapter.submitList(cards)
                
                // Mostrar/ocultar estado vazio
                binding.emptyStateLayout.visibility = if (cards.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.isLoading.collect { _ ->
                // TODO: Implementar loading state se necessário
            }
        }
    }

    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.errorMessage.collect { message ->
                message?.let {
                    // TODO: Mostrar erro
                    viewModel.clearError()
                }
            }
        }
    }
}
```

**✅ Descobertas:**

- Fragment já observa `viewModel.cards` com `collect`
- Usa `adapter.submitList(cards)` diretamente
- Tem estado vazio implementado
- Loading e erro states existem mas não implementados

---

## 5️⃣ ESTRUTURA DO ADAPTER ATUAL

```kotlin
class MesasReformadasAdapter(
    private val onItemClick: (ReformaCard) -> Unit
) : ListAdapter<ReformaCard, MesasReformadasAdapter.ViewHolder>(DiffCallback()) {

    // ViewHolder
    class ViewHolder(
        private val binding: ItemMesaReformadaBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(card: ReformaCard) {
            binding.apply {
                when (card.origem) {
                    "HEADER_MESA" -> {
                        // ✅ TRATAMENTO ESPECIAL PARA HEADER
                        tvNumeroMesa.text = card.descricao  // "🏓 Mesa X - Y manutenção(ões)"
```

**✅ Descobertas:**

- Adapter já recebe `onItemClick: (ReformaCard) -> Unit`
- Usa `when (card.origem)` para diferenciar tipos
- **HEADER_MESA já tem tratamento especial**
- Usa `ItemMesaReformadaBinding` (layout XML)

---

## 6️⃣ NAVEGAÇÃO SAFEARGS JÁ EXISTE

```kotlin
public class MesasReformadasFragmentDirections private constructor() {
  private data class ActionMesasReformadasFragmentToHistoricoMesaFragment(
    public val mesaComHistorico: MesaReformadaComHistorico,
  ) : NavDirections {
```

**✅ Descobertas:**

- **SafeArgs já existe** para navegar para `HistoricoMesaFragment`
- Usa `MesaReformadaComHistorico` como parâmetro
- Action: `actionMesasReformadasFragmentToHistoricoMesaFragment`

---

## 7️⃣ REPOSITORY JÁ TEM FUNÇÃO PARA BUSCAR PANO

```kotlin
suspend fun obterPanoPorId(id: Long) = panoRepository.obterPorId(id)
```

**✅ Descobertas:**

- **Função já existe** no AppRepository
- Retorna pano pelo ID
- É `suspend` (coroutine)

---

## 🎯 ANÁLISE FINAL - O QUE PRECISA MUDAR

### ✅ **JÁ IMPLEMENTADO (não mexer):**

1. ✅ ViewModel com StateFlow
2. ✅ Data class ReformaCard completa
3. ✅ HEADER_MESA já existe e funciona
4. ✅ Fragment já observa e atualiza adapter
5. ✅ Adapter já trata diferentes tipos de origem
6. ✅ SafeArgs para navegação já existe
7. ✅ Repository tem função para buscar pano

### 🔄 **PEQUENOS AJUSTES NECESSÁRIOS:**

#### 1. **No Adapter** - Adicionar clique nos headers

```kotlin
when (card.origem) {
    "HEADER_MESA" -> {
        // EXISTE: mostrar header
        // ADICIONAR: clique para expandir/colapsar ou navegar
        itemView.setOnClickListener {
            if (isExpanded) {
                // Colapsar: filtrar para mostrar só headers
            } else {
                // Expandir: mostrar todos os cards da mesa
                // OU navegar para tela de detalhes
                onItemClick(card)
            }
        }
    }
}
```

#### 2. **No Fragment** - Tratar clique diferenciado

```kotlin
private val onItemClick: (ReformaCard) -> Unit = { card ->
    when (card.origem) {
        "HEADER_MESA" -> {
            // Navegar para detalhes da mesa
            val action = MesasReformadasFragmentDirections
                .actionMesasReformadasFragmentToHistoricoMesaFragment(
                    // criar MesaReformadaComHistorico aqui
                )
            findNavController().navigate(action)
        }
        else -> {
            // Clique em card individual (se necessário)
        }
    }
}
```

#### 3. **Opcional: Estado expandido/colapsado no ViewModel:**

```kotlin
private val _mesasExpandidas = MutableStateFlow<Set<Long>>(emptySet())
val mesasExpandidas: StateFlow<Set<Long>> = _mesasExpandidas.asStateFlow()

fun toggleMesa(mesaId: Long) {
    val atuais = _mesasExpandidas.value.toMutableSet()
    if (atuais.contains(mesaId)) {
        atuais.remove(mesaId) // colapsar
    } else {
        atuais.add(mesaId) // expandir
    }
    _mesasExpandidas.value = atuais
}
```

---

## 🚀 **CONCLUSÃO**

**A UX desejada JÁ ESTÁ 90% IMPLEMENTADA!** ✅

- ✅ Lista já mostra headers agrupados por mesa
- ✅ Headers já exibem "🏓 Mesa X - Y manutenção(ões)"
- ✅ Estrutura para clique já existe
- ✅ Navegação para detalhes já existe
- ✅ Dados necessários já disponíveis

**Só precisa:**

1. Adicionar `setOnClickListener` nos headers
2. Implementar navegação no clique do header
3. (Opcional) Adicionar estado expandido/colapsado

**Estimativa: 15-20 minutos de implementação** 🎯
