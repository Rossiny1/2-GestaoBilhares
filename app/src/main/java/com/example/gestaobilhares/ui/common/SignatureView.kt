package com.example.gestaobilhares.ui.common

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import java.io.ByteArrayOutputStream

/**
 * View personalizada para captura de assinatura digital.
 * Implementação nativa sem dependências externas.
 */
class SignatureView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        color = Color.BLACK
        strokeWidth = 4f
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    private val path = Path()
    private val paths = mutableListOf<Path>()
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

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // ✅ CORREÇÃO: Iniciar novo path para cada toque
                path.reset()
                path.moveTo(x, y)
                lastTouchX = x
                lastTouchY = y
                onSignedListener?.onStartSigning()
            }
            MotionEvent.ACTION_MOVE -> {
                // ✅ CORREÇÃO: Usar quadTo para linhas suaves
                path.quadTo(lastTouchX, lastTouchY, (x + lastTouchX) / 2, (y + lastTouchY) / 2)
                lastTouchX = x
                lastTouchY = y
            }
            MotionEvent.ACTION_UP -> {
                // ✅ CORREÇÃO: Finalizar path e adicionar à lista
                path.lineTo(x, y)
                paths.add(Path(path))
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
        invalidate()
        onSignedListener?.onClear()
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
     * Obtém estatísticas da assinatura
     */
    fun getSignatureStatistics(): SignatureStatistics {
        return SignatureStatistics(
            pointCount = paths.size * 5, // Aproximação mais realista
            isEmpty = isEmpty()
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

    /**
     * Classe para estatísticas da assinatura
     */
    data class SignatureStatistics(
        val pointCount: Int,
        val isEmpty: Boolean
    ) {
        fun isValidSignature(): Boolean = pointCount > 3 && !isEmpty
    }
}
