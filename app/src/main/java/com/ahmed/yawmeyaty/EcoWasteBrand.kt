package com.ahmed.yawmeyaty

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun EcoWasteBrandMark(
    modifier: Modifier = Modifier,
    size: Dp = 92.dp
) {
    Box(modifier = modifier.size(size)) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = this.size.width
            val h = this.size.height
            val stroke = w * 0.095f
            val centre = Offset(w / 2f, h / 2f)

            drawArc(
                brush = Brush.sweepGradient(
                    listOf(Color(0xFF74E2B6), Color(0xFF11A579), Color(0xFF075D57))
                ),
                startAngle = 38f,
                sweepAngle = 292f,
                useCenter = false,
                topLeft = Offset(stroke, stroke),
                size = Size(w - stroke * 2f, h - stroke * 2f),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )

            val arrow = Path().apply {
                moveTo(w * 0.77f, h * 0.13f)
                lineTo(w * 0.94f, h * 0.24f)
                lineTo(w * 0.75f, h * 0.31f)
                close()
            }
            drawPath(arrow, Color(0xFF075D57))

            val leaf = Path().apply {
                moveTo(w * 0.50f, h * 0.79f)
                cubicTo(w * 0.23f, h * 0.66f, w * 0.25f, h * 0.35f, w * 0.56f, h * 0.28f)
                cubicTo(w * 0.78f, h * 0.45f, w * 0.75f, h * 0.70f, w * 0.50f, h * 0.79f)
                close()
            }
            drawPath(
                leaf,
                Brush.linearGradient(
                    colors = listOf(Color(0xFF7BE0A8), Color(0xFF0FA56E), Color(0xFF00695C)),
                    start = Offset(w * 0.25f, h * 0.28f),
                    end = Offset(w * 0.76f, h * 0.80f)
                )
            )

            val vein = Path().apply {
                moveTo(w * 0.50f, h * 0.80f)
                cubicTo(w * 0.48f, h * 0.62f, w * 0.50f, h * 0.48f, w * 0.61f, h * 0.36f)
            }
            drawPath(vein, Color(0xFFE8FFF5), style = Stroke(width = w * 0.035f, cap = StrokeCap.Round))
        }
    }
}
