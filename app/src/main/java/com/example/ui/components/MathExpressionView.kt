package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.MathEngine
import com.example.engine.MathNode
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.OledTextMuted
import com.example.ui.theme.OledTextPrimary
import com.example.ui.theme.OledTextSecondary
import kotlin.math.max

private sealed interface FlowSegment {
  data class NodeSeg(val node: MathNode) : FlowSegment
  data class OpSeg(val opText: String) : FlowSegment
}

/**
 * Multi-row mathematical expression renderer supporting stacked fractions and
 * fast single-pass font sizing that wraps long expressions cleanly across rows
 * before shrinking down to [minFontSize].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MathExpressionView(
  expression: String,
  modifier: Modifier = Modifier,
  showFractionFormat: Boolean = true,
  variableColors: Map<String, Color> = emptyMap(),
  maxFontSize: TextUnit = 36.sp,
  minFontSize: TextUnit = 11.sp,
  alignEnd: Boolean = true,
  placeholderText: String = "0",
) {
  if (expression.isBlank()) {
    Box(
      modifier = modifier.fillMaxWidth(),
      contentAlignment = if (alignEnd) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
      Text(
        text = placeholderText,
        style = TextStyle(
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Normal,
          fontSize = maxFontSize,
          color = OledTextMuted,
          fontFeatureSettings = "tnum",
        ),
        maxLines = 1,
      )
    }
    return
  }

  val scrollState = rememberScrollState()
  LaunchedEffect(expression) {
    if (alignEnd && scrollState.maxValue > 0) {
      scrollState.scrollTo(scrollState.maxValue)
    }
  }

  val segments = remember(expression, showFractionFormat) {
    if (showFractionFormat) {
      flattenForFlow(MathEngine.parseForDisplay(expression))
    } else {
      emptyList()
    }
  }

  BoxWithConstraints(
    modifier = modifier.fillMaxWidth(),
    contentAlignment = if (alignEnd) Alignment.CenterEnd else Alignment.CenterStart,
  ) {
    val availW = maxWidth.value.coerceAtLeast(60f)
    val availH = if (maxHeight.value.isFinite()) maxHeight.value.coerceAtLeast(36f) else 180f

    val fittedFontSize = remember(segments, expression, showFractionFormat, availW, availH, maxFontSize, minFontSize) {
      if (showFractionFormat) {
        findOptimalMultiRowFontSize(
          segments = segments,
          availW = availW,
          availH = availH,
          maxSp = maxFontSize.value,
          minSp = minFontSize.value,
        ).sp
      } else {
        findOptimalInlineFontSize(
          charCount = expression.length,
          availW = availW,
          availH = availH,
          maxSp = maxFontSize.value,
          minSp = minFontSize.value,
        ).sp
      }
    }

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(scrollState),
      contentAlignment = if (alignEnd) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
      if (showFractionFormat) {
        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = if (alignEnd) Arrangement.End else Arrangement.Start,
          verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        ) {
          segments.forEach { seg ->
            when (seg) {
              is FlowSegment.NodeSeg -> {
                Box(modifier = Modifier.align(Alignment.CenterVertically)) {
                  MathNodeContent(
                    node = seg.node,
                    fontSize = fittedFontSize,
                    variableColors = variableColors,
                    depth = 0,
                  )
                }
              }
              is FlowSegment.OpSeg -> {
                Text(
                  text = seg.opText,
                  style = TextStyle(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = fittedFontSize,
                    color = AccentCyan,
                    fontFeatureSettings = "tnum",
                  ),
                  maxLines = 1,
                  modifier = Modifier.align(Alignment.CenterVertically),
                )
              }
            }
          }
        }
      } else {
        InlineMathText(
          expression = expression,
          fontSize = fittedFontSize,
          alignEnd = alignEnd,
        )
      }
    }
  }
}

private fun flattenForFlow(node: MathNode): List<FlowSegment> {
  val out = mutableListOf<FlowSegment>()
  fun visit(n: MathNode) {
    if (n is MathNode.BinaryNode) {
      visit(n.left)
      val opText = when (n.op) {
        '*' -> " × "
        '-' -> " − "
        '+' -> " + "
        else -> " ${n.op} "
      }
      out.add(FlowSegment.OpSeg(opText))
      visit(n.right)
    } else {
      out.add(FlowSegment.NodeSeg(n))
    }
  }
  visit(node)
  return out
}

private fun findOptimalMultiRowFontSize(
  segments: List<FlowSegment>,
  availW: Float,
  availH: Float,
  maxSp: Float,
  minSp: Float,
): Float {
  if (segments.isEmpty()) return maxSp
  val metrics = segments.map { seg ->
    when (seg) {
      is FlowSegment.NodeSeg -> measureNodeUnits(seg.node)
      is FlowSegment.OpSeg -> (seg.opText.length * 0.56f) to 1.35f
    }
  }
  val widestSeg = metrics.maxOfOrNull { it.first } ?: 1f
  val tallestSeg = metrics.maxOfOrNull { it.second } ?: 1.35f

  val hardUpper = minOf(
    maxSp,
    (availW - 10f) / max(widestSeg, 1f),
    (availH - 6f) / max(tallestSeg, 1f),
  ).coerceAtLeast(minSp)

  var low = minSp
  var high = hardUpper
  var best = minSp

  repeat(8) {
    val mid = (low + high) * 0.5f
    val maxUnitsPerLine = ((availW - 10f) / mid).coerceAtLeast(1f)
    var curLineW = 0f
    var curLineH = 0f
    var totalH = 0f
    var rows = 1

    for ((w, h) in metrics) {
      if (curLineW > 0f && curLineW + w > maxUnitsPerLine) {
        totalH += curLineH * mid + 4f
        rows++
        curLineW = w
        curLineH = h
      } else {
        curLineW += w
        curLineH = max(curLineH, h)
      }
    }
    totalH += curLineH * mid

    if (totalH <= availH - 4f) {
      best = mid
      low = mid
    } else {
      high = mid
    }
  }
  return best.coerceIn(minSp, maxSp)
}

private fun findOptimalInlineFontSize(
  charCount: Int,
  availW: Float,
  availH: Float,
  maxSp: Float,
  minSp: Float,
): Float {
  var low = minSp
  var high = maxSp
  var best = minSp
  repeat(7) {
    val mid = (low + high) * 0.5f
    val charsPerLine = ((availW - 8f) / (mid * 0.63f)).coerceAtLeast(1f)
    val rows = kotlin.math.ceil(charCount / charsPerLine).toInt().coerceAtLeast(1)
    val totalH = rows * (mid * 1.35f)
    if (totalH <= availH - 4f) {
      best = mid
      low = mid
    } else {
      high = mid
    }
  }
  return best.coerceIn(minSp, maxSp)
}

private fun measureNodeUnits(node: MathNode): Pair<Float, Float> {
  return when (node) {
    is MathNode.NumberNode -> (node.text.length * 0.62f).coerceAtLeast(0.6f) to 1.35f
    is MathNode.VariableNode -> (node.name.length * 0.64f).coerceAtLeast(0.7f) to 1.35f
    is MathNode.PercentNode -> {
      val (w, h) = measureNodeUnits(node.inner)
      (w + 0.68f) to h
    }
    is MathNode.UnaryNode -> {
      val (w, h) = measureNodeUnits(node.inner)
      (w + 0.68f) to h
    }
    is MathNode.ParenNode -> {
      val (w, h) = measureNodeUnits(node.inner)
      (w + 1.3f) to h
    }
    is MathNode.BinaryNode -> {
      val (lw, lh) = measureNodeUnits(node.left)
      val (rw, rh) = measureNodeUnits(node.right)
      (lw + 1.45f + rw) to max(lh, rh)
    }
    is MathNode.FractionNode -> {
      val (nw, nh) = measureNodeUnits(node.numerator)
      val (dw, dh) = measureNodeUnits(node.denominator)
      val scale = 0.85f
      (max(nw, dw) * scale + 0.6f) to ((nh + dh) * scale + 0.45f)
    }
    MathNode.PlaceholderNode -> 0.7f to 1.25f
  }
}

@Composable
private fun InlineMathText(
  expression: String,
  fontSize: TextUnit,
  alignEnd: Boolean,
) {
  val formatted = remember(expression) {
    expression.replace('*', '×').replace('/', '÷')
  }
  Text(
    text = formatted,
    style = TextStyle(
      fontFamily = JetBrainsMonoFontFamily,
      fontWeight = FontWeight.Medium,
      fontSize = fontSize,
      lineHeight = (fontSize.value * 1.25f).sp,
      color = OledTextPrimary,
      fontFeatureSettings = "tnum",
      textAlign = if (alignEnd) TextAlign.End else TextAlign.Start,
    ),
    modifier = Modifier.fillMaxWidth(),
    softWrap = true,
  )
}

@Composable
private fun MathNodeContent(
  node: MathNode,
  fontSize: TextUnit,
  variableColors: Map<String, Color>,
  depth: Int,
) {
  val effectiveSize = if (depth == 0) {
    fontSize
  } else {
    (fontSize.value * 0.84f).coerceAtLeast(8.5f).sp
  }
  val textStyle = TextStyle(
    fontFamily = JetBrainsMonoFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = effectiveSize,
    color = OledTextPrimary,
    fontFeatureSettings = "tnum",
  )

  when (node) {
    is MathNode.NumberNode -> {
      Text(text = node.text, style = textStyle, maxLines = 1)
    }

    is MathNode.VariableNode -> {
      val color = when {
        node.name.equals("ANS", ignoreCase = true) -> AccentCyan
        variableColors.containsKey(node.name) -> variableColors.getValue(node.name)
        else -> AccentEmerald
      }
      Text(
        text = node.name,
        style = textStyle.copy(color = color, fontWeight = FontWeight.Bold),
        maxLines = 1,
      )
    }

    is MathNode.PercentNode -> {
      Row(verticalAlignment = Alignment.CenterVertically) {
        MathNodeContent(node.inner, effectiveSize, variableColors, depth)
        Text(
          text = "%",
          style = textStyle.copy(color = AccentAmber, fontWeight = FontWeight.SemiBold),
          maxLines = 1,
        )
      }
    }

    is MathNode.UnaryNode -> {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = if (node.op == '-') "−" else "+",
          style = textStyle.copy(color = AccentCyan),
          maxLines = 1,
        )
        MathNodeContent(node.inner, effectiveSize, variableColors, depth)
      }
    }

    is MathNode.ParenNode -> {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = "(",
          style = textStyle.copy(color = OledTextSecondary),
          maxLines = 1,
        )
        MathNodeContent(node.inner, effectiveSize, variableColors, depth)
        Text(
          text = ")",
          style = textStyle.copy(color = OledTextSecondary),
          maxLines = 1,
        )
      }
    }

    is MathNode.BinaryNode -> {
      val opText = when (node.op) {
        '*' -> " × "
        '-' -> " − "
        '+' -> " + "
        else -> " ${node.op} "
      }
      Row(verticalAlignment = Alignment.CenterVertically) {
        MathNodeContent(node.left, effectiveSize, variableColors, depth)
        Text(
          text = opText,
          style = textStyle.copy(color = AccentCyan),
          maxLines = 1,
        )
        MathNodeContent(node.right, effectiveSize, variableColors, depth)
      }
    }

    is MathNode.FractionNode -> {
      val fractionBarThickness = (effectiveSize.value * 0.055f).coerceIn(1.2f, 2.2f).dp
      val paddingH = (effectiveSize.value * 0.16f).coerceIn(3f, 8f).dp
      Column(
        modifier = Modifier
          .padding(horizontal = 3.dp)
          .width(IntrinsicSize.Max),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        Box(
          modifier = Modifier.padding(horizontal = paddingH, vertical = 1.dp),
          contentAlignment = Alignment.Center,
        ) {
          MathNodeContent(node.numerator, effectiveSize, variableColors, depth + 1)
        }
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .widthIn(min = 18.dp)
            .height(fractionBarThickness)
            .background(AccentCyan.copy(alpha = 0.85f), RoundedCornerShape(1.dp)),
        )
        Box(
          modifier = Modifier.padding(horizontal = paddingH, vertical = 1.dp),
          contentAlignment = Alignment.Center,
        ) {
          MathNodeContent(node.denominator, effectiveSize, variableColors, depth + 1)
        }
      }
    }

    MathNode.PlaceholderNode -> {
      val boxSize = (effectiveSize.value * 0.55f).coerceIn(8f, 18.dp.value).dp
      Box(
        modifier = Modifier
          .padding(horizontal = 2.dp, vertical = 2.dp)
          .width(boxSize)
          .height(boxSize)
          .background(AccentCyan.copy(alpha = 0.16f), RoundedCornerShape(4.dp)),
      )
    }
  }
}
