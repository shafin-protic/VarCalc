package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject

enum class CalculatorMode {
  REGULAR,
  FUNCTION,
  COMPARE,
}

enum class CompareOp {
  ADD,
  SUBTRACT,
}

data class HistoryItem(
  val name: String,
  val expression: String,
  val value: Double,
  val isSecondary: Boolean,
  val colorIndex: Int,
)

data class CompareLogItem(
  val op: CompareOp,
  val delta: Double,
  val expression: String,
  val resultingValue: Double,
  val timestamp: Long,
)

@Entity(tableName = "regular_variables")
data class RegularVariableEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val name: String,
  val expression: String,
  val value: Double,
  val isSecondary: Boolean,
  val colorIndex: Int,
  val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "history_sessions")
data class HistorySessionEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val timestamp: Long = System.currentTimeMillis(),
  val itemsJson: String,
  val ansValue: Double,
)

@Entity(tableName = "function_presets")
data class FunctionPresetEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val name: String,
  val inputVarsJson: String,
  val outputVarsJson: String,
  val formulasJson: String,
  val lastInputsJson: String,
  val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "compare_variables")
data class CompareVariableEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val name: String,
  val baseValue: Double,
  val currentValue: Double,
  val colorIndex: Int,
  val logsJson: String = "[]",
  val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "workspace_state")
data class WorkspaceStateEntity(
  @PrimaryKey val id: Int = 1,
  val activeMode: String = CalculatorMode.REGULAR.name,
  val regularExpression: String = "",
  val regularAns: Double = 0.0,
  val hasRegularAns: Boolean = false,
  val compareExpression: String = "",
  val compareActiveOp: String = CompareOp.ADD.name,
  val selectedCompareVarId: Long? = null,
  val activeFunctionPresetId: Long? = null,
  val decimalPlaces: Int = 2,
  val showFractionFormat: Boolean = true,
  val trimTrailingZeros: Boolean = false,
  val smartPercentage: Boolean = true,
  val hapticFeedback: Boolean = true,
  val keypressSound: Boolean = true,
  val keepScreenAwake: Boolean = false,
)

object JsonCodec {

  fun encodeHistoryItems(items: List<HistoryItem>): String {
    val arr = JSONArray()
    items.forEach { item ->
      arr.put(
        JSONObject().apply {
          put("name", item.name)
          put("expression", item.expression)
          put("value", item.value)
          put("isSecondary", item.isSecondary)
          put("colorIndex", item.colorIndex)
        }
      )
    }
    return arr.toString()
  }

  fun decodeHistoryItems(json: String): List<HistoryItem> {
    return runCatching {
      val arr = JSONArray(json)
      List(arr.length()) { idx ->
        val obj = arr.getJSONObject(idx)
        HistoryItem(
          name = obj.optString("name", ""),
          expression = obj.optString("expression", ""),
          value = obj.optDouble("value", 0.0),
          isSecondary = obj.optBoolean("isSecondary", false),
          colorIndex = obj.optInt("colorIndex", idx),
        )
      }
    }.getOrDefault(emptyList())
  }

  fun encodeStringList(list: List<String>): String {
    val arr = JSONArray()
    list.forEach { arr.put(it) }
    return arr.toString()
  }

  fun decodeStringList(json: String): List<String> {
    return runCatching {
      val arr = JSONArray(json)
      List(arr.length()) { idx -> arr.optString(idx, "") }.filter { it.isNotEmpty() }
    }.getOrDefault(emptyList())
  }

  fun encodeStringMap(map: Map<String, String>): String {
    val obj = JSONObject()
    map.forEach { (k, v) -> obj.put(k, v) }
    return obj.toString()
  }

  fun decodeStringMap(json: String): Map<String, String> {
    return runCatching {
      val obj = JSONObject(json)
      val out = linkedMapOf<String, String>()
      val keys = obj.keys()
      while (keys.hasNext()) {
        val key = keys.next()
        out[key] = obj.optString(key, "")
      }
      out
    }.getOrDefault(emptyMap())
  }

  fun encodeCompareLogs(logs: List<CompareLogItem>): String {
    val arr = JSONArray()
    logs.forEach { log ->
      arr.put(
        JSONObject().apply {
          put("op", log.op.name)
          put("delta", log.delta)
          put("expression", log.expression)
          put("resultingValue", log.resultingValue)
          put("timestamp", log.timestamp)
        }
      )
    }
    return arr.toString()
  }

  fun decodeCompareLogs(json: String): List<CompareLogItem> {
    return runCatching {
      val arr = JSONArray(json)
      List(arr.length()) { idx ->
        val obj = arr.getJSONObject(idx)
        val opName = obj.optString("op", CompareOp.ADD.name)
        CompareLogItem(
          op = runCatching { CompareOp.valueOf(opName) }.getOrDefault(CompareOp.ADD),
          delta = obj.optDouble("delta", 0.0),
          expression = obj.optString("expression", ""),
          resultingValue = obj.optDouble("resultingValue", 0.0),
          timestamp = obj.optLong("timestamp", 0L),
        )
      }
    }.getOrDefault(emptyList())
  }
}
