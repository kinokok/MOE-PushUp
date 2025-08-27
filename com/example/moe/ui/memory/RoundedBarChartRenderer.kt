package com.example.moe.ui.memory

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import com.example.moe.ui.option.Option
import com.github.mikephil.charting.animation.ChartAnimator
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.github.mikephil.charting.renderer.BarChartRenderer
import com.github.mikephil.charting.utils.ViewPortHandler

class RoundedBarChartRenderer(
    chart: BarChart,
    animator: ChartAnimator,
    viewPortHandler: ViewPortHandler
) : BarChartRenderer(chart, animator, viewPortHandler) {

    private val barShadowPaint: Paint = Paint()

    init {
        barShadowPaint.color = 0xE5343537.toInt() // より暗い半透明の黒色
    }

    override fun drawDataSet(c: Canvas, dataSet: IBarDataSet, index: Int) {
        val trans = mChart.getTransformer(dataSet.axisDependency)

        mBarBorderPaint.color = dataSet.barBorderColor
        mBarBorderPaint.strokeWidth = dataSet.barBorderWidth

        val drawBorder = dataSet.barBorderWidth > 0f

        val phaseX = mAnimator.phaseX
        val phaseY = mAnimator.phaseY

        val buffer = mBarBuffers[index]
        buffer.setPhases(phaseX, phaseY)
        buffer.setDataSet(index)
        buffer.setInverted(mChart.isInverted(dataSet.axisDependency))
        buffer.setBarWidth(mChart.barData.barWidth)

        buffer.feed(dataSet)

        trans.pointValuesToPixel(buffer.buffer)

        val isSingleColor = dataSet.colors.size == 1

        if (isSingleColor) {
            mRenderPaint.color = dataSet.color
        }

        for (j in buffer.buffer.indices step 4) {
            if (!mViewPortHandler.isInBoundsLeft(buffer.buffer[j + 2])) continue
            if (!mViewPortHandler.isInBoundsRight(buffer.buffer[j])) break

            if (!isSingleColor) {
                mRenderPaint.color = dataSet.getColor(j / 4)
            }

            val left = buffer.buffer[j]
            val top = buffer.buffer[j + 1]
            val right = buffer.buffer[j + 2]
            val bottom = buffer.buffer[j + 3]

            val radius = 20f // 角の半径

            // Determine color or gradient based on value
            val entry = dataSet.getEntryForIndex(j / 4) as BarEntry
            if (entry.y >= Option.OBJECTIVE) {
                // グラデーションのシェーダーを設定
                val gradient = LinearGradient(
                    left, top, left, bottom,
                    Color.parseColor("#66a0fa"), // 上部の薄い色
                    Color.parseColor("#0083FF"), // 下部の濃い色
                    Shader.TileMode.CLAMP
                )
                mRenderPaint.shader = gradient
            } else {
                mRenderPaint.color = 0xE5888888.toInt() // 未達成時のグレー
                mRenderPaint.shader = null
            }

            // 影を描画
            val shadowRect = RectF(left, mViewPortHandler.contentTop(), right, mViewPortHandler.contentBottom())
            c.drawRoundRect(shadowRect, radius, radius, barShadowPaint)

            // Draw the bar
            val rect = RectF(left, top, right, bottom)
            c.drawRoundRect(rect, radius, radius, mRenderPaint)

            if (drawBorder) {
                c.drawRoundRect(rect, radius, radius, mBarBorderPaint)
            }

            // Reset shader after drawing the bar
            mRenderPaint.shader = null
        }
    }

    override fun drawHighlighted(c: Canvas, indices: Array<out Highlight>?) {
        super.drawHighlighted(c, indices)
        // Highlight logic can remain unchanged
    }
}
