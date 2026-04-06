package com.rajnishkumar.bookbuddy.ui.canvas

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import com.rajnishkumar.bookbuddy.R

class VoiceRippleView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val ripplePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    
    private val primaryColor = ContextCompat.getColor(context, R.color.primary)
    private val rippleCount = 3
    private var progress = 0f
    
    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 2000
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            progress = it.animatedValue as Float
            invalidate()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        animator.start()
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val centerX = width / 2f
        val centerY = height / 2f
        val maxRadius = Math.min(width, height) / 2f

        for (i in 0 until rippleCount) {
            val rippleProgress = (progress + i.toFloat() / rippleCount) % 1f
            val radius = maxRadius * rippleProgress
            val alpha = (255 * (1 - rippleProgress)).toInt()
            
            ripplePaint.color = primaryColor
            ripplePaint.alpha = (alpha * 0.3f).toInt()
            
            canvas.drawCircle(centerX, centerY, radius, ripplePaint)
        }
        
        // Static center circle
        ripplePaint.alpha = 255
        canvas.drawCircle(centerX, centerY, maxRadius * 0.2f, ripplePaint)
    }
}