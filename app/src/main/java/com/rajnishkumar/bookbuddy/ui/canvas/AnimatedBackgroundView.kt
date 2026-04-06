package com.rajnishkumar.bookbuddy.ui.canvas

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.rajnishkumar.bookbuddy.R
import java.util.*

class AnimatedBackgroundView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val circles = mutableListOf<Particle>()
    private val random = Random()
    private val numParticles = 15
    private var primaryColor: Int = 0

    init {
        paint.style = Paint.Style.FILL
        primaryColor = ContextCompat.getColor(context, R.color.primary)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        circles.clear()
        for (i in 0 until numParticles) {
            circles.add(
                Particle(
                    x = random.nextFloat() * w,
                    y = random.nextFloat() * h,
                    radius = 50f + random.nextFloat() * 150f,
                    vx = (random.nextFloat() - 0.5f) * 1.5f,
                    vy = (random.nextFloat() - 0.5f) * 1.5f,
                    alpha = 15 + random.nextInt(40)
                )
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        circles.forEach { particle ->
            particle.x += particle.vx
            particle.y += particle.vy

            if (particle.x < -particle.radius) particle.x = width + particle.radius
            if (particle.x > width + particle.radius) particle.x = -particle.radius
            if (particle.y < -particle.radius) particle.y = height + particle.radius
            if (particle.y > height + particle.radius) particle.y = -particle.radius

            paint.color = primaryColor
            paint.alpha = particle.alpha
            canvas.drawCircle(particle.x, particle.y, particle.radius, paint)
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
