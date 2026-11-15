package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.CompareOp
import com.example.data.CompareVariableEntity
import com.example.data.JsonCodec
import com.example.engine.EvalResult
import com.example.engine.MathEngine
import com.example.ui.CalculatorUiState
import com.example.ui.components.CalculatorKeypad
import com.example.ui.components.MathExpressionView
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentRose
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
fun CompareModeScreen(
  uiState: CalculatorUiState,
  onSetActiveOp: (CompareOp) -> Unit,
  onInput: (String) -> Unit,
  onDelete: () -> Unit,
  onClearDisplay: () -> Unit,
  onEquals: () -> Unit,
  onAddBaseVariable: (String, String) -> Unit,
  onBaseClicked: (CompareVariableEntity) -> Unit,
  onUndoLastStep: (CompareVariableEntity) -> Unit,
  onResetBase: (CompareVariableEntity) -> Unit,
  onUpdateBaseValue: (CompareVariableEntity, String) -> Unit,
  onDeleteBase: (CompareVariableEntity) -> Unit,
  modifier: Modifier = Modifier,
) {
  val ws = uiState.workspace
  val compareVars = uiState.compareVariables
  val activeOp = runCatching { CompareOp.valueOf(ws.compareActiveOp) }.getOrDefault(CompareOp.ADD)
  val selectedVar = remember(compareVars, ws.selectedCompareVarId) {
    compareVars.firstOrNull { it.id == ws.selectedCompareVarId } ?: compareVars.firstOrNull()
  }
  val varColors = remember(compareVars) {
    compareVars.associate { it.name to variableColorAt(it.colorIndex) }
  }
  val baseMap = remember(compareVars) {
    compareVars.associate { it.name to it.currentValue }
  }

  val livePreview = remember(ws.compareExpression, baseMap, ws.smartPercentage) {
    if (ws.compareExpression.isBlank()) {
      null
    } else {
      MathEngine.evaluate(ws.compareExpression, baseMap, smartPercentage = ws.smartPercentage)
    }
  }

  var showAddBaseDialog by remember { mutableStateOf(false) }
  var prefillBaseName by remember { mutableStateOf("") }
  var editingBaseVar by remember { mutableStateOf<CompareVariableEntity?>(null) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 14.dp)
      .padding(bottom = 10.dp),
    verticalArrangement = Arrangement.SpaceBetween,
  ) {
    // Upper Display + Base vs Current Comparison Panel
    Column(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .clip(RoundedCornerShape(22.dp))
        .background(OledSurface)
        .padding(14.dp),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      if (compareVars.isEmpty()) {
        // Initial suggestion prompt to enter base values
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AccentCyan.copy(alpha = 0.09f))
            .padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = "Enter Base Values to Compare",
            color = AccentCyan,
            style = MaterialTheme.typography.titleMedium,
          )
          Text(
            text = "Create base variables (a, b, c or custom text like budget, target). Then calculate and tap a base button to ADD or SUBTRACT.",
            color = OledTextSecondary,
            style = MaterialTheme.typography.bodyMedium,
          )
          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            listOf("a", "b", "c").forEach { quickName ->
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(10.dp))
                  .background(OledSurfaceHigh)
                  .clickable {
                    prefillBaseName = quickName
                    showAddBaseDialog = true
                  }
                  .padding(horizontal = 12.dp, vertical = 8.dp),
              ) {
                Text(
                  text = "+ $quickName",
                  color = AccentCyan,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp,
                )
              }
            }
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(AccentCyan)
                .clickable {
                  prefillBaseName = ""
                  showAddBaseDialog = true
                }
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .testTag("add_base_prompt_button"),
            ) {
              Text(
                text = "+ Custom",
                color = OledBlack,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
              )
            }
          }
        }
      } else if (selectedVar != null) {
        val accent = variableColorAt(selectedVar.colorIndex)
        val delta = selectedVar.currentValue - selectedVar.baseValue
        val deltaColor = when {
          delta > 1e-9 -> AccentEmerald
          delta < -1e-9 -> AccentRose
          else -> OledTextSecondary
        }
        val deltaPrefix = if (delta > 1e-9) "+" else ""
        val logs = remember(selectedVar.logsJson) { JsonCodec.decodeCompareLogs(selectedVar.logsJson) }

        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(OledSurfaceElevated)
            .padding(horizontal = 12.dp, vertical = 10.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .background(accent)
                  .padding(horizontal = 8.dp, vertical = 3.dp),
              ) {
                Text(
                  text = selectedVar.name,
                  color = OledBlack,
                  fontWeight = FontWeight.Bold,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontSize = 13.sp,
                )
              }
              Text(
                text = "$deltaPrefix${MathEngine.formatNumber(delta, ws.decimalPlaces, ws.trimTrailingZeros)}",
                color = deltaColor,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
              )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
              if (logs.isNotEmpty()) {
                IconButton(
                  onClick = { onUndoLastStep(selectedVar) },
                  modifier = Modifier.size(36.dp),
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Undo,
                    contentDescription = "Undo last operation",
                    tint = OledTextSecondary,
                    modifier = Modifier.size(18.dp),
                  )
                }
              }
              IconButton(
                onClick = { onResetBase(selectedVar) },
                modifier = Modifier.size(36.dp),
              ) {
                Icon(
                  imageVector = Icons.Outlined.Refresh,
                  contentDescription = "Reset to base value",
                  tint = OledTextSecondary,
                  modifier = Modifier.size(18.dp),
                )
              }
              IconButton(
                onClick = { editingBaseVar = selectedVar },
                modifier = Modifier.size(36.dp),
              ) {
                Icon(
                  imageVector = Icons.Outlined.Edit,
                  contentDescription = "Edit base value",
                  tint = OledTextSecondary,
                  modifier = Modifier.size(18.dp),
                )
              }
              IconButton(
                onClick = { onDeleteBase(selectedVar) },
                modifier = Modifier.size(36.dp),
              ) {
                Icon(
                  imageVector = Icons.Outlined.DeleteOutline,
                  contentDescription = "Delete base variable",
                  tint = AccentRose,
                  modifier = Modifier.size(18.dp),
                )
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "CURRENT VALUE",
                color = OledTextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
              )
              Text(
                text = MathEngine.formatNumber(
                  selectedVar.currentValue,
                  ws.decimalPlaces,
                  ws.trimTrailingZeros,
                ),
                color = accent,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = JetBrainsMonoFontFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
              Text(
                text = "BASE VALUE",
                color = OledTextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
              )
              Text(
                text = MathEngine.formatNumber(
                  selectedVar.baseValue,
                  ws.decimalPlaces,
                  ws.trimTrailingZeros,
                ),
                color = OledTextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = JetBrainsMonoFontFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
      }

      // Calculation Display
      Column(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.End,
      ) {
        MathExpressionView(
          expression = ws.compareExpression,
          showFractionFormat = ws.showFractionFormat,
          variableColors = varColors,
          maxFontSize = 32.sp,
          minFontSize = 10.sp,
          placeholderText = if (compareVars.isNotEmpty()) "Calc & tap base" else "0",
          modifier = Modifier.heightIn(min = 44.dp, max = 96.dp),
        )
        Spacer(modifier = Modifier.height(4.dp))
        when (livePreview) {
          is EvalResult.Success -> {
            val opHint = if (activeOp == CompareOp.ADD) "+" else "−"
            val formatted = MathEngine.formatNumber(livePreview.value, ws.decimalPlaces, ws.trimTrailingZeros)
            Text(
              text = "= $formatted  (tap base to $opHint)",
              color = if (activeOp == CompareOp.ADD) AccentEmerald else AccentRose,
              fontSize = 16.sp,
              fontWeight = FontWeight.SemiBold,
              fontFamily = JetBrainsMonoFontFamily,
              maxLines = 1,
            )
          }
          else -> {
            uiState.compareFeedback?.let { fb ->
              val opSign = if (fb.op == CompareOp.ADD) "+" else "−"
              val color = if (fb.op == CompareOp.ADD) AccentEmerald else AccentRose
              Text(
                text = "${fb.varName}: $opSign${MathEngine.formatNumber(fb.delta, ws.decimalPlaces, ws.trimTrailingZeros)} → ${MathEngine.formatNumber(fb.currentValue, ws.decimalPlaces, ws.trimTrailingZeros)}",
                color = color,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = JetBrainsMonoFontFamily,
                maxLines = 1,
              )
            }
          }
        }
      }
    }

    // Base Variables Buttons Row below display
    LazyRow(
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 8.dp, bottom = 6.dp)
        .testTag("compare_bases_row"),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      contentPadding = PaddingValues(horizontal = 2.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      items(compareVars, key = { it.id }) { variable ->
        val isSelected = variable.id == selectedVar?.id
        CompareBaseChip(
          variable = variable,
          isSelected = isSelected,
          formattedCurrent = MathEngine.formatNumber(
            variable.currentValue,
            ws.decimalPlaces,
            ws.trimTrailingZeros,
          ),
          formattedBase = MathEngine.formatNumber(
            variable.baseValue,
            ws.decimalPlaces,
            ws.trimTrailingZeros,
          ),
          hapticEnabled = ws.hapticFeedback,
          onClick = { onBaseClicked(variable) },
          onLongClick = { editingBaseVar = variable },
        )
      }
      item {
        Row(
          modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(OledSurfaceHigh)
            .clickable {
              prefillBaseName = ""
              showAddBaseDialog = true
            }
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("add_base_chip"),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.Outlined.Add,
            contentDescription = "Add base variable",
            tint = AccentCyan,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "Base",
            color = AccentCyan,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
          )
        }
      }
    }

    // Two Large Buttons: ADD and SUBTRACT
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      CompareActionButton(
        label = "ADD",
        icon = Icons.Outlined.Add,
        selected = activeOp == CompareOp.ADD,
        accentColor = AccentEmerald,
        tag = "compare_add_button",
        modifier = Modifier.weight(1f),
        onClick = { onSetActiveOp(CompareOp.ADD) },
      )
      CompareActionButton(
        label = "SUBTRACT",
        icon = Icons.Outlined.Remove,
        selected = activeOp == CompareOp.SUBTRACT,
        accentColor = AccentRose,
        tag = "compare_subtract_button",
        modifier = Modifier.weight(1f),
        onClick = { onSetActiveOp(CompareOp.SUBTRACT) },
      )
    }

    // Compact Keypad
    CalculatorKeypad(
      onInput = onInput,
      onDelete = onDelete,
      onClearDisplay = onClearDisplay,
      onEquals = onEquals,
      ansEnabled = false,
      hapticEnabled = ws.hapticFeedback,
      soundEnabled = ws.keypressSound,
      keyHeight = 48.dp,
      utilityHeight = 40.dp,
    )
  }

  if (showAddBaseDialog) {
    AddOrEditBaseDialog(
      title = "Add Base Variable",
      initialName = prefillBaseName,
      initialValue = "",
      nameEditable = true,
      onDismiss = { showAddBaseDialog = false },
      onConfirm = { name, value ->
        onAddBaseVariable(name, value)
        showAddBaseDialog = false
      },
    )
  }

  editingBaseVar?.let { target ->
    AddOrEditBaseDialog(
      title = "Edit Base '${target.name}'",
      initialName = target.name,
      initialValue = MathEngine.formatNumber(target.baseValue, ws.decimalPlaces, true),
      nameEditable = false,
      onDismiss = { editingBaseVar = null },
      onConfirm = { _, value ->
        onUpdateBaseValue(target, value)
        editingBaseVar = null
      },
    )
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CompareBaseChip(
  variable: CompareVariableEntity,
  isSelected: Boolean,
  formattedCurrent: String,
  formattedBase: String,
  hapticEnabled: Boolean,
  onClick: () -> Unit,
  onLongClick: () -> Unit,
) {
  val accent = variableColorAt(variable.colorIndex)
  val haptic = LocalHapticFeedback.current
  val shape = RoundedCornerShape(14.dp)
  val bg = if (isSelected) accent.copy(alpha = 0.26f) else accent.copy(alpha = 0.12f)

  Row(
    modifier = Modifier
      .heightIn(min = 48.dp)
      .clip(shape)
      .background(bg, shape)
      .combinedClickable(
        onClick = {
          if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          onClick()
        },
        onLongClick = {
          if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
          onLongClick()
        },
      )
      .padding(horizontal = 12.dp, vertical = 6.dp)
      .testTag("compare_base_chip_${variable.name}"),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Box(
      modifier = Modifier
        .clip(RoundedCornerShape(8.dp))
        .background(accent)
        .padding(horizontal = 7.dp, vertical = 2.dp),
    ) {
      Text(
        text = variable.name,
        color = OledBlack,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = JetBrainsMonoFontFamily,
      )
    }
    Column {
      Text(
        text = formattedCurrent,
        color = OledTextPrimary,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = JetBrainsMonoFontFamily,
      )
      Text(
        text = "base $formattedBase",
        color = OledTextMuted,
        fontSize = 10.sp,
        fontFamily = JetBrainsMonoFontFamily,
      )
    }
  }
}

@Composable
private fun CompareActionButton(
  label: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  selected: Boolean,
  accentColor: Color,
  tag: String,
  modifier: Modifier = Modifier,
  onClick: () -> Unit,
) {
  val bg = if (selected) accentColor else accentColor.copy(alpha = 0.14f)
  val fg = if (selected) OledBlack else accentColor
  val shape = RoundedCornerShape(16.dp)

  Row(
    modifier = modifier
      .height(50.dp)
      .clip(shape)
      .background(bg, shape)
      .clickable(onClick = onClick)
      .testTag(tag),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = label,
      tint = fg,
      modifier = Modifier.size(20.dp),
    )
    Spacer(modifier = Modifier.width(6.dp))
    Text(
      text = label,
      color = fg,
      fontSize = 15.sp,
      fontWeight = FontWeight.Bold,
    )
  }
}

@Composable
private fun AddOrEditBaseDialog(
  title: String,
  initialName: String,
  initialValue: String,
  nameEditable: Boolean,
  onDismiss: () -> Unit,
  onConfirm: (String, String) -> Unit,
) {
  var name by remember { mutableStateOf(initialName) }
  var value by remember { mutableStateOf(initialValue) }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = OledSurfaceElevated,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text(
          text = title,
          color = OledTextPrimary,
          style = MaterialTheme.typography.headlineMedium,
        )
        if (nameEditable) {
          OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Variable name (e.g. a, b, budget)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("base_name_input"),
          )
        }
        OutlinedTextField(
          value = value,
          onValueChange = { value = it },
          label = { Text("Base value (e.g. 500 or 100*5)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("base_value_input"),
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
        ) {
          TextButton(onClick = onDismiss) {
            Text("Cancel")
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = { onConfirm(name, value) },
            colors = ButtonDefaults.buttonColors(
              containerColor = AccentCyan,
              contentColor = OledBlack,
            ),
            modifier = Modifier.testTag("confirm_base_button"),
          ) {
            Text("Save", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}
