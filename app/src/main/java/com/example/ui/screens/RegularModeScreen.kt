package com.example.ui.screens

import androidx.compose.foundation.layout.BoxWithConstraints
import com.example.ui.components.LocalSoundEffectPlayer
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.RegularVariableEntity
import com.example.engine.EvalResult
import com.example.engine.MathEngine
import com.example.ui.CalculatorUiState
import com.example.ui.components.CalculatorKeypad
import com.example.ui.components.MathExpressionView
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.OledBlack
import com.example.ui.theme.OledSurface
import com.example.ui.theme.OledSurfaceElevated
import com.example.ui.theme.OledSurfaceHigh
import com.example.ui.theme.OledTextMuted
import com.example.ui.theme.OledTextPrimary
import com.example.ui.theme.OledTextSecondary
import com.example.ui.theme.variableColorAt

@Composable
fun RegularModeScreen(
  uiState: CalculatorUiState,
  onInput: (String) -> Unit,
  onDelete: () -> Unit,
  onClearDisplay: () -> Unit,
  onLongPressAc: () -> Unit,
  onEquals: () -> Unit,
  onInsertVariable: (String) -> Unit,
  onReuseExpression: (String) -> Unit,
  onDeleteVariable: (RegularVariableEntity) -> Unit,
  onShowToast: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val ws = uiState.workspace
  val variables = uiState.regularVariables
  val varMap = remember(variables) { variables.associate { it.name to it.value } }
  val varColors = remember(variables) {
    variables.associate { it.name to variableColorAt(it.colorIndex) }
  }

  val livePreview = remember(ws.regularExpression, varMap, ws.regularAns, ws.hasRegularAns, ws.smartPercentage) {
    if (ws.regularExpression.isBlank()) {
      null
    } else {
      MathEngine.evaluate(
        expression = ws.regularExpression,
        variables = varMap,
        ansValue = if (ws.hasRegularAns) ws.regularAns else null,
        smartPercentage = ws.smartPercentage,
      )
    }
  }

  var inspectedVariable by remember { mutableStateOf<RegularVariableEntity?>(null) }

  val soundPlayer = LocalSoundEffectPlayer.current

  BoxWithConstraints(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 14.dp)
      .padding(bottom = 10.dp),
  ) {
    // 30% taller keypad than original (56.dp -> 73.dp, 44.dp -> 57.dp),
    // clamping gracefully only on unusually short split-screen windows.
    val maxKeypadBudget = (maxHeight - if (variables.isNotEmpty()) 215.dp else 160.dp).coerceAtLeast(240.dp)
    val desiredTotal = 57.dp + (73.dp * 5) + (8.dp * 5) // 462.dp (+30%)
    val scale = if (maxKeypadBudget < desiredTotal) (maxKeypadBudget / desiredTotal).coerceAtLeast(0.68f) else 1f
    val regularKeyHeight = (73f * scale).dp
    val regularUtilHeight = (57f * scale).dp

    Column(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
    // Display Area
    Column(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .clip(RoundedCornerShape(24.dp))
        .background(OledSurface)
        .padding(horizontal = 16.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      // Top status strip: ANS pill + Hold AC hint
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        if (ws.hasRegularAns) {
          val ansText = MathEngine.formatNumber(
            ws.regularAns,
            ws.decimalPlaces,
            ws.trimTrailingZeros,
          )
          Row(
            modifier = Modifier
              .clip(RoundedCornerShape(10.dp))
              .background(AccentCyan.copy(alpha = 0.14f))
              .clickable {
                soundPlayer?.playClick(ws.keypressSound)
                onInput("ANS")
              }
              .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "ANS",
              color = AccentCyan,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = JetBrainsMonoFontFamily,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = ansText,
              color = OledTextPrimary,
              fontSize = 12.sp,
              fontWeight = FontWeight.Medium,
              fontFamily = JetBrainsMonoFontFamily,
            )
          }
        } else {
          Text(
            text = "REGULAR",
            color = OledTextMuted,
            style = MaterialTheme.typography.labelMedium,
          )
        }

        if (variables.isNotEmpty()) {
          Text(
            text = "Hold AC to archive • Hold var for formula",
            color = OledTextMuted,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      // Main Math Expression Display
      Column(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.End,
      ) {
        val banner = uiState.regularBanner
        if (ws.regularExpression.isBlank() && banner != null) {
          val bannerColor = varColors[banner.varName] ?: AccentCyan
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 6.dp),
          ) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(bannerColor.copy(alpha = 0.18f))
                .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
              Text(
                text = "Saved → ${banner.varName}",
                color = bannerColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = JetBrainsMonoFontFamily,
              )
            }
          }
          MathExpressionView(
            expression = banner.expression,
            showFractionFormat = ws.showFractionFormat,
            variableColors = varColors,
            maxFontSize = 26.sp,
            minFontSize = 11.sp,
            modifier = Modifier.heightIn(max = 88.dp),
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "= ${MathEngine.formatNumber(banner.value, ws.decimalPlaces, ws.trimTrailingZeros)}",
            color = bannerColor,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = JetBrainsMonoFontFamily,
            maxLines = 1,
          )
        } else {
          MathExpressionView(
            expression = ws.regularExpression,
            showFractionFormat = ws.showFractionFormat,
            variableColors = varColors,
            maxFontSize = 38.sp,
            minFontSize = 11.sp,
            modifier = Modifier
              .weight(1f, fill = false)
              .heightIn(min = 56.dp, max = 168.dp),
          )

          Spacer(modifier = Modifier.height(6.dp))

          when (livePreview) {
            is EvalResult.Success -> {
              Text(
                text = "= ${MathEngine.formatNumber(livePreview.value, ws.decimalPlaces, ws.trimTrailingZeros)}",
                color = AccentCyan,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = JetBrainsMonoFontFamily,
                maxLines = 1,
              )
            }
            is EvalResult.Error -> {
              Text(
                text = " ",
                fontSize = 24.sp,
              )
            }
            null -> {
              if (ws.hasRegularAns) {
                Text(
                  text = "Ready • ANS = ${MathEngine.formatNumber(ws.regularAns, ws.decimalPlaces, ws.trimTrailingZeros)}",
                  color = OledTextMuted,
                  fontSize = 13.sp,
                  fontFamily = JetBrainsMonoFontFamily,
                )
              }
            }
          }
        }
      }
    }

    // Colorful Variable Buttons Row just below the display
    if (variables.isNotEmpty()) {
      LazyRow(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 8.dp)
          .testTag("regular_variables_row"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 2.dp),
      ) {
        items(variables, key = { it.id }) { variable ->
          VariablePillButton(
            variable = variable,
            formattedValue = MathEngine.formatNumber(
              variable.value,
              ws.decimalPlaces,
              ws.trimTrailingZeros,
            ),
            hapticEnabled = ws.hapticFeedback,
            soundEnabled = ws.keypressSound,
            onTap = { onInsertVariable(variable.name) },
            onLongPress = { inspectedVariable = variable },
          )
        }
      }
    } else {
      Spacer(modifier = Modifier.height(10.dp))
    }

    // Keypad (+30% height)
    CalculatorKeypad(
      onInput = onInput,
      onDelete = onDelete,
      onClearDisplay = onClearDisplay,
      onLongPressAc = onLongPressAc,
      onLongPressDelete = onClearDisplay,
      onEquals = onEquals,
      onAns = { onInput("ANS") },
      ansEnabled = ws.hasRegularAns,
      hapticEnabled = ws.hapticFeedback,
      soundEnabled = ws.keypressSound,
      keyHeight = regularKeyHeight,
      utilityHeight = regularUtilHeight,
    )
  }
  }

  inspectedVariable?.let { targetVar ->
    VariableInspectionDialog(
      variable = targetVar,
      decimalPlaces = ws.decimalPlaces,
      trimTrailingZeros = ws.trimTrailingZeros,
      showFractionFormat = ws.showFractionFormat,
      varColors = varColors,
      onDismiss = { inspectedVariable = null },
      onInsert = {
        onInsertVariable(targetVar.name)
        inspectedVariable = null
      },
      onReuse = {
        onReuseExpression(targetVar.expression)
        inspectedVariable = null
      },
      onDelete = if (!targetVar.isSecondary) {
        {
          onDeleteVariable(targetVar)
          inspectedVariable = null
        }
      } else null,
      onCopied = {
        onShowToast("Copied ${targetVar.name} value")
        inspectedVariable = null
      },
    )
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VariablePillButton(
  variable: RegularVariableEntity,
  formattedValue: String,
  hapticEnabled: Boolean,
  soundEnabled: Boolean,
  onTap: () -> Unit,
  onLongPress: () -> Unit,
) {
  val accent = variableColorAt(variable.colorIndex)
  val haptic = LocalHapticFeedback.current
  val soundPlayer = LocalSoundEffectPlayer.current
  val shape = RoundedCornerShape(14.dp)
  val interactionSource = remember { MutableInteractionSource() }

  Row(
    modifier = Modifier
      .heightIn(min = 48.dp)
      .clip(shape)
      .background(accent.copy(alpha = 0.16f), shape)
      .combinedClickable(
        interactionSource = interactionSource,
        indication = ripple(bounded = true, color = accent),
        onClick = {
          soundPlayer?.playClick(soundEnabled)
          if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          onTap()
        },
        onLongClick = {
          soundPlayer?.playClick(soundEnabled)
          if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
          onLongPress()
        },
      )
      .padding(horizontal = 12.dp, vertical = 8.dp)
      .testTag("var_chip_${variable.name}"),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Box(
      modifier = Modifier
        .clip(RoundedCornerShape(8.dp))
        .background(accent)
        .padding(horizontal = 7.dp, vertical = 2.dp),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = variable.name,
        color = OledBlack,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = JetBrainsMonoFontFamily,
      )
    }
    Text(
      text = formattedValue,
      color = OledTextPrimary,
      fontSize = 14.sp,
      fontWeight = FontWeight.SemiBold,
      fontFamily = JetBrainsMonoFontFamily,
    )
  }
}

@Composable
private fun VariableInspectionDialog(
  variable: RegularVariableEntity,
  decimalPlaces: Int,
  trimTrailingZeros: Boolean,
  showFractionFormat: Boolean,
  varColors: Map<String, androidx.compose.ui.graphics.Color>,
  onDismiss: () -> Unit,
  onInsert: () -> Unit,
  onReuse: () -> Unit,
  onDelete: (() -> Unit)?,
  onCopied: () -> Unit,
) {
  val accent = variableColorAt(variable.colorIndex)
  val clipboard = LocalClipboardManager.current
  val formatted = MathEngine.formatNumber(variable.value, decimalPlaces, trimTrailingZeros)
  val exactFormatted = MathEngine.formatNumber(variable.value, 6, trimTrailingZeros = true)

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = OledSurfaceElevated,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(accent)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
              Text(
                text = variable.name,
                color = OledBlack,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = JetBrainsMonoFontFamily,
              )
            }
            Column {
              Text(
                text = if (variable.isSecondary) "Derived Variable" else "Primary Variable",
                color = accent,
                style = MaterialTheme.typography.titleMedium,
              )
              Text(
                text = "Calculation breakdown",
                color = OledTextSecondary,
                style = MaterialTheme.typography.bodyMedium,
              )
            }
          }

          if (onDelete != null) {
            IconButton(
              onClick = onDelete,
              modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(com.example.ui.theme.AccentRose.copy(alpha = 0.14f))
                .testTag("delete_primary_var_button"),
            ) {
              Icon(
                imageVector = Icons.Outlined.DeleteOutline,
                contentDescription = "Delete primary variable",
                tint = com.example.ui.theme.AccentRose,
                modifier = Modifier.size(20.dp),
              )
            }
          }
        }

        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(OledSurfaceHigh)
            .padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = "Expression",
            color = OledTextMuted,
            style = MaterialTheme.typography.labelMedium,
          )
          MathExpressionView(
            expression = variable.expression,
            showFractionFormat = showFractionFormat,
            variableColors = varColors,
            maxFontSize = 24.sp,
            minFontSize = 11.sp,
            alignEnd = false,
            modifier = Modifier.heightIn(min = 36.dp, max = 90.dp),
          )
          Spacer(modifier = Modifier.height(2.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "= $formatted",
              color = accent,
              fontSize = 24.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = JetBrainsMonoFontFamily,
            )
            if (exactFormatted != formatted) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Outlined.Info,
                  contentDescription = "Exact value",
                  tint = OledTextMuted,
                  modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = exactFormatted,
                  color = OledTextSecondary,
                  fontSize = 12.sp,
                  fontFamily = JetBrainsMonoFontFamily,
                )
              }
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          TextButton(
            onClick = {
              clipboard.setText(AnnotatedString(exactFormatted))
              onCopied()
            },
            modifier = Modifier.weight(1f),
          ) {
            Icon(
              imageVector = Icons.Outlined.ContentCopy,
              contentDescription = "Copy",
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Copy")
          }
          TextButton(
            onClick = onReuse,
            modifier = Modifier.weight(1f),
          ) {
            Icon(
              imageVector = Icons.Outlined.Edit,
              contentDescription = "Edit expression",
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Reuse")
          }
          Button(
            onClick = onInsert,
            colors = ButtonDefaults.buttonColors(
              containerColor = accent,
              contentColor = OledBlack,
            ),
            modifier = Modifier.weight(1f),
          ) {
            Text("Use ${variable.name}", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}
