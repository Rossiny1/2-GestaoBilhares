package com.example.gestaobilhares.data.entities

/**
 * ✅ NOVO: Função de extensão para formatar tipos de mesa de forma amigável
 */
fun TipoMesa.getDisplayName(): String {
    return when (this) {
        TipoMesa.SINUCA -> "Sinuca"
        TipoMesa.JUKEBOX -> "Jukebox"
        TipoMesa.PEMBOLIM -> "Pembolim"
        TipoMesa.OUTROS -> "Outros"
    }
}

/**
 * ✅ NOVO: Função de extensão para formatar tamanhos de mesa
 */
fun TamanhoMesa.getDisplayName(): String {
    return when (this) {
        TamanhoMesa.PEQUENA -> "Pequena"
        TamanhoMesa.MEDIA -> "Média"
        TamanhoMesa.GRANDE -> "Grande"
    }
}

/**
 * ✅ NOVO: Função de extensão para formatar estado de conservação
 */
fun EstadoConservacao.getDisplayName(): String {
    return when (this) {
        EstadoConservacao.OTIMO -> "Ótimo"
        EstadoConservacao.BOM -> "Bom"
        EstadoConservacao.RUIM -> "Ruim"
    }
}
