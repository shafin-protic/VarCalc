package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BookmarkAdded
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Functions
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Save
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.FunctionPresetEntity
import com.example.engine.EvalResult
import com.example.engine.MathEngine
import com.example.ui.CalculatorUiState
import com.example.ui.components.CalculatorKeypad
import com.example.ui.components.MathExpressionView
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentRose
import com.example.ui.theme.AccentViolet
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.OledBlack
import com.example.ui.theme.OledSurface
import com.example.ui.theme.OledSurfaceElevated
import com.example.ui.theme.OledSurfaceHigh
import com.example.ui.theme.OledTextMuted
import com.example.ui.theme.OledTextPrimary
import com.example.ui.theme.OledTextSecondary
import com.example.ui.theme.variableColorAt

private val DefaultInputSuggestions = listOf("a", "b", "c", "d", "e", "f")
private val DefaultOutputSuggestions = listOf("x", "y", "z", "p", "q", "r", "s", "t", "u", "v", "w")

@Composable
fun FunctionModeScreen(
  uiState: CalculatorUiState,
  onToggleEditingMode: (Boolean) -> Unit,
  onNewFunction: () -> Unit,
  onLoadPreset: (FunctionPresetEntity) -> Unit,
  onDeletePreset: (Long) -> Unit,
  onSavePreset: (String?) -> Unit,
  onUpdatePresetName: (String) -> Unit,
  onToggleInputVar: (String) -> Unit,
  onAddCustomInputVar: (String) -> Unit,
  onToggleOutputVar: (String) -> Unit,
  onAddCustomOutputVar: (String) -> Unit,
  onSelectFormulaTarget: (String) -> Unit,
  onSetFormulaExpression: (String, String) -> Unit,
  onSelectInputTarget: (String) -> Unit,
  onKeypadInput: (String) -> Unit,
  onKeypadDelete: () -> Unit,
  onKeypadClear: () -> Unit,
  onKeypadEquals: () -> Unit,
  onClearAllInputs: () -> Unit,
  computeOutputs: () -> Pair<Map<String, Double>, Map<String, EvalResult>>,
  onShowToast: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val ws = uiState.workspace
  val fn = uiState.functionState
  val presets = uiState.functionPresets

  var showAddCustomVarForInput by remember { mutableStateOf<Boolean?>(null) }
  var showSavePresetDialog by remember { mutableStateOf(false) }
  var editingFormulaTextVar by remember { mutableStateOf<String?>(null) }

  val varColors = remember(fn.inputVars, fn.outputVars) {
    val map = mutableMapOf<String, Color>()
    fn.inputVars.forEachIndexed { idx, v -> map[v] = variableColorAt(idx) }
    fn.outputVars.forEachIndexed { idx, v -> map[v] = variableColorAt(idx + 3) }
    map
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 14.dp)
      .padding(bottom = 10.dp),
    verticalArrangement = Arrangement.SpaceBetween,
  ) {
    // Presets Strip + Builder/Calculate Toggle
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      LazyRow(
        modifier = Modifier.weight(1f),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(vertical = 2.dp),
      ) {
        item {
          Row(
            modifier = Modifier
              .heightIn(min = 38.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(AccentCyan.copy(alpha = 0.16f))
              .clickable { onNewFunction() }
              .padding(horizontal = 10.dp, vertical = 6.dp)
              .testTag("fn_new_preset_btn"),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Outlined.Add,
              contentDescription = "New function",
              tint = AccentCyan,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "New",
              color = AccentCyan,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
            )
          }
        }
        items(presets, key = { it.id }) { preset ->
          val active = preset.id == fn.presetId
          PresetPill(
            preset = preset,
            selected = active,
            onClick = { onLoadPreset(preset) },
          )
        }
      }

      // Switch Build vs Run
      Row(
        modifier = Modifier
          .clip(RoundedCornerShape(12.dp))
          .background(OledSurfaceElevated)
          .padding(3.dp),
      ) {
        ModeMiniChip(
          label = "Build",
          icon = Icons.Outlined.Functions,
          selected = fn.isEditingMode,
          onClick = { onToggleEditingMode(true) },
        )
        ModeMiniChip(
          label = "Run",
          icon = Icons.Outlined.PlayArrow,
          selected = !fn.isEditingMode,
          onClick = { onToggleEditingMode(false) },
        )
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Main Scrollable Workspace
    Column(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .clip(RoundedCornerShape(22.dp))
        .background(OledSurface)
        .verticalScroll(rememberScrollState())
        .padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      if (fn.isEditingMode) {
        FunctionBuilderContent(
          uiState = uiState,
          varColors = varColors,
          onToggleInputVar = onToggleInputVar,
          onRequestCustomInput = { showAddCustomVarForInput = true },
          onToggleOutputVar = onToggleOutputVar,
          onRequestCustomOutput = { showAddCustomVarForInput = false },
          onSelectFormulaTarget = onSelectFormulaTarget,
          onOpenKeyboardFormulaEdit = { editingFormulaTextVar = it },
          onPromptSavePreset = { showSavePresetDialog = true },
          onDeletePreset = onDeletePreset,
        )
      } else {
        FunctionRunnerContent(
          uiState = uiState,
          varColors = varColors,
          computeOutputs = computeOutputs,
          onSelectInputTarget = onSelectInputTarget,
          onClearAllInputs = onClearAllInputs,
          onSwitchToEdit = { onToggleEditingMode(true) },
          onPromptSavePreset = { showSavePresetDialog = true },
          onDeletePreset = onDeletePreset,
          onShowToast = onShowToast,
        )
      }
    }

    // Variable Quick-Insert Strip (when building formulas)
    AnimatedVisibility(visible = fn.isEditingMode && (fn.inputVars.isNotEmpty() || fn.outputVars.isNotEmpty())) {
      LazyRow(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        items(fn.inputVars) { v ->
          val c = varColors[v] ?: AccentEmerald
          QuickVarInsertChip(name = v, color = c, onClick = { onKeypadInput(v) })
        }
        items(fn.outputVars) { v ->
          if (v != fn.activeFormulaTarget) {
            val c = varColors[v] ?: AccentViolet
            QuickVarInsertChip(name = v, color = c, onClick = { onKeypadInput(v) })
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Compact Keypad
    CalculatorKeypad(
      onInput = onKeypadInput,
      onDelete = onKeypadDelete,
      onClearDisplay = onKeypadClear,
      onEquals = onKeypadEquals,
      ansEnabled = false,
      hapticEnabled = ws.hapticFeedback,
      soundEnabled = ws.keypressSound,
      equalsLabel = if (fn.isEditingMode) "→" else "Next",
      keyHeight = 46.dp,
      utilityHeight = 38.dp,
    )
  }

  showAddCustomVarForInput?.let { isInput ->
    AddCustomVariableDialog(
      isInput = isInput,
      onDismiss = { showAddCustomVarForInput = null },
      onAdd = { name ->
        if (isInput) onAddCustomInputVar(name) else onAddCustomOutputVar(name)
        showAddCustomVarForInput = null
      },
    )
  }

  if (showSavePresetDialog) {
    SavePresetDialog(
      initialName = fn.presetName,
      isUpdating = fn.presetId != null,
      onDismiss = { showSavePresetDialog = false },
      onSave = { name ->
        onUpdatePresetName(name)
        onSavePreset(name)
        showSavePresetDialog = false
      },
    )
  }

  editingFormulaTextVar?.let { outVar ->
    EditFormulaTextDialog(
      outputVar = outVar,
      initialExpr = fn.formulas[outVar].orEmpty(),
      onDismiss = { editingFormulaTextVar = null },
      onConfirm = { newExpr ->
        onSetFormulaExpression(outVar, newExpr)
        editingFormulaTextVar = null
      },
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FunctionBuilderContent(
  uiState: CalculatorUiState,
  varColors: Map<String, Color>,
  onToggleInputVar: (String) -> Unit,
  onRequestCustomInput: () -> Unit,
  onToggleOutputVar: (String) -> Unit,
  onRequestCustomOutput: () -> Unit,
  onSelectFormulaTarget: (String) -> Unit,
  onOpenKeyboardFormulaEdit: (String) -> Unit,
  onPromptSavePreset: () -> Unit,
  onDeletePreset: (Long) -> Unit,
) {
  val fn = uiState.functionState
  val ws = uiState.workspace

  val inputChoices = remember(fn.inputVars) {
    (DefaultInputSuggestions + fn.inputVars).distinct()
  }
  val outputChoices = remember(fn.outputVars) {
    (DefaultOutputSuggestions + fn.outputVars).distinct()
  }

  // Input Variables Selector
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "1. INPUT VARIABLES",
        color = OledTextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
      )
      if (fn.presetId != null) {
        TextButton(
          onClick = { onDeletePreset(fn.presetId) },
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
        ) {
          Icon(
            imageVector = Icons.Outlined.DeleteOutline,
            contentDescription = "Delete preset",
            tint = AccentRose,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text("Delete Preset", color = AccentRose, fontSize = 12.sp)
        }
      }
    }

    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      inputChoices.forEach { v ->
        val selected = v in fn.inputVars
        val color = varColors[v] ?: AccentEmerald
        SelectableVarChip(
          label = v,
          selected = selected,
          accentColor = color,
          onClick = { onToggleInputVar(v) },
        )
      }
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(10.dp))
          .background(OledSurfaceHigh)
          .clickable { onRequestCustomInput() }
          .padding(horizontal = 10.dp, vertical = 6.dp)
          .testTag("fn_add_custom_input"),
      ) {
        Text(
          text = "+ Custom",
          color = AccentCyan,
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold,
        )
      }
    }
  }

  // Output Variables Selector
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Text(
      text = "2. OUTPUT VARIABLES",
      color = OledTextSecondary,
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
    )
    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      outputChoices.forEach { v ->
        val selected = v in fn.outputVars
        val color = varColors[v] ?: AccentViolet
        SelectableVarChip(
          label = v,
          selected = selected,
          accentColor = color,
          onClick = { onToggleOutputVar(v) },
        )
      }
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(10.dp))
          .background(OledSurfaceHigh)
          .clickable { onRequestCustomOutput() }
          .padding(horizontal = 10.dp, vertical = 6.dp)
          .testTag("fn_add_custom_output"),
      ) {
        Text(
          text = "+ Custom",
          color = AccentViolet,
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold,
        )
      }
    }
  }

  // Formulas Editor
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Text(
      text = "3. FORMULAS  (e.g. x = (a+b)/2*5%, y = 5%*x)",
      color = OledTextSecondary,
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
    )

    if (fn.outputVars.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(14.dp))
          .background(OledSurfaceElevated)
          .padding(14.dp),
      ) {
        Text(
          text = "Select one or more output variables above (like x, y, z) to write formulas.",
          color = OledTextMuted,
          style = MaterialTheme.typography.bodyMedium,
        )
      }
    } else {
      fn.outputVars.forEach { outVar ->
        val isActive = outVar == fn.activeFormulaTarget
        val accent = varColors[outVar] ?: AccentViolet
        val expr = fn.formulas[outVar].orEmpty()

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isActive) accent.copy(alpha = 0.14f) else OledSurfaceElevated)
            .clickable { onSelectFormulaTarget(outVar) }
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("fn_formula_card_$outVar"),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(accent)
              .padding(horizontal = 8.dp, vertical = 4.dp),
          ) {
            Text(
              text = "$outVar =",
              color = OledBlack,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp,
            )
          }

          Box(modifier = Modifier.weight(1f)) {
            MathExpressionView(
              expression = expr,
              showFractionFormat = ws.showFractionFormat,
              variableColors = varColors,
              maxFontSize = 20.sp,
              minFontSize = 10.sp,
              alignEnd = false,
              placeholderText = "Tap to type with keypad",
              modifier = Modifier.heightIn(min = 28.dp, max = 64.dp),
            )
          }

          IconButton(
            onClick = { onOpenKeyboardFormulaEdit(outVar) },
            modifier = Modifier.size(36.dp),
          ) {
            Icon(
              imageVector = Icons.Outlined.Edit,
              contentDescription = "Edit formula text",
              tint = OledTextSecondary,
              modifier = Modifier.size(18.dp),
            )
          }
        }
      }
    }
  }

  // Save as Preset Suggestion Banner
  val hasValidFormula = fn.outputVars.isNotEmpty() && fn.formulas.values.any { it.isNotBlank() }
  AnimatedVisibility(visible = hasValidFormula && (fn.isDirty || fn.presetId == null)) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(14.dp))
        .background(AccentEmerald.copy(alpha = 0.13f))
        .padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = if (fn.presetId == null) "Save as a Preset?" else "Save preset changes?",
          color = AccentEmerald,
          fontWeight = FontWeight.Bold,
          fontSize = 13.sp,
        )
        Text(
          text = "Reuse these formulas anytime & calculate fast",
          color = OledTextSecondary,
          fontSize = 11.sp,
        )
      }
      Spacer(modifier = Modifier.width(8.dp))
      Button(
        onClick = onPromptSavePreset,
        colors = ButtonDefaults.buttonColors(
          containerColor = AccentEmerald,
          contentColor = OledBlack,
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        modifier = Modifier.testTag("fn_save_preset_banner_btn"),
      ) {
        Icon(
          imageVector = Icons.Outlined.Save,
          contentDescription = "Save preset",
          modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text("Save", fontWeight = FontWeight.Bold, fontSize = 12.sp)
      }
    }
  }
}

@Composable
private fun FunctionRunnerContent(
  uiState: CalculatorUiState,
  varColors: Map<String, Color>,
  computeOutputs: () -> Pair<Map<String, Double>, Map<String, EvalResult>>,
  onSelectInputTarget: (String) -> Unit,
  onClearAllInputs: () -> Unit,
  onSwitchToEdit: () -> Unit,
  onPromptSavePreset: () -> Unit,
  onDeletePreset: (Long) -> Unit,
  onShowToast: (String) -> Unit,
) {
  val fn = uiState.functionState
  val ws = uiState.workspace
  val clipboard = LocalClipboardManager.current
  val (_, results) = remember(fn.inputValues, fn.formulas, fn.outputVars, ws.smartPercentage) {
    computeOutputs()
  }

  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = fn.presetName.ifBlank { "Custom Function" },
        color = OledTextPrimary,
        style = MaterialTheme.typography.titleMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = "Enter input values below — outputs calculate automatically",
        color = OledTextMuted,
        fontSize = 11.sp,
      )
    }
    Row {
      TextButton(onClick = onClearAllInputs) {
        Text("Clear", color = OledTextSecondary, fontSize = 12.sp)
      }
      TextButton(onClick = onSwitchToEdit) {
        Text("Edit", color = AccentCyan, fontSize = 12.sp)
      }
      if (fn.presetId != null) {
        IconButton(
          onClick = { onDeletePreset(fn.presetId) },
          modifier = Modifier.size(36.dp),
        ) {
          Icon(
            imageVector = Icons.Outlined.DeleteOutline,
            contentDescription = "Delete preset",
            tint = AccentRose,
            modifier = Modifier.size(18.dp),
          )
        }
      }
    }
  }

  if (fn.presetId == null && fn.outputVars.isNotEmpty() && fn.formulas.values.any { it.isNotBlank() }) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(AccentEmerald.copy(alpha = 0.13f))
        .clickable { onPromptSavePreset() }
        .padding(horizontal = 12.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Outlined.BookmarkAdded,
          contentDescription = null,
          tint = AccentEmerald,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Tap to save this function as a preset",
          color = AccentEmerald,
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold,
        )
      }
      Text("Save", color = AccentEmerald, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
  }

  if (fn.inputVars.isEmpty()) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(14.dp))
        .background(OledSurfaceElevated)
        .padding(14.dp),
    ) {
      Text(
        text = "No input variables defined yet. Switch to Build to select inputs & formulas.",
        color = OledTextMuted,
        style = MaterialTheme.typography.bodyMedium,
      )
    }
  } else {
    Text(
      text = "INPUTS",
      color = OledTextSecondary,
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
    )
    fn.inputVars.forEach { inVar ->
      val isActive = inVar == fn.activeInputTarget
      val accent = varColors[inVar] ?: AccentEmerald
      val valExpr = fn.inputValues[inVar].orEmpty()

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(14.dp))
          .background(if (isActive) accent.copy(alpha = 0.16f) else OledSurfaceElevated)
          .clickable { onSelectInputTarget(inVar) }
          .padding(horizontal = 12.dp, vertical = 10.dp)
          .testTag("fn_input_field_$inVar"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(accent)
            .padding(horizontal = 9.dp, vertical = 4.dp),
        ) {
          Text(
            text = inVar,
            color = OledBlack,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
          )
        }
        Box(modifier = Modifier.weight(1f)) {
          MathExpressionView(
            expression = valExpr,
            showFractionFormat = ws.showFractionFormat,
            variableColors = varColors,
            maxFontSize = 20.sp,
            minFontSize = 11.sp,
            alignEnd = true,
            placeholderText = "Tap & enter value",
            modifier = Modifier.heightIn(min = 26.dp, max = 56.dp),
          )
        }
      }
    }
  }

  if (fn.outputVars.isNotEmpty()) {
    Text(
      text = "OUTPUTS",
      color = OledTextSecondary,
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
    )
    fn.outputVars.forEach { outVar ->
      val accent = varColors[outVar] ?: AccentViolet
      val formula = fn.formulas[outVar].orEmpty()
      val eval = results[outVar]

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(14.dp))
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
            modifier = Modifier.weight(1f),
          ) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(accent.copy(alpha = 0.22f))
                .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
              Text(
                text = outVar,
                color = accent,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
              )
            }
            Box(modifier = Modifier.weight(1f)) {
              MathExpressionView(
                expression = formula,
                showFractionFormat = ws.showFractionFormat,
                variableColors = varColors,
                maxFontSize = 14.sp,
                minFontSize = 9.sp,
                alignEnd = false,
                placeholderText = "No formula",
                modifier = Modifier.heightIn(min = 20.dp, max = 44.dp),
              )
            }
          }

          when (eval) {
            is EvalResult.Success -> {
              val formatted = MathEngine.formatNumber(
                eval.value,
                ws.decimalPlaces,
                ws.trimTrailingZeros,
              )
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .clickable {
                    clipboard.setText(AnnotatedString(formatted))
                    onShowToast("Copied $outVar = $formatted")
                  }
                  .padding(horizontal = 6.dp, vertical = 2.dp),
              ) {
                Text(
                  text = "= $formatted",
                  color = accent,
                  fontSize = 20.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = JetBrainsMonoFontFamily,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                  imageVector = Icons.Outlined.ContentCopy,
                  contentDescription = "Copy output",
                  tint = OledTextMuted,
                  modifier = Modifier.size(14.dp),
                )
              }
            }
            is EvalResult.Error -> {
              Text(
                text = eval.message,
                color = AccentAmber,
                fontSize = 11.sp,
                maxLines = 1,
              )
            }
            null -> {}
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PresetPill(
  preset: FunctionPresetEntity,
  selected: Boolean,
  onClick: () -> Unit,
) {
  val bg = if (selected) AccentCyan else OledSurfaceElevated
  val fg = if (selected) OledBlack else OledTextPrimary
  Row(
    modifier = Modifier
      .heightIn(min = 38.dp)
      .clip(RoundedCornerShape(12.dp))
      .background(bg)
      .combinedClickable(onClick = onClick)
      .padding(horizontal = 12.dp, vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = preset.name,
      color = fg,
      fontSize = 12.sp,
      fontWeight = FontWeight.SemiBold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun ModeMiniChip(
  label: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  selected: Boolean,
  onClick: () -> Unit,
) {
  Row(
    modifier = Modifier
      .clip(RoundedCornerShape(10.dp))
      .background(if (selected) AccentCyan else Color.Transparent)
      .clickable(onClick = onClick)
      .padding(horizontal = 10.dp, vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = label,
      tint = if (selected) OledBlack else OledTextSecondary,
      modifier = Modifier.size(14.dp),
    )
    Spacer(modifier = Modifier.width(4.dp))
    Text(
      text = label,
      color = if (selected) OledBlack else OledTextSecondary,
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
    )
  }
}

@Composable
private fun SelectableVarChip(
  label: String,
  selected: Boolean,
  accentColor: Color,
  onClick: () -> Unit,
) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(10.dp))
      .background(if (selected) accentColor else OledSurfaceHigh)
      .clickable(onClick = onClick)
      .padding(horizontal = 11.dp, vertical = 6.dp)
      .testTag("fn_var_chip_$label"),
  ) {
    Text(
      text = label,
      color = if (selected) OledBlack else OledTextPrimary,
      fontFamily = JetBrainsMonoFontFamily,
      fontWeight = FontWeight.Bold,
      fontSize = 13.sp,
    )
  }
}

@Composable
private fun QuickVarInsertChip(
  name: String,
  color: Color,
  onClick: () -> Unit,
) {
  Box(
    modifier = Modifier
      .heightIn(min = 36.dp)
      .clip(RoundedCornerShape(10.dp))
      .background(color.copy(alpha = 0.18f))
      .clickable(onClick = onClick)
      .padding(horizontal = 12.dp, vertical = 6.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = name,
      color = color,
      fontFamily = JetBrainsMonoFontFamily,
      fontWeight = FontWeight.Bold,
      fontSize = 13.sp,
    )
  }
}

@Composable
private fun AddCustomVariableDialog(
  isInput: Boolean,
  onDismiss: () -> Unit,
  onAdd: (String) -> Unit,
) {
  var name by remember { mutableStateOf("") }
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
          text = if (isInput) "Custom Input Variable" else "Custom Output Variable",
          color = OledTextPrimary,
          style = MaterialTheme.typography.headlineMedium,
        )
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("e.g. man, distance, money, tax") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
          TextButton(onClick = onDismiss) { Text("Cancel") }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = { onAdd(name) },
            colors = ButtonDefaults.buttonColors(
              containerColor = AccentCyan,
              contentColor = OledBlack,
            ),
          ) {
            Text("Add", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

@Composable
private fun SavePresetDialog(
  initialName: String,
  isUpdating: Boolean,
  onDismiss: () -> Unit,
  onSave: (String) -> Unit,
) {
  var name by remember { mutableStateOf(initialName) }
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
          text = if (isUpdating) "Update Function Preset" else "Save Function Preset",
          color = OledTextPrimary,
          style = MaterialTheme.typography.headlineMedium,
        )
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Preset name (e.g. Profit & Tax)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
          TextButton(onClick = onDismiss) { Text("Cancel") }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = { onSave(name) },
            colors = ButtonDefaults.buttonColors(
              containerColor = AccentEmerald,
              contentColor = OledBlack,
            ),
          ) {
            Text("Save", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

@Composable
private fun EditFormulaTextDialog(
  outputVar: String,
  initialExpr: String,
  onDismiss: () -> Unit,
  onConfirm: (String) -> Unit,
) {
  var expr by remember { mutableStateOf(initialExpr) }
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
          text = "Formula for $outputVar",
          color = OledTextPrimary,
          style = MaterialTheme.typography.headlineMedium,
        )
        OutlinedTextField(
          value = expr,
          onValueChange = { expr = it },
          label = { Text("e.g. (a+b)/2*5%") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
          TextButton(onClick = onDismiss) { Text("Cancel") }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = { onConfirm(expr) },
            colors = ButtonDefaults.buttonColors(
              containerColor = AccentCyan,
              contentColor = OledBlack,
            ),
          ) {
            Text("Apply", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}
