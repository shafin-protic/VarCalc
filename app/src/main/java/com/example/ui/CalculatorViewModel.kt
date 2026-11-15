package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.CalculatorMode
import com.example.data.CalculatorRepository
import com.example.data.CompareLogItem
import com.example.data.CompareOp
import com.example.data.CompareVariableEntity
import com.example.data.FunctionPresetEntity
import com.example.data.HistorySessionEntity
import com.example.data.JsonCodec
import com.example.data.RegularVariableEntity
import com.example.data.WorkspaceStateEntity
import com.example.engine.EvalResult
import com.example.engine.FunctionFormula
import com.example.engine.MathEngine
import com.example.ui.components.SoundEffectPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class FunctionWorkspaceState(
  val presetId: Long? = null,
  val presetName: String = "",
  val inputVars: List<String> = emptyList(),
  val outputVars: List<String> = emptyList(),
  val formulas: Map<String, String> = emptyMap(),
  val inputValues: Map<String, String> = emptyMap(),
  val isEditingMode: Boolean = true,
  val activeFormulaTarget: String? = null,
  val activeInputTarget: String? = null,
  val isDirty: Boolean = false,
)

data class RegularBannerInfo(
  val varName: String,
  val expression: String,
  val value: Double,
)

data class CompareAppliedFeedback(
  val varName: String,
  val op: CompareOp,
  val delta: Double,
  val baseValue: Double,
  val currentValue: Double,
)

data class CalculatorUiState(
  val workspace: WorkspaceStateEntity = WorkspaceStateEntity(),
  val regularVariables: List<RegularVariableEntity> = emptyList(),
  val historySessions: List<HistorySessionEntity> = emptyList(),
  val functionPresets: List<FunctionPresetEntity> = emptyList(),
  val compareVariables: List<CompareVariableEntity> = emptyList(),
  val functionState: FunctionWorkspaceState = FunctionWorkspaceState(),
  val regularBanner: RegularBannerInfo? = null,
  val compareFeedback: CompareAppliedFeedback? = null,
  val statusMessage: String? = null,
  val showHistoryScreen: Boolean = false,
  val showSettingsScreen: Boolean = false,
)

class CalculatorViewModel(application: Application) : AndroidViewModel(application) {

  val soundPlayer = SoundEffectPlayer(application)

  private val fastPrefs = application.getSharedPreferences("varcalc_fast_prefs", Context.MODE_PRIVATE)
  private val repository by lazy {
    CalculatorRepository(AppDatabase.getInstance(application).calculatorDao())
  }

  // Loaded synchronously from SharedPreferences in <0.5ms so Frame 1 renders Regular view immediately.
  private val workspaceFlow = MutableStateFlow(loadFastWorkspaceSnapshot())
  private val regularVariablesFlow = MutableStateFlow(loadFastRegularVarsSnapshot())

  // Secondary mode flows — deferred until after Regular startup frame renders.
  private val historySessionsFlow = MutableStateFlow<List<HistorySessionEntity>>(emptyList())
  private val functionPresetsFlow = MutableStateFlow<List<FunctionPresetEntity>>(emptyList())
  private val compareVariablesFlow = MutableStateFlow<List<CompareVariableEntity>>(emptyList())

  private val functionState = MutableStateFlow(FunctionWorkspaceState())
  private val regularBanner = MutableStateFlow<RegularBannerInfo?>(null)
  private val compareFeedback = MutableStateFlow<CompareAppliedFeedback?>(null)
  private val statusMessage = MutableStateFlow<String?>(null)
  private val showHistoryScreen = MutableStateFlow(false)
  private val showSettingsScreen = MutableStateFlow(false)

  private var presetHydrated = false
  private var debouncedPersistJob: Job? = null
  private var primaryRoomStarted = false
  private var secondaryRoomStarted = false

  private val auxStateFlow = combine(
    functionState,
    regularBanner,
    compareFeedback,
    statusMessage,
    combine(showHistoryScreen, showSettingsScreen) { h, s -> h to s },
  ) { fn, banner, cmpFb, msg, (hist, settings) ->
    AuxTuple(fn, banner, cmpFb, msg, hist, settings)
  }

  val uiState: StateFlow<CalculatorUiState> = combine(
    workspaceFlow,
    regularVariablesFlow,
    historySessionsFlow,
    functionPresetsFlow,
    combine(compareVariablesFlow, auxStateFlow) { cmpVars, aux -> cmpVars to aux },
  ) { workspace, regVars, history, presets, (cmpVars, aux) ->
    CalculatorUiState(
      workspace = workspace,
      regularVariables = regVars,
      historySessions = history,
      functionPresets = presets,
      compareVariables = cmpVars,
      functionState = aux.fn,
      regularBanner = aux.banner,
      compareFeedback = aux.cmpFb,
      statusMessage = aux.msg,
      showHistoryScreen = aux.showHistory,
      showSettingsScreen = aux.showSettings,
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.Eagerly,
    initialValue = CalculatorUiState(
      workspace = workspaceFlow.value,
      regularVariables = regularVariablesFlow.value,
    ),
  )

  /**
   * Called after Frame 1 renders so Room SQLite never blocks cold start.
   */
  fun startDeferredLoading() {
    if (!primaryRoomStarted) {
      primaryRoomStarted = true
      viewModelScope.launch(Dispatchers.IO) {
        delay(80)
        val dbState = repository.ensureInitialState()
        val startupState = dbState.copy(activeMode = CalculatorMode.REGULAR.name)
        workspaceFlow.value = startupState
        saveFastWorkspaceSnapshot(startupState)

        repository.regularVariables.collect { list ->
          regularVariablesFlow.value = list
          saveFastRegularVarsSnapshot(list)
        }
      }
    }
    viewModelScope.launch(Dispatchers.IO) {
      delay(450)
      ensureSecondaryDataLoaded()
    }
  }

  fun ensureSecondaryDataLoaded() {
    if (secondaryRoomStarted) return
    secondaryRoomStarted = true
    viewModelScope.launch(Dispatchers.IO) {
      repository.functionPresets.collect { presets ->
        functionPresetsFlow.value = presets
        val activePresetId = workspaceFlow.value.activeFunctionPresetId
        if (!presetHydrated && presets.isNotEmpty() && activePresetId != null) {
          val saved = presets.firstOrNull { it.id == activePresetId }
          if (saved != null) {
            presetHydrated = true
            hydrateFunctionFromPreset(saved, editing = false)
          }
        }
      }
    }
    viewModelScope.launch(Dispatchers.IO) {
      repository.compareVariables.collect { list ->
        compareVariablesFlow.value = list
      }
    }
    viewModelScope.launch(Dispatchers.IO) {
      repository.historySessions.collect { list ->
        historySessionsFlow.value = list
      }
    }
  }

  override fun onCleared() {
    super.onCleared()
    debouncedPersistJob?.cancel()
    soundPlayer.release()
  }

  private data class AuxTuple(
    val fn: FunctionWorkspaceState,
    val banner: RegularBannerInfo?,
    val cmpFb: CompareAppliedFeedback?,
    val msg: String?,
    val showHistory: Boolean,
    val showSettings: Boolean,
  )

  private fun loadFastWorkspaceSnapshot(): WorkspaceStateEntity {
    return WorkspaceStateEntity(
      activeMode = CalculatorMode.REGULAR.name,
      regularExpression = fastPrefs.getString("regularExpression", "") ?: "",
      regularAns = Double.fromBits(fastPrefs.getLong("regularAnsBits", 0.0.toRawBits())),
      hasRegularAns = fastPrefs.getBoolean("hasRegularAns", false),
      compareExpression = fastPrefs.getString("compareExpression", "") ?: "",
      compareActiveOp = fastPrefs.getString("compareActiveOp", CompareOp.ADD.name) ?: CompareOp.ADD.name,
      decimalPlaces = fastPrefs.getInt("decimalPlaces", 2),
      showFractionFormat = fastPrefs.getBoolean("showFractionFormat", true),
      trimTrailingZeros = fastPrefs.getBoolean("trimTrailingZeros", false),
      smartPercentage = fastPrefs.getBoolean("smartPercentage", true),
      hapticFeedback = fastPrefs.getBoolean("hapticFeedback", true),
      keypressSound = fastPrefs.getBoolean("keypressSound", true),
      keepScreenAwake = fastPrefs.getBoolean("keepScreenAwake", false),
    )
  }

  private fun saveFastWorkspaceSnapshot(ws: WorkspaceStateEntity) {
    fastPrefs.edit()
      .putString("regularExpression", ws.regularExpression)
      .putLong("regularAnsBits", ws.regularAns.toRawBits())
      .putBoolean("hasRegularAns", ws.hasRegularAns)
      .putString("compareExpression", ws.compareExpression)
      .putString("compareActiveOp", ws.compareActiveOp)
      .putInt("decimalPlaces", ws.decimalPlaces)
      .putBoolean("showFractionFormat", ws.showFractionFormat)
      .putBoolean("trimTrailingZeros", ws.trimTrailingZeros)
      .putBoolean("smartPercentage", ws.smartPercentage)
      .putBoolean("hapticFeedback", ws.hapticFeedback)
      .putBoolean("keypressSound", ws.keypressSound)
      .putBoolean("keepScreenAwake", ws.keepScreenAwake)
      .apply()
  }

  private fun loadFastRegularVarsSnapshot(): List<RegularVariableEntity> {
    val raw = fastPrefs.getString("regularVarsJson", null) ?: return emptyList()
    return runCatching {
      val arr = JSONArray(raw)
      List(arr.length()) { idx ->
        val obj = arr.getJSONObject(idx)
        RegularVariableEntity(
          id = obj.optLong("id", idx.toLong() + 1L),
          name = obj.optString("name", ""),
          expression = obj.optString("expression", ""),
          value = obj.optDouble("value", 0.0),
          isSecondary = obj.optBoolean("isSecondary", false),
          colorIndex = obj.optInt("colorIndex", idx),
          createdAt = obj.optLong("createdAt", 0L),
        )
      }.filter { it.name.isNotEmpty() }
    }.getOrDefault(emptyList())
  }

  private fun saveFastRegularVarsSnapshot(vars: List<RegularVariableEntity>) {
    val arr = JSONArray()
    vars.forEach { v ->
      arr.put(
        JSONObject().apply {
          put("id", v.id)
          put("name", v.name)
          put("expression", v.expression)
          put("value", v.value)
          put("isSecondary", v.isSecondary)
          put("colorIndex", v.colorIndex)
          put("createdAt", v.createdAt)
        }
      )
    }
    fastPrefs.edit().putString("regularVarsJson", arr.toString()).apply()
  }

  private fun updateWorkspaceInstant(
    immediatePersist: Boolean = false,
    transform: (WorkspaceStateEntity) -> WorkspaceStateEntity,
  ) {
    val updated = transform(workspaceFlow.value)
    workspaceFlow.value = updated
    if (immediatePersist) {
      debouncedPersistJob?.cancel()
      saveFastWorkspaceSnapshot(updated)
      viewModelScope.launch(Dispatchers.IO) {
        repository.saveWorkspaceState(updated)
      }
    } else {
      debouncedPersistJob?.cancel()
      debouncedPersistJob = viewModelScope.launch(Dispatchers.IO) {
        delay(280)
        val latest = workspaceFlow.value
        saveFastWorkspaceSnapshot(latest)
        repository.saveWorkspaceState(latest)
      }
    }
  }

  fun clearStatusMessage() {
    statusMessage.value = null
  }

  fun setShowHistory(show: Boolean) {
    if (show) ensureSecondaryDataLoaded()
    showHistoryScreen.value = show
    if (show) showSettingsScreen.value = false
  }

  fun setShowSettings(show: Boolean) {
    if (show) ensureSecondaryDataLoaded()
    showSettingsScreen.value = show
    if (show) showHistoryScreen.value = false
  }

  fun selectMode(mode: CalculatorMode) {
    if (mode != CalculatorMode.REGULAR) {
      ensureSecondaryDataLoaded()
    }
    updateWorkspaceInstant(immediatePersist = true) { it.copy(activeMode = mode.name) }
  }

  // =========================================================================
  // REGULAR MODE
  // =========================================================================

  fun appendToRegular(token: String) {
    val ws = workspaceFlow.value
    val current = ws.regularExpression
    val nextExpr = smartAppendToken(
      current = current,
      token = token,
      hasAns = ws.hasRegularAns,
      isEmptyStartOperatorUsesAns = true,
    )
    if (current.isEmpty() && token !in listOf("+", "-", "*", "/")) {
      regularBanner.value = null
    }
    updateWorkspaceInstant(immediatePersist = false) { it.copy(regularExpression = nextExpr) }
  }

  fun insertRegularVariable(varName: String) {
    val current = workspaceFlow.value.regularExpression
    val prefix = if (current.lastOrNull()?.let { it.isLetter() || it == '_' } == true) "*" else ""
    regularBanner.value = null
    updateWorkspaceInstant(immediatePersist = false) { it.copy(regularExpression = current + prefix + varName) }
  }

  fun deleteFromRegular() {
    val current = workspaceFlow.value.regularExpression
    if (current.isEmpty()) {
      regularBanner.value = null
      return
    }
    updateWorkspaceInstant(immediatePersist = false) {
      it.copy(regularExpression = smartDeleteLastToken(current))
    }
  }

  fun clearRegularDisplay() {
    regularBanner.value = null
    updateWorkspaceInstant(immediatePersist = true) { it.copy(regularExpression = "") }
  }

  fun longPressAcRegular() {
    val vars = regularVariablesFlow.value
    val ws = workspaceFlow.value
    regularBanner.value = null
    val cleared = ws.copy(
      regularExpression = "",
      regularAns = 0.0,
      hasRegularAns = false,
    )
    workspaceFlow.value = cleared
    regularVariablesFlow.value = emptyList()
    debouncedPersistJob?.cancel()
    saveFastWorkspaceSnapshot(cleared)
    saveFastRegularVarsSnapshot(emptyList())
    viewModelScope.launch(Dispatchers.IO) {
      repository.archiveAndClearRegularSession(
        variables = vars,
        ansValue = ws.regularAns,
        currentState = ws,
      )
    }
    statusMessage.value = if (vars.isNotEmpty()) {
      "Cleared & saved ${vars.size} calculation${if (vars.size == 1) "" else "s"} to History"
    } else {
      "Session cleared"
    }
  }

  fun evaluateRegularEquals() {
    val ws = workspaceFlow.value
    val expr = ws.regularExpression.trim()
    if (expr.isEmpty()) return

    val vars = regularVariablesFlow.value
    val varMap = vars.associate { it.name to it.value }
    val ans = if (ws.hasRegularAns) ws.regularAns else null

    when (
      val res = MathEngine.evaluate(
        expression = expr,
        variables = varMap,
        ansValue = ans,
        smartPercentage = ws.smartPercentage,
      )
    ) {
      is EvalResult.Error -> {
        statusMessage.value = res.message
      }
      is EvalResult.Success -> {
        val usesSavedVariables = res.usedVariables.any { it in varMap }
        val nextVarName = allocateNextRegularVarName(
          existing = vars,
          isSecondary = usesSavedVariables,
        )
        val colorIdx = vars.size
        val entity = RegularVariableEntity(
          name = nextVarName,
          expression = expr,
          value = res.value,
          isSecondary = usesSavedVariables,
          colorIndex = colorIdx,
          createdAt = System.currentTimeMillis(),
        )
        regularBanner.value = RegularBannerInfo(
          varName = nextVarName,
          expression = expr,
          value = res.value,
        )
        val nextWs = ws.copy(
          regularExpression = "",
          regularAns = res.value,
          hasRegularAns = true,
        )
        workspaceFlow.value = nextWs
        val optimisticList = vars + entity.copy(id = -(System.nanoTime()))
        regularVariablesFlow.value = optimisticList
        debouncedPersistJob?.cancel()
        saveFastWorkspaceSnapshot(nextWs)
        saveFastRegularVarsSnapshot(optimisticList)
        viewModelScope.launch(Dispatchers.IO) {
          repository.addRegularVariable(entity)
          repository.saveWorkspaceState(nextWs)
        }
      }
    }
  }

  fun deleteRegularVariable(variable: RegularVariableEntity) {
    val remaining = regularVariablesFlow.value.filterNot { it.id == variable.id }
    regularVariablesFlow.value = remaining
    saveFastRegularVarsSnapshot(remaining)
    if (regularBanner.value?.varName == variable.name) {
      regularBanner.value = null
    }
    viewModelScope.launch(Dispatchers.IO) {
      repository.deleteRegularVariable(variable.id)
    }
    statusMessage.value = "Deleted variable ${variable.name}"
  }

  fun reuseVariableExpression(expression: String) {
    regularBanner.value = null
    updateWorkspaceInstant(immediatePersist = false) { it.copy(regularExpression = expression) }
  }

  private fun allocateNextRegularVarName(
    existing: List<RegularVariableEntity>,
    isSecondary: Boolean,
  ): String {
    val usedNames = existing.map { it.name }.toSet()
    if (isSecondary) {
      val pool = ('p'..'z').map { it.toString() }
      pool.firstOrNull { it !in usedNames }?.let { return it }
      var suffix = 1
      while (true) {
        for (ch in 'p'..'z') {
          val candidate = "$ch$suffix"
          if (candidate !in usedNames) return candidate
        }
        suffix++
      }
    } else {
      val pool = ('A'..'Z').map { it.toString() }
      pool.firstOrNull { it !in usedNames }?.let { return it }
      var suffix = 1
      while (true) {
        for (ch in 'A'..'Z') {
          val candidate = "$ch$suffix"
          if (candidate !in usedNames) return candidate
        }
        suffix++
      }
    }
  }

  // =========================================================================
  // HISTORY
  // =========================================================================

  fun restoreHistorySession(session: HistorySessionEntity) {
    val ws = workspaceFlow.value
    regularBanner.value = null
    showHistoryScreen.value = false
    val restoredState = ws.copy(
      activeMode = CalculatorMode.REGULAR.name,
      regularExpression = "",
      regularAns = session.ansValue,
      hasRegularAns = session.itemsJson != "[]",
    )
    workspaceFlow.value = restoredState
    saveFastWorkspaceSnapshot(restoredState)
    viewModelScope.launch(Dispatchers.IO) {
      repository.restoreSession(session, ws)
    }
    statusMessage.value = "Session restored to Regular Mode"
  }

  fun deleteHistorySession(id: Long) {
    viewModelScope.launch(Dispatchers.IO) {
      repository.deleteHistorySession(id)
    }
  }

  fun clearAllHistory() {
    viewModelScope.launch(Dispatchers.IO) {
      repository.clearAllHistory()
    }
    statusMessage.value = "History cleared"
  }

  // =========================================================================
  // FUNCTION MODE
  // =========================================================================

  fun toggleFunctionEditingMode(isEditing: Boolean) {
    functionState.update { state ->
      val firstInput = state.inputVars.firstOrNull()
      val firstOutput = state.outputVars.firstOrNull()
      state.copy(
        isEditingMode = isEditing,
        activeFormulaTarget = if (isEditing) state.activeFormulaTarget ?: firstOutput else state.activeFormulaTarget,
        activeInputTarget = if (!isEditing) state.activeInputTarget ?: firstInput else state.activeInputTarget,
      )
    }
  }

  fun startNewFunction() {
    functionState.value = FunctionWorkspaceState(
      isEditingMode = true,
      isDirty = false,
    )
    updateWorkspaceInstant(immediatePersist = true) { it.copy(activeFunctionPresetId = null) }
  }

  fun loadFunctionPreset(preset: FunctionPresetEntity) {
    hydrateFunctionFromPreset(preset, editing = false)
    updateWorkspaceInstant(immediatePersist = true) { it.copy(activeFunctionPresetId = preset.id) }
  }

  private fun hydrateFunctionFromPreset(preset: FunctionPresetEntity, editing: Boolean) {
    val inputs = JsonCodec.decodeStringList(preset.inputVarsJson)
    val outputs = JsonCodec.decodeStringList(preset.outputVarsJson)
    val formulas = JsonCodec.decodeStringMap(preset.formulasJson)
    val lastInputs = JsonCodec.decodeStringMap(preset.lastInputsJson)
    functionState.value = FunctionWorkspaceState(
      presetId = preset.id,
      presetName = preset.name,
      inputVars = inputs,
      outputVars = outputs,
      formulas = formulas,
      inputValues = lastInputs,
      isEditingMode = editing,
      activeFormulaTarget = outputs.firstOrNull(),
      activeInputTarget = inputs.firstOrNull(),
      isDirty = false,
    )
  }

  fun updateFunctionPresetName(name: String) {
    functionState.update { it.copy(presetName = name, isDirty = true) }
  }

  fun toggleFunctionInputVar(rawName: String) {
    val clean = sanitizeVarName(rawName)
    if (clean.isEmpty()) return
    if (clean.equals("ANS", ignoreCase = true)) {
      statusMessage.value = "ANS is reserved"
      return
    }
    functionState.update { st ->
      val exists = clean in st.inputVars
      if (exists) {
        val nextInputs = st.inputVars - clean
        val nextInputVals = st.inputValues - clean
        st.copy(
          inputVars = nextInputs,
          inputValues = nextInputVals,
          activeInputTarget = if (st.activeInputTarget == clean) nextInputs.firstOrNull() else st.activeInputTarget,
          isDirty = true,
        )
      } else {
        if (clean in st.outputVars) {
          statusMessage.value = "'$clean' is already an output variable"
          return@update st
        }
        val nextInputs = st.inputVars + clean
        st.copy(
          inputVars = nextInputs,
          activeInputTarget = st.activeInputTarget ?: clean,
          isDirty = true,
        )
      }
    }
  }

  fun addCustomFunctionInputVar(rawName: String) {
    val clean = sanitizeVarName(rawName)
    if (clean.isEmpty()) return
    if (clean.equals("ANS", ignoreCase = true)) {
      statusMessage.value = "ANS is reserved"
      return
    }
    functionState.update { st ->
      if (clean in st.outputVars) {
        statusMessage.value = "'$clean' is already an output variable"
        return@update st
      }
      if (clean in st.inputVars) return@update st
      val nextInputs = st.inputVars + clean
      st.copy(
        inputVars = nextInputs,
        activeInputTarget = st.activeInputTarget ?: clean,
        isDirty = true,
      )
    }
  }

  fun toggleFunctionOutputVar(rawName: String) {
    val clean = sanitizeVarName(rawName)
    if (clean.isEmpty()) return
    if (clean.equals("ANS", ignoreCase = true)) {
      statusMessage.value = "ANS is reserved"
      return
    }
    functionState.update { st ->
      val exists = clean in st.outputVars
      if (exists) {
        val nextOutputs = st.outputVars - clean
        val nextFormulas = st.formulas - clean
        st.copy(
          outputVars = nextOutputs,
          formulas = nextFormulas,
          activeFormulaTarget = if (st.activeFormulaTarget == clean) nextOutputs.firstOrNull() else st.activeFormulaTarget,
          isDirty = true,
        )
      } else {
        if (clean in st.inputVars) {
          statusMessage.value = "'$clean' is already an input variable"
          return@update st
        }
        val nextOutputs = st.outputVars + clean
        st.copy(
          outputVars = nextOutputs,
          activeFormulaTarget = clean,
          isDirty = true,
        )
      }
    }
  }

  fun addCustomFunctionOutputVar(rawName: String) {
    val clean = sanitizeVarName(rawName)
    if (clean.isEmpty()) return
    if (clean.equals("ANS", ignoreCase = true)) {
      statusMessage.value = "ANS is reserved"
      return
    }
    functionState.update { st ->
      if (clean in st.inputVars) {
        statusMessage.value = "'$clean' is already an input variable"
        return@update st
      }
      if (clean in st.outputVars) {
        return@update st.copy(activeFormulaTarget = clean)
      }
      val nextOutputs = st.outputVars + clean
      st.copy(
        outputVars = nextOutputs,
        activeFormulaTarget = clean,
        isDirty = true,
      )
    }
  }

  fun selectFormulaTarget(outputVar: String) {
    functionState.update { it.copy(activeFormulaTarget = outputVar) }
  }

  fun setFormulaExpression(outputVar: String, expr: String) {
    functionState.update { st ->
      st.copy(
        formulas = st.formulas + (outputVar to expr),
        isDirty = true,
      )
    }
  }

  fun selectInputTarget(inputVar: String) {
    functionState.update { it.copy(activeInputTarget = inputVar) }
  }

  fun setInputVarValue(inputVar: String, valueExpr: String) {
    functionState.update { st ->
      st.copy(inputValues = st.inputValues + (inputVar to valueExpr))
    }
    persistCurrentPresetInputValues()
  }

  fun appendToFunctionActiveField(token: String) {
    val st = functionState.value
    if (st.isEditingMode) {
      val target = st.activeFormulaTarget ?: st.outputVars.firstOrNull() ?: return
      val curr = st.formulas[target].orEmpty()
      val next = smartAppendToken(curr, token, hasAns = false, isEmptyStartOperatorUsesAns = false)
      setFormulaExpression(target, next)
    } else {
      val target = st.activeInputTarget ?: st.inputVars.firstOrNull() ?: return
      val curr = st.inputValues[target].orEmpty()
      val next = smartAppendToken(curr, token, hasAns = false, isEmptyStartOperatorUsesAns = false)
      setInputVarValue(target, next)
    }
  }

  fun deleteFromFunctionActiveField() {
    val st = functionState.value
    if (st.isEditingMode) {
      val target = st.activeFormulaTarget ?: return
      val curr = st.formulas[target].orEmpty()
      setFormulaExpression(target, smartDeleteLastToken(curr))
    } else {
      val target = st.activeInputTarget ?: return
      val curr = st.inputValues[target].orEmpty()
      setInputVarValue(target, smartDeleteLastToken(curr))
    }
  }

  fun clearFunctionActiveField() {
    val st = functionState.value
    if (st.isEditingMode) {
      val target = st.activeFormulaTarget ?: return
      setFormulaExpression(target, "")
    } else {
      val target = st.activeInputTarget ?: return
      setInputVarValue(target, "")
    }
  }

  fun clearAllFunctionInputs() {
    functionState.update { it.copy(inputValues = emptyMap()) }
    persistCurrentPresetInputValues()
  }

  fun advanceFunctionRunnerField() {
    val st = functionState.value
    if (st.isEditingMode) {
      val idx = st.outputVars.indexOf(st.activeFormulaTarget)
      if (idx >= 0 && idx + 1 < st.outputVars.size) {
        functionState.update { it.copy(activeFormulaTarget = st.outputVars[idx + 1]) }
      } else if (st.outputVars.isNotEmpty() && st.formulas.values.any { it.isNotBlank() }) {
        toggleFunctionEditingMode(false)
      }
    } else {
      val idx = st.inputVars.indexOf(st.activeInputTarget)
      if (idx >= 0 && idx + 1 < st.inputVars.size) {
        functionState.update { it.copy(activeInputTarget = st.inputVars[idx + 1]) }
      }
    }
  }

  fun saveFunctionPreset(customName: String? = null) {
    val st = functionState.value
    if (st.outputVars.isEmpty()) {
      statusMessage.value = "Select at least one output variable"
      return
    }
    val hasFormula = st.outputVars.any { !st.formulas[it].isNullOrBlank() }
    if (!hasFormula) {
      statusMessage.value = "Write at least one formula first"
      return
    }
    val effectiveName = (customName ?: st.presetName).trim().ifEmpty {
      st.outputVars.joinToString(", ") { o -> "$o = ${st.formulas[o].orEmpty()}" }.take(28)
    }
    val entity = FunctionPresetEntity(
      id = st.presetId ?: 0L,
      name = effectiveName,
      inputVarsJson = JsonCodec.encodeStringList(st.inputVars),
      outputVarsJson = JsonCodec.encodeStringList(st.outputVars),
      formulasJson = JsonCodec.encodeStringMap(st.formulas),
      lastInputsJson = JsonCodec.encodeStringMap(st.inputValues),
      updatedAt = System.currentTimeMillis(),
    )
    viewModelScope.launch(Dispatchers.IO) {
      val savedId = repository.saveFunctionPreset(entity)
      updateWorkspaceInstant(immediatePersist = true) { it.copy(activeFunctionPresetId = savedId) }
      functionState.update {
        it.copy(
          presetId = savedId,
          presetName = effectiveName,
          isDirty = false,
          isEditingMode = false,
          activeInputTarget = it.activeInputTarget ?: it.inputVars.firstOrNull(),
        )
      }
    }
    statusMessage.value = "Saved preset \"$effectiveName\""
  }

  fun deleteFunctionPreset(presetId: Long) {
    val currentId = functionState.value.presetId
    viewModelScope.launch(Dispatchers.IO) {
      repository.deleteFunctionPreset(presetId)
      if (currentId == presetId) {
        updateWorkspaceInstant(immediatePersist = true) { it.copy(activeFunctionPresetId = null) }
        functionState.value = FunctionWorkspaceState(isEditingMode = true)
      }
    }
    statusMessage.value = "Preset deleted"
  }

  private fun persistCurrentPresetInputValues() {
    val st = functionState.value
    val presetId = st.presetId ?: return
    val existing = functionPresetsFlow.value.firstOrNull { it.id == presetId } ?: return
    viewModelScope.launch(Dispatchers.IO) {
      repository.saveFunctionPreset(
        existing.copy(
          lastInputsJson = JsonCodec.encodeStringMap(st.inputValues),
          updatedAt = System.currentTimeMillis(),
        )
      )
    }
  }

  fun computeFunctionOutputs(): Pair<Map<String, Double>, Map<String, EvalResult>> {
    val st = functionState.value
    val ws = workspaceFlow.value
    val evaluatedInputs = linkedMapOf<String, Double>()
    for (inVar in st.inputVars) {
      val raw = st.inputValues[inVar]?.trim().orEmpty()
      if (raw.isNotEmpty()) {
        val res = MathEngine.evaluate(
          expression = raw,
          variables = emptyMap(),
          ansValue = null,
          smartPercentage = ws.smartPercentage,
        )
        if (res is EvalResult.Success) {
          evaluatedInputs[inVar] = res.value
        }
      }
    }
    val formulas = st.outputVars.map { outVar ->
      FunctionFormula(outputVar = outVar, expression = st.formulas[outVar].orEmpty())
    }
    val outputs = MathEngine.evaluateFunctions(
      formulas = formulas,
      inputs = evaluatedInputs,
      smartPercentage = ws.smartPercentage,
    )
    return evaluatedInputs to outputs
  }

  // =========================================================================
  // COMPARE MODE
  // =========================================================================

  fun setCompareActiveOp(op: CompareOp) {
    updateWorkspaceInstant(immediatePersist = true) { it.copy(compareActiveOp = op.name) }
  }

  fun appendToCompare(token: String) {
    val ws = workspaceFlow.value
    val next = smartAppendToken(
      current = ws.compareExpression,
      token = token,
      hasAns = false,
      isEmptyStartOperatorUsesAns = false,
    )
    updateWorkspaceInstant(immediatePersist = false) { it.copy(compareExpression = next) }
  }

  fun deleteFromCompare() {
    val curr = workspaceFlow.value.compareExpression
    if (curr.isEmpty()) return
    updateWorkspaceInstant(immediatePersist = false) {
      it.copy(compareExpression = smartDeleteLastToken(curr))
    }
  }

  fun clearCompareDisplay() {
    compareFeedback.value = null
    updateWorkspaceInstant(immediatePersist = true) { it.copy(compareExpression = "") }
  }

  fun evaluateCompareEquals() {
    val ws = workspaceFlow.value
    val expr = ws.compareExpression.trim()
    if (expr.isEmpty()) return
    val baseMap = compareVariablesFlow.value.associate { it.name to it.currentValue }
    when (
      val res = MathEngine.evaluate(
        expression = expr,
        variables = baseMap,
        ansValue = null,
        smartPercentage = ws.smartPercentage,
      )
    ) {
      is EvalResult.Error -> statusMessage.value = res.message
      is EvalResult.Success -> {
        val formatted = MathEngine.formatNumber(res.value, ws.decimalPlaces, ws.trimTrailingZeros)
        updateWorkspaceInstant(immediatePersist = true) { it.copy(compareExpression = formatted) }
      }
    }
  }

  fun addCompareBaseVariable(rawName: String, valueExpr: String) {
    val name = sanitizeVarName(rawName)
    if (name.isEmpty()) {
      statusMessage.value = "Enter a valid variable name"
      return
    }
    val ws = workspaceFlow.value
    val existing = compareVariablesFlow.value
    if (existing.any { it.name == name }) {
      statusMessage.value = "Base variable '$name' already exists"
      return
    }
    val res = MathEngine.evaluate(
      expression = valueExpr.ifBlank { "0" },
      variables = existing.associate { it.name to it.currentValue },
      smartPercentage = ws.smartPercentage,
    )
    if (res is EvalResult.Error) {
      statusMessage.value = res.message
      return
    }
    val value = (res as EvalResult.Success).value
    val entity = CompareVariableEntity(
      name = name,
      baseValue = value,
      currentValue = value,
      colorIndex = existing.size,
      logsJson = "[]",
      createdAt = System.currentTimeMillis(),
    )
    viewModelScope.launch(Dispatchers.IO) {
      val id = repository.addCompareVariable(entity)
      updateWorkspaceInstant(immediatePersist = true) { it.copy(selectedCompareVarId = id) }
    }
  }

  fun onCompareBaseClicked(variable: CompareVariableEntity) {
    val ws = workspaceFlow.value
    val expr = ws.compareExpression.trim()

    if (expr.isEmpty()) {
      compareFeedback.value = null
      updateWorkspaceInstant(immediatePersist = true) { it.copy(selectedCompareVarId = variable.id) }
      return
    }

    val baseMap = compareVariablesFlow.value.associate { it.name to it.currentValue }
    when (
      val res = MathEngine.evaluate(
        expression = expr,
        variables = baseMap,
        smartPercentage = ws.smartPercentage,
      )
    ) {
      is EvalResult.Error -> statusMessage.value = res.message
      is EvalResult.Success -> {
        val op = runCatching { CompareOp.valueOf(ws.compareActiveOp) }
          .getOrDefault(CompareOp.ADD)
        val delta = res.value
        val newCurrent = if (op == CompareOp.ADD) {
          variable.currentValue + delta
        } else {
          variable.currentValue - delta
        }
        val oldLogs = JsonCodec.decodeCompareLogs(variable.logsJson)
        val newLog = CompareLogItem(
          op = op,
          delta = delta,
          expression = expr,
          resultingValue = newCurrent,
          timestamp = System.currentTimeMillis(),
        )
        val updated = variable.copy(
          currentValue = newCurrent,
          logsJson = JsonCodec.encodeCompareLogs((listOf(newLog) + oldLogs).take(30)),
        )
        compareFeedback.value = CompareAppliedFeedback(
          varName = variable.name,
          op = op,
          delta = delta,
          baseValue = variable.baseValue,
          currentValue = newCurrent,
        )
        updateWorkspaceInstant(immediatePersist = true) {
          it.copy(compareExpression = "", selectedCompareVarId = variable.id)
        }
        viewModelScope.launch(Dispatchers.IO) {
          repository.updateCompareVariable(updated)
        }
      }
    }
  }

  fun undoLastCompareStep(variable: CompareVariableEntity) {
    val logs = JsonCodec.decodeCompareLogs(variable.logsJson)
    if (logs.isEmpty()) return
    val latest = logs.first()
    val restored = if (latest.op == CompareOp.ADD) {
      variable.currentValue - latest.delta
    } else {
      variable.currentValue + latest.delta
    }
    val updated = variable.copy(
      currentValue = restored,
      logsJson = JsonCodec.encodeCompareLogs(logs.drop(1)),
    )
    viewModelScope.launch(Dispatchers.IO) {
      repository.updateCompareVariable(updated)
    }
  }

  fun resetCompareBase(variable: CompareVariableEntity) {
    val updated = variable.copy(
      currentValue = variable.baseValue,
      logsJson = "[]",
    )
    compareFeedback.value = null
    viewModelScope.launch(Dispatchers.IO) {
      repository.updateCompareVariable(updated)
    }
    statusMessage.value = "Reset ${variable.name} to base value"
  }

  fun updateCompareBaseValue(variable: CompareVariableEntity, newBaseExpr: String) {
    val ws = workspaceFlow.value
    val res = MathEngine.evaluate(
      expression = newBaseExpr,
      variables = emptyMap(),
      smartPercentage = ws.smartPercentage,
    )
    if (res is EvalResult.Error) {
      statusMessage.value = res.message
      return
    }
    val newBase = (res as EvalResult.Success).value
    val diffFromOldBase = variable.currentValue - variable.baseValue
    val updated = variable.copy(
      baseValue = newBase,
      currentValue = newBase + diffFromOldBase,
    )
    viewModelScope.launch(Dispatchers.IO) {
      repository.updateCompareVariable(updated)
    }
  }

  fun deleteCompareBase(variable: CompareVariableEntity) {
    val ws = workspaceFlow.value
    if (ws.selectedCompareVarId == variable.id) {
      updateWorkspaceInstant(immediatePersist = true) { it.copy(selectedCompareVarId = null) }
    }
    viewModelScope.launch(Dispatchers.IO) {
      repository.deleteCompareVariable(variable.id)
    }
  }

  // =========================================================================
  // SETTINGS
  // =========================================================================

  fun updateDecimalPlaces(places: Int) {
    updateWorkspaceInstant(immediatePersist = true) { it.copy(decimalPlaces = places.coerceIn(0, 8)) }
  }

  fun updateShowFractionFormat(enabled: Boolean) {
    updateWorkspaceInstant(immediatePersist = true) { it.copy(showFractionFormat = enabled) }
  }

  fun updateTrimTrailingZeros(enabled: Boolean) {
    updateWorkspaceInstant(immediatePersist = true) { it.copy(trimTrailingZeros = enabled) }
  }

  fun updateSmartPercentage(enabled: Boolean) {
    updateWorkspaceInstant(immediatePersist = true) { it.copy(smartPercentage = enabled) }
  }

  fun updateHapticFeedback(enabled: Boolean) {
    updateWorkspaceInstant(immediatePersist = true) { it.copy(hapticFeedback = enabled) }
  }

  fun updateKeypressSound(enabled: Boolean) {
    updateWorkspaceInstant(immediatePersist = true) { it.copy(keypressSound = enabled) }
  }

  fun updateKeepScreenAwake(enabled: Boolean) {
    updateWorkspaceInstant(immediatePersist = true) { it.copy(keepScreenAwake = enabled) }
  }

  // =========================================================================
  // HELPERS
  // =========================================================================

  private fun sanitizeVarName(raw: String): String {
    return raw.trim().filter { it.isLetterOrDigit() || it == '_' }
      .let { if (it.isNotEmpty() && it.first().isDigit()) "_$it" else it }
  }

  private fun smartAppendToken(
    current: String,
    token: String,
    hasAns: Boolean,
    isEmptyStartOperatorUsesAns: Boolean,
  ): String {
    if (current.isEmpty() && isEmptyStartOperatorUsesAns && hasAns &&
      token in listOf("+", "-", "*", "/", "%")
    ) {
      return "ANS$token"
    }
    if (token.firstOrNull()?.isLetter() == true &&
      current.lastOrNull()?.let { it.isLetter() || it == '_' } == true
    ) {
      return "$current*$token"
    }
    if (token == ".") {
      val lastChunk = current.takeLastWhile { it.isDigit() || it == '.' }
      if ('.' in lastChunk) return current
      if (lastChunk.isEmpty()) return "${current}0."
    }
    if (token in listOf("+", "*", "/") && current.isEmpty()) {
      return current
    }
    if (token in listOf("+", "-", "*", "/") && current.isNotEmpty()) {
      val last = current.last()
      if (last in listOf('+', '-', '*', '/')) {
        if (token == "-" && last in listOf('*', '/', '(')) {
          return "$current-"
        }
        return current.dropLast(1) + token
      }
    }
    return current + token
  }

  private fun smartDeleteLastToken(current: String): String {
    if (current.isEmpty()) return ""
    val last = current.last()
    if (last.isLetter() || last == '_') {
      return current.dropLastWhile { it.isLetterOrDigit() || it == '_' }
    }
    return current.dropLast(1)
  }
}
