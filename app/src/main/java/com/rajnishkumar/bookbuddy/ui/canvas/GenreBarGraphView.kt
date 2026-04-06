package com.rajnishkumar.bookbuddy.ui.canvas

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.rajnishkumar.bookbuddy.R

class GenreBarGraphView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var data: Map<String, Int> = emptyMap()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 30f
        textAlign = Paint.Align.CENTER
    }

    fun setData(stats: Map<String, Int>) {
        // Take top 5 genres for the graph
        data = stats.toList().sortedByDescending { it.second }.take(5).toMap()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (data.isEmpty()) return

        val w = width.toFloat()
        val h = height.toFloat()
        val margin = 80f
        val barWidth = (w - (margin * 2)) / data.size
        val maxCount = data.values.maxOrNull()?.toFloat() ?: 1f

        data.entries.forEachIndexed { index, entry ->
            val barHeight = (entry.value / maxCount) * (h - (margin * 2))
            val left = margin + (index * barWidth) + 20f
            val top = h - margin - barHeight
            val right = left + barWidth - 40f
            val bottom = h - margin

            // Draw Bar
            paint.color = ContextCompat.getColor(context, R.color.primary)
            paint.alpha = 200
            canvas.drawRoundRect(left, top, right, bottom, 12f, 12f, paint)

            // Draw Label
            canvas.drawText(entry.key.take(8), (left + right) / 2, h - margin + 40f, textPaint)
            
            // Draw Value
            canvas.drawText(entry.value.toString(), (left + right) / 2, top - 10f, textPaint)
        }
    }
}
