package com.example

import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.CalculatorMode
import com.example.ui.CalculatorUiState
import com.example.ui.CalculatorViewModel
import com.example.ui.components.LocalSoundEffectPlayer
import com.example.ui.screens.CompareModeScreen
import com.example.ui.screens.FunctionModeScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.RegularModeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.OledBlack
import com.example.ui.theme.OledSurfaceElevated
import com.example.ui.theme.OledSurfaceHigh
import com.example.ui.theme.OledTextPrimary
import com.example.ui.theme.OledTextSecondary
import com.example.ui.theme.VarCalcTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(Color.BLACK),
      navigationBarStyle = SystemBarStyle.dark(Color.BLACK),
    )
    setContent {
      val viewModel: CalculatorViewModel = viewModel()
      val uiState by viewModel.uiState.collectAsStateWithLifecycle()

      DisposableEffect(uiState.workspace.keepScreenAwake) {
        if (uiState.workspace.keepScreenAwake) {
          window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
          window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {}
      }

      CompositionLocalProvider(LocalSoundEffectPlayer provides viewModel.soundPlayer) {
        VarCalcTheme {
          VarCalcRoot(
            uiState = uiState,
            viewModel = viewModel,
          )
        }
      }
    }
  }
}

@Composable
fun VarCalcRoot(
  uiState: CalculatorUiState,
  viewModel: CalculatorViewModel,
) {
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()

  LaunchedEffect(Unit) {
    viewModel.startDeferredLoading()
  }

  LaunchedEffect(uiState.statusMessage) {
    val msg = uiState.statusMessage ?: return@LaunchedEffect
    snackbarHostState.showSnackbar(msg)
    viewModel.clearStatusMessage()
  }

  val showToast: (String) -> Unit = remember(scope, snackbarHostState) {
    { msg ->
      scope.launch {
        snackbarHostState.showSnackbar(msg)
      }
    }
  }

  Scaffold(
    containerColor = OledBlack,
    contentWindowInsets = WindowInsets.safeDrawing,
    snackbarHost = {
      SnackbarHost(hostState = snackbarHostState) { data ->
        Snackbar(
          snackbarData = data,
          containerColor = OledSurfaceHigh,
          contentColor = OledTextPrimary,
          shape = RoundedCornerShape(14.dp),
        )
      }
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(OledBlack)
        .padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .widthIn(max = 600.dp),
      ) {
        when {
          uiState.showHistoryScreen -> {
            HistoryScreen(
              sessions = uiState.historySessions,
              decimalPlaces = uiState.workspace.decimalPlaces,
              trimTrailingZeros = uiState.workspace.trimTrailingZeros,
              showFractionFormat = uiState.workspace.showFractionFormat,
              onRestoreSession = viewModel::restoreHistorySession,
              onDeleteSession = viewModel::deleteHistorySession,
              onClearAllHistory = viewModel::clearAllHistory,
              onBack = { viewModel.setShowHistory(false) },
            )
          }
          uiState.showSettingsScreen -> {
            SettingsScreen(
              uiState = uiState,
              onUpdateDecimalPlaces = viewModel::updateDecimalPlaces,
              onUpdateShowFractionFormat = viewModel::updateShowFractionFormat,
              onUpdateTrimTrailingZeros = viewModel::updateTrimTrailingZeros,
              onUpdateSmartPercentage = viewModel::updateSmartPercentage,
              onUpdateHapticFeedback = viewModel::updateHapticFeedback,
              onUpdateKeypressSound = viewModel::updateKeypressSound,
              onUpdateKeepScreenAwake = viewModel::updateKeepScreenAwake,
              onBack = { viewModel.setShowSettings(false) },
            )
          }
          else -> {
            val activeMode = runCatching {
              CalculatorMode.valueOf(uiState.workspace.activeMode)
            }.getOrDefault(CalculatorMode.REGULAR)

            TopModeHeader(
              activeMode = activeMode,
              soundEnabled = uiState.workspace.keypressSound,
              onSelectMode = viewModel::selectMode,
              onOpenHistory = { viewModel.setShowHistory(true) },
              onOpenSettings = { viewModel.setShowSettings(true) },
            )

            Box(modifier = Modifier.weight(1f)) {
              when (activeMode) {
                CalculatorMode.REGULAR -> {
                  RegularModeScreen(
                    uiState = uiState,
                    onInput = viewModel::appendToRegular,
                    onDelete = viewModel::deleteFromRegular,
                    onClearDisplay = viewModel::clearRegularDisplay,
                    onLongPressAc = viewModel::longPressAcRegular,
                    onEquals = viewModel::evaluateRegularEquals,
                    onInsertVariable = viewModel::insertRegularVariable,
                    onReuseExpression = viewModel::reuseVariableExpression,
                    onDeleteVariable = viewModel::deleteRegularVariable,
                    onShowToast = showToast,
                  )
                }
                CalculatorMode.FUNCTION -> {
                  FunctionModeScreen(
                    uiState = uiState,
                    onToggleEditingMode = viewModel::toggleFunctionEditingMode,
                    onNewFunction = viewModel::startNewFunction,
                    onLoadPreset = viewModel::loadFunctionPreset,
                    onDeletePreset = viewModel::deleteFunctionPreset,
                    onSavePreset = viewModel::saveFunctionPreset,
                    onUpdatePresetName = viewModel::updateFunctionPresetName,
                    onToggleInputVar = viewModel::toggleFunctionInputVar,
                    onAddCustomInputVar = viewModel::addCustomFunctionInputVar,
                    onToggleOutputVar = viewModel::toggleFunctionOutputVar,
                    onAddCustomOutputVar = viewModel::addCustomFunctionOutputVar,
                    onSelectFormulaTarget = viewModel::selectFormulaTarget,
                    onSetFormulaExpression = viewModel::setFormulaExpression,
                    onSelectInputTarget = viewModel::selectInputTarget,
                    onKeypadInput = viewModel::appendToFunctionActiveField,
                    onKeypadDelete = viewModel::deleteFromFunctionActiveField,
                    onKeypadClear = viewModel::clearFunctionActiveField,
                    onKeypadEquals = viewModel::advanceFunctionRunnerField,
                    onClearAllInputs = viewModel::clearAllFunctionInputs,
                    computeOutputs = viewModel::computeFunctionOutputs,
                    onShowToast = showToast,
                  )
                }
                CalculatorMode.COMPARE -> {
                  CompareModeScreen(
                    uiState = uiState,
                    onSetActiveOp = viewModel::setCompareActiveOp,
                    onInput = viewModel::appendToCompare,
                    onDelete = viewModel::deleteFromCompare,
                    onClearDisplay = viewModel::clearCompareDisplay,
                    onEquals = viewModel::evaluateCompareEquals,
                    onAddBaseVariable = viewModel::addCompareBaseVariable,
                    onBaseClicked = viewModel::onCompareBaseClicked,
                    onUndoLastStep = viewModel::undoLastCompareStep,
                    onResetBase = viewModel::resetCompareBase,
                    onUpdateBaseValue = viewModel::updateCompareBaseValue,
                    onDeleteBase = viewModel::deleteCompareBase,
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun TopModeHeader(
  activeMode: CalculatorMode,
  soundEnabled: Boolean,
  onSelectMode: (CalculatorMode) -> Unit,
  onOpenHistory: () -> Unit,
  onOpenSettings: () -> Unit,
) {
  val soundPlayer = LocalSoundEffectPlayer.current

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 14.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    Row(
      modifier = Modifier
        .weight(1f)
        .clip(RoundedCornerShape(16.dp))
        .background(OledSurfaceElevated)
        .padding(4.dp),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      ModeTabPill(
        label = "Regular",
        selected = activeMode == CalculatorMode.REGULAR,
        tag = "mode_regular",
        modifier = Modifier.weight(1f),
        onClick = {
          soundPlayer?.playClick(soundEnabled)
          onSelectMode(CalculatorMode.REGULAR)
        },
      )
      ModeTabPill(
        label = "Function",
        selected = activeMode == CalculatorMode.FUNCTION,
        tag = "mode_function",
        modifier = Modifier.weight(1f),
        onClick = {
          soundPlayer?.playClick(soundEnabled)
          onSelectMode(CalculatorMode.FUNCTION)
        },
      )
      ModeTabPill(
        label = "Compare",
        selected = activeMode == CalculatorMode.COMPARE,
        tag = "mode_compare",
        modifier = Modifier.weight(1f),
        onClick = {
          soundPlayer?.playClick(soundEnabled)
          onSelectMode(CalculatorMode.COMPARE)
        },
      )
    }

    IconButton(
      onClick = {
        soundPlayer?.playClick(soundEnabled)
        onOpenHistory()
      },
      modifier = Modifier
        .size(48.dp)
        .testTag("open_history_btn"),
    ) {
      Icon(
        imageVector = Icons.Outlined.History,
        contentDescription = "Session History",
        tint = OledTextSecondary,
        modifier = Modifier.size(22.dp),
      )
    }

    IconButton(
      onClick = {
        soundPlayer?.playClick(soundEnabled)
        onOpenSettings()
      },
      modifier = Modifier
        .size(48.dp)
        .testTag("open_settings_btn"),
    ) {
      Icon(
        imageVector = Icons.Outlined.Settings,
        contentDescription = "Settings",
        tint = OledTextSecondary,
        modifier = Modifier.size(22.dp),
      )
    }
  }
}

@Composable
private fun ModeTabPill(
  label: String,
  selected: Boolean,
  tag: String,
  modifier: Modifier = Modifier,
  onClick: () -> Unit,
) {
  val bg = if (selected) AccentCyan else androidx.compose.ui.graphics.Color.Transparent
  val fg = if (selected) OledBlack else OledTextSecondary

  Box(
    modifier = modifier
      .heightIn(min = 38.dp)
      .clip(RoundedCornerShape(12.dp))
      .background(bg)
      .clickable(onClick = onClick)
      .padding(horizontal = 8.dp, vertical = 8.dp)
      .testTag(tag),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = label,
      color = fg,
      fontSize = 13.sp,
      fontWeight = FontWeight.Bold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}
