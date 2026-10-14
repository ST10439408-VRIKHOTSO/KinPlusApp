package za.co.kinplus.app.ui.screens.auth

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import za.co.kinplus.app.R

/**
 * Faint family silhouette (two adults and a child) drawn behind the dark
 * forest hero on the splash and sign in screens, with a soft evergreen glow.
 * A custom View that draws on a Canvas, scaled from a 390 x 380 design frame
 * so it fits any screen size.
 */
class AuthFamilyGraphicView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val glowColor = ContextCompat.getColor(context, R.color.kin_forest_glow)
    private val bodyTop = ContextCompat.getColor(context, R.color.kin_forest_body_top)
    private val bodyBottom = ContextCompat.getColor(context, R.color.kin_forest_body_bottom)

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w == 0 || h == 0) return
        glowPaint.shader = RadialGradient(
            fx(200f), fy(150f), fx(230f),
            intArrayOf(ColorUtils.setAlphaComponent(glowColor, (0.55f * 255).toInt()), 0x00000000),
            null,
            Shader.TileMode.CLAMP
        )
        bodyPaint.shader = LinearGradient(0f, 0f, 0f, h.toFloat(), bodyTop, bodyBottom, Shader.TileMode.CLAMP)
    }

    private fun fx(v: Float) = v * width / 390f
    private fun fy(v: Float) = v * height / 380f

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return
        canvas.drawCircle(fx(200f), fy(150f), fx(230f), glowPaint)

        person(canvas, 155f, 120f, 16f, 138f, 76f, 250f, 130f)
        person(canvas, 205f, 160f, 11f, 173f, 44f, 250f, 70f)
        person(canvas, 250f, 115f, 16f, 133f, 76f, 250f, 130f)
    }

    /** One figure: a round head and a softly curved body from shoulders to hem. */
    private fun person(
        canvas: Canvas,
        headCx: Float, headCy: Float, headR: Float,
        shoulderY: Float, shoulderW: Float,
        hemY: Float, hemW: Float
    ) {
        canvas.drawCircle(fx(headCx), fy(headCy), fx(headR), bodyPaint)
        val sx1 = headCx - shoulderW / 2f
        val sx2 = headCx + shoulderW / 2f
        val hx1 = headCx - hemW / 2f
        val hx2 = headCx + hemW / 2f
        path.reset()
        path.moveTo(fx(sx1), fy(shoulderY))
        path.cubicTo(fx(sx1 - 12f), fy(shoulderY + 37f), fx(hx1 - 5f), fy(hemY - 35f), fx(hx1), fy(hemY))
        path.lineTo(fx(hx2), fy(hemY))
        path.cubicTo(fx(hx2 + 5f), fy(hemY - 35f), fx(sx2 + 12f), fy(shoulderY + 37f), fx(sx2), fy(shoulderY))
        path.close()
        canvas.drawPath(path, bodyPaint)
    }
}
