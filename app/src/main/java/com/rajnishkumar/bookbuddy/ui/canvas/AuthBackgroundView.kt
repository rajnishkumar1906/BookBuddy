package com.rajnishkumar.bookbuddy.ui.canvas

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.rajnishkumar.bookbuddy.R
import java.util.*

import android.util.Log

class AuthBackgroundView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val TAG = "AuthBackgroundView"
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val particles = mutableListOf<Particle>()
    private val random = Random()
    private val numParticles = 40
    private var primaryColor = ContextCompat.getColor(context, R.color.primary)

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        Log.d(TAG, "📏 Size changed: ${w}x${h}. Generating $numParticles particles.")
        particles.clear()
        for (i in 0 until numParticles) {
            particles.add(
                Particle(
                    x = random.nextFloat() * w,
                    y = random.nextFloat() * h,
                    radius = 2f + random.nextFloat() * 4f,
                    vx = (random.nextFloat() - 0.5f) * 0.8f,
                    vy = (random.nextFloat() - 0.5f) * 0.8f,
                    alpha = 40 + random.nextInt(60)
                )
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val w = width.toFloat()
        val h = height.toFloat()

        particles.forEach { p ->
            p.x += p.vx
            p.y += p.vy

            if (p.x < 0) p.x = w
            if (p.x > w) p.x = 0f
            if (p.y < 0) p.y = h
            if (p.y > h) p.y = 0f

            paint.color = primaryColor
            paint.alpha = p.alpha
            canvas.drawCircle(p.x, p.y, p.radius, paint)
        }

        // Draw connections for nearby particles
        paint.strokeWidth = 1f
        for (i in particles.indices) {
            for (j in i + 1 until particles.size) {
                val p1 = particles[i]
                val p2 = particles[j]
                val dx = p1.x - p2.x
                val dy = p1.y - p2.y
                val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()

                if (dist < 200f) {
                    paint.alpha = ((1f - dist / 200f) * 40).toInt()
                    canvas.drawLine(p1.x, p1.y, p2.x, p2.y, paint)
                }
            }
        }

        postInvalidateOnAnimation()
    }

    private data class Particle(
        var x: Float,
        var y: Float,
        var radius: Float,
        var vx: Float,
        var vy: Float,
        val alpha: Int
    )
}
