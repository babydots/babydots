package com.serwylo.babydots

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.min

class AnimatedDots @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class ColourScheme {
        Monochrome,
        SplashOfColour,
        Rainbow,
    }

    enum class Size {
        Large,
        Medium,
        Small,
    }

    enum class Speed {
        Slow,
        Normal,
        Fast,
    }

    var size: Size = Size.Medium
    var colourScheme: ColourScheme = ColourScheme.SplashOfColour
    var speed: Speed = Speed.Normal
        set(value) {
            val current = animator.animatedValue as Float

            animator.duration = when(value) {
                Speed.Slow -> (DURATION * 3).toLong()
                Speed.Normal -> DURATION.toLong()
                Speed.Fast -> (DURATION * 0.4).toLong()
            }

            animator.setCurrentFraction(current)

            field = value
        }

    private val dots = mutableListOf<Dot>()
    private val dotRadius = DOT_RADIUS
    private val animator = ValueAnimator.ofFloat(0f, 1f)

    companion object {
        const val DOT_RADIUS = 120f
        const val DURATION = 12000
    }

    // These items are used regularly in the onDraw method, so to prevent too many object
    // allocations they are created as member variables.
    private val point = FloatArray(2)
    private val dotFillPaint = Paint()
    private val dotStrokePaint = Paint()
    private val linePaint = Paint()

    private val dotFillPaints = mapOf(
            ColourScheme.Monochrome to context.resources.getIntArray(R.array.dotsSchemeMonochrome),
            ColourScheme.SplashOfColour to context.resources.getIntArray(R.array.dotsSchemeSplashOfColour),
            ColourScheme.Rainbow to context.resources.getIntArray(R.array.dotsSchemeRainbow),
    )

    private val dotStrokePaints = mapOf(
        ColourScheme.Monochrome to context.resources.getIntArray(R.array.dotsSchemeMonochromeBorders),
        ColourScheme.SplashOfColour to context.resources.getIntArray(R.array.dotsSchemeSplashOfColourBorders),
        ColourScheme.Rainbow to context.resources.getIntArray(R.array.dotsSchemeRainbowBorders),
    )

    private val numDots = 15

    /**
     * For debugging purposes, will show the paths each dot takes in the background.
     */
    private val drawPaths = false

    init {

        dotStrokePaint.style = Paint.Style.STROKE
        dotStrokePaint.strokeWidth = 8f

        linePaint.style = Paint.Style.STROKE
        linePaint.color = Color.BLACK

        animator.repeatCount = ValueAnimator.INFINITE
        animator.repeatMode = ValueAnimator.RESTART
        animator.duration = 12000
        animator.interpolator = LinearInterpolator()
        animator.addUpdateListener { invalidate() }
        animator.start()

    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        restartDots()
    }

    fun restartDots() {
        dots.clear()

        for (x in 0 until numDots) {
            dots.add(Dot(createRandomPath(width, height)))
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas?.drawColor(Color.WHITE)

        val radius = when (size) {
            Size.Large -> dotRadius * 1.5
            Size.Small -> dotRadius * 0.5
            else -> dotRadius
        }

        val colourScheme = dotFillPaints[colourScheme] ?: intArrayOf(android.R.color.black)
        val borderColourScheme = dotStrokePaints[this.colourScheme] ?: intArrayOf(android.R.color.black)
        var colours = colourScheme.iterator()
        var borderColours = borderColourScheme.iterator()

        dots.forEach { dot ->
            dot.pathMeasure.getPosTan(dot.pathMeasure.length * animator.animatedValue as Float, point, null)

            if (!colours.hasNext()) {
                colours = colourScheme.iterator()
            }

            if (!borderColours.hasNext()) {
                borderColours = borderColourScheme.iterator()
            }

            dotFillPaint.color = colours.next()
            dotStrokePaint.color = borderColours.next()

            val x = point[0]
            val y = point[1]

            if (x > -radius.toFloat() && x < width + radius.toFloat() && y > -radius.toFloat() && y < height + radius.toFloat()) {
                canvas?.drawCircle(x, y, radius.toFloat(), dotFillPaint)
                canvas?.drawCircle(x, y, radius.toFloat(), dotStrokePaint)
            }

            if (drawPaths) {
                canvas?.drawPath(dot.path, linePaint)
            }
        }
    }

    private fun createRandomPath(w: Int, h: Int): Path {

        val maxMovement = min(w, h) / 2

        val startX = Math.random().toFloat() * w
        val startY = Math.random().toFloat() * h

        val points = mutableListOf<Point>()

        var x = startX
        var y = startY

        val numPoints = 4

        for (i in 0..numPoints * 3) {
            x += Math.random().toFloat() * maxMovement - maxMovement / 2
            y += Math.random().toFloat() * maxMovement - maxMovement / 2

            points.add(Point(x.toInt(), y.toInt()))
        }

        val path = Path()
        path.moveTo(startX, startY)
        for (i in 0 until numPoints) {
            path.cubicTo(
                    points[i * 3].x.toFloat(), points[i * 3].y.toFloat(),
                    points[i * 3 + 1].x.toFloat(), points[i * 3 + 1].y.toFloat(),
                    points[i * 3 + 2].x.toFloat(), points[i * 3 + 2].y.toFloat(),
            )
        }

        path.quadTo(startX, startY, startX, startY)

        return path
    }

    data class Dot(val path: Path) {
        val pathMeasure = PathMeasure(path, true)
    }

}