package com.example.gestaobilhares

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

/**
 * ✅ PRODUÇÃO: Timber Tree que envia logs importantes para o Crashlytics.
 * 
 * Estratégia de logs:
 * - ERROR: Sempre enviado (crítico)
 * - WARN: Sempre enviado (importante)
 * - INFO: Enviado em produção (contexto útil)
 * - DEBUG/VERBOSE: Não enviado (muito verboso)
 * 
 * Exceções são sempre registradas no Crashlytics.
 */
class CrashlyticsTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        // Filtrar apenas logs importantes (INFO, WARN, ERROR)
        if (priority == Log.VERBOSE || priority == Log.DEBUG) {
            return
        }

        val crashlytics = FirebaseCrashlytics.getInstance()
        
        // Formatar mensagem com tag se disponível
        val formattedMessage = if (tag != null) {
            "[$tag] $message"
        } else {
            message
        }
        
        // Enviar log para Crashlytics
        crashlytics.log(formattedMessage)
        
        // Se houver exceção, registrar separadamente
        if (t != null) {
            crashlytics.recordException(t)
        }
        
        // Para logs de erro, também adicionar como chave customizada para facilitar busca
        if (priority == Log.ERROR) {
            crashlytics.setCustomKey("last_error_message", message.take(100)) // Limitar tamanho
        }
    }
}
