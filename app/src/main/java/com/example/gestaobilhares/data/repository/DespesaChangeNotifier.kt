package com.example.gestaobilhares.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton para notificar mudanças em despesas globalmente
 * Permite que diferentes componentes sejam notificados quando despesas são modificadas
 */
object DespesaChangeNotifier {
    
    private val _despesaModificada = MutableStateFlow<DespesaChangeEvent?>(null)
    val despesaModificada: StateFlow<DespesaChangeEvent?> = _despesaModificada.asStateFlow()
    
    /**
     * Notifica que uma despesa foi modificada
     */
    fun notificarMudanca(event: DespesaChangeEvent) {
        _despesaModificada.value = event
    }
    
    /**
     * Limpa a notificação atual
     */
    fun limparNotificacao() {
        _despesaModificada.value = null
    }
}

/**
 * Evento de mudança em despesa
 */
data class DespesaChangeEvent(
    val cicloId: Long,
    val rotaId: Long,
    val tipo: TipoMudanca,
    val despesaId: Long? = null
)

/**
 * Tipo de mudança na despesa
 */
enum class TipoMudanca {
    ADICIONADA,
    EDITADA,
    REMOVIDA
} 