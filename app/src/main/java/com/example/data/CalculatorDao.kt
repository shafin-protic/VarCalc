package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CalculatorDao {

  @Query("SELECT * FROM workspace_state WHERE id = 1")
  fun observeWorkspaceState(): Flow<WorkspaceStateEntity?>

  @Query("SELECT * FROM workspace_state WHERE id = 1")
  suspend fun getWorkspaceState(): WorkspaceStateEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertWorkspaceState(state: WorkspaceStateEntity)

  @Query("SELECT * FROM regular_variables ORDER BY createdAt ASC, id ASC")
  fun observeRegularVariables(): Flow<List<RegularVariableEntity>>

  @Query("SELECT * FROM regular_variables ORDER BY createdAt ASC, id ASC")
  suspend fun getRegularVariablesOnce(): List<RegularVariableEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertRegularVariable(variable: RegularVariableEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertRegularVariables(variables: List<RegularVariableEntity>)

  @Query("DELETE FROM regular_variables WHERE id = :id")
  suspend fun deleteRegularVariable(id: Long)

  @Query("DELETE FROM regular_variables")
  suspend fun clearRegularVariables()

  @Query("SELECT * FROM history_sessions ORDER BY timestamp DESC, id DESC")
  fun observeHistorySessions(): Flow<List<HistorySessionEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertHistorySession(session: HistorySessionEntity): Long

  @Query("DELETE FROM history_sessions WHERE id = :id")
  suspend fun deleteHistorySession(id: Long)

  @Query("DELETE FROM history_sessions")
  suspend fun clearAllHistory()

  @Query("SELECT * FROM function_presets ORDER BY updatedAt DESC, id DESC")
  fun observeFunctionPresets(): Flow<List<FunctionPresetEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertFunctionPreset(preset: FunctionPresetEntity): Long

  @Query("DELETE FROM function_presets WHERE id = :id")
  suspend fun deleteFunctionPreset(id: Long)

  @Query("SELECT * FROM compare_variables ORDER BY createdAt ASC, id ASC")
  fun observeCompareVariables(): Flow<List<CompareVariableEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCompareVariable(variable: CompareVariableEntity): Long

  @Update
  suspend fun updateCompareVariable(variable: CompareVariableEntity)

  @Query("DELETE FROM compare_variables WHERE id = :id")
  suspend fun deleteCompareVariable(id: Long)

  @Query("DELETE FROM compare_variables")
  suspend fun clearCompareVariables()

  @Transaction
  suspend fun archiveAndClearRegularSession(
    session: HistorySessionEntity?,
    newState: WorkspaceStateEntity,
  ) {
    if (session != null) {
      insertHistorySession(session)
    }
    clearRegularVariables()
    upsertWorkspaceState(newState)
  }

  @Transaction
  suspend fun restoreRegularSession(
    variables: List<RegularVariableEntity>,
    newState: WorkspaceStateEntity,
  ) {
    clearRegularVariables()
    insertRegularVariables(variables)
    upsertWorkspaceState(newState)
  }
}
