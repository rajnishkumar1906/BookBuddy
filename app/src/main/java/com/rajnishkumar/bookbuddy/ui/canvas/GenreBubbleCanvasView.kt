package com.rajnishkumar.bookbuddy.ui.canvas

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import java.util.*
import kotlin.math.*

data class GenreBubble(
    val genre: String,
    val count: Int,
    var x: Float = 0f,
    var y: Float = 0f,
    var radius: Float = 0f,
    var color: Int = 0,
    var vx: Float = 0f,
    var vy: Float = 0f
)

class GenreBubbleCanvasView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val bubbles = mutableListOf<GenreBubble>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = Color.WHITE
        typeface = Typeface.DEFAULT_BOLD
    }
    
    private val random = Random()
    private val colors = listOf("#6366F1", "#8B5CF6", "#EC4899", "#3B82F6", "#10B981", "#F59E0B")

    fun setData(stats: Map<String, Int>) {
        if (stats.isEmpty()) return
        bubbles.clear()

        val maxCount = stats.values.maxOrNull() ?: 1
        
        stats.entries.take(8).forEach { (genre, count) ->
            bubbles.add(
                GenreBubble(
                    genre = genre,
                    count = count,
                    color = Color.parseColor(colors[bubbles.size % colors.size]),
                    vx = (random.nextFloat() - 0.5f) * 2f,
                    vy = (random.nextFloat() - 0.5f) * 2f
                )
            )
        }
        
        if (width > 0 && height > 0) layoutBubbles(width, height)
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (bubbles.isNotEmpty()) layoutBubbles(w, h)
    }

    private fun layoutBubbles(w: Int, h: Int) {
        val maxCount = bubbles.maxOfOrNull { it.count } ?: 1
        val minRadius = w / 8f
        val maxRadius = w / 4f

        bubbles.forEach { bubble ->
            bubble.radius = minRadius + (bubble.count.toFloat() / maxCount) * (maxRadius - minRadius)
            bubble.x = random.nextFloat() * (w - bubble.radius * 2) + bubble.radius
            bubble.y = random.nextFloat() * (h - bubble.radius * 2) + bubble.radius
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        bubbles.forEach { bubble ->
            // Update position (subtle animation)
            bubble.x += bubble.vx
            bubble.y += bubble.vy

            // Bounce
            if (bubble.x - bubble.radius < 0 || bubble.x + bubble.radius > width) bubble.vx *= -1
            if (bubble.y - bubble.radius < 0 || bubble.y + bubble.radius > height) bubble.vy *= -1

            // Draw bubble
            paint.color = bubble.color
            paint.alpha = 200
            canvas.drawCircle(bubble.x, bubble.y, bubble.radius, paint)
            
            // Draw text
            textPaint.textSize = bubble.radius * 0.25f
            canvas.drawText(bubble.genre, bubble.x, bubble.y, textPaint)
            
            textPaint.textSize = bubble.radius * 0.18f
            canvas.drawText("${bubble.count} books", bubble.x, bubble.y + (bubble.radius * 0.3f), textPaint)
        }

        if (bubbles.isNotEmpty()) postInvalidateOnAnimation()
    }
}
