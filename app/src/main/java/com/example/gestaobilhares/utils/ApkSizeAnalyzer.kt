package com.example.gestaobilhares.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import java.io.File

/**
 * ✅ FASE 12.10: Analisador de Tamanho do APK
 * 
 * Analisa o tamanho do APK instalado e fornece recomendações de otimização
 */
object ApkSizeAnalyzer {
    
    private const val TAG = "ApkSizeAnalyzer"
    
    /**
     * Obtém informações sobre o tamanho do APK instalado
     */
    fun getApkInfo(context: Context): ApkInfo? {
        return try {
            val packageManager = context.packageManager
            val packageName = context.packageName
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            
            val applicationInfo = packageInfo.applicationInfo
            val apkPath = applicationInfo.sourceDir
            val apkFile = File(apkPath)
            
            if (apkFile.exists()) {
                val sizeBytes = apkFile.length()
                val sizeMB = sizeBytes / (1024.0 * 1024.0)
                val sizeKB = sizeBytes / 1024.0
                
                ApkInfo(
                    path = apkPath,
                    sizeBytes = sizeBytes,
                    sizeMB = sizeMB,
                    sizeKB = sizeKB,
                    versionCode = packageInfo.longVersionCode,
                    versionName = packageInfo.versionName ?: "Unknown"
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao obter informações do APK: ${e.message}")
            null
        }
    }
    
    /**
     * Analisa o tamanho do APK e retorna recomendações
     */
    fun analyzeApkSize(context: Context): AnalysisResult {
        val apkInfo = getApkInfo(context) ?: return AnalysisResult(
            apkInfo = null,
            recommendations = listOf("Não foi possível obter informações do APK"),
            isOptimal = false
        )
        
        val recommendations = mutableListOf<String>()
        var isOptimal = true
        
        // Verificar tamanho do APK
        when {
            apkInfo.sizeMB > 50 -> {
                recommendations.add("APK muito grande (${String.format("%.2f", apkInfo.sizeMB)} MB). Considere usar Android App Bundle (AAB)")
                isOptimal = false
            }
            apkInfo.sizeMB > 30 -> {
                recommendations.add("APK grande (${String.format("%.2f", apkInfo.sizeMB)} MB). Verifique se ProGuard/R8 está habilitado")
                isOptimal = false
            }
            apkInfo.sizeMB > 20 -> {
                recommendations.add("APK moderado (${String.format("%.2f", apkInfo.sizeMB)} MB). Considere otimizações adicionais")
            }
            else -> {
                recommendations.add("Tamanho do APK está dentro do esperado (${String.format("%.2f", apkInfo.sizeMB)} MB)")
            }
        }
        
        return AnalysisResult(
            apkInfo = apkInfo,
            recommendations = recommendations,
            isOptimal = isOptimal
        )
    }
    
    /**
     * Data class para informações do APK
     */
    data class ApkInfo(
        val path: String,
        val sizeBytes: Long,
        val sizeMB: Double,
        val sizeKB: Double,
        val versionCode: Long,
        val versionName: String
    )
    
    /**
     * Data class para resultado da análise
     */
    data class AnalysisResult(
        val apkInfo: ApkInfo?,
        val recommendations: List<String>,
        val isOptimal: Boolean
    )
}

