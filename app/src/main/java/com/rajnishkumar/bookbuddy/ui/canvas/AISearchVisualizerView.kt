package com.rajnishkumar.bookbuddy.ui.canvas

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.rajnishkumar.bookbuddy.R
import java.util.*
import kotlin.math.sqrt

class AISearchVisualizerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val nodes = mutableListOf<Node>()
    private val random = Random()
    private val numNodes = 15 // Fewer nodes but much bigger
    private val maxDistance = 450f
    
    private var primaryColor: Int = ContextCompat.getColor(context, R.color.primary)
    private var isSearching = false
    private var pulseRadius = 0f
    private var pulseAlpha = 255

    init {
        linePaint.strokeWidth = 4f
        linePaint.style = Paint.Style.STROKE
    }

    fun setSearching(searching: Boolean) {
        isSearching = searching
        if (searching) {
            pulseRadius = 0f
            pulseAlpha = 255
        }
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        nodes.clear()
        for (i in 0 until numNodes) {
            nodes.add(
                Node(
                    x = random.nextFloat() * w,
                    y = random.nextFloat() * h,
                    vx = (random.nextFloat() - 0.5f) * 2.5f,
                    vy = (random.nextFloat() - 0.5f) * 2.5f,
                    radius = 20f + random.nextFloat() * 40f // INCREASED SIZE: 20-60f
                )
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val w = width.toFloat()
        val h = height.toFloat()

        if (isSearching) {
            pulseRadius += 12f
            pulseAlpha -= 4
            if (pulseAlpha <= 0) {
                pulseRadius = 0f
                pulseAlpha = 255
            }
            
            paint.color = primaryColor
            paint.alpha = pulseAlpha / 2
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 15f // BOLDER PULSE
            canvas.drawCircle(w / 2, h / 2, pulseRadius, paint)
            canvas.drawCircle(w / 2, h / 2, pulseRadius * 0.6f, paint)
        }

        nodes.forEach { node ->
            node.x += node.vx
            node.y += node.vy

            if (node.x < -node.radius) node.x = w + node.radius
            if (node.x > w + node.radius) node.x = -node.radius
            if (node.y < -node.radius) node.y = h + node.radius
            if (node.y > h + node.radius) node.y = -node.radius

            paint.color = primaryColor
            paint.style = Paint.Style.FILL
            paint.alpha = 60 // Soft transparency for big bubbles
            canvas.drawCircle(node.x, node.y, node.radius, paint)
            
            // Inner core
            paint.alpha = 120
            canvas.drawCircle(node.x, node.y, node.radius * 0.4f, paint)
        }

        for (i in nodes.indices) {
            for (j in i + 1 until nodes.size) {
                val n1 = nodes[i]
                val n2 = nodes[j]
                val dist = sqrt(((n1.x-n2.x)*(n1.x-n2.x) + (n1.y-n2.y)*(n1.y-n2.y)).toDouble()).toFloat()

                if (dist < maxDistance) {
                    val alpha = (1f - dist / maxDistance) * 80
                    linePaint.color = primaryColor
                    linePaint.alpha = alpha.toInt()
                    canvas.drawLine(n1.x, n1.y, n2.x, n2.y, linePaint)
                }
            }
        }

        postInvalidateOnAnimation()
    }

    private data class Node(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var radius: Float
    )
}
