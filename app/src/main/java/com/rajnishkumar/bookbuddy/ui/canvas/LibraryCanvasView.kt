package com.rajnishkumar.bookbuddy.ui.canvas

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import java.util.*
import androidx.core.graphics.toColorInt
import androidx.core.graphics.withTranslation
import kotlin.math.sin

class LibraryCanvasView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val random = Random()
    private val books = mutableListOf<FloatingBook>()
    private val stars = mutableListOf<Star>()
    private val numBooks = 12
    private val numStars = 40

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        books.clear()
        stars.clear()
        val colors = listOf("#6366F1", "#8B5CF6", "#EC4899", "#3B82F6")
        
        for (i in 0 until numBooks) {
            books.add(
                FloatingBook(
                    x = random.nextFloat() * w,
                    y = random.nextFloat() * h,
                    size = 50f + random.nextFloat() * 70f,
                    speed = 0.4f + random.nextFloat() * 1.2f,
                    angle = random.nextFloat() * 360f,
                    rotationSpeed = (random.nextFloat() - 0.5f) * 1.5f,
                    color = colors[random.nextInt(colors.size)].toColorInt(),
                    alpha = 15 + random.nextInt(35)
                )
            )
        }

        for (i in 0 until numStars) {
            stars.add(
                Star(
                    x = random.nextFloat() * w,
                    y = random.nextFloat() * h,
                    size = 2f + random.nextFloat() * 5f,
                    alpha = 50 + random.nextInt(150),
                    pulseSpeed = 0.002f + random.nextFloat() * 0.005f
                )
            )
        }
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Draw Shimmering Stars
        stars.forEach { star ->
            val alpha = (100 + 100 * sin(System.currentTimeMillis() * star.pulseSpeed)).toInt().coerceIn(0, 255)
            paint.color = Color.WHITE
            paint.alpha = alpha
            canvas.drawCircle(star.x, star.y, star.size, paint)
        }

        // Draw Floating Books
        books.forEach { book ->
            book.y -= book.speed
            book.angle += book.rotationSpeed
            
            if (book.y + book.size < 0) {
                book.y = height.toFloat() + book.size
                book.x = random.nextFloat() * width
            }

            canvas.withTranslation(book.x, book.y) {
                rotate(book.angle)

                paint.color = book.color
                paint.alpha = book.alpha
                val rect = RectF(-book.size / 2, -book.size / 1.4f, book.size / 2, book.size / 1.4f)
                drawRoundRect(rect, 12f, 12f, paint)

                paint.color = Color.WHITE
                paint.alpha = (book.alpha * 1.5).toInt().coerceAtMost(255)
                val pageRect = RectF(-book.size / 2.5f, -book.size / 1.5f, book.size / 2.2f, book.size / 1.5f)
                drawRect(pageRect, paint)

                paint.color = Color.BLACK
                paint.alpha = 20
                drawLine(-book.size / 3, -book.size / 1.4f, -book.size / 3, book.size / 1.4f, paint)
            }
        }

        postInvalidateOnAnimation()
    }

    private data class FloatingBook(
        var x: Float,
        var y: Float,
        val size: Float,
        val speed: Float,
        var angle: Float,
        val rotationSpeed: Float,
        val color: Int,
        val alpha: Int
    )

    private data class Star(
        val x: Float,
        val y: Float,
        val size: Float,
        var alpha: Int,
        val pulseSpeed: Float
    )
}
