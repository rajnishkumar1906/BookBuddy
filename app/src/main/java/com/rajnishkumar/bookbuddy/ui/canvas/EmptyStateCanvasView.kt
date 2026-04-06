package com.rajnishkumar.bookbuddy.ui.canvas

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.rajnishkumar.bookbuddy.R

import android.util.Log

class EmptyStateCanvasView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val TAG = "EmptyStateCanvasView"
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var primaryColor = ContextCompat.getColor(context, R.color.primary)
    private var animTime = 0f

    init {
        Log.d(TAG, "😴 Initializing EmptyStateCanvasView")
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2
        val cy = h / 2
        
        animTime += 0.05f
        val hover = Math.sin(animTime.toDouble()).toFloat() * 20f
        
        // Draw a "searching" magnifying glass or a sleeping book
        paint.color = primaryColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 8f
        paint.alpha = 100
        
        // Outer glow circle
        val pulse = 100 + (Math.sin(animTime.toDouble() * 0.5).toFloat() * 50)
        canvas.drawCircle(cx, cy + hover, 150f + pulse/10, paint)
        
        // Draw a book shape
        paint.style = Paint.Style.FILL
        paint.alpha = 200
        val bw = 120f
        val bh = 160f
        val rect = RectF(cx - bw/2, cy - bh/2 + hover, cx + bw/2, cy + bh/2 + hover)
        canvas.drawRoundRect(rect, 15f, 15f, paint)
        
        // Draw "Zzz" for sleeping/empty effect
        paint.color = Color.GRAY
        paint.textSize = 40f
        paint.style = Paint.Style.FILL
        paint.alpha = (150 + Math.sin(animTime.toDouble()).toFloat() * 100).toInt().coerceIn(0, 255)
        
        canvas.drawText("Z", cx + 80f, cy - 100f + hover * 0.5f, paint)
        canvas.drawText("z", cx + 110f, cy - 130f + hover * 0.3f, paint)
        canvas.drawText("z", cx + 130f, cy - 150f + hover * 0.1f, paint)

        postInvalidateOnAnimation()
    }
}
