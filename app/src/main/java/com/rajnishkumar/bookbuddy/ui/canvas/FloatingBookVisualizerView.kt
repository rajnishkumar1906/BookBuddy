package com.rajnishkumar.bookbuddy.ui.canvas

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.rajnishkumar.bookbuddy.R
import java.util.*
import kotlin.math.sin

class FloatingBookVisualizerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bubbles = mutableListOf<Bubble>()
    private val random = Random()
    private var numBubbles = 20
    private var primaryColor = ContextCompat.getColor(context, R.color.primary)
    private var isAnimating = false

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.FloatingBookVisualizerView,
            0, 0
        ).apply {
            try {
                numBubbles = getInt(R.styleable.FloatingBookVisualizerView_numBubbles, 20)
                primaryColor = getColor(R.styleable.FloatingBookVisualizerView_bubbleColor, primaryColor)
            } finally {
                recycle()
            }
        }
        // Use hardware acceleration for smoother canvas drawing
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isAnimating = true
        postInvalidateOnAnimation()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        isAnimating = false
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        isAnimating = visibility == VISIBLE
        if (isAnimating) postInvalidateOnAnimation()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        bubbles.clear()
        for (i in 0 until numBubbles) {
            bubbles.add(
                Bubble(
                    x = random.nextFloat() * w,
                    y = random.nextFloat() * h,
                    radius = 10f + random.nextFloat() * 30f,
                    speed = 0.3f + random.nextFloat() * 1.2f,
                    swing = 0.01f + random.nextFloat() * 0.04f,
                    phase = random.nextFloat() * Math.PI.toFloat(),
                    alpha = 20 + random.nextInt(50)
                )
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (numBubbles <= 0) return

        bubbles.forEach { bubble ->
            bubble.y -= bubble.speed
            bubble.x += sin((bubble.y * bubble.swing + bubble.phase).toDouble()).toFloat()

            if (bubble.y + bubble.radius < 0) {
                bubble.y = height.toFloat() + bubble.radius
                bubble.x = random.nextFloat() * width
            }

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f
            paint.color = primaryColor
            paint.alpha = bubble.alpha
            canvas.drawCircle(bubble.x, bubble.y, bubble.radius, paint)

            paint.style = Paint.Style.FILL
            paint.alpha = bubble.alpha / 2
            val shineRadius = bubble.radius * 0.3f
            canvas.drawCircle(bubble.x - bubble.radius * 0.3f, bubble.y - bubble.radius * 0.3f, shineRadius, paint)
        }

        if (isAnimating) {
            postInvalidateOnAnimation()
        }
    }

    private data class Bubble(
        var x: Float,
        var y: Float,
        val radius: Float,
        val speed: Float,
        val swing: Float,
        val phase: Float,
        val alpha: Int
    )
}
