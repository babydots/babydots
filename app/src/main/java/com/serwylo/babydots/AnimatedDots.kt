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
        BrightRainbow,
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

    enum class Shape {
        Circle,
        Square,
        Triangle,
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

    /**
     * A null value here means "Random shapes" . We don't add a new "Random" entry in the [Shape]
     * enum because it is helpful to be able to guarantee that a [Shape] is something tangeable.
     * This allows us to correctly use "when" statements for rendering and collision detection
     * without having to ask the question "What shape is this 'Random' shape?".
     */
    var shape: Shape? = Shape.Circle
        set (value) {
            field = value
            dots.onEach {
                it.shape = value ?: Shape.values().random()
            }
            invalidate()
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
        ColourScheme.BrightRainbow to context.resources.getIntArray(R.array.scheme_bright_rainbow_dots),
        ColourScheme.Dark to context.resources.getIntArray(R.array.scheme_dark_dots),
        ColourScheme.Neon to context.resources.getIntArray(R.array.scheme_neon_dots),
    )

    private val dotStrokePaints = mapOf(
        ColourScheme.Monochrome to context.resources.getIntArray(R.array.scheme_monochrome_dot_borders),
        ColourScheme.SplashOfColour to context.resources.getIntArray(R.array.scheme_splash_of_colour_dot_borders),
        ColourScheme.Rainbow to context.resources.getIntArray(R.array.scheme_rainbow_dot_borders),
        ColourScheme.BrightRainbow to context.resources.getIntArray(R.array.scheme_bright_rainbow_dot_borders),
        ColourScheme.Dark to context.resources.getIntArray(R.array.scheme_dark_dot_borders),
        ColourScheme.Neon to context.resources.getIntArray(R.array.scheme_neon_dot_borders),
    )

    private val backgroundPaints = mapOf(
        ColourScheme.Monochrome to R.color.scheme_monochrome_background,
        ColourScheme.SplashOfColour to R.color.scheme_splash_of_colour_background,
        ColourScheme.Rainbow to R.color.scheme_rainbow_background,
        ColourScheme.BrightRainbow to R.color.scheme_bright_rainbow_background,
        ColourScheme.Dark to R.color.scheme_dark_background,
        ColourScheme.Neon to R.color.scheme_neon_background,
    )

    private val numDots = 15

    /**
     * For debugging purposes, will show the paths each dot takes in the background.
     */
    private val drawPaths = false

    init {
        // see comment at onDraw()
        //dotStrokePaint.style = Paint.Style.STROKE
        //dotStrokePaint.strokeWidth = 8f

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
    override fun onTouchEvent(event: MotionEvent): Boolean {

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

        return super.onTouchEvent(event)
    }

    fun restartDots() {
        dots.clear()

        for (x in 0 until numDots) {
            val dotShape = shape ?: Shape.values().random()
            dots.add(Dot(createRandomPath(width, height), dotShape))
        }
    }

    private fun dotRadius() = when (size) {
        Size.Large -> dotRadius * 1.5f
        Size.Small -> dotRadius * 0.5f
        else -> dotRadius
    }

    private var lastTimeStep: Long = System.currentTimeMillis()

    /**
     * Drawing dot borders with Paint.style.STROKE causes graphics memory leak on some devices.
     * Because of that, drawing the borders is currently substituted by drawing a larger circle below/before
     * the actual filler circle.
     * This is not an optimal solution but it does not use that much extra resources in this case.
     * See:
     *  - Bug report: https://github.com/babydots/babydots/issues/49
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val deltaMs = System.currentTimeMillis() - lastTimeStep
        val delta = deltaMs / 1000f
        lastTimeStep += deltaMs

        canvas.drawColor(context.resources.getColor(backgroundPaints[colourScheme] ?: android.R.color.white))

        val colourScheme = dotFillPaints[colourScheme] ?: intArrayOf(android.R.color.black)
        val borderColourScheme = dotStrokePaints[this.colourScheme] ?: intArrayOf(android.R.color.black)
        var colours = colourScheme.iterator()
        var borderColours = borderColourScheme.iterator()
        val radius = dotRadius()

        dots.forEach { dot ->
            if (drawPaths) {
                canvas.drawPath(dot.path, linePaint)
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

            val isTouching = isTouching(x, y, currentSize, dot.shape)
            if (isTouching && dot.zoomAnimation < 1f) {
                dot.zoomAnimation = (dot.zoomAnimation + 10f * delta).coerceAtMost(1f)
            } else if (!isTouching && dot.zoomAnimation > 0f) {
                dot.zoomAnimation = (dot.zoomAnimation - 10f * delta).coerceAtLeast(0f)
            }

            if (x > -radius && x < width + radius && y > -radius && y < height + radius) {
                val newSize = radius + (radius * dot.zoomAnimation)
                //draw slightly larger shape first to simulate dot border
                drawShape(canvas, x, y, newSize + 8f, dot.shape, dotStrokePaint)
                drawShape(canvas, x, y, newSize, dot.shape, dotFillPaint)
            }
        }
    }

    private fun drawShape(canvas: Canvas, x: Float, y: Float, size: Float, dotShape: Shape, paint: Paint) {
        when (dotShape) {
            Shape.Circle -> {
                canvas.drawCircle(x, y, size, paint)
            }
            Shape.Square -> {
                canvas.drawRect(
                    x - size, y - size, x + size, y + size, paint,
                )
            }
            Shape.Triangle -> {
                val p = Path()

                p.moveTo(x - size, y - 0.5769f * size)
                p.lineTo(x + size, y - 0.5769f * size)
                p.lineTo(x, y + size)

                canvas.drawPath(p, paint)
            }
        }
    }

    private fun isTouching(x: Float, y: Float, size: Float, shape: Shape): Boolean {

        return touchLocations.values.any { touchPoint ->

            when (shape) {
                Shape.Triangle -> isTouchingTriangle(touchPoint, x, y, size)
                Shape.Circle -> isTouchingCircle(touchPoint, x, y, size)
                Shape.Square -> isTouchingSquare(touchPoint, x, y, size)
            }

        }

    }

    private fun isTouchingSquare(touchPoint: Point, shapeX: Float, shapeY: Float, shapeSize: Float): Boolean {
        return touchPoint.x > shapeX - shapeSize
                && touchPoint.x < shapeX + shapeSize
                && touchPoint.y > shapeY - shapeSize
                && touchPoint.y < shapeY + shapeSize
    }

    private fun isTouchingTriangle(touchPoint: Point, shapeX: Float, shapeY: Float, shapeSize: Float): Boolean {
        /*
         * For reference, this is the drawing code used above:
         *   p.moveTo(x - size, y - 0.5769f * size)
         *   p.lineTo(x + size, y - 0.5769f * size)
         *   p.lineTo(x, y + size)
         */

        val x1 = shapeX - shapeSize
        val x2 = shapeX + shapeSize
        val x3 = shapeX

        val y1 = shapeY - 0.5769f * shapeSize
        val y2 = y1
        val y3 = shapeY + shapeSize

        val px = touchPoint.x
        val py = touchPoint.y

        // Bounding box check before the more expensive and accurate check below...
        if (touchPoint.x < shapeX - shapeSize
            || touchPoint.x > shapeX + shapeSize
            || touchPoint.y < shapeY - 0.5769f * shapeSize
            || touchPoint.y > shapeY + shapeSize) {
            return false
        }

        // Almost verbatim copied from: http://jeffreythompson.org/collision-detection/tri-point.php

        // Get the area of the triangle
        val areaOrig = abs((x2-x1)*(y3-y1) - (x3-x1)*(y2-y1))

        // Get the area of 3 triangles made between the point and the corners of the triangle
        val area1 = abs((x1-px)*(y2-py) - (x2-px)*(y1-py))
        val area2 = abs((x2-px)*(y3-py) - (x3-px)*(y2-py))
        val area3 = abs((x3-px)*(y1-py) - (x1-px)*(y3-py))

        // if the sum of the three areas equals the original, we're inside the triangle!
        val sum = area1 + area2 + area3

        // Account for floating point errors by allowing a little bit of variation when comparing
        // to the area of the original triangle.
        return (sum > areaOrig - shapeSize * 0.001f && sum < areaOrig + shapeSize * 0.001f)
    }

    private fun isTouchingCircle(touchPoint: Point, shapeX: Float, shapeY: Float, shapeSize: Float): Boolean {
        // Use a bounding box check to eliminate those which are definitely not touching...
        return if (!isTouchingSquare(touchPoint, shapeX, shapeY, shapeSize)) {
            false
        } else {

            // ...before performing a more accurate check.
            val dx = abs(touchPoint.x - shapeX)
            val dy = abs(touchPoint.y - shapeY)
            val distance = sqrt(dx * dx + dy * dy)

            distance < shapeSize

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

    data class Dot(val path: Path, var shape: Shape) {
        val pathMeasure = PathMeasure(path, true)
        var zoomAnimation: Float = 0f
    }

}