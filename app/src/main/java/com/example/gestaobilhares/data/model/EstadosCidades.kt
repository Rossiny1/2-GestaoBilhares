package com.example.gestaobilhares.data.model

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class Estado(
    @SerializedName("sigla") val sigla: String,
    @SerializedName("nome") val nome: String,
    @SerializedName("cidades") val cidades: List<String>
)

data class EstadosCidades(
    @SerializedName("estados") val estados: List<Estado>
) {
    companion object {
        fun carregarDados(context: Context): EstadosCidades {
            val jsonString = context.assets.open("estados_cidades.json").bufferedReader().use { it.readText() }
            return Gson().fromJson(jsonString, EstadosCidades::class.java)
        }
    }
}
