package com.example.gestaobilhares.ui.contracts

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class SignatureView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    private val paint = Paint().apply {
        color = Color.BLACK
        strokeWidth = 5f
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }
    
    private val path = Path()
    private val paths = mutableListOf<Path>()
    
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
    
    fun addPath(newPath: Path) {
        paths.add(Path(newPath))
        invalidate()
    }
    
    fun clear() {
        path.reset()
        paths.clear()
        invalidate()
    }
    
    fun getSignatureBitmap(): Bitmap? {
        if (paths.isEmpty() && path.isEmpty) {
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
        
        // Desenhar linha atual
        canvas.drawPath(path, paint)
        
        return bitmap
    }
    
    fun hasSignature(): Boolean {
        return !paths.isEmpty() || !path.isEmpty
    }
}
