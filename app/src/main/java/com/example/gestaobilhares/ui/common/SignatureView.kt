package com.example.gestaobilhares.ui.common

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.example.gestaobilhares.utils.SignaturePoint
// import com.example.gestaobilhares.core.utils.SignatureStatistics // TODO: Classe removida
import java.io.ByteArrayOutputStream

/**
 * View personalizada para captura de assinatura digital.
 * Implementação nativa sem dependências externas.
 * ✅ CONFORMIDADE JURÍDICA CLÁUSULA 9.3: Atualizado para capturar metadados completos
 */
class SignatureView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "SignatureView"
    }

    private val paint = Paint().apply {
        color = Color.BLACK
        strokeWidth = 4f
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    private val path = Path()
    private val paths = mutableListOf<Path>()
    
    // ✅ CONFORMIDADE JURÍDICA CLÁUSULA 9.3: Metadados da assinatura para análise jurídica
    private val signaturePoints = mutableListOf<SignaturePoint>()
    private var startTime = 0L
    private var lastTouchTime = 0L
    
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    private var onSignedListener: OnSignedListener? = null

    interface OnSignedListener {
        fun onStartSigning()
        fun onSigned()
        fun onClear()
    }

    fun setOnSignedListener(listener: OnSignedListener?) {
        onSignedListener = listener
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Desenhar fundo branco
        canvas.drawColor(Color.WHITE)
        
        // Desenhar todos os caminhos
        for (path in paths) {
            canvas.drawPath(path, paint)
        }
        
        // Desenhar caminho atual
        canvas.drawPath(path, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // ✅ CORREÇÃO CRÍTICA: Interceptar TODOS os eventos de toque para evitar scroll
        parent?.requestDisallowInterceptTouchEvent(true)
        
        val x = event.x
        val y = event.y
        val currentTime = System.currentTimeMillis()

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // ✅ CORREÇÃO: Iniciar novo path para cada toque
                path.reset()
                path.moveTo(x, y)
                lastTouchX = x
                lastTouchY = y
                lastTouchTime = currentTime
                
                // ✅ CONFORMIDADE JURÍDICA CLÁUSULA 9.3: Iniciar captura de metadados
                if (startTime == 0L) {
                    startTime = currentTime
                }
                captureSignaturePoint(x, y, event.pressure, currentTime, 0f)
                
                onSignedListener?.onStartSigning()
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = Math.abs(x - lastTouchX)
                val dy = Math.abs(y - lastTouchY)
                
                if (dx >= 2 || dy >= 2) {
                    // ✅ CORREÇÃO: Usar quadTo para linhas suaves
                    path.quadTo(lastTouchX, lastTouchY, (x + lastTouchX) / 2, (y + lastTouchY) / 2)
                    
                    // ✅ CONFORMIDADE JURÍDICA CLÁUSULA 9.3: Calcular velocidade e capturar ponto
                    val timeDelta = currentTime - lastTouchTime
                    val distance = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                    val velocity = if (timeDelta > 0) distance / timeDelta else 0f
                    captureSignaturePoint(x, y, event.pressure, currentTime, velocity)
                    
                    lastTouchX = x
                    lastTouchY = y
                    lastTouchTime = currentTime
                }
            }
            MotionEvent.ACTION_UP -> {
                // ✅ CORREÇÃO: Finalizar path e adicionar à lista
                path.lineTo(x, y)
                paths.add(Path(path))
                
                // ✅ CONFORMIDADE JURÍDICA CLÁUSULA 9.3: Capturar ponto final
                val timeDelta = currentTime - lastTouchTime
                val dx = Math.abs(x - lastTouchX)
                val dy = Math.abs(y - lastTouchY)
                val distance = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                val velocity = if (timeDelta > 0) distance / timeDelta else 0f
                captureSignaturePoint(x, y, event.pressure, currentTime, velocity)
                
                path.reset()
                onSignedListener?.onSigned()
            }
            MotionEvent.ACTION_CANCEL -> {
                // ✅ CORREÇÃO: Limpar path em caso de cancelamento
                path.reset()
            }
        }

        invalidate()
        return true
    }
    
    /**
     * ✅ CONFORMIDADE JURÍDICA CLÁUSULA 9.3: Captura ponto da assinatura com metadados
     */
    private fun captureSignaturePoint(
        x: Float,
        y: Float,
        pressure: Float,
        timestamp: Long,
        velocity: Float
    ) {
        val point = SignaturePoint(
            x = x,
            y = y,
            pressure = pressure,
            timestamp = timestamp,
            velocity = velocity
        )
        signaturePoints.add(point)
    }

    /**
     * Verifica se há assinatura
     */
    fun isEmpty(): Boolean = paths.isEmpty()

    /**
     * Verifica se há assinatura (alias para isEmpty)
     */
    fun hasSignature(): Boolean = !isEmpty()

    /**
     * Limpa a assinatura
     */
    fun clear() {
        paths.clear()
        path.reset()
        signaturePoints.clear()
        startTime = 0L
        lastTouchTime = 0L
        invalidate()
        onSignedListener?.onClear()
    }
    
    /**
     * ✅ CONFORMIDADE JURÍDICA CLÁUSULA 9.3: Obtém duração da assinatura
     */
    private fun getSignatureDuration(): Long {
        return if (startTime > 0 && signaturePoints.isNotEmpty()) {
            val lastPoint = signaturePoints.maxByOrNull { it.timestamp }
            lastPoint?.timestamp?.minus(startTime) ?: 0L
        } else 0L
    }

    /**
     * Libera o intercept de toque (para permitir scroll quando necessário)
     */
    fun releaseTouchIntercept() {
        parent?.requestDisallowInterceptTouchEvent(false)
    }

    /**
     * Obtém a assinatura como Bitmap
     */
    val signatureBitmap: Bitmap?
        get() = if (isEmpty()) null else {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            
            for (path in paths) {
                canvas.drawPath(path, paint)
            }
            bitmap
        }

    /**
     * Obtém a assinatura como Bitmap transparente
     */
    val transparentSignatureBitmap: Bitmap?
        get() = if (isEmpty()) null else {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            
            for (path in paths) {
                canvas.drawPath(path, paint)
            }
            bitmap
        }

    /**
     * ✅ CONFORMIDADE JURÍDICA CLÁUSULA 9.3: Obtém estatísticas da assinatura para análise jurídica
     * TODO: Retornar objeto SignatureStatistics quando classe for recriada
     */
    fun getSignatureStatistics(): Map<String, Any> {
        val totalPoints = signaturePoints.size
        val duration = getSignatureDuration()
        val averagePressure = if (totalPoints > 0) {
            signaturePoints.map { it.pressure }.average()
        } else 0.0
        val averageVelocity = if (totalPoints > 0) {
            signaturePoints.map { it.velocity }.average()
        } else 0.0
        
        Log.d(TAG, "Estatísticas calculadas: pontos=$totalPoints, duração=${duration}ms, pressão=${averagePressure}, velocidade=${averageVelocity}")
        
        return mapOf(
            "totalPoints" to totalPoints,
            "duration" to duration,
            "averagePressure" to averagePressure.toFloat(),
            "averageVelocity" to averageVelocity.toFloat(),
            "startTime" to startTime
        )
    }

    /**
     * Converte Bitmap para Base64
     */
    fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        val byteArray = outputStream.toByteArray()
        return android.util.Base64.encodeToString(byteArray, android.util.Base64.DEFAULT)
    }
}
