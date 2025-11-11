package com.example.gestaobilhares.ui.contracts

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Base64
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.example.gestaobilhares.utils.SignaturePoint
import com.example.gestaobilhares.core.utils.SignatureStatistics
import java.io.ByteArrayOutputStream

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
        strokeWidth = 8f
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }
    
    private val path = Path()
    private val paths = mutableListOf<Path>()
    
    // Metadados da assinatura para validade jurídica
    private val signaturePoints = mutableListOf<SignaturePoint>()
    private var startTime = 0L
    private var lastTouchTime = 0L
    
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Desenhar fundo branco
        canvas.drawColor(Color.WHITE)
        
        // Desenhar todas as linhas da assinatura
        paths.forEach { pathToDraw ->
            canvas.drawPath(pathToDraw, paint)
        }
        
        // Desenhar linha atual
        canvas.drawPath(path, paint)
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        val currentTime = System.currentTimeMillis()
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Solicitar que o parent não intercepte os eventos de toque
                parent?.requestDisallowInterceptTouchEvent(true)
                path.moveTo(x, y)
                lastTouchX = x
                lastTouchY = y
                lastTouchTime = currentTime
                
                // Iniciar captura de metadados
                if (startTime == 0L) {
                    startTime = currentTime
                }
                
                // Capturar ponto inicial da assinatura
                captureSignaturePoint(x, y, event.pressure, currentTime, 0f)
                
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = Math.abs(x - lastTouchX)
                val dy = Math.abs(y - lastTouchY)
                
                // Reduzir threshold para capturar mais pontos (ajustado para assinaturas simples)
                if (dx >= 2 || dy >= 2) {
                    path.quadTo(lastTouchX, lastTouchY, (x + lastTouchX) / 2, (y + lastTouchY) / 2)
                    
                    // Calcular velocidade
                    val timeDelta = currentTime - lastTouchTime
                    val distance = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                    val velocity = if (timeDelta > 0) distance / timeDelta else 0f
                    
                    // Capturar ponto de movimento
                    captureSignaturePoint(x, y, event.pressure, currentTime, velocity)
                    
                    lastTouchX = x
                    lastTouchY = y
                    lastTouchTime = currentTime
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // Liberar o controle do parent
                parent?.requestDisallowInterceptTouchEvent(false)
                path.lineTo(lastTouchX, lastTouchY)
                paths.add(Path(path))
                
                // Capturar ponto final da assinatura
                val timeDelta = currentTime - lastTouchTime
                val dx = Math.abs(x - lastTouchX)
                val dy = Math.abs(y - lastTouchY)
                val distance = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                val velocity = if (timeDelta > 0) distance / timeDelta else 0f
                
                captureSignaturePoint(x, y, event.pressure, currentTime, velocity)
                
                path.reset()
                invalidate()
                return true
            }
        }
        return false
    }
    
    /**
     * Captura ponto da assinatura com metadados para análise jurídica
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
        
        Log.d(TAG, "Ponto capturado: x=$x, y=$y, pressure=$pressure, velocity=$velocity, total=${signaturePoints.size}")
    }
    
    fun clear() {
        path.reset()
        paths.clear()
        signaturePoints.clear()
        startTime = 0L
        lastTouchTime = 0L
        invalidate()
    }
    
    fun getSignatureBitmap(): Bitmap? {
        if (paths.isEmpty() || width <= 0 || height <= 0) {
            return null
        }
        
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Desenhar fundo branco
        canvas.drawColor(Color.WHITE)
        
        // Desenhar todas as linhas da assinatura
        paths.forEach { pathToDraw ->
            canvas.drawPath(pathToDraw, paint)
        }
        
        return bitmap
    }
    
    fun hasSignature(): Boolean {
        return paths.isNotEmpty()
    }
    
    /**
     * Obtém metadados da assinatura para análise jurídica
     */
    fun getSignaturePoints(): List<SignaturePoint> {
        return signaturePoints.toList()
    }
    
    /**
     * Obtém tempo total de assinatura
     */
    fun getSignatureDuration(): Long {
        return if (startTime > 0 && signaturePoints.isNotEmpty()) {
            val lastPoint = signaturePoints.maxByOrNull { it.timestamp }
            lastPoint?.timestamp?.minus(startTime) ?: 0L
        } else 0L
    }
    
    /**
     * Obtém estatísticas da assinatura para análise de autenticidade
     */
    fun getSignatureStatistics(): SignatureStatistics {
        val totalPoints = signaturePoints.size
        val duration = getSignatureDuration()
        val averagePressure = if (totalPoints > 0) {
            signaturePoints.map { it.pressure }.average()
        } else 0.0
        val averageVelocity = if (totalPoints > 0) {
            signaturePoints.map { it.velocity }.average()
        } else 0.0
        
        Log.d(TAG, "Estatísticas calculadas: pontos=$totalPoints, duração=${duration}ms, pressão=${averagePressure}, velocidade=${averageVelocity}")
        
        return SignatureStatistics(
            totalPoints = totalPoints,
            duration = duration,
            averagePressure = averagePressure.toFloat(),
            averageVelocity = averageVelocity.toFloat(),
            startTime = startTime
        )
    }
    
    /**
     * Obtém a assinatura em formato Base64 para armazenamento
     */
    fun getSignatureBase64(): String {
        val bitmap = getSignatureBitmap() ?: return ""
        
        return try {
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            val byteArray = outputStream.toByteArray()
            
            Base64.encodeToString(byteArray, Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao converter assinatura para Base64", e)
            ""
        }
    }
}
