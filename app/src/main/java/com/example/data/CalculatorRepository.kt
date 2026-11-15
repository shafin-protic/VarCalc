package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CalculatorRepository(private val dao: CalculatorDao) {

  val workspaceState: Flow<WorkspaceStateEntity> =
    dao.observeWorkspaceState().map { it ?: WorkspaceStateEntity() }

  val regularVariables: Flow<List<RegularVariableEntity>> =
    dao.observeRegularVariables()

  val historySessions: Flow<List<HistorySessionEntity>> =
    dao.observeHistorySessions()

  val functionPresets: Flow<List<FunctionPresetEntity>> =
    dao.observeFunctionPresets()

  val compareVariables: Flow<List<CompareVariableEntity>> =
    dao.observeCompareVariables()

  suspend fun ensureInitialState(): WorkspaceStateEntity {
    val existing = dao.getWorkspaceState()
    if (existing != null) return existing
    val initial = WorkspaceStateEntity()
    dao.upsertWorkspaceState(initial)
    return initial
  }

  suspend fun saveWorkspaceState(state: WorkspaceStateEntity) {
    dao.upsertWorkspaceState(state)
  }

  suspend fun addRegularVariable(variable: RegularVariableEntity): Long {
    return dao.insertRegularVariable(variable)
  }

  suspend fun deleteRegularVariable(id: Long) {
    dao.deleteRegularVariable(id)
  }

  suspend fun archiveAndClearRegularSession(
    variables: List<RegularVariableEntity>,
    ansValue: Double,
    currentState: WorkspaceStateEntity,
  ) {
    val session = if (variables.isNotEmpty()) {
      val items = variables.map {
        HistoryItem(
          name = it.name,
          expression = it.expression,
          value = it.value,
          isSecondary = it.isSecondary,
          colorIndex = it.colorIndex,
        )
      }
      HistorySessionEntity(
        timestamp = System.currentTimeMillis(),
        itemsJson = JsonCodec.encodeHistoryItems(items),
        ansValue = ansValue,
      )
    } else {
      null
    }
    val clearedState = currentState.copy(
      regularExpression = "",
      regularAns = 0.0,
      hasRegularAns = false,
    )
    dao.archiveAndClearRegularSession(session, clearedState)
  }

  suspend fun restoreSession(session: HistorySessionEntity, currentState: WorkspaceStateEntity) {
    val items = JsonCodec.decodeHistoryItems(session.itemsJson)
    val now = System.currentTimeMillis()
    val restoredVars = items.mapIndexed { idx, item ->
      RegularVariableEntity(
        name = item.name,
        expression = item.expression,
        value = item.value,
        isSecondary = item.isSecondary,
        colorIndex = item.colorIndex,
        createdAt = now + idx,
      )
    }
    val hasAns = items.isNotEmpty()
    val restoredState = currentState.copy(
      activeMode = CalculatorMode.REGULAR.name,
      regularExpression = "",
      regularAns = session.ansValue,
      hasRegularAns = hasAns,
    )
    dao.restoreRegularSession(restoredVars, restoredState)
  }

  suspend fun deleteHistorySession(id: Long) {
    dao.deleteHistorySession(id)
  }

  suspend fun clearAllHistory() {
    dao.clearAllHistory()
  }

  suspend fun saveFunctionPreset(preset: FunctionPresetEntity): Long {
    return dao.upsertFunctionPreset(preset)
  }

  suspend fun deleteFunctionPreset(id: Long) {
    dao.deleteFunctionPreset(id)
  }

  suspend fun addCompareVariable(variable: CompareVariableEntity): Long {
    return dao.insertCompareVariable(variable)
  }

  suspend fun updateCompareVariable(variable: CompareVariableEntity) {
    dao.updateCompareVariable(variable)
  }

  suspend fun deleteCompareVariable(id: Long) {
    dao.deleteCompareVariable(id)
  }
}
