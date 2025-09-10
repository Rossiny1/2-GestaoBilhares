package com.example.gestaobilhares.ui.contracts

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class SignatureView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
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
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                path.moveTo(x, y)
                lastTouchX = x
                lastTouchY = y
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = Math.abs(x - lastTouchX)
                val dy = Math.abs(y - lastTouchY)
                
                if (dx >= 4 || dy >= 4) {
                    path.quadTo(lastTouchX, lastTouchY, (x + lastTouchX) / 2, (y + lastTouchY) / 2)
                    lastTouchX = x
                    lastTouchY = y
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                path.lineTo(lastTouchX, lastTouchY)
                paths.add(Path(path))
                path.reset()
                invalidate()
                return true
            }
        }
        return false
    }
    
    fun clear() {
        path.reset()
        paths.clear()
        invalidate()
    }
    
    fun getSignatureBitmap(): Bitmap? {
        if (paths.isEmpty()) {
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
}
