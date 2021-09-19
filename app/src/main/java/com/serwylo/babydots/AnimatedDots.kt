package com.serwylo.babydots

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt

class AnimatedDots @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class ColourScheme {
        Monochrome,
        SplashOfColour,
        Rainbow,
        Dark,
        Neon,
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
        set(value) {
            field = value
            invalidate()
        }

    var colourScheme: ColourScheme = ColourScheme.SplashOfColour
        set(value) {
            field = value
            invalidate()
        }

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
    private val touchLocations = mutableMapOf<Int, Point>()
    private val dotRadius = DOT_RADIUS
    private val animator = ValueAnimator.ofFloat(0f, 1f)

    companion object {
        const val DOT_RADIUS = 120f

        const val DURATION = 20000

        const val PATH_MIN_POINTS = 4
        const val PATH_MAX_POINTS = 12
    }

    // These items are used regularly in the onDraw method, so to prevent too many object
    // allocations they are created as member variables.
    private val point = FloatArray(2)
    private val dotFillPaint = Paint()
    private val dotStrokePaint = Paint()
    private val linePaint = Paint()

    private val dotFillPaints = mapOf(
        ColourScheme.Monochrome to context.resources.getIntArray(R.array.scheme_monochrome_dots),
        ColourScheme.SplashOfColour to context.resources.getIntArray(R.array.scheme_splash_of_colour_dots),
        ColourScheme.Rainbow to context.resources.getIntArray(R.array.scheme_rainbow_dots),
        ColourScheme.Dark to context.resources.getIntArray(R.array.scheme_dark_dots),
        ColourScheme.Neon to context.resources.getIntArray(R.array.scheme_neon_dots),
    )

    private val dotStrokePaints = mapOf(
        ColourScheme.Monochrome to context.resources.getIntArray(R.array.scheme_monochrome_dot_borders),
        ColourScheme.SplashOfColour to context.resources.getIntArray(R.array.scheme_splash_of_colour_dot_borders),
        ColourScheme.Rainbow to context.resources.getIntArray(R.array.scheme_rainbow_dot_borders),
        ColourScheme.Dark to context.resources.getIntArray(R.array.scheme_dark_dot_borders),
        ColourScheme.Neon to context.resources.getIntArray(R.array.scheme_neon_dot_borders),
    )

    private val backgroundPaints = mapOf(
        ColourScheme.Monochrome to R.color.scheme_monochrome_background,
        ColourScheme.SplashOfColour to R.color.scheme_splash_of_colour_background,
        ColourScheme.Rainbow to R.color.scheme_rainbow_background,
        ColourScheme.Dark to R.color.scheme_dark_background,
        ColourScheme.Neon to R.color.scheme_neon_background,
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

    /**
     * Record the location of each touch event. Later on during rendering we will use this information
     * to render touched dots differently.
     *
     * Doesn't implement 'performClick' because a click probably means "a dot was touched", however
     * this happens many times for one ACTION_POINTER_DOWN, as dots move in or out of the cursors path.
     * There also isn't much to be gained in terms of accessibility from this click.
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {

        if (event != null) {
            if (event.actionMasked == MotionEvent.ACTION_POINTER_DOWN || event.actionMasked == MotionEvent.ACTION_DOWN) {
                (0 until event.pointerCount).map { pointerId ->
                    touchLocations[pointerId] = Point(event.getX(pointerId).toInt(), event.getY(pointerId).toInt())
                }
            } else if (event.actionMasked == MotionEvent.ACTION_MOVE) {
                (0 until event.pointerCount).map { pointerId ->
                    touchLocations[pointerId]?.set(event.getX(pointerId).toInt(), event.getY(pointerId).toInt())
                }
            } else if (event.actionMasked == MotionEvent.ACTION_POINTER_UP || event.actionMasked == MotionEvent.ACTION_UP) {
                (0 until event.pointerCount).map { pointerId ->
                    touchLocations.remove(pointerId)
                }
            } else if (event.actionMasked == MotionEvent.ACTION_CANCEL) {
                touchLocations.clear()
            }
        }

        return super.onTouchEvent(event)
    }

    fun restartDots() {
        dots.clear()

        for (x in 0 until numDots) {
            dots.add(Dot(createRandomPath(width, height)))
        }
    }

    private fun dotRadius() = when (size) {
        Size.Large -> dotRadius * 1.5f
        Size.Small -> dotRadius * 0.5f
        else -> dotRadius
    }

    private var lastTimeStep: Long = System.currentTimeMillis()

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        val deltaMs = System.currentTimeMillis() - lastTimeStep
        val delta = deltaMs / 1000f
        lastTimeStep += deltaMs

        canvas?.drawColor(context.resources.getColor(backgroundPaints[colourScheme] ?: android.R.color.white))

        val colourScheme = dotFillPaints[colourScheme] ?: intArrayOf(android.R.color.black)
        val borderColourScheme = dotStrokePaints[this.colourScheme] ?: intArrayOf(android.R.color.black)
        var colours = colourScheme.iterator()
        var borderColours = borderColourScheme.iterator()
        val radius = dotRadius()

        dots.forEach { dot ->
            if (drawPaths) {
                canvas?.drawPath(dot.path, linePaint)
            }

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

            val currentSize = radius + (radius * dot.zoomAnimation)

            val isTouching = isTouching(x, y, currentSize)
            if (isTouching && dot.zoomAnimation < 1f) {
                dot.zoomAnimation = (dot.zoomAnimation + 10f * delta).coerceAtMost(1f)
            } else if (!isTouching && dot.zoomAnimation > 0f) {
                dot.zoomAnimation = (dot.zoomAnimation - 10f * delta).coerceAtLeast(0f)
            }

            if (x > -radius && x < width + radius && y > -radius && y < height + radius) {
                val newSize = radius + (radius * dot.zoomAnimation)
                canvas?.drawCircle(x, y, newSize, dotFillPaint)
                canvas?.drawCircle(x, y, newSize, dotStrokePaint)
            }
        }
    }

    private fun isTouching(x: Float, y: Float, radius: Float): Boolean {

        return touchLocations.values.any { touchPoint ->

            // Use a bounding box check to eliminate those which are definitely not touching...
            if (touchPoint.x < x - radius || touchPoint.x > x + radius || touchPoint.y < y - radius || touchPoint.y > y + radius) {

                false

            } else {

                // ...before performing a more accurate check.
                val dx = abs(touchPoint.x - x)
                val dy = abs(touchPoint.y - y)
                val distance = sqrt(dx * dx + dy * dy)

                distance < radius

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

        val numPoints = (Math.random() * (PATH_MAX_POINTS - PATH_MIN_POINTS) + PATH_MIN_POINTS).toInt()

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
        var zoomAnimation: Float = 0f
    }

}