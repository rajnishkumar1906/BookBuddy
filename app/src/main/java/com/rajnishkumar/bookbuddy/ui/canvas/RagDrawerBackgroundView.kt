package com.rajnishkumar.bookbuddy.ui.canvas

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import java.util.*
import android.util.Log

class RagDrawerBackgroundView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val random = Random()
    private val particles = mutableListOf<DataParticle>()
    private val numParticles = 15 // Slightly fewer but much larger for better visibility

    private val colors = listOf("#6366F1", "#8B5CF6", "#EC4899", "#3B82F6", "#10B981")

    init {
        paint.style = Paint.Style.FILL
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        particles.clear()
        for (i in 0 until numParticles) {
            particles.add(
                DataParticle(
                    x = random.nextFloat() * w,
                    y = random.nextFloat() * h,
                    size = 40f + random.nextFloat() * 80f, // INCREASED SIZE: From 4-12 to 40-120
                    speed = 0.3f + random.nextFloat() * 1.5f,
                    color = Color.parseColor(colors[random.nextInt(colors.size)]),
                    alpha = 30 + random.nextInt(40) // Slightly lower alpha for overlapping transparency
                )
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Soft gradient background
        val bgPaint = Paint().apply {
            shader = LinearGradient(0f, 0f, 0f, height.toFloat(), 
                Color.parseColor("#FDFDFF"), Color.parseColor("#F3F4F6"), Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Draw tech grid
        paint.strokeWidth = 1f
        paint.style = Paint.Style.STROKE
        paint.color = Color.parseColor("#6366F1")
        paint.alpha = 10
        for (i in 0..width step 80) {
            canvas.drawLine(i.toFloat(), 0f, i.toFloat(), height.toFloat(), paint)
        }
        for (i in 0..height step 80) {
            canvas.drawLine(0f, i.toFloat(), width.toFloat(), i.toFloat(), paint)
        }

        // Draw Moving Data Particles (Bubbles)
        paint.style = Paint.Style.FILL
        particles.forEach { p ->
            p.y -= p.speed
            // Drift side to side
            p.x += (Math.sin(p.y.toDouble() * 0.01) * 0.5).toFloat()

            if (p.y < -p.size * 2) {
                p.y = height.toFloat() + p.size * 2
                p.x = random.nextFloat() * width
            }

            paint.color = p.color
            paint.alpha = p.alpha
            
            // Draw large circles (Bubbles)
            canvas.drawCircle(p.x, p.y, p.size / 2, paint)
            
            // Draw a subtle highlight shine inside the bubble
            paint.alpha = p.alpha + 20
            canvas.drawCircle(p.x - p.size/4, p.y - p.size/4, p.size / 6, paint)
        }

        postInvalidateOnAnimation()
    }

    private data class DataParticle(
        var x: Float,
        var y: Float,
        val size: Float,
        val speed: Float,
        val color: Int,
        val alpha: Int
    )
}
